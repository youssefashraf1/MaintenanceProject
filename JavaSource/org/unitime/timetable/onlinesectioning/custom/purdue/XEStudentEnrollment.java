/*
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * The Apereo Foundation licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
*/
package org.unitime.timetable.onlinesectioning.custom.purdue;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.MediaType;
import org.restlet.data.Protocol;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import org.unitime.localization.impl.Localization;
import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.defaults.ApplicationProperty;
import org.unitime.timetable.gwt.resources.StudentSectioningMessages;
import org.unitime.timetable.gwt.shared.OnlineSectioningInterface.EligibilityCheck;
import org.unitime.timetable.gwt.shared.OnlineSectioningInterface.GradeMode;
import org.unitime.timetable.gwt.shared.OnlineSectioningInterface.GradeModes;
import org.unitime.timetable.gwt.shared.OnlineSectioningInterface.EligibilityCheck.EligibilityFlag;
import org.unitime.timetable.gwt.shared.SectioningException;
import org.unitime.timetable.gwt.shared.ClassAssignmentInterface.ErrorMessage;
import org.unitime.timetable.onlinesectioning.AcademicSessionInfo;
import org.unitime.timetable.onlinesectioning.OnlineSectioningHelper;
import org.unitime.timetable.onlinesectioning.OnlineSectioningLog;
import org.unitime.timetable.onlinesectioning.OnlineSectioningServer;
import org.unitime.timetable.onlinesectioning.custom.ExternalTermProvider;
import org.unitime.timetable.onlinesectioning.custom.StudentEnrollmentProvider;
import org.unitime.timetable.onlinesectioning.custom.purdue.XEInterface.CourseReferenceNumber;
import org.unitime.timetable.onlinesectioning.custom.purdue.XEInterface.RegisterAction;
import org.unitime.timetable.onlinesectioning.model.XConfig;
import org.unitime.timetable.onlinesectioning.model.XCourse;
import org.unitime.timetable.onlinesectioning.model.XCourseId;
import org.unitime.timetable.onlinesectioning.model.XCourseRequest;
import org.unitime.timetable.onlinesectioning.model.XEnrollment;
import org.unitime.timetable.onlinesectioning.model.XOffering;
import org.unitime.timetable.onlinesectioning.model.XRequest;
import org.unitime.timetable.onlinesectioning.model.XSection;
import org.unitime.timetable.onlinesectioning.model.XStudent;
import org.unitime.timetable.onlinesectioning.model.XSubpart;
import org.unitime.timetable.onlinesectioning.solver.SectioningRequest;
import org.unitime.timetable.util.Formats;
import org.unitime.timetable.util.Formats.Format;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class XEStudentEnrollment implements StudentEnrollmentProvider {
	private static Log sLog = LogFactory.getLog(XEStudentEnrollment.class);
	private static StudentSectioningMessages MESSAGES = Localization.create(StudentSectioningMessages.class);
	protected static Format<Number> sCreditFormat = Formats.getNumberFormat("0.##");
	
	private Client iClient;
	private ExternalTermProvider iExternalTermProvider;
	
	public XEStudentEnrollment() {
		List<Protocol> protocols = new ArrayList<Protocol>();
		protocols.add(Protocol.HTTP);
		protocols.add(Protocol.HTTPS);
		iClient = new Client(protocols);
		Context cx = new Context();
		cx.getParameters().add("readTimeout", getBannerReadTimeout());
		iClient.setContext(cx);
		try {
			String clazz = ApplicationProperty.CustomizationExternalTerm.value();
			if (clazz == null || clazz.isEmpty())
				iExternalTermProvider = new BannerTermProvider();
			else
				iExternalTermProvider = (ExternalTermProvider)Class.forName(clazz).getConstructor().newInstance();
		} catch (Exception e) {
			sLog.error("Failed to create external term provider, using the default one instead.", e);
			iExternalTermProvider = new BannerTermProvider();
		}
	}
	
	public static enum ConditionalDropType {
		/** do not use conditional add drop */
		NEVER,
		/** only use conditional add drop when there is a drop action */
		HAS_DROP,
		/** use conditional add drop when the student is already registered in at least one section */
		IS_REGISTERED,
		/** only use conditional add drop during re-sectioning (automated wait-listing) */
		RESECTION,
		/** always use conditional add drop */
		ALWAYS,
		;
		
		public static ConditionalDropType parseType(String value) {
			if (value == null) return ConditionalDropType.NEVER;
			try {
				return ConditionalDropType.valueOf(value);
			} catch (Exception e) {
				return ("true".equalsIgnoreCase(value) ? ConditionalDropType.HAS_DROP : ConditionalDropType.NEVER);
			}
		}
	}
	
	protected String getBannerReadTimeout() {
		return ApplicationProperties.getProperty("banner.xe.readTimeout", "60000");
	}
	
	protected String getBannerSite() {
		return ApplicationProperties.getProperty("banner.xe.site");
	}
	
	protected String getBannerUser(boolean manager) {
		if (manager) {
			String user = ApplicationProperties.getProperty("banner.xe.admin.user");
			if (user != null) return user;
		}
		return ApplicationProperties.getProperty("banner.xe.user");
	}
	
	protected String getBannerPassword(boolean manager) {
		if (manager) {
			String pwd = ApplicationProperties.getProperty("banner.xe.admin.password");
			if (pwd != null) return pwd;
		}
		return ApplicationProperties.getProperty("banner.xe.password");
	}
	
	protected String getBannerUser(OnlineSectioningHelper helper) {
		if (helper.isAdmin()) {
			String user = ApplicationProperties.getProperty("banner.xe.admin.user");
			if (user != null) return user;
		}
		return ApplicationProperties.getProperty("banner.xe.user");
	}
	
	protected String getBannerPassword(OnlineSectioningHelper helper) {
		if (helper.isAdmin()) {
			String pwd = ApplicationProperties.getProperty("banner.xe.admin.password");
			if (pwd != null) return pwd;
		}
		return ApplicationProperties.getProperty("banner.xe.password");
	}
	
	protected String getBannerRecheck() {
		return ApplicationProperties.getProperty("banner.xe.recheck");
	}
	
	protected boolean isBannerAdmin() {
		return "true".equalsIgnoreCase(ApplicationProperties.getProperty("banner.xe.admin", "false"));	
	}
	
	protected boolean isBannerAdmin(OnlineSectioningHelper helper) {
		if (helper.isAdmin() && isBannerAdmin()) {
			if (helper.hasAdminPermission())
				return true;
			else if (helper.hasAvisorPermission())
				return "true".equalsIgnoreCase(ApplicationProperties.getProperty("banner.xe.advisorIsAdmin", "false"));
		}
		return false;
	}
	
	protected boolean isBannerWaitlist() {
		return "true".equalsIgnoreCase(ApplicationProperties.getProperty("banner.xe.waitlist", "false"));
	}
	
	protected float getMaxCredit(boolean admin) {
		if (admin) {
			String maxCredit = ApplicationProperties.getProperty("banner.xe.admin.maxCredit");
			if (maxCredit != null) return Float.parseFloat(maxCredit);
		}
		return Float.parseFloat(ApplicationProperties.getProperty("banner.xe.maxCredit", "-1"));
	}

	protected boolean isCheckMaxHours(boolean admin) {
		if (admin) {
			String checkMaxHours = ApplicationProperties.getProperty("banner.xe.admin.checkMaxHours");
			if (checkMaxHours != null) return "true".equalsIgnoreCase(checkMaxHours);
		}
		return "true".equalsIgnoreCase(ApplicationProperties.getProperty("banner.xe.checkMaxHours", "false"));
	}
	
	protected ConditionalDropType getConditionalAddDrop(boolean admin) {
		if (admin) {
			String conditionalAddDrop = ApplicationProperties.getProperty("banner.xe.admin.conditionalAddDrop");
			if (conditionalAddDrop != null) return ConditionalDropType.parseType(conditionalAddDrop);
		}
		return ConditionalDropType.parseType(ApplicationProperties.getProperty("banner.xe.conditionalAddDrop"));
	}
	
	protected boolean throwExceptionWhenNoChange(boolean admin) {
		if (admin) {
			String errorWhenNoChange = ApplicationProperties.getProperty("banner.xe.admin.errorWhenNoChange");
			if (errorWhenNoChange != null) return "true".equalsIgnoreCase(errorWhenNoChange);
		}
		return "true".equalsIgnoreCase(ApplicationProperties.getProperty("banner.xe.errorWhenNoChange", "false"));
	}

	protected float getMaxHoursDefault() {
		return Float.parseFloat(ApplicationProperties.getProperty("banner.xe.maxHoursDefault", "18"));
	}
	
	protected String getAdminParameter() {
		return ApplicationProperties.getProperty("banner.xe.adminParameter", "systemIn");
	}

	protected String getBannerId(XStudent student) {
		String id = student.getExternalId();
		while (id.length() < 9) id = "0" + id;
		return id;
	}
	
	protected String getBannerTerm(AcademicSessionInfo session) {
		return iExternalTermProvider.getExternalTerm(session);
	}
	
	protected String getBannerCampus(AcademicSessionInfo session) {
		return iExternalTermProvider.getExternalCampus(session);
	}
	
	protected boolean isPreserveGradeMode() {
		return "true".equalsIgnoreCase(ApplicationProperties.getProperty("banner.xe.keepGradeMode", "false"));
	}
	
	protected String getResetGradeModesRegExp() {
		return ApplicationProperties.getProperty("banner.xe.resetGradeModes", "H|Q|R");
	}
	
	protected Gson getGson(OnlineSectioningHelper helper) {
		GsonBuilder builder = new GsonBuilder()
		.registerTypeAdapter(DateTime.class, new JsonSerializer<DateTime>() {
			@Override
			public JsonElement serialize(DateTime src, Type typeOfSrc, JsonSerializationContext context) {
				return new JsonPrimitive(src.toString("yyyy-MM-dd'T'HH:mm:ss'Z'"));
			}
		})
		.registerTypeAdapter(DateTime.class, new JsonDeserializer<DateTime>() {
			@Override
			public DateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
				return new DateTime(json.getAsJsonPrimitive().getAsString(), DateTimeZone.UTC);
			}
		});
		if (helper.isDebugEnabled()) builder.setPrettyPrinting();
		return builder.create();
	}
	
	@Override
	public void checkEligibility(OnlineSectioningServer server, OnlineSectioningHelper helper, EligibilityCheck check, XStudent student) throws SectioningException {
		// Cannot enroll -> no additional check is needed (unless it is the case when UniTime does not know about the student)
		if (!check.hasFlag(EligibilityFlag.CAN_ENROLL) && student.getStudentId() != null) return;

		ClientResource resource = null;
		try {
			String pin = helper.getPin();
			if ((pin == null || pin.isEmpty()) && student.hasReleasedPin()) pin = student.getPin();
			AcademicSessionInfo session = server.getAcademicSession();
			String term = getBannerTerm(session);
			boolean admin = isBannerAdmin(helper);
			if (helper.isDebugEnabled())
				helper.debug("Checking eligility for " + student.getName() + " (term: " + term + ", id:" + getBannerId(student) + (admin ? ", admin" : pin != null ? ", pin:" + pin : "") + ")");
			
			// First, check student registration status
			resource = new ClientResource(getBannerSite());
			resource.setNext(iClient);
			resource.setChallengeResponse(ChallengeScheme.HTTP_BASIC, getBannerUser(helper), getBannerPassword(helper));
			Gson gson = getGson(helper);
			XEInterface.RegisterResponse original = null;

			resource.addQueryParameter("term", term);
			resource.addQueryParameter("bannerId", getBannerId(student));
			helper.getAction().addOptionBuilder().setKey("term").setValue(term);
			helper.getAction().addOptionBuilder().setKey("bannerId").setValue(getBannerId(student));
			if (admin) {
				String param = getAdminParameter();
				resource.addQueryParameter(param, "SB");
				helper.getAction().addOptionBuilder().setKey(param).setValue("SB");
			} else if (pin != null && !pin.isEmpty()) {
				resource.addQueryParameter("altPin", pin);
				helper.getAction().addOptionBuilder().setKey("pin").setValue(pin);
			}
			long t0 = System.currentTimeMillis();
			try {
				resource.get(MediaType.APPLICATION_JSON);
			} catch (ResourceException exception) {
				helper.getAction().setApiException(exception.getMessage());
				try {
					XEInterface.ErrorResponse response = new GsonRepresentation<XEInterface.ErrorResponse>(resource.getResponseEntity(), XEInterface.ErrorResponse.class).getObject();
					helper.getAction().addOptionBuilder().setKey("exception").setValue(gson.toJson(response));
					XEInterface.Error error = response.getError();
					if (error != null && error.message != null) {
						throw new SectioningException(error.message);
					} else if (error != null && error.description != null) {
						throw new SectioningException(error.description);
					} else if (error != null && error.errorMessage != null) {
						throw new SectioningException(error.errorMessage);
					} else {
						throw exception;
					}
				} catch (SectioningException e) {
					helper.getAction().setApiException(e.getMessage());
					throw e;
				} catch (Throwable t) {
					throw exception;
				}
			} finally {
				helper.getAction().setApiGetTime(System.currentTimeMillis() - t0);
			}
			List<XEInterface.RegisterResponse> current = new GsonRepresentation<List<XEInterface.RegisterResponse>>(resource.getResponseEntity(), XEInterface.RegisterResponse.TYPE_LIST).getObject();
			helper.getAction().addOptionBuilder().setKey("response").setValue(gson.toJson(current));
			if (current != null && !current.isEmpty())
				original = current.get(0);
			
			// Check status, memorize enrolled sections
			if (original != null && helper.isDebugEnabled())
				helper.debug("Current registration: " + gson.toJson(original));
			if (original != null && original.maxHours != null)
				check.setMaxCredit(original.maxHours);
			if (original == null || !original.validStudent) {
				String bannerRecheck = getBannerRecheck();
				String reason = null;
				boolean noreason = true;
				boolean recheck = true;
				if (original != null && original.failureReasons != null) {
					for (String m: original.failureReasons) {
						if (bannerRecheck == null || !m.matches(bannerRecheck)) recheck = false;
						noreason = false;
						if ("Your PIN is invalid.".equals(m)) {
							check.setFlag(EligibilityFlag.PIN_REQUIRED, true);
							if (pin == null || pin.isEmpty()) continue;
						}
						if (reason == null)
							reason = m;
						else
							reason += "<br>" + m;
					}
				}
				if (noreason) {
					reason = "Failed to check student registration eligility.";
					if (bannerRecheck == null || !reason.matches(bannerRecheck)) recheck = false;
				}
				if (recheck) {
					check.setFlag(EligibilityFlag.RECHECK_BEFORE_ENROLLMENT, true);
				} else {
					check.setFlag(EligibilityFlag.CAN_ENROLL, false);
				}
				check.setMessage(reason);
			} else if (student.getStudentId() == null) {
				boolean keepMessage = check.hasMessage() && (
						((Number)helper.getHibSession().createQuery(
								"select count(s) from Student s where s.externalUniqueId = :id and " +
								"s.session.academicYear = :year and s.session.academicTerm = :term and s.session.academicInitiative != :campus"
								).setString("id", student.getExternalId())
								.setString("term", server.getAcademicSession().getTerm())
								.setString("year", server.getAcademicSession().getYear())
								.setString("campus", server.getAcademicSession().getCampus())
								.uniqueResult()).intValue() > 0);
				if (!keepMessage)
					check.setMessage("UniTime enrollment data are not synchronized with Banner enrollment data, please try again later.");
				check.setFlag(EligibilityFlag.CAN_ENROLL, false);
				if (isCanRequestUpdates()) {
					List<XStudent> students = new ArrayList<XStudent>(1); students.add(student);
					requestUpdate(server, helper, students);
				}
			} else {
				// Check enrollments
				OnlineSectioningLog.Enrollment.Builder stored = OnlineSectioningLog.Enrollment.newBuilder();
				stored.setType(OnlineSectioningLog.Enrollment.EnrollmentType.STORED);
				Set<String> sectionExternalIds = new HashSet<String>();
				for (XRequest request: student.getRequests()) {
					helper.getAction().addRequest(OnlineSectioningHelper.toProto(request));
					if (request instanceof XCourseRequest) {
						XCourseRequest r = (XCourseRequest)request;
						XEnrollment e = r.getEnrollment();
						if (e == null) continue;
						XOffering offering = server.getOffering(e.getOfferingId());
						for (XSection section: offering.getSections(e)) {
							stored.addSection(OnlineSectioningHelper.toProto(section, e));
							String extId = section.getExternalId(e.getCourseId());
							if (extId != null)
								sectionExternalIds.add(extId);
						}
					}
				}
				helper.getAction().addEnrollment(stored);
				OnlineSectioningLog.Enrollment.Builder external = OnlineSectioningLog.Enrollment.newBuilder();
				external.setType(OnlineSectioningLog.Enrollment.EnrollmentType.EXTERNAL);
				String added = "";
				String resetGradeModes = getResetGradeModesRegExp();
				float currentCredit = 0f;
				if (original.registrations != null)
					for (XEInterface.Registration reg: original.registrations) {
						if (reg.isRegistered()) {
							if (!sectionExternalIds.remove(reg.courseReferenceNumber) && !eligibilityIgnoreBannerRegistration(server, helper, student, reg))
								added += (added.isEmpty() ? "" : ", ") + reg.courseReferenceNumber;
							OnlineSectioningLog.Section.Builder section = external.addSectionBuilder()
								.setClazz(OnlineSectioningLog.Entity.newBuilder().setName(reg.courseReferenceNumber))
								.setCourse(OnlineSectioningLog.Entity.newBuilder().setName(reg.subject + " " + reg.courseNumber))
								.setSubpart(OnlineSectioningLog.Entity.newBuilder().setName(reg.scheduleType));
							if (reg.registrationStatusDate != null)
								section.setTimeStamp(reg.registrationStatusDate.getMillis());
							if (reg.gradingMode != null && !reg.gradingMode.isEmpty())
								check.addGradeMode(reg.courseReferenceNumber, reg.gradingMode, reg.gradingModeDescription, resetGradeModes != null && !resetGradeModes.isEmpty() && reg.gradingMode.matches(resetGradeModes));
							if (reg.creditHour != null)
								check.addCreditHour(reg.courseReferenceNumber, reg.creditHour);
						}
						if (reg.hasCredit())
							currentCredit += reg.creditHour;
					}
				check.setCurrentCredit(currentCredit);
				helper.getAction().addEnrollment(external);
				String removed = "";
				for (String s: sectionExternalIds)
					removed += (removed.isEmpty() ? "" : ", ") + s;
				if (!added.isEmpty() || !removed.isEmpty()) {
					if (updateStudentRegistration(server, helper, student, original.registrations)) return;
					check.setMessage("UniTime enrollment data are not synchronized with Banner enrollment data, please try again later" +
							" (" + (removed.isEmpty() ? "added " + added : added.isEmpty() ? "dropped " + removed : "added " + added + ", dropped " + removed) + ")");
					check.setFlag(EligibilityFlag.CAN_ENROLL, false);
					if (isCanRequestUpdates()) {
						List<XStudent> students = new ArrayList<XStudent>(1); students.add(student);
						requestUpdate(server, helper, students);
					}
				}
			}
		} catch (SectioningException e) {
			helper.info("Banner eligibility failed: " + e.getMessage());
			throw e;
		} catch (Exception e) {
			helper.warn("Banner eligibility failed: " + e.getMessage(), e);
			throw new SectioningException(e.getMessage());
		} finally {
			if (resource != null) {
				if (resource.getResponse() != null) resource.getResponse().release();
				resource.release();
			}
		}
	}
	
	protected boolean eligibilityIgnoreBannerRegistration(OnlineSectioningServer server, OnlineSectioningHelper helper, XStudent student, XEInterface.Registration reg) {
		return false;
	}
	
	@Override
	public List<EnrollmentFailure> enroll(OnlineSectioningServer server, OnlineSectioningHelper helper, XStudent student, List<EnrollmentRequest> enrollments, Set<Long> lockedCourses, GradeModes gradeModes, boolean hasWaitListedCourses) throws SectioningException {
		ClientResource resource = null;
		try {
			String pin = helper.getPin();
			if ((pin == null || pin.isEmpty()) && student.hasReleasedPin()) pin = student.getPin();
			AcademicSessionInfo session = server.getAcademicSession();
			String term = getBannerTerm(session);
			String campus = getBannerCampus(session);
			boolean admin = isBannerAdmin(helper);
			if (helper.isDebugEnabled())
				helper.debug("Enrolling " + student.getName() + " to " + enrollments + " (term: " + term + ", id:" + getBannerId(student) + (admin ? ", admin" : pin != null ? ", pin:" + pin : "") + ")");
			
			float maxCredit = getMaxCredit(admin);
			if (maxCredit > 0f) {
				float enrolled = 0f;
				for (XRequest r: student.getRequests())
					if (r instanceof XCourseRequest) {
						XCourseRequest cr = (XCourseRequest)r;
						if (cr.getEnrollment() != null)
							enrolled += cr.getEnrollment().getCredit(server);
					}
				// Check credits
				float credit = 0f;
				for (EnrollmentRequest req: enrollments) {
					credit += req.getCredit();
				}
				if (credit > maxCredit && credit > enrolled) {
					throw new SectioningException(ApplicationProperties.getProperty("banner.xe.messages.maxCredit", "Maximum of {max} hours exceeded.")
							.replace("{max}", sCreditFormat.format(maxCredit > enrolled ? maxCredit : enrolled)).replace("{credit}", sCreditFormat.format(credit))).setCanRequestOverride(true);
				}
			}
			
			// First, check student registration status
			resource = new ClientResource(getBannerSite());
			resource.setNext(iClient);
			resource.setChallengeResponse(ChallengeScheme.HTTP_BASIC, getBannerUser(helper), getBannerPassword(helper));
			Gson gson = getGson(helper);
			XEInterface.RegisterResponse original = null;
			
			resource.addQueryParameter("term", term);
			resource.addQueryParameter("bannerId", getBannerId(student));
			helper.getAction().addOptionBuilder().setKey("term").setValue(term);
			helper.getAction().addOptionBuilder().setKey("bannerId").setValue(getBannerId(student));
			if (admin) {
				String param = getAdminParameter();
				resource.addQueryParameter(param, "SB");
				helper.getAction().addOptionBuilder().setKey(param).setValue("SB");
			} else if (pin != null && !pin.isEmpty()) {
				resource.addQueryParameter("altPin", pin);
				helper.getAction().addOptionBuilder().setKey("pin").setValue(pin);
			}
			long t0 = System.currentTimeMillis();
			try {
				resource.get(MediaType.APPLICATION_JSON);
			} catch (ResourceException exception) {
				helper.getAction().setApiException(exception.getMessage());
				try {
					XEInterface.ErrorResponse response = new GsonRepresentation<XEInterface.ErrorResponse>(resource.getResponseEntity(), XEInterface.ErrorResponse.class).getObject();
					helper.getAction().addOptionBuilder().setKey("exception").setValue(gson.toJson(response));
					XEInterface.Error error = response.getError();
					if (error != null && error.message != null) {
						throw new SectioningException(error.message);
					} else if (error != null && error.description != null) {
						throw new SectioningException(error.description);
					} else if (error != null && error.errorMessage != null) {
						throw new SectioningException(error.errorMessage);
					} else {
						throw exception;
					}
				} catch (SectioningException e) {
					helper.getAction().setApiException(e.getMessage());
					throw e;
				} catch (Throwable t) {
					throw exception;
				}
			} finally {
				helper.getAction().setApiGetTime(System.currentTimeMillis() - t0);
			}
			List<XEInterface.RegisterResponse> current = new GsonRepresentation<List<XEInterface.RegisterResponse>>(resource.getResponseEntity(), XEInterface.RegisterResponse.TYPE_LIST).getObject();
			helper.getAction().addOptionBuilder().setKey("original").setValue(gson.toJson(current));
			if (current != null && !current.isEmpty())
				original = current.get(0);
			
			// Check status, memorize enrolled sections
			if (original == null || !original.validStudent) {
				String reason = null;
				if (original != null && original.failureReasons != null) {
					for (String m: original.failureReasons) {
						if (reason == null)
							reason = m;
						else
							reason += "\n" + m;
					}
				}
				throw new SectioningException(reason == null ? "Failed to check student registration status." : reason);
			}
			Map<String, XEInterface.Registration> registered = new HashMap<String, XEInterface.Registration>();
			Set<String> noadd = new HashSet<String>();
			Set<String> nodrop = new HashSet<String>();
			Map<String, String> actions = new HashMap<String, String>();
			Set<String> notregistered = new HashSet<String>();
			if (helper.isDebugEnabled())
				helper.debug("Current registration: " + gson.toJson(original));
			Map<String, String> code2desc = new HashMap<String, String>();
			if (original.registrations != null)
				for (XEInterface.Registration reg: original.registrations) {
					if (reg.isRegistered()) {
						registered.put(reg.courseReferenceNumber, reg);
						if (!reg.canDrop(admin, actions))
							nodrop.add(reg.courseReferenceNumber);
						if (reg.gradingMode != null)
							code2desc.put(reg.gradingMode, reg.gradingModeDescription);
					} else {
						notregistered.add(reg.courseReferenceNumber);
						if (!reg.canAdd(admin))
							noadd.add(reg.courseReferenceNumber);
					}
				}
			
			// Check max hours
			if (isCheckMaxHours(admin)) {
				Float maxHours = original.maxHours;
				if (maxHours == null || maxHours <= 0)
					maxHours = getMaxHoursDefault();
				float credit = 0f;
				for (EnrollmentRequest req: enrollments) {
					credit += req.getCredit();
				}
				if (credit > maxHours) {
					throw new SectioningException(ApplicationProperties.getProperty("banner.xe.messages.maxHours", "Maximum of {max} hours exceeded.")
							.replace("{max}", sCreditFormat.format(maxHours)).replace("{credit}", sCreditFormat.format(credit))).setCanRequestOverride(true);
				}
			}
			
			// Next, try to enroll student into the given courses
			boolean changed = false, hasDrop = false;
			Map<String, List<XSection>> id2section = new HashMap<String, List<XSection>>();
			Map<String, XCourse> id2course = new HashMap<String, XCourse>();
			Set<String> added = new HashSet<String>();
			XEInterface.RegisterRequest req = new XEInterface.RegisterRequest(term, getBannerId(student), pin, admin);
			List<EnrollmentFailure> fails = new ArrayList<EnrollmentFailure>();
			Set<String> failed = new HashSet<String>();
			Set<String> checked = new HashSet<String>();
			Set<Long> lockedCoursesWithChanges = new HashSet<Long>();
			for (EnrollmentRequest request: enrollments) {
				XCourse course = request.getCourse();
				if (lockedCourses.contains(course.getCourseId()) && !request.getSections().isEmpty()) {
					// offering is locked, make no changes
					for (XSection section: request.getSections()) {
						String id = section.getExternalId(course.getCourseId());
						XEInterface.Registration r = registered.get(id);
						if (r != null) {
							// no change to this section: keep the enrollment
							if (added.add(id)) req.keep(id);
							List<XSection> sections = id2section.get(id);
							if (sections == null) {
								sections = new ArrayList<XSection>();
								id2section.put(id, sections);
							}
							sections.add(section);
							id2course.put(id, course);
							request.setGradeMode(r.gradingMode);
							request.setCreditHour(id, r.creditHour);
						} else {
							// student had a different section: just put warning on the new enrollment
							fails.add(new EnrollmentFailure(course, section, MESSAGES.courseLocked(course.getCourseName()), false, new EnrollmentError(ErrorMessage.UniTimeCode.UT_LOCKED.name(), MESSAGES.courseLocked(course.getCourseName()))));
							checked.add(id); failed.add(id);
							List<XSection> sections = id2section.get(id);
							if (sections == null) {
								sections = new ArrayList<XSection>();
								id2section.put(id, sections);
							}
							sections.add(section);
							id2course.put(id, course);
							lockedCoursesWithChanges.add(course.getCourseId());
						}
					}
				} else {
					// offering is not locked: propose the changes
					for (XSection section: request.getSections()) {
						String id = section.getExternalId(course.getCourseId());
						XEInterface.Registration r = registered.get(id);
						if (r == null && (!section.isEnabledForScheduling() || noadd.contains(id))) {
							fails.add(new EnrollmentFailure(course, section, "Section not available for student scheduling.", false, new EnrollmentError(ErrorMessage.UniTimeCode.UT_DISABLED.name(), "Section not available for student scheduling.")));
							checked.add(id); failed.add(id);
						} else {
							if (r != null) {
								if (added.add(id)) req.keep(id);
								request.setGradeMode(r.gradingMode);
								request.setCreditHour(id, r.creditHour);
							} else {
								changed = true;
								if (added.add(id)) req.add(id, notregistered.contains(id));
							}
						}
						List<XSection> sections = id2section.get(id);
						if (sections == null) {
							sections = new ArrayList<XSection>();
							id2section.put(id, sections);
						}
						sections.add(section);
						id2course.put(id, course);
					}
				}
			}
			// drop old sections
			for (Map.Entry<String, XEInterface.Registration> entry: registered.entrySet()) {
				String id = entry.getKey();
				if (added.contains(id)) continue;
				boolean drop = true;
				boolean found = false;
				for (XRequest r: student.getRequests())
					if (r instanceof XCourseRequest) {
						for (XCourseId c: ((XCourseRequest)r).getCourseIds()) {
							XOffering offering = server.getOffering(c.getOfferingId());
							if (offering == null) continue;
							for (XConfig f: offering.getConfigs())
								for (XSubpart s: f.getSubparts())
									for (XSection x: s.getSections())
										if (id.equals(x.getExternalId(c.getCourseId()))) {
											found = true;
											List<XSection> sections = id2section.get(id);
											if (sections == null) {
												sections = new ArrayList<XSection>();
												id2section.put(id, sections);
											}
											sections.add(x);
											id2course.put(id, offering.getCourse(c.getCourseId()));
											if (!x.isEnabledForScheduling() || nodrop.contains(id)) {
												fails.add(new EnrollmentFailure(offering.getCourse(c), x, "Section not available for student scheduling.", true, new EnrollmentError(ErrorMessage.UniTimeCode.UT_DISABLED.name(), "Section not available for student scheduling.")));
												checked.add(id); failed.add(id);
												drop = false;
											} else if (lockedCoursesWithChanges.contains(c.getCourseId())) {
												fails.add(new EnrollmentFailure(offering.getCourse(c), x, MESSAGES.courseLocked(c.getCourseName()), true, new EnrollmentError(ErrorMessage.UniTimeCode.UT_LOCKED.name(), MESSAGES.courseLocked(c.getCourseName()))));
												checked.add(id); failed.add(id);
												drop = false;
											}
											for (EnrollmentRequest request: enrollments) {
												if (request.getCourse().equals(c)) {
													request.setGradeMode(entry.getValue().gradingMode);
													request.setCreditHour(id, entry.getValue().creditHour);
												}
											}
										}
						}
					}
				if (!found && !campus.equals(entry.getValue().campus)) {
					if (added.add(id)) req.keep(id);
				} else  if (drop) {
					changed = true;
					req.drop(id, actions);
					hasDrop = true;
				} else {
					if (added.add(id)) req.keep(id);
				}
			}
			
			switch (getConditionalAddDrop(admin)) {
			case ALWAYS:
				req.setConditionalAddDrop(true);
				break;
			case HAS_DROP:
				if (hasDrop) req.setConditionalAddDrop(true);
				break;
			case IS_REGISTERED:
				if (!registered.isEmpty()) req.setConditionalAddDrop(true);
				break;
			default:
				break;
			}
			
			if (helper.isDebugEnabled())
				helper.debug("Request: " + gson.toJson(req));
			helper.getAction().addOptionBuilder().setKey("request").setValue(gson.toJson(req));
			
			if (req.isEmpty() || !changed) {
				// no classes to add or drop -> return no failures
				if (!fails.isEmpty())
					for (EnrollmentFailure f: fails)
						helper.getAction().addMessageBuilder().setText(f.toString()).setLevel(OnlineSectioningLog.Message.Level.WARN);
				return fails;
			}

			long t1 = System.currentTimeMillis();
			try {
				resource.post(new GsonRepresentation<XEInterface.RegisterRequest>(req));
			} catch (ResourceException exception) {
				helper.getAction().setApiException(exception.getMessage());
				try {
					XEInterface.ErrorResponse response = new GsonRepresentation<XEInterface.ErrorResponse>(resource.getResponseEntity(), XEInterface.ErrorResponse.class).getObject();
					helper.getAction().addOptionBuilder().setKey("exception").setValue(gson.toJson(response));
					XEInterface.Error error = response.getError();
					if (error != null && error.message != null) {
						throw new SectioningException(error.message);
					} else if (error != null && error.description != null) {
						throw new SectioningException(error.description);
					} else if (error != null && error.errorMessage != null) {
						throw new SectioningException(error.errorMessage);
					} else {
						throw exception;
					}
				} catch (SectioningException e) {
					helper.getAction().setApiException(e.getMessage());
					throw e;
				} catch (Throwable t) {
 					throw exception;
				}
			} finally {
				helper.getAction().setApiPostTime(System.currentTimeMillis() - t1);
			}
			
			// Finally, check the response
			XEInterface.RegisterResponse response = new GsonRepresentation<XEInterface.RegisterResponse>(resource.getResponseEntity(), XEInterface.RegisterResponse.class).getObject();
			if (helper.isDebugEnabled())
				helper.debug("Response: " + gson.toJson(response));
			helper.getAction().addOptionBuilder().setKey("response").setValue(gson.toJson(response));
			if (response == null || !response.validStudent) {
				String reason = null;
				if (response != null && response.failureReasons != null) {
					for (String m: response.failureReasons) {
						if (reason == null)
							reason = m;
						else
							reason += "\n" + m;
					}
				}
				throw new SectioningException(reason == null ? "Failed to enroll student." : reason);
			}
			
			Set<String> outcome = new HashSet<String>();
			String resetGradeModes = getResetGradeModesRegExp();
			float currentCredit = 0f;
			if (response.registrations != null) {
				OnlineSectioningLog.Enrollment.Builder external = OnlineSectioningLog.Enrollment.newBuilder();
				external.setType(OnlineSectioningLog.Enrollment.EnrollmentType.EXTERNAL);
				for (XEInterface.Registration reg: response.registrations) {
					String id = reg.courseReferenceNumber;
					checked.add(id);
					
					List<EnrollmentError> errors = new ArrayList<EnrollmentError>();
					String error = null;
					if (reg.crnErrors != null)
						for (XEInterface.CrnError e: reg.crnErrors) {
							if (error == null)
								error = e.message;
							else
								error += "\n" + e.message;
							errors.add(new EnrollmentError(e.messageType, e.message));
						}
					
					if ("Registered".equals(reg.statusDescription)) {
						external.addSectionBuilder()
							.setClazz(OnlineSectioningLog.Entity.newBuilder().setName(reg.courseReferenceNumber))
							.setCourse(OnlineSectioningLog.Entity.newBuilder().setName(reg.subject + " " + reg.courseNumber))
							.setSubpart(OnlineSectioningLog.Entity.newBuilder().setName(reg.scheduleType));
						outcome.add(reg.courseReferenceNumber);
						if (gradeModes != null && reg.gradingMode != null) {
							gradeModes.addGradeMode(reg.courseReferenceNumber, new GradeMode(reg.gradingMode, reg.gradingModeDescription, resetGradeModes != null && !resetGradeModes.isEmpty() && reg.gradingMode.matches(resetGradeModes)));
						}
						if (gradeModes != null && reg.creditHour != null) {
							gradeModes.addCreditHour(reg.courseReferenceNumber, reg.creditHour);
							currentCredit += reg.creditHour;
						}
						if (added.contains(id)) {
							// skip successfully registered enrollments
							if (error != null) {
								XCourse course = id2course.get(id);
								if (course != null)
									for (XSection section: id2section.get(id)) {
										fails.add(new EnrollmentFailure(course, section, error, true, errors));
										failed.add(id);
									}
							}
							continue;
						}
					}
					if ("Deleted".equals(reg.statusDescription) || "Dropped".equals(reg.statusDescription)) {
						// skip deleted enrollments
						continue;
					}
					if ("Withdrawn".equals(reg.statusDescription) && gradeModes != null && reg.creditHour != null)
						currentCredit += reg.creditHour;
					if (error == null && response.registrationException != null) {
						error = response.registrationException;
						errors.add(new EnrollmentError("Unable to make requested changes so your schedule was not changed.".equals(response.registrationException) ? "IGNORE" : "UNKNOWN", response.registrationException));
					}
					XCourse course = id2course.get(id);
					if (course != null)
						for (XSection section: id2section.get(id)) {
							if (error == null && !failed.add(id)) continue;
							if (error == null) {
								errors.add(new EnrollmentError("UNKNOWN", added.contains(id) ? "Enrollment failed." : "Drop failed."));
							}
							fails.add(new EnrollmentFailure(course, section, error == null ? added.contains(id) ? "Enrollment failed." : "Drop failed." : error, "Registered".equals(reg.statusDescription), errors));
						}
				}
				helper.getAction().addEnrollment(external);
			}
			if (gradeModes != null)
				gradeModes.setCurrentCredit(currentCredit);
			if (response.failedRegistrations != null) {
				Set<EnrollmentError> error = new TreeSet<EnrollmentError>();
				for (XEInterface.FailedRegistration reg: response.failedRegistrations) {
					if (reg.failedCRN != null) {
						String id = reg.failedCRN;
						XCourse course = id2course.get(id);
						if (course != null)
							for (XSection section: id2section.get(id)) {
								if (reg.failure == null && !failed.add(id)) continue;
								fails.add(new EnrollmentFailure(course, section, reg.failure == null ? "Enrollment failed." : reg.failure, false, new EnrollmentError("UNKNOWN", reg.failure == null ? "Enrollment failed." : reg.failure)));
							}
						checked.add(id);
					} else {
						if (reg.failure != null)
							error.add(new EnrollmentError("UNKNOWN", reg.failure));
					}
				}
				String em = null;
				for (EnrollmentError m: error) {
					if (em == null)
						em = m.getMessage();
					else
						em += "\n" + m.getMessage();
				}
				if (response.registrationException != null) {
					if (em == null)
						em = response.registrationException;
					else
						em += "\n" + response.registrationException;
					error.add(new EnrollmentError("Unable to make requested changes so your schedule was not changed.".equals(response.registrationException) ? "IGNORE" : "UNKNOWN", response.registrationException));
				}
				for (EnrollmentRequest request: enrollments) {
					XCourse course = request.getCourse();
					for (XSection section: request.getSections()) {
						String id = section.getExternalId(course.getCourseId());
						if (!checked.contains(id) && (em != null || failed.add(id))) {
							if (em == null)
								fails.add(new EnrollmentFailure(course, section, "Enrollment failed.", false, new EnrollmentError("UNKNOWN", "Enrollment failed.")));
							else
								fails.add(new EnrollmentFailure(course, section, em, false, error));
						}
					}
				}
			}
			
			if (outcome.equals(registered.keySet()) && !fails.isEmpty() && throwExceptionWhenNoChange(admin) && !hasWaitListedCourses) {
				String message = "";
				for (EnrollmentFailure f: fails) {
					if (!"Unable to make requested changes so your schedule was not changed.".equals(f.getMessage()))
						message += (message.isEmpty() ? "" : "<br>") + f;
				}
				SectioningException e = new SectioningException(message.replace("\n", " ")).setCanRequestOverride(true);
				for (EnrollmentFailure f: fails) {
					if (f.getSection() != null && !"Unable to make requested changes so your schedule was not changed.".equals(f.getMessage()))
						e.setSectionMessage(f.getSection().getSectionId(), f.getMessage());
					if (f.hasErrors())
						for (EnrollmentError err: f.getErrors())
							if (!"IGNORE".equals(err.getCode()))
								e.addError(new ErrorMessage(f.getCourse().getCourseName(), f.getSection().getExternalId(f.getCourse().getCourseId()), err.getCode(), err.getMessage()));
				}
				throw e;
			}
			
			if (isPreserveGradeMode() && response.registrations != null) {
				XEInterface.RegisterRequest reqGM = new XEInterface.RegisterRequest(getBannerTerm(server.getAcademicSession()), getBannerId(student), null, true);
				try {
					if (resetGradeModes != null && !resetGradeModes.isEmpty()) {
						request: for (EnrollmentRequest request: enrollments) {
							String gm = request.getGradeMode();
							if (gm != null && gm.matches(resetGradeModes)) {
								for (XEInterface.Registration reg: response.registrations) {
									if ("Registered".equals(reg.statusDescription) && request.getCourse().equals(id2course.get(reg.courseReferenceNumber))) {
										if (reg.gradingMode != null && !gm.equals(reg.gradingMode)) {
											request.resetGradeMode(reg.gradingMode);
											helper.info("Resetting grade mode for " + request.getCourse().getCourseName() + " from " + gm + " to " + reg.gradingMode);
											continue request;
										}
									}
								}
							}
						}
					}
					reqGM.courseReferenceNumbers = new ArrayList<CourseReferenceNumber>();
					reqGM.actionsAndOptions = new ArrayList<RegisterAction>();
					for (XEInterface.Registration reg: response.registrations) {
						if ("Registered".equals(reg.statusDescription)) {
							reqGM.courseReferenceNumbers.add(new CourseReferenceNumber(reg.courseReferenceNumber));
							XCourse course = id2course.get(reg.courseReferenceNumber);
							if (course != null)
								for (EnrollmentRequest request: enrollments) {
									if (request.getCourse().equals(course)) {
										String gm = request.getGradeMode();
										if (gm != null && !gm.equals(reg.gradingMode)) {
											helper.info("Changing grade mode for " + request.getCourse().getCourseName() + " " + reg.courseReferenceNumber + " from " + reg.gradingMode + " to " + gm);
											RegisterAction action = new RegisterAction(reg.courseReferenceNumber);
											action.selectedGradingMode = gm;
											reqGM.actionsAndOptions.add(action);
										}
										break;
									}
								}
						}
					}
					if (!reqGM.actionsAndOptions.isEmpty()) {
						helper.getAction().addOptionBuilder().setKey("gm_request").setValue(gson.toJson(reqGM));
						long t2 = System.currentTimeMillis();
						try {
							resource.post(new GsonRepresentation<XEInterface.RegisterRequest>(reqGM));
						} catch (ResourceException exception) {
							helper.getAction().setApiException(exception.getMessage());
							try {
								XEInterface.ErrorResponse responseGM = new GsonRepresentation<XEInterface.ErrorResponse>(resource.getResponseEntity(), XEInterface.ErrorResponse.class).getObject();
								helper.getAction().addOptionBuilder().setKey("exception").setValue(gson.toJson(responseGM));
								XEInterface.Error error = responseGM.getError();
								if (error != null && error.message != null) {
									throw new SectioningException(error.message);
								} else if (error != null && error.description != null) {
									throw new SectioningException(error.description);
								} else if (error != null && error.errorMessage != null) {
									throw new SectioningException(error.errorMessage);
								} else {
									throw exception;
								}
							} catch (SectioningException e) {
								helper.getAction().setApiException(e.getMessage());
								throw e;
							} catch (Throwable t) {
			 					throw exception;
							}
						} finally {
							helper.getAction().setApiPostTime(helper.getAction().getApiPostTime() + (System.currentTimeMillis() - t2));
						}

						XEInterface.RegisterResponse responseGM = new GsonRepresentation<XEInterface.RegisterResponse>(resource.getResponseEntity(), XEInterface.RegisterResponse.class).getObject();
						helper.getAction().addOptionBuilder().setKey("gm_response").setValue(gson.toJson(responseGM));
						
						if (responseGM == null || !responseGM.validStudent) {
							String reason = null;
							if (responseGM != null && responseGM.failureReasons != null) {
								for (String m: responseGM.failureReasons) {
									if (reason == null)
										reason = m;
									else
										reason += "\n" + m;
								}
							}
							throw new SectioningException(reason == null ? "Failed to change grade modes." : reason);
						}
						currentCredit = 0f;
						if (responseGM.registrations != null) {
							for (XEInterface.Registration reg: responseGM.registrations) {
								if ("Registered".equals(reg.statusDescription)) {
									if (reg.gradingMode != null) {
										String desc = code2desc.get(reg.gradingMode);
										gradeModes.addGradeMode(reg.courseReferenceNumber, new GradeMode(reg.gradingMode, desc != null ? desc : reg.gradingModeDescription, resetGradeModes != null && !resetGradeModes.isEmpty() && reg.gradingMode.matches(resetGradeModes)));
									}
									if (reg.creditHour != null) {
										gradeModes.addCreditHour(reg.courseReferenceNumber, reg.creditHour);
										currentCredit += reg.creditHour;
									}
								}
								if ("Withdrawn".equals(reg.statusDescription) && reg.creditHour != null)
									currentCredit += reg.creditHour;
							}
						}
						gradeModes.setCurrentCredit(currentCredit);
					}
				} catch (Exception e) {
					action: for (RegisterAction action: reqGM.actionsAndOptions) {
						XCourse course = id2course.get(action.courseReferenceNumber);
						if (course == null) continue;
						for (XSection section: id2section.get(action.courseReferenceNumber)) {
							for (EnrollmentFailure ef: fails)
								if (section.equals(ef.getSection())) {
									ef.addError(ErrorMessage.UniTimeCode.UT_GRADE_MODE.name(), "Failed to change grade mode for " + action.courseReferenceNumber + ": " + e.getMessage());
									ef.setMessage(ef.getMessage() + "\nFailed to change grade mode for " + action.courseReferenceNumber + ": " + e.getMessage());
									continue action;
								}
						}
						for (XSection section: id2section.get(action.courseReferenceNumber)) {
							fails.add(new EnrollmentFailure(course, section, "Failed to change grade mode for " + action.courseReferenceNumber + ": " + e.getMessage(), true,
									new EnrollmentError(ErrorMessage.UniTimeCode.UT_GRADE_MODE.name(), "Failed to change grade mode for " + action.courseReferenceNumber + ": " + e.getMessage())
									));
							break;
						}
					}
				}
			}
			
			if ("true".equalsIgnoreCase(ApplicationProperties.getProperty("banner.xe.gradeModeWarnings", "true"))) {
				for (EnrollmentRequest request: enrollments) {
					if (request.hasGradeMode()) {
						String originalGradeMode = request.getGradeMode();
						for (XSection section: request.getSections()) {
							GradeMode gm = gradeModes.getGradeMode(section.getExternalId(request.getCourse().getCourseId()));
							if (gm != null && !originalGradeMode.equals(gm.getCode())) {
								fails.add(new EnrollmentFailure(request.getCourse(), section,
										ApplicationProperties.getProperty("banner.xe.warn.gradeModeChanged", "Grade mode changed back to {label}.")
										.replace("{course}", request.getCourse().getCourseName())
										.replace("{label}", gm.getLabel())
										.replace("{code}", gm.getCode()), true).setInfo());
								break;
							}
						}
					}
				}
			}
			if ("true".equalsIgnoreCase(ApplicationProperties.getProperty("banner.xe.creditWarnings", "true"))) {
				for (EnrollmentRequest request: enrollments) {
					if (request.hasCreditHour()) {
						float originalCreditHour = request.getCreditHour();
						float creditHour = 0f;
						XSection creditSection = null;
						String last = null; 
						for (XSection section: request.getSections()) {
							String crn = section.getExternalId(request.getCourse().getCourseId());
							if (crn == null || crn.equals(last)) continue;
							Float credit = gradeModes.getCreditHour(crn);
							if (credit != null) {
								creditHour += credit;
								if (creditSection == null || credit > 0f) creditSection = section;
							}
							last = crn;
						}
						if (Math.abs(creditHour - originalCreditHour) > 0.01 && creditSection != null) {
							fails.add(new EnrollmentFailure(request.getCourse(), creditSection,
									ApplicationProperties.getProperty("banner.xe.warn.creditChanged", "Variable credit changed back to {credit} credit hour(s).")
									.replace("{course}", request.getCourse().getCourseName())
									.replace("{credit}", Formats.getNumberFormat("0.#").format(creditHour)), true).setInfo());
						}
					}
				}
			}
			
			if (helper.isDebugEnabled())
				helper.debug("Return: " + fails);
			if (!fails.isEmpty())
				for (EnrollmentFailure f: fails)
					helper.getAction().addMessageBuilder().setText(f.toString()).setLevel(OnlineSectioningLog.Message.Level.WARN);

			return fails;
		} catch (SectioningException e) {
			helper.info("Banner enrollment failed: " + e.getMessage());
			throw e;
		} catch (Exception e) {
			helper.warn("Banner enrollment failed: " + e.getMessage(), e);
			throw new SectioningException(e.getMessage());
		} finally {
			if (resource != null) {
				if (resource.getResponse() != null) resource.getResponse().release();
				resource.release();
			}
		}
	}
	
	@Override
	public void dispose() {
		try {
			iClient.stop();
		} catch (Exception e) {
			sLog.error(e.getMessage(), e);
		}
	}
	
	@Override
	public boolean isAllowWaitListing() {
		return isBannerAdmin() && isBannerWaitlist();
	}

	@Override
	public boolean requestUpdate(OnlineSectioningServer server, OnlineSectioningHelper helper, Collection<XStudent> students) throws SectioningException {
		return false;
	}
	
	protected boolean updateStudentRegistration(OnlineSectioningServer server, OnlineSectioningHelper helper, XStudent student, List<XEInterface.Registration> registration) throws SectioningException {
		return false;
	}

	@Override
	public boolean isCanRequestUpdates() {
		return false;
	}

	@Override
	public XEnrollment resection(OnlineSectioningServer server, OnlineSectioningHelper helper, SectioningRequest sectioningRequest, XEnrollment enrollment) throws SectioningException {
		ClientResource resource = null;
		try {
			// Start with a few lookups
			XStudent student = sectioningRequest.getStudent();
			XCourseId course = sectioningRequest.getRequest().getCourseIdByOfferingId(sectioningRequest.getOffering().getOfferingId());
			
			Set<String> idsToAdd = new TreeSet<String>(), idsToDrop = new TreeSet<String>();
			if (sectioningRequest.getLastEnrollment() != null)
				for (XSection section: sectioningRequest.getOldOffering().getSections(sectioningRequest.getLastEnrollment()))
					idsToDrop.add(section.getExternalId(course.getCourseId()));
			if (enrollment != null) {
				for (XSection section: sectioningRequest.getOffering().getSections(enrollment))
					idsToAdd.add(section.getExternalId(course.getCourseId()));
			}
			
			XEnrollment dropEnrollment = sectioningRequest.getDropEnrollment();
			if (dropEnrollment != null) {
				XOffering dropOffering = server.getOffering(dropEnrollment.getOfferingId());
				if (dropOffering != null)
					for (XSection section: dropOffering.getSections(dropEnrollment))
						idsToDrop.add(section.getExternalId(dropEnrollment.getCourseId()));		
			}

			// Remove sections that are to be kept (they are included in both enrollments)
			for (Iterator<String> i = idsToDrop.iterator(); i.hasNext(); )
				if (idsToAdd.remove(i.next())) i.remove();

			// Return the new enrollment when there is no change detected
			if (idsToAdd.isEmpty() && idsToDrop.isEmpty())
				return enrollment;
			
			// Retrieve academic session, banner term etc.
			AcademicSessionInfo session = server.getAcademicSession();
			String term = getBannerTerm(session);
			
			// First, check student registration status
			resource = new ClientResource(getBannerSite());
			resource.setNext(iClient);
			resource.setChallengeResponse(ChallengeScheme.HTTP_BASIC, getBannerUser(helper), getBannerPassword(helper));

			resource.addQueryParameter("term", term);
			resource.addQueryParameter("bannerId", getBannerId(student));
			resource.addQueryParameter(getAdminParameter(), "SB");
			sectioningRequest.getAction().addOptionBuilder().setKey("term").setValue(term);
			sectioningRequest.getAction().addOptionBuilder().setKey("bannerId").setValue(getBannerId(student));
			Gson gson = getGson(helper);
			
			long t0 = System.currentTimeMillis();
			try {
				resource.get(MediaType.APPLICATION_JSON);
			} catch (ResourceException exception) {
				sectioningRequest.getAction().setApiException(exception.getMessage());
				try {
					XEInterface.ErrorResponse response = new GsonRepresentation<XEInterface.ErrorResponse>(resource.getResponseEntity(), XEInterface.ErrorResponse.class).getObject();
					sectioningRequest.getAction().addOptionBuilder().setKey("exception").setValue(gson.toJson(response));
					XEInterface.Error error = response.getError();
					if (error != null && error.message != null) {
						throw new SectioningException(error.message);
					} else if (error != null && error.description != null) {
						throw new SectioningException(error.description);
					} else if (error != null && error.errorMessage != null) {
						throw new SectioningException(error.errorMessage);
					} else {
						throw exception;
					}
				} catch (SectioningException e) {
					sectioningRequest.getAction().setApiException(e.getMessage());
					throw e;
				} catch (Throwable t) {
					throw exception;
				}
			} finally {
				sectioningRequest.getAction().setApiGetTime(System.currentTimeMillis() - t0);
			}
			List<XEInterface.RegisterResponse> current = new GsonRepresentation<List<XEInterface.RegisterResponse>>(resource.getResponseEntity(), XEInterface.RegisterResponse.TYPE_LIST).getObject();
			sectioningRequest.getAction().addOptionBuilder().setKey("original").setValue(gson.toJson(current));
			XEInterface.RegisterResponse original = (current != null && !current.isEmpty() ? current.get(0) : null);
			
			// Check status, memorize enrolled sections
			if (original == null || !original.validStudent) {
				String reason = null;
				if (original != null && original.failureReasons != null) {
					for (String m: original.failureReasons) {
						if (reason == null)
							reason = m;
						else
							reason += "\n" + m;
					}
				}
				throw new SectioningException(reason == null ? "Failed to check student registration status." : reason);
			}
			
			XEInterface.RegisterRequest req = new XEInterface.RegisterRequest(term, getBannerId(student), null, true);
			if (getConditionalAddDrop(true) != ConditionalDropType.NEVER)
				req.setConditionalAddDrop(true);
			boolean changed = false;
			Map<String, String> actions = new HashMap<String, String>();
			if (original.registrations != null)
				for (XEInterface.Registration reg: original.registrations) {
					if (reg.isRegistered()) {
						if (idsToDrop.contains(reg.courseReferenceNumber)) {
							if (!reg.canDrop(true, actions))
								throw new SectioningException("Section " + reg.courseReferenceNumber + " is not available for student scheduling.");
							req.drop(reg.courseReferenceNumber, actions);
							changed = true;
						} else {
							req.keep(reg.courseReferenceNumber);
						}
					} else if (idsToAdd.remove(reg.courseReferenceNumber)) {
						if (!reg.canAdd(true))
							throw new SectioningException("Section " + reg.courseReferenceNumber + " is not available for student scheduling.");
						req.add(reg.courseReferenceNumber, true);
						changed = true;
					}
				}
			for (String id: idsToAdd) {
				req.add(id, false);
				changed = true;
			}
			
			sectioningRequest.getAction().addOptionBuilder().setKey("request").setValue(gson.toJson(req));
			
			if (req.isEmpty() || !changed) {
				// no classes to add or drop -> return no failures
				return enrollment;
			}

			long t1 = System.currentTimeMillis();
			try {
				resource.post(new GsonRepresentation<XEInterface.RegisterRequest>(req));
			} catch (ResourceException exception) {
				sectioningRequest.getAction().setApiException(exception.getMessage());
				try {
					XEInterface.ErrorResponse response = new GsonRepresentation<XEInterface.ErrorResponse>(resource.getResponseEntity(), XEInterface.ErrorResponse.class).getObject();
					sectioningRequest.getAction().addOptionBuilder().setKey("exception").setValue(gson.toJson(response));
					XEInterface.Error error = response.getError();
					if (error != null && error.message != null) {
						throw new SectioningException(error.message);
					} else if (error != null && error.description != null) {
						throw new SectioningException(error.description);
					} else if (error != null && error.errorMessage != null) {
						throw new SectioningException(error.errorMessage);
					} else {
						throw exception;
					}
				} catch (SectioningException e) {
					sectioningRequest.getAction().setApiException(e.getMessage());
					throw e;
				} catch (Throwable t) {
					throw exception;
				}
			} finally {
				sectioningRequest.getAction().setApiPostTime(System.currentTimeMillis() - t1);
			}
			
			// Finally, check the response
			XEInterface.RegisterResponse response = new GsonRepresentation<XEInterface.RegisterResponse>(resource.getResponseEntity(), XEInterface.RegisterResponse.class).getObject();
			sectioningRequest.getAction().addOptionBuilder().setKey("response").setValue(gson.toJson(response));
			if (response == null || !response.validStudent) {
				String reason = null;
				if (response != null && response.failureReasons != null) {
					for (String m: response.failureReasons) {
						if (reason == null)
							reason = m;
						else
							reason += "\n" + m;
					}
				}
				throw new SectioningException(reason == null ? "Failed to enroll student." : reason);
			}
			
			SectioningException exception = null;
			if (response.registrationException != null)
				exception = new SectioningException(response.registrationException);
			
			XEnrollment ret = new XEnrollment(enrollment == null ? sectioningRequest.getLastEnrollment() : enrollment);
			ret.getSectionIds().clear();
			Set<String> registered = new TreeSet<String>();
			if (response.registrations != null) {
				OnlineSectioningLog.Enrollment.Builder external = OnlineSectioningLog.Enrollment.newBuilder();
				external.setType(OnlineSectioningLog.Enrollment.EnrollmentType.EXTERNAL);
				for (XEInterface.Registration reg: response.registrations) {
					String id = reg.courseReferenceNumber;
					
					if (reg.crnErrors != null)
						for (XEInterface.CrnError e: reg.crnErrors) {
							sectioningRequest.getAction().addMessageBuilder().setText(
									reg.subject + " " + reg.courseNumber + " " + reg.courseReferenceNumber + ": " + e.message + " (" + e.messageType + ")"
									).setLevel(OnlineSectioningLog.Message.Level.WARN);
							if (exception != null)
								exception.addError(new ErrorMessage(reg.subject + " " +reg.courseNumber, reg.courseReferenceNumber, e.messageType, e.message)); 
						}
					List<XSection> sections = sectioningRequest.getOffering().getSections(course.getCourseId(), id);
					if (!sections.isEmpty() && "Registered".equals(reg.statusDescription)) {
						for (XSection section: sections)
							ret.getSectionIds().add(section.getSectionId());
						registered.add(id);
					}
					if ("Registered".equals(reg.statusDescription)) {
						external.addSectionBuilder()
							.setClazz(OnlineSectioningLog.Entity.newBuilder().setName(reg.courseReferenceNumber))
							.setCourse(OnlineSectioningLog.Entity.newBuilder().setName(reg.subject + " " + reg.courseNumber))
							.setSubpart(OnlineSectioningLog.Entity.newBuilder().setName(reg.scheduleType));
					}
				}
				sectioningRequest.getAction().addEnrollment(external);
			}
			if (response.failedRegistrations != null) {
				for (XEInterface.FailedRegistration reg: response.failedRegistrations) {
					if (reg.failedCRN != null)
						sectioningRequest.getAction().addMessageBuilder().setText(reg.failedCRN + ": " + reg.failure).setLevel(OnlineSectioningLog.Message.Level.WARN);
					else
						sectioningRequest.getAction().addMessageBuilder().setText(reg.failure).setLevel(OnlineSectioningLog.Message.Level.WARN);
					if (exception == null && ret.getSectionIds().isEmpty())
						exception = new SectioningException(reg.failure);
					if (exception != null)
						exception.addError(new ErrorMessage(course.getCourseName(), reg.failedCRN, "", reg.failure));
				}
			}
			if (exception != null) throw exception;		
			return (ret.getSectionIds().isEmpty() ? null : ret);
		} catch (SectioningException e) {
			sectioningRequest.getAction().addMessageBuilder().setText("Banner enrollment failed: " + e.getMessage()).setLevel(OnlineSectioningLog.Message.Level.INFO);
			throw e;
		} catch (Exception e) {
			sectioningRequest.getAction().addMessageBuilder().setText("Banner enrollment failed: " + e.getMessage()).setLevel(OnlineSectioningLog.Message.Level.WARN);
			throw new SectioningException(e.getMessage());
		} finally {
			if (resource != null) {
				if (resource.getResponse() != null) resource.getResponse().release();
				resource.release();
			}
		}
	}
}
