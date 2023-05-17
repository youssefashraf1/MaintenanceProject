/*
urse * Licensed to The Apereo Foundation under one or more contributor license
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.AssignmentMap;
import org.cpsolver.studentsct.model.Config;
import org.cpsolver.studentsct.model.Course;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.SctAssignment;
import org.cpsolver.studentsct.model.Section;
import org.cpsolver.studentsct.model.Subpart;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Protocol;
import org.restlet.resource.ClientResource;
import org.unitime.localization.impl.Localization;
import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.defaults.ApplicationProperty;
import org.unitime.timetable.gwt.resources.StudentSectioningConstants;
import org.unitime.timetable.gwt.resources.StudentSectioningMessages;
import org.unitime.timetable.gwt.shared.CourseRequestInterface;
import org.unitime.timetable.gwt.shared.PageAccessException;
import org.unitime.timetable.gwt.shared.SectioningException;
import org.unitime.timetable.gwt.shared.CourseRequestInterface.CheckCoursesResponse;
import org.unitime.timetable.gwt.shared.CourseRequestInterface.CourseMessage;
import org.unitime.timetable.gwt.shared.CourseRequestInterface.RequestedCourse;
import org.unitime.timetable.gwt.shared.CourseRequestInterface.RequestedCourseStatus;
import org.unitime.timetable.gwt.shared.OnlineSectioningInterface.EligibilityCheck;
import org.unitime.timetable.gwt.shared.OnlineSectioningInterface.EligibilityCheck.EligibilityFlag;
import org.unitime.timetable.gwt.shared.OnlineSectioningInterface.WaitListMode;
import org.unitime.timetable.model.CourseDemand;
import org.unitime.timetable.model.Student;
import org.unitime.timetable.model.StudentClassEnrollment;
import org.unitime.timetable.model.StudentSectioningStatus;
import org.unitime.timetable.model.dao.CourseDemandDAO;
import org.unitime.timetable.model.dao.StudentDAO;
import org.unitime.timetable.model.CourseRequest.CourseRequestOverrideIntent;
import org.unitime.timetable.model.CourseRequest.CourseRequestOverrideStatus;
import org.unitime.timetable.onlinesectioning.AcademicSessionInfo;
import org.unitime.timetable.onlinesectioning.OnlineSectioningHelper;
import org.unitime.timetable.onlinesectioning.OnlineSectioningLog;
import org.unitime.timetable.onlinesectioning.OnlineSectioningLog.Action.Builder;
import org.unitime.timetable.onlinesectioning.OnlineSectioningServer;
import org.unitime.timetable.onlinesectioning.custom.ExternalTermProvider;
import org.unitime.timetable.onlinesectioning.custom.WaitListValidationProvider;
import org.unitime.timetable.onlinesectioning.custom.purdue.SpecialRegistrationInterface.ApiMode;
import org.unitime.timetable.onlinesectioning.custom.purdue.SpecialRegistrationInterface.Change;
import org.unitime.timetable.onlinesectioning.custom.purdue.SpecialRegistrationInterface.ChangeError;
import org.unitime.timetable.onlinesectioning.custom.purdue.SpecialRegistrationInterface.ChangeOperation;
import org.unitime.timetable.onlinesectioning.custom.purdue.SpecialRegistrationInterface.ChangeStatus;
import org.unitime.timetable.onlinesectioning.custom.purdue.SpecialRegistrationInterface.CheckEligibilityResponse;
import org.unitime.timetable.onlinesectioning.custom.purdue.SpecialRegistrationInterface.CheckRestrictionsRequest;
import org.unitime.timetable.onlinesectioning.custom.purdue.SpecialRegistrationInterface.CheckRestrictionsResponse;
import org.unitime.timetable.onlinesectioning.custom.purdue.SpecialRegistrationInterface.CourseCredit;
import org.unitime.timetable.onlinesectioning.custom.purdue.SpecialRegistrationInterface.DeniedMaxCredit;
import org.unitime.timetable.onlinesectioning.custom.purdue.SpecialRegistrationInterface.DeniedRequest;
import org.unitime.timetable.onlinesectioning.custom.purdue.SpecialRegistrationInterface.EligibilityProblem;
import org.unitime.timetable.onlinesectioning.custom.purdue.SpecialRegistrationInterface.Problem;
import org.unitime.timetable.onlinesectioning.custom.purdue.SpecialRegistrationInterface.RequestorRole;
import org.unitime.timetable.onlinesectioning.custom.purdue.SpecialRegistrationInterface.ResponseStatus;
import org.unitime.timetable.onlinesectioning.custom.purdue.SpecialRegistrationInterface.SpecialRegistration;
import org.unitime.timetable.onlinesectioning.custom.purdue.SpecialRegistrationInterface.SpecialRegistrationMultipleStatusResponse;
import org.unitime.timetable.onlinesectioning.custom.purdue.SpecialRegistrationInterface.SpecialRegistrationRequest;
import org.unitime.timetable.onlinesectioning.custom.purdue.SpecialRegistrationInterface.SpecialRegistrationResponseList;
import org.unitime.timetable.onlinesectioning.custom.purdue.SpecialRegistrationInterface.SpecialRegistrationStatus;
import org.unitime.timetable.onlinesectioning.custom.purdue.SpecialRegistrationInterface.SpecialRegistrationStatusResponse;
import org.unitime.timetable.onlinesectioning.model.XCourse;
import org.unitime.timetable.onlinesectioning.model.XCourseId;
import org.unitime.timetable.onlinesectioning.model.XCourseRequest;
import org.unitime.timetable.onlinesectioning.model.XEnrollment;
import org.unitime.timetable.onlinesectioning.model.XOffering;
import org.unitime.timetable.onlinesectioning.model.XOverride;
import org.unitime.timetable.onlinesectioning.model.XRequest;
import org.unitime.timetable.onlinesectioning.model.XSection;
import org.unitime.timetable.onlinesectioning.model.XStudent;
import org.unitime.timetable.onlinesectioning.server.DatabaseServer;
import org.unitime.timetable.onlinesectioning.solver.SectioningRequest;
import org.unitime.timetable.onlinesectioning.updates.ReloadStudent;
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

/**
 * @author Tomas Muller
 */

public class PurdueWaitListValidationProvider implements WaitListValidationProvider {
	private static Log sLog = LogFactory.getLog(PurdueWaitListValidationProvider.class);
	protected static final StudentSectioningMessages MESSAGES = Localization.create(StudentSectioningMessages.class);
	protected static final StudentSectioningConstants CONSTANTS = Localization.create(StudentSectioningConstants.class);
	protected static Format<Number> sCreditFormat = Formats.getNumberFormat("0.##");

	private Client iClient;
	private ExternalTermProvider iExternalTermProvider;

	public PurdueWaitListValidationProvider() {
		List<Protocol> protocols = new ArrayList<Protocol>();
		protocols.add(Protocol.HTTP);
		protocols.add(Protocol.HTTPS);
		iClient = new Client(protocols);
		Context cx = new Context();
		cx.getParameters().add("readTimeout", getSpecialRegistrationApiReadTimeout());
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
	
	protected String getSpecialRegistrationApiReadTimeout() {
		return ApplicationProperties.getProperty("purdue.specreg.readTimeout", "60000");
	}
	
	protected String getSpecialRegistrationApiSite() {
		return ApplicationProperties.getProperty("purdue.specreg.site");
	}
	
	protected String getSpecialRegistrationApiSiteCheckEligibility() {
		return ApplicationProperties.getProperty("purdue.specreg.site.checkEligibility", getSpecialRegistrationApiSite() + "/checkEligibility");
	}
	
	protected String getSpecialRegistrationApiValidationSite() {
		return ApplicationProperties.getProperty("purdue.specreg.site.validation", getSpecialRegistrationApiSite() + "/checkRestrictions");
	}
	
	protected String getSpecialRegistrationApiSiteSubmitRegistration() {
		return ApplicationProperties.getProperty("purdue.specreg.site.submitRegistration", getSpecialRegistrationApiSite() + "/submitRegistration");
	}
	
	protected String getSpecialRegistrationApiSiteCheckSpecialRegistrationStatus() {
		return ApplicationProperties.getProperty("purdue.specreg.site.checkSpecialRegistrationStatus", getSpecialRegistrationApiSite() + "/checkSpecialRegistrationStatus");
	}
	
	protected String getSpecialRegistrationApiSiteCheckAllSpecialRegistrationStatus() {
		return ApplicationProperties.getProperty("purdue.specreg.site.checkAllSpecialRegistrationStatus", getSpecialRegistrationApiSite() + "/checkAllSpecialRegistrationStatus");
	}
	
	protected String getSpecialRegistrationApiKey() {
		return ApplicationProperties.getProperty("purdue.specreg.apiKey");
	}
	
	protected ApiMode getSpecialRegistrationApiMode() {
		return ApiMode.valueOf(ApplicationProperties.getProperty("purdue.specreg.mode.waitlist", "WAITL"));
	}
	
	protected String getBannerId(org.unitime.timetable.model.Student student) {
		String id = student.getExternalUniqueId();
		while (id.length() < 9) id = "0" + id;
		return id;
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
	
	protected String getRequestorId(OnlineSectioningLog.Entity user) {
		if (user == null || user.getExternalId() == null) return null;
		String id = user.getExternalId();
		while (id.length() < 9) id = "0" + id;
		return id;
	}
	
	protected RequestorRole getRequestorType(OnlineSectioningLog.Entity user, XStudent student) {
		if (user == null || user.getExternalId() == null) return null;
		if (student != null) return (user.getExternalId().equals(student.getExternalId()) ? RequestorRole.STUDENT : RequestorRole.MANAGER);
		if (user.hasType()) {
			switch (user.getType()) {
			case MANAGER: return RequestorRole.MANAGER;
			case STUDENT: return RequestorRole.STUDENT;
			default: return RequestorRole.MANAGER;
			}
		}
		return null;
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
		})
		.registerTypeAdapter(Date.class, new JsonSerializer<Date>() {
			@Override
			public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
				return new JsonPrimitive(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(src));
			}
		})
		.registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
			@Override
			public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
				try {
					return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(json.getAsJsonPrimitive().getAsString());
				} catch (ParseException e) {
					throw new JsonParseException(e.getMessage(), e);
				}
			}
		});
		if (helper.isDebugEnabled()) builder.setPrettyPrinting();
		return builder.create();
	}
	
	protected String getCRN(Section section, Course course) {
		String name = section.getName(course.getId());
		if (name != null && name.indexOf('-') >= 0)
			return name.substring(0, name.indexOf('-'));
		return name;
	}
	
	protected boolean isValidationEnabled(org.unitime.timetable.model.Student student) {
		if (student == null) return false;
		StudentSectioningStatus status = student.getEffectiveStatus();
		return status != null && status.hasOption(StudentSectioningStatus.Option.waitlist) && status.hasOption(StudentSectioningStatus.Option.specreg);
	}
	
	protected boolean isValidationEnabled(OnlineSectioningServer server, OnlineSectioningHelper helper, XStudent student) {
		String status = student.getStatus();
		if (status == null) status = server.getAcademicSession().getDefaultSectioningStatus();
		if (status == null) return true;
		StudentSectioningStatus dbStatus = StudentSectioningStatus.getPresentStatus(status, server.getAcademicSession().getUniqueId(), helper.getHibSession());
		return dbStatus != null && dbStatus.hasOption(StudentSectioningStatus.Option.waitlist) && dbStatus.hasOption(StudentSectioningStatus.Option.specreg);
	}
	
	protected Enrollment firstEnrollment(CourseRequest request, Assignment<Request, Enrollment> assignment, Course course, Config config, HashSet<Section> sections, int idx) {
        if (config.getSubparts().size() == idx) {
        	Enrollment e = new Enrollment(request, request.getCourses().indexOf(course), null, config, new HashSet<SctAssignment>(sections), null);
        	if (request.isNotAllowed(e)) return null;
        	return e;
        } else {
            Subpart subpart = config.getSubparts().get(idx);
            List<Section> sectionsThisSubpart = subpart.getSections();
            List<Section> matchingSectionsThisSubpart = new ArrayList<Section>(subpart.getSections().size());
            for (Section section : sectionsThisSubpart) {
                if (section.isCancelled())
                    continue;
                if (section.getParent() != null && !sections.contains(section.getParent()))
                    continue;
                if (section.isOverlapping(sections))
                    continue;
                if (request.isNotAllowed(course, section))
                	continue;
                matchingSectionsThisSubpart.add(section);
            }
            for (Section section: matchingSectionsThisSubpart) {
                sections.add(section);
                Enrollment e = firstEnrollment(request, assignment, course, config, sections, idx + 1);
                if (e != null) return e;
                sections.remove(section);
            }
        }
        return null;
    }
	
	protected RequestedCourseStatus status(ChangeStatus status) {
		if (status == null) return RequestedCourseStatus.OVERRIDE_PENDING;
		switch (status) {
		case denied:
			return RequestedCourseStatus.OVERRIDE_REJECTED;
		case approved:
			return RequestedCourseStatus.OVERRIDE_APPROVED;
		case cancelled:
			return RequestedCourseStatus.OVERRIDE_CANCELLED;
		default:
			return RequestedCourseStatus.OVERRIDE_PENDING;
		}
	}
	
	protected RequestedCourseStatus combine(RequestedCourseStatus s1, RequestedCourseStatus s2) {
		if (s1 == null) return s2;
		if (s2 == null) return s1;
		if (s1 == s2) return s1;
		if (s1 == RequestedCourseStatus.OVERRIDE_REJECTED || s2 == RequestedCourseStatus.OVERRIDE_REJECTED) return RequestedCourseStatus.OVERRIDE_REJECTED;
		if (s1 == RequestedCourseStatus.OVERRIDE_PENDING || s2 == RequestedCourseStatus.OVERRIDE_PENDING) return RequestedCourseStatus.OVERRIDE_PENDING;
		if (s1 == RequestedCourseStatus.OVERRIDE_APPROVED || s2 == RequestedCourseStatus.OVERRIDE_APPROVED) return RequestedCourseStatus.OVERRIDE_APPROVED;
		if (s1 == RequestedCourseStatus.OVERRIDE_CANCELLED || s2 == RequestedCourseStatus.OVERRIDE_CANCELLED) return RequestedCourseStatus.OVERRIDE_CANCELLED;
		return s1;
	}
	
	protected RequestedCourseStatus status(SpecialRegistration request, boolean credit) {
		RequestedCourseStatus ret = null;
		if (request.changes != null)
			for (Change ch: request.changes) {
				if (ch.status == null) continue;
				if (credit && ch.subject == null && ch.courseNbr == null)
					ret = combine(ret, status(ch.status));
				if (!credit && ch.subject != null && ch.courseNbr != null)
					ret = combine(ret, status(ch.status));
			}
		if (ret != null) return ret;
		if (request.completionStatus != null)
			switch (request.completionStatus) {
			case completed:
				return RequestedCourseStatus.OVERRIDE_APPROVED;
			case cancelled:
				return RequestedCourseStatus.OVERRIDE_CANCELLED;
			case inProgress:
				return RequestedCourseStatus.OVERRIDE_PENDING;
			}
		return RequestedCourseStatus.OVERRIDE_PENDING;
	}
	
	public void validate(OnlineSectioningServer server, OnlineSectioningHelper helper, CourseRequestInterface request, CheckCoursesResponse response) throws SectioningException {
		XStudent original = (request.getStudentId() == null ? null : server.getStudent(request.getStudentId()));
		if (original == null) throw new PageAccessException(MESSAGES.exceptionEnrollNotStudent(server.getAcademicSession().toString()));
		// Do not validate when validation is disabled
		if (!isValidationEnabled(server, helper, original)) return;
		
		Integer CONF_NONE = null;
		Integer CONF_BANNER = 1;
		String creditError = null;
		Float maxCreditNeeded = null;
		
		for (CourseRequestInterface.Request line: request.getCourses()) {
			// only for wait-listed course requests
			if (line.isWaitList() && line.hasRequestedCourse()) {
				
				// skip enrolled courses
				XEnrollment enrolled = null;
				for (RequestedCourse rc: line.getRequestedCourse()) {
					if (rc.hasCourseId()) {
						XCourseRequest cr = original.getRequestForCourse(rc.getCourseId());
						if (cr != null && cr.getEnrollment() != null)  { enrolled = cr.getEnrollment(); break; }
					}
				}
				// when enrolled, only continue when swap for enrolled course is enabled (section wait-list)
				if (enrolled != null && !enrolled.getCourseId().equals(line.getWaitListSwapWithCourseOfferingId()))
					continue;

				XCourse dropCourse = null;
				Set<String> dropCrns = null;
				if (line.hasWaitListSwapWithCourseOfferingId()) {
					dropCourse = server.getCourse(line.getWaitListSwapWithCourseOfferingId());
					XCourseRequest cr = original.getRequestForCourse(line.getWaitListSwapWithCourseOfferingId());
					if (cr != null && cr.getEnrollment() != null && cr.getEnrollment().getCourseId().equals(line.getWaitListSwapWithCourseOfferingId())) {
						dropCrns = new TreeSet<String>();
						XOffering dropOffering = server.getOffering(dropCourse.getOfferingId());
						for (XSection section: dropOffering.getSections(cr.getEnrollment())) {
							dropCrns.add(section.getExternalId(line.getWaitListSwapWithCourseOfferingId())); 
						}
					}
				}
				
				for (RequestedCourse rc: line.getRequestedCourse()) {
					if (rc.hasCourseId()) {
						XCourse xcourse = server.getCourse(rc.getCourseId());
						if (xcourse == null) continue;
						
						// when enrolled, skip courses of lower choice than the enrollment
						if (enrolled != null && line.getIndex(rc.getCourseId()) > line.getIndex(enrolled.getCourseId())) continue;
						
						// skip offerings that cannot be wait-listed
						XOffering offering = server.getOffering(xcourse.getOfferingId());
						if (offering == null || !offering.isWaitList()) continue;
						
						// when enrolled, skip the enrolled course if the enrollment matches the requirements
						if (enrolled != null && enrolled.getCourseId().equals(rc.getCourseId()) && enrolled.isRequired(rc, offering)) continue;
						
						// get possible enrollments into the course
						Assignment<Request, Enrollment> assignment = new AssignmentMap<Request, Enrollment>();
						CourseRequest courseRequest = SectioningRequest.convert(assignment, new XCourseRequest(original, xcourse, rc), dropCourse, server, WaitListMode.WaitList);
						Collection<Enrollment> enrls = courseRequest.getEnrollmentsSkipSameTime(assignment);
						
						// get a test enrollment (preferably a non-conflicting one)
						Enrollment testEnrollment = null;
						for (Iterator<Enrollment> e = enrls.iterator(); e.hasNext();) {
							testEnrollment = e.next();
							boolean overlaps = false;
							for (Request q: testEnrollment.getStudent().getRequests()) {
								if (q.equals(courseRequest)) continue;
								Enrollment x = assignment.getValue(q);
								if (x == null || x.getAssignments() == null || x.getAssignments().isEmpty()) continue;
								for (Iterator<SctAssignment> i = x.getAssignments().iterator(); i.hasNext();) {
						        	SctAssignment a = i.next();
									if (a.isOverlapping(testEnrollment.getAssignments())) {
										overlaps = true;
									}
						        }
							}
							if (!overlaps) break;
						}
						// no test enrollment, take first possible enrollment
						if (testEnrollment == null) {
							Course c = courseRequest.getCourses().get(0);
							for (Config config: c.getOffering().getConfigs()) {
								if (courseRequest.isNotAllowed(c, config)) continue;
								testEnrollment = firstEnrollment(courseRequest, assignment, c, config, new HashSet<Section>(), 0);
							}
						}
						// still no test enrollment -> ignore
						if (testEnrollment == null) continue;
						
						// create request
						CheckRestrictionsRequest req = new CheckRestrictionsRequest();
						req.studentId = getBannerId(original);
						req.term = getBannerTerm(server.getAcademicSession());
						req.campus = getBannerCampus(server.getAcademicSession());
						req.mode = getSpecialRegistrationApiMode();
						
						Set<String> crns = new HashSet<String>();
						Set<String> keep = new HashSet<String>();
						for (Section section: testEnrollment.getSections()) {
							String crn = getCRN(section, testEnrollment.getCourse());
							if (dropCrns != null && dropCrns.contains(crn)) {
								keep.add(crn);
							} else {
								SpecialRegistrationHelper.addWaitListCrn(req, crn);
								crns.add(crn);
							}
						}
						if (dropCrns != null)
							for (String crn: dropCrns) {
								if (!keep.contains(crn)) {
									SpecialRegistrationHelper.dropWaitListCrn(req, crn);
									crns.add(crn);
								}
							}
						// no CRNs to check -> continue
						if (crns.isEmpty()) continue;
						
						// call validation
						CheckRestrictionsResponse resp = null;
						ClientResource resource = null;
						try {
							resource = new ClientResource(getSpecialRegistrationApiValidationSite());
							resource.setNext(iClient);
							resource.addQueryParameter("apiKey", getSpecialRegistrationApiKey());
							
							Gson gson = getGson(helper);
							if (helper.isDebugEnabled())
								helper.debug("Request: " + gson.toJson(req));
							helper.getAction().addOptionBuilder().setKey("wl-req-" + testEnrollment.getCourse().getName().replace(" ", "").toLowerCase()).setValue(gson.toJson(req));
							long t1 = System.currentTimeMillis();
							
							resource.post(new GsonRepresentation<CheckRestrictionsRequest>(req));
							
							helper.getAction().setApiPostTime(
									(helper.getAction().hasApiPostTime() ? helper.getAction().getApiPostTime() : 0) + 
									System.currentTimeMillis() - t1);
							
							resp = (CheckRestrictionsResponse)new GsonRepresentation<CheckRestrictionsResponse>(resource.getResponseEntity(), CheckRestrictionsResponse.class).getObject();
							if (helper.isDebugEnabled())
								helper.debug("Response: " + gson.toJson(resp));
							helper.getAction().addOptionBuilder().setKey("wl-resp-" + testEnrollment.getCourse().getName().replace(" ", "").toLowerCase()).setValue(gson.toJson(resp));
							
							if (ResponseStatus.success != resp.status)
								throw new SectioningException(resp.message == null || resp.message.isEmpty() ? "Failed to check student eligibility (" + resp.status + ")." : resp.message);
						} catch (SectioningException e) {
							helper.getAction().setApiException(e.getMessage());
							throw (SectioningException)e;
						} catch (Exception e) {
							helper.getAction().setApiException(e.getMessage());
							sLog.error(e.getMessage(), e);
							throw new SectioningException(e.getMessage());
						} finally {
							if (resource != null) {
								if (resource.getResponse() != null) resource.getResponse().release();
								resource.release();
							}
						}
						
						// student max credit
						Float maxCredit = resp.maxCredit;
						if (maxCredit == null) maxCredit = Float.parseFloat(ApplicationProperties.getProperty("purdue.specreg.maxCreditDefault", "18"));

						Float maxCreditDenied = null;
						if (resp.deniedMaxCreditRequests != null) {
							for (DeniedMaxCredit r: resp.deniedMaxCreditRequests) {
								if (r.maxCredit != null && r.maxCredit > maxCredit && (maxCreditDenied == null || maxCreditDenied > r.maxCredit))
									maxCreditDenied = r.maxCredit;
							}
						}
						
						Map<String, Map<String, RequestedCourseStatus>> overrides = new HashMap<String, Map<String, RequestedCourseStatus>>();
						Float maxCreditOverride = null;
						RequestedCourseStatus maxCreditOverrideStatus = null;
						
						if (resp.cancelRegistrationRequests != null)
							for (SpecialRegistration r: resp.cancelRegistrationRequests) {
								if (r.changes == null || r.changes.isEmpty()) continue;
								for (Change ch: r.changes) {
									if (ch.status == ChangeStatus.cancelled || ch.status == ChangeStatus.denied) continue;
									if (ch.subject != null && ch.courseNbr != null) {
										String course = ch.subject + " " + ch.courseNbr;
										Map<String, RequestedCourseStatus> problems = overrides.get(course);
										if (problems == null) {
											problems = new HashMap<String, RequestedCourseStatus>();
											overrides.put(course, problems);
										}
										if (ch.errors != null)
											for (ChangeError err: ch.errors) {
												if (err.code != null)
													problems.put(err.code, status(ch.status));
											}
									} else if (r.maxCredit != null && (maxCreditOverride == null || maxCreditOverride < r.maxCredit)) {
										maxCreditOverride = r.maxCredit;
										maxCreditOverrideStatus = status(ch.status);
									}
								}
							}
						
						Float neededCredit = null;
						if (resp.outJson != null && resp.outJson.maxHoursCalc != null)
							neededCredit = resp.outJson.maxHoursCalc;
						if (neededCredit != null) {
							if (maxCreditNeeded == null || maxCreditNeeded < neededCredit)
								maxCreditNeeded = neededCredit;
						}

						
						if (maxCreditDenied != null && neededCredit != null && neededCredit >= maxCreditDenied) {
							response.addMessage(rc.getCourseId(), rc.getCourseName(), "WL-CREDIT",
									ApplicationProperties.getProperty("purdue.specreg.messages.maxCredit", "Maximum of {max} credit hours exceeded.").replace("{max}", sCreditFormat.format(maxCredit)).replace("{credit}", sCreditFormat.format(neededCredit))
									, CONF_NONE);
							response.setCreditWarning(ApplicationProperties.getProperty("purdue.specreg.messages.maxCredit",
									"Maximum of {max} credit hours exceeded.")
									.replace("{max}", sCreditFormat.format(maxCredit)).replace("{credit}", sCreditFormat.format(neededCredit)).replace("{maxCreditDenied}", sCreditFormat.format(maxCreditDenied))
							);
							response.setMaxCreditOverrideStatus(RequestedCourseStatus.OVERRIDE_REJECTED);
							creditError = ApplicationProperties.getProperty("purdue.specreg.messages.maxCreditDeniedError",
											"Maximum of {max} credit hours exceeded.\nThe request to increase the maximum credit hours to {maxCreditDenied} has been denied.\nYou may not be able to get a full schedule.")
									.replace("{max}", sCreditFormat.format(maxCredit)).replace("{credit}", sCreditFormat.format(neededCredit)).replace("{maxCreditDenied}", sCreditFormat.format(maxCreditDenied));
							response.setMaxCreditNeeded(maxCreditNeeded);
						}
						
						if (creditError == null && neededCredit != null && maxCredit < neededCredit) {
							response.addMessage(rc.getCourseId(), rc.getCourseName(), "WL-CREDIT",
									ApplicationProperties.getProperty("purdue.specreg.messages.maxCredit", "Maximum of {max} credit hours exceeded.").replace("{max}", sCreditFormat.format(maxCredit)).replace("{credit}", sCreditFormat.format(neededCredit)),
									maxCreditOverride == null || maxCreditOverride < neededCredit ? CONF_BANNER : CONF_NONE);
							response.setCreditWarning(ApplicationProperties.getProperty("purdue.specreg.messages.maxCredit", "Maximum of {max} credit hours exceeded.").replace("{max}", sCreditFormat.format(maxCredit)).replace("{credit}", sCreditFormat.format(neededCredit)));
							response.setMaxCreditOverrideStatus(maxCreditOverrideStatus == null || maxCreditOverride < neededCredit ? RequestedCourseStatus.OVERRIDE_NEEDED : maxCreditOverrideStatus);
							response.setMaxCreditNeeded(maxCreditNeeded);
						}
						
						Map<String, Set<String>> deniedOverrides = new HashMap<String, Set<String>>();
						if (resp.deniedRequests != null)
							for (DeniedRequest r: resp.deniedRequests) {
								if (r.mode != req.mode) continue;
								String course = r.subject + " " + r.courseNbr;
								Set<String> problems = deniedOverrides.get(course);
								if (problems == null) {
									problems = new TreeSet<String>();
									deniedOverrides.put(course, problems);
								}
								problems.add(r.code);
							}
						
						if (resp.outJson != null && resp.outJson.message != null && resp.outJson.status != null && resp.outJson.status != ResponseStatus.success) {
							response.addError(null, null, "Failure", resp.outJson.message);
							response.setErrorMessage(resp.outJson.message);
						}
						
						if (resp.outJson != null && resp.outJson.problems != null)
							for (Problem problem: resp.outJson.problems) {
								if ("HOLD".equals(problem.code)) {
									response.addError(null, null, problem.code, problem.message);
									response.setErrorMessage(ApplicationProperties.getProperty("purdue.specreg.messages.holdError", problem.message));
									//throw new SectioningException(problem.message);
								}
								if ("DUPL".equals(problem.code)) continue;
								if ("MAXI".equals(problem.code)) continue;
								if ("CLOS".equals(problem.code)) continue;
								if ("TIME".equals(problem.code)) continue;
								if (!crns.contains(problem.crn)) continue;
								String bc = xcourse.getSubjectArea() + " " + xcourse.getCourseNumber();
								Map<String, RequestedCourseStatus> problems = (bc == null ? null : overrides.get(bc));
								Set<String> denied = (bc == null ? null : deniedOverrides.get(bc));
								if (denied != null && denied.contains(problem.code)) {
									response.addMessage(xcourse.getCourseId(), xcourse.getCourseName(), problem.code, "Denied " + problem.message, CONF_NONE).setStatus(RequestedCourseStatus.OVERRIDE_REJECTED);
								} else {
									RequestedCourseStatus status = (problems == null ? null : problems.get(problem.code));
									if (status == null) {
										if (resp.overrides != null && !resp.overrides.contains(problem.code)) {
											response.addError(xcourse.getCourseId(), xcourse.getCourseName(), problem.code, "Not Allowed " + problem.message).setStatus(RequestedCourseStatus.OVERRIDE_REJECTED);
											response.setErrorMessage(ApplicationProperties.getProperty("purdue.specreg.messages.waitlist.deniedOverrideError",
													"One or more wait-listed courses require registration overrides which is not allowed.\nYou cannot wait-list these courses."));
											continue;
										} else {
											if (!xcourse.isOverrideEnabled(problem.code)) {
												response.addError(xcourse.getCourseId(), xcourse.getCourseName(), problem.code, "Not Allowed " + problem.message).setStatus(RequestedCourseStatus.OVERRIDE_REJECTED);
												response.setErrorMessage(ApplicationProperties.getProperty("purdue.specreg.messages.waitlist.deniedOverrideError",
														"One or more wait-listed courses require registration overrides which is not allowed.\nYou cannot wait-list these courses."));
												continue;
											}
										}
									}
									response.addMessage(xcourse.getCourseId(), xcourse.getCourseName(), problem.code, problem.message, status == null ? CONF_BANNER : CONF_NONE)
										.setStatus(status == null ? RequestedCourseStatus.OVERRIDE_NEEDED : status);
								}
							}
						
						if (response.hasMessages())
							for (CourseMessage m: response.getMessages()) {
								if (m.getCourse() != null && m.getMessage().indexOf("this section") >= 0)
									m.setMessage(m.getMessage().replace("this section", m.getCourse()));
								if (m.getCourse() != null && m.getMessage().indexOf(" (CRN ") >= 0)
									m.setMessage(m.getMessage().replaceFirst(" \\(CRN [0-9][0-9][0-9][0-9][0-9]\\) ", " "));
							}
						
						
					}
				}
			}
		}
		
		if (response.getConfirms().contains(CONF_BANNER)) {
			response.addConfirmation(ApplicationProperties.getProperty("purdue.specreg.messages.waitlist.bannerProblemsFound", "The following registration errors for the wait-listed courses have been detected:"), CONF_BANNER, -1);
			String note = ApplicationProperties.getProperty("purdue.specreg.messages.waitlist.courseRequestNote", "<b>Request Note:</b>");
			int idx = 1;
			if (note != null && !note.isEmpty()) {
				response.addConfirmation(note, CONF_BANNER, idx++);
				Set<String> courses = new HashSet<String>();
				boolean hasCredit = false;
				for (CourseMessage x: response.getMessages(CONF_BANNER)) {
					if ("WL-CREDIT".equals(x.getCode()) || "MAXI".equals(x.getCode())) { hasCredit = true; continue; }
					if (x.hasCourse() && courses.add(x.getCourse())) {
						CourseMessage cm = response.addConfirmation("", CONF_BANNER, idx++);
						cm.setCourse(x.getCourse()); cm.setCourseId(x.getCourseId());
						cm.setCode("REQUEST_NOTE");
						for (String suggestion: ApplicationProperties.getProperty("purdue.specreg.prereg.requestorNoteSuggestions", "").split("[\r\n]+"))
							if (!suggestion.isEmpty()) cm.addSuggestion(suggestion); 
					}
				}
				if (hasCredit) {
					CourseMessage cm = response.addConfirmation("", CONF_BANNER, idx++);
					cm.setCourse(MESSAGES.tabRequestNoteMaxCredit());
					cm.setCode("REQUEST_NOTE");
					for (String suggestion: ApplicationProperties.getProperty("purdue.specreg.prereg.requestorNoteSuggestions", "").split("[\r\n]+"))
						if (!suggestion.isEmpty()) cm.addSuggestion(suggestion);
				}
			}
			response.addConfirmation(
					ApplicationProperties.getProperty("purdue.specreg.messages.waitlist.requestOverrides",
							"\nIf you have already discussed these courses with your advisor and were advised to request " +
							"registration in them please select Request Overrides. If you aren\u2019t sure, click Cancel Submit and " +
							"consult with your advisor before wait-listing these courses."),
					CONF_BANNER, idx++);
		}
		
		Set<Integer> conf = response.getConfirms();
		if (conf.contains(CONF_BANNER)) {
			response.setConfirmation(CONF_BANNER, ApplicationProperties.getProperty("purdue.specreg.confirm.waitlist.bannerDialogName", "Request Wait-List Overrides"),
					ApplicationProperties.getProperty("purdue.specreg.confirm.waitlist.bannerYesButton", "Request Overrides"),
					ApplicationProperties.getProperty("purdue.specreg.confirm.waitlist.bannerNoButton", "Cancel Submit"),
					ApplicationProperties.getProperty("purdue.specreg.confirm.waitlist.bannerYesButtonTitle", "Request overrides for the above registration errors"),
					ApplicationProperties.getProperty("purdue.specreg.confirm.waitlist.bannerNoButtonTitle", "Go back to Scheduling Assistant"));
		}
	}
	
	@Override
	public void submit(OnlineSectioningServer server, OnlineSectioningHelper helper, CourseRequestInterface request, Float neededCredit) throws SectioningException {
		XStudent original = (request.getStudentId() == null ? null : server.getStudent(request.getStudentId()));
		if (original == null) return;
		// Do not submit when validation is disabled
		if (!isValidationEnabled(server, helper, original)) return;
		
		request.setMaxCreditOverrideStatus(RequestedCourseStatus.SAVED);

		ClientResource resource = null;
		Map<String, Set<String>> overrides = new HashMap<String, Set<String>>();
		Float maxCredit = null;
		Float oldCredit = null;
		try {
			resource = new ClientResource(getSpecialRegistrationApiSiteCheckSpecialRegistrationStatus());
			resource.setNext(iClient);
			
			AcademicSessionInfo session = server.getAcademicSession();
			String term = getBannerTerm(session);
			String campus = getBannerCampus(session);
			resource.addQueryParameter("term", term);
			resource.addQueryParameter("campus", campus);
			resource.addQueryParameter("studentId", getBannerId(original));
			resource.addQueryParameter("mode", getSpecialRegistrationApiMode().name());
			helper.getAction().addOptionBuilder().setKey("term").setValue(term);
			helper.getAction().addOptionBuilder().setKey("campus").setValue(campus);
			helper.getAction().addOptionBuilder().setKey("studentId").setValue(getBannerId(original));
			resource.addQueryParameter("apiKey", getSpecialRegistrationApiKey());
			
			long t1 = System.currentTimeMillis();
			
			resource.get(MediaType.APPLICATION_JSON);
			
			helper.getAction().setApiGetTime(System.currentTimeMillis() - t1);
			
			SpecialRegistrationStatusResponse status = (SpecialRegistrationStatusResponse)new GsonRepresentation<SpecialRegistrationStatusResponse>(resource.getResponseEntity(), SpecialRegistrationStatusResponse.class).getObject();
			Gson gson = getGson(helper);
			
			if (helper.isDebugEnabled())
				helper.debug("Status: " + gson.toJson(status));
			helper.getAction().addOptionBuilder().setKey("wl-status").setValue(gson.toJson(status));
			
			if (status != null && status.data != null) {
				maxCredit = status.data.maxCredit;
				request.setMaxCredit(status.data.maxCredit);
			}
			if (maxCredit == null) maxCredit = Float.parseFloat(ApplicationProperties.getProperty("purdue.specreg.maxCreditDefault", "18"));
			
			if (status != null && status.data != null && status.data.requests != null) {
				for (SpecialRegistration r: status.data.requests) {
					if (r.changes != null)
						for (Change ch: r.changes) {
							if (status(ch.status) == RequestedCourseStatus.OVERRIDE_PENDING && ch.subject != null && ch.courseNbr != null) { 
								String course = ch.subject + " " + ch.courseNbr;
								Set<String> problems = overrides.get(course);
								if (problems == null) {
									problems = new TreeSet<String>();
									overrides.put(course, problems);
								}
								if (ch.errors != null)
									for (ChangeError err: ch.errors) {
										if (err.code != null)
											problems.add(err.code);
									}
							} else if (status(ch.status) == RequestedCourseStatus.OVERRIDE_PENDING && r.maxCredit != null) {
								oldCredit = r.maxCredit;
							}
						}
				}
			}
		} catch (SectioningException e) {
			helper.getAction().setApiException(e.getMessage());
			throw (SectioningException)e;
		} catch (Exception e) {
			helper.getAction().setApiException(e.getMessage());
			sLog.error(e.getMessage(), e);
			throw new SectioningException(e.getMessage());
		} finally {
			if (resource != null) {
				if (resource.getResponse() != null) resource.getResponse().release();
				resource.release();
			}
		}
		
		SpecialRegistrationRequest req = new SpecialRegistrationRequest();
		req.studentId = getBannerId(original);
		req.pgrmcode = SpecialRegistrationHelper.getProgramCode(original);
		req.term = getBannerTerm(server.getAcademicSession());
		req.campus = getBannerCampus(server.getAcademicSession());
		req.mode = getSpecialRegistrationApiMode();
		req.changes = new ArrayList<Change>();
		if (helper.getUser() != null) {
			req.requestorId = getRequestorId(helper.getUser());
			req.requestorRole = getRequestorType(helper.getUser(), original);
		}

		if (request.hasConfirmations()) {
			for (CourseMessage m: request.getConfirmations()) {
				if ("REQUEST_NOTE".equals(m.getCode()) && m.getMessage() != null && !m.getMessage().isEmpty() && !m.hasCourseId()) {
					req.maxCreditRequestorNotes = m.getMessage();
				}
			}
			for (CourseRequestInterface.Request c: request.getCourses())
				if (c.hasRequestedCourse() && c.isWaitList()) {
					for (CourseRequestInterface.RequestedCourse rc: c.getRequestedCourse()) {
						XCourseId cid = server.getCourse(rc.getCourseId(), rc.getCourseName());
						if (cid == null) continue;
						XCourse course = (cid instanceof XCourse ? (XCourse)cid : server.getCourse(cid.getCourseId()));
						if (course == null) continue;
						String subject = course.getSubjectArea();
						String courseNbr = course.getCourseNumber();
						List<ChangeError> errors = new ArrayList<ChangeError>();
						for (CourseMessage m: request.getConfirmations()) {
							if ("WL-CREDIT".equals(m.getCode())) continue;
							if ("NO_ALT".equals(m.getCode())) continue;
							if ("DROP_CRIT".equals(m.getCode())) continue;
							if ("WL-OVERLAP".equals(m.getCode())) continue;
							if ("WL-INACTIVE".equals(m.getCode())) continue;
							if ("NOT-ONLINE".equals(m.getCode())) continue;
							if ("NOT-RESIDENTIAL".equals(m.getCode())) continue;
							if ("REQUEST_NOTE".equals(m.getCode())) continue;
							if (!m.hasCourse()) continue;
							if (!m.isError() && (course.getCourseId().equals(m.getCourseId()) || course.getCourseName().equals(m.getCourse()))) {
								ChangeError e = new ChangeError();
								e.code = m.getCode(); e.message = m.getMessage();
								errors.add(e);
							}
						}
						if (!errors.isEmpty()) {
							Change ch = new Change();
							ch.setCourse(subject, courseNbr, iExternalTermProvider, server.getAcademicSession());
							ch.crn = "";
							ch.errors = errors;
							ch.operation = ChangeOperation.ADD;
							req.changes.add(ch);
							for (CourseMessage m: request.getConfirmations()) {
								if ("REQUEST_NOTE".equals(m.getCode()) && m.getMessage() != null && !m.getMessage().isEmpty() && course.getCourseName().equals(m.getCourse())) {
									ch.requestorNotes = m.getMessage();
								}
							}
							overrides.remove(subject + " " + courseNbr);
						}
					}
				}
		}
		req.courseCreditHrs = new ArrayList<CourseCredit>();
		Float wlCredit = null;
		for (CourseRequestInterface.Request r: request.getCourses()) {
			if (r.hasRequestedCourse() && r.isWaitList()) {
				CourseCredit cc = null;
				Float credit = null;
				for (RequestedCourse rc: r.getRequestedCourse()) {
					XCourseId cid = server.getCourse(rc.getCourseId(), rc.getCourseName());
					if (cid == null) continue;
					XCourse course = (cid instanceof XCourse ? (XCourse)cid : server.getCourse(cid.getCourseId()));
					if (course == null) continue;
					if (cc == null) {
						cc = new CourseCredit();
						cc.setCourse(course.getSubjectArea(), course.getCourseNumber(), iExternalTermProvider, server.getAcademicSession());
						cc.title = course.getTitle();
						cc.creditHrs = (course.hasCredit() ? course.getMinCredit() : 0f);
					} else {
						if (cc.alternatives == null) cc.alternatives = new ArrayList<CourseCredit>();
						CourseCredit acc = new CourseCredit();
						acc.setCourse(course.getSubjectArea(), course.getCourseNumber(), iExternalTermProvider, server.getAcademicSession());
						acc.title = course.getTitle();
						acc.creditHrs = (course.hasCredit() ? course.getMinCredit() : 0f);
						cc.alternatives.add(acc);
					}
					if (rc.hasCredit() && (credit == null || credit < rc.getCreditMin())) credit = rc.getCreditMin();
				}
				if (cc != null) req.courseCreditHrs.add(cc);
				if (credit != null && (wlCredit == null || wlCredit < credit)) wlCredit = credit;
			}
		}
		if (neededCredit != null && maxCredit < neededCredit) {
			req.maxCredit = neededCredit;
		}
		
		if (!req.changes.isEmpty() || !overrides.isEmpty() || req.maxCredit != null || oldCredit != null) {
			resource = null;
			try {
				resource = new ClientResource(getSpecialRegistrationApiSiteSubmitRegistration());
				resource.setNext(iClient);
				resource.addQueryParameter("apiKey", getSpecialRegistrationApiKey());
				
				Gson gson = getGson(helper);
				if (helper.isDebugEnabled())
					helper.debug("Request: " + gson.toJson(req));
				helper.getAction().addOptionBuilder().setKey("wl-request").setValue(gson.toJson(req));
				long t1 = System.currentTimeMillis();
				
				resource.post(new GsonRepresentation<SpecialRegistrationRequest>(req));
				
				helper.getAction().setApiPostTime(System.currentTimeMillis() - t1);
				
				SpecialRegistrationResponseList response = (SpecialRegistrationResponseList)new GsonRepresentation<SpecialRegistrationResponseList>(resource.getResponseEntity(), SpecialRegistrationResponseList.class).getObject();
				if (helper.isDebugEnabled())
					helper.debug("Response: " + gson.toJson(response));
				helper.getAction().addOptionBuilder().setKey("wl-response").setValue(gson.toJson(response));
				
				if (ResponseStatus.success != response.status)
					throw new SectioningException(response.message == null || response.message.isEmpty() ? "Failed to request overrides (" + response.status + ")." : response.message);
				
				if (response.data != null) {
					for (CourseRequestInterface.Request c: request.getCourses())
						if (c.hasRequestedCourse()) {
							for (CourseRequestInterface.RequestedCourse rc: c.getRequestedCourse()) {
								if (rc.getStatus() != null && rc.getStatus() != RequestedCourseStatus.OVERRIDE_REJECTED && rc.getStatus() != RequestedCourseStatus.WAITLIST_INACTIVE
										&& rc.getStatus() != RequestedCourseStatus.ENROLLED) {
									rc.setStatus(null);
									rc.setOverrideExternalId(null);
									rc.setOverrideTimeStamp(null);
								}
								XCourseId cid = server.getCourse(rc.getCourseId(), rc.getCourseName());
								if (cid == null) continue;
								XCourse course = (cid instanceof XCourse ? (XCourse)cid : server.getCourse(cid.getCourseId()));
								if (course == null) continue;
								String subject = course.getSubjectArea();
								String courseNbr = course.getCourseNumber();
								for (SpecialRegistration r: response.data) {
									if (r.changes != null)
										for (Change ch: r.changes) {
											if (subject.equals(ch.subject) && courseNbr.equals(ch.courseNbr)) {
												rc.setOverrideTimeStamp(r.dateCreated == null ? null : r.dateCreated.toDate());
												rc.setOverrideExternalId(r.regRequestId);
												rc.setStatus(status(r, false));
												rc.setStatusNote(SpecialRegistrationHelper.note(r, false));
												rc.setRequestId(r.regRequestId);
												rc.setRequestorNote(SpecialRegistrationHelper.requestorNotes(r, subject, courseNbr));
												break;
											}
										}
								}
							}
						}
					for (CourseRequestInterface.Request c: request.getAlternatives())
						if (c.hasRequestedCourse()) {
							for (CourseRequestInterface.RequestedCourse rc: c.getRequestedCourse()) {
								if (rc.getStatus() != null && rc.getStatus() != RequestedCourseStatus.OVERRIDE_REJECTED && rc.getStatus() != RequestedCourseStatus.WAITLIST_INACTIVE
										&& rc.getStatus() != RequestedCourseStatus.ENROLLED) {
									rc.setStatus(null);
									rc.setOverrideExternalId(null);
									rc.setOverrideTimeStamp(null);
								}
								XCourseId cid = server.getCourse(rc.getCourseId(), rc.getCourseName());
								if (cid == null) continue;
								XCourse course = (cid instanceof XCourse ? (XCourse)cid : server.getCourse(cid.getCourseId()));
								if (course == null) continue;
								String subject = course.getSubjectArea();
								String courseNbr = course.getCourseNumber();
								for (SpecialRegistration r: response.data)
									if (r.changes != null)
									for (Change ch: r.changes) {
										if (subject.equals(ch.subject) && courseNbr.equals(ch.courseNbr)) {
											rc.setOverrideTimeStamp(r.dateCreated == null ? null : r.dateCreated.toDate());
											rc.setOverrideExternalId(r.regRequestId);
											rc.setStatus(status(r, false));
											rc.setStatusNote(SpecialRegistrationHelper.note(r, false));
											break;
										}
									}
							}
						}
					if (req.maxCredit != null) {
						for (SpecialRegistration r: response.data) {
							if (r.maxCredit != null) {
								request.setMaxCreditOverride(r.maxCredit);
								request.setMaxCreditOverrideExternalId(r.regRequestId);
								request.setMaxCreditOverrideTimeStamp(r.dateCreated == null ? null : r.dateCreated.toDate());
								request.setMaxCreditOverrideStatus(status(r, true));
								request.setCreditWarning(
										ApplicationProperties.getProperty("purdue.specreg.messages.maxCredit", "Maximum of {max} credit hours exceeded.")
										.replace("{max}", sCreditFormat.format(maxCredit)).replace("{credit}", sCreditFormat.format(req.maxCredit))
										);
								request.setCreditNote(SpecialRegistrationHelper.note(r, true));
								request.setRequestorNote(SpecialRegistrationHelper.maxCreditRequestorNotes(r));
								request.setRequestId(r.regRequestId);
								break;
							}
						}
					} else {
						request.setMaxCreditOverrideStatus(RequestedCourseStatus.SAVED);
					}
				}
				if (request.hasConfirmations()) {
					for (CourseMessage message: request.getConfirmations()) {
						if (message.getStatus() == RequestedCourseStatus.OVERRIDE_NEEDED)
							message.setStatus(RequestedCourseStatus.OVERRIDE_PENDING);
					}
				}
			} catch (SectioningException e) {
				helper.getAction().setApiException(e.getMessage());
				throw (SectioningException)e;
			} catch (Exception e) {
				helper.getAction().setApiException(e.getMessage());
				sLog.error(e.getMessage(), e);
				throw new SectioningException(e.getMessage());
			} finally {
				if (resource != null) {
					if (resource.getResponse() != null) resource.getResponse().release();
					resource.release();
				}
			}
		} else {
			for (CourseRequestInterface.Request c: request.getCourses())
				if (c.hasRequestedCourse()) {
					for (CourseRequestInterface.RequestedCourse rc: c.getRequestedCourse()) {
						if (rc.getStatus() != null && rc.getStatus() != RequestedCourseStatus.OVERRIDE_REJECTED && rc.getStatus() != RequestedCourseStatus.WAITLIST_INACTIVE
								&& rc.getStatus() != RequestedCourseStatus.ENROLLED) {
							rc.setStatus(null);
							rc.setOverrideExternalId(null);
							rc.setOverrideTimeStamp(null);
						}
					}
				}
			for (CourseRequestInterface.Request c: request.getAlternatives())
				if (c.hasRequestedCourse()) {
					for (CourseRequestInterface.RequestedCourse rc: c.getRequestedCourse()) {
						if (rc.getStatus() != null && rc.getStatus() != RequestedCourseStatus.OVERRIDE_REJECTED && rc.getStatus() != RequestedCourseStatus.WAITLIST_INACTIVE
								&& rc.getStatus() != RequestedCourseStatus.ENROLLED) {
							rc.setStatus(null);
							rc.setOverrideExternalId(null);
							rc.setOverrideTimeStamp(null);
						}
					}
				}
		}
		
	}

	@Override
	public void check(OnlineSectioningServer server, OnlineSectioningHelper helper, CourseRequestInterface request) throws SectioningException {
		XStudent original = (request.getStudentId() == null ? null : server.getStudent(request.getStudentId()));
		if (original == null) return;
		// Do not check when validation is disabled
		if (!isValidationEnabled(server, helper, original)) return;

		Map<String, RequestedCourse> rcs = new HashMap<String, RequestedCourse>();
		for (CourseRequestInterface.Request r: request.getCourses()) {
			if (r.hasRequestedCourse() && r.isWaitList())
				for (RequestedCourse rc: r.getRequestedCourse()) {
					if (rc.getOverrideExternalId() != null)
						rcs.put(rc.getOverrideExternalId(), rc);
					rcs.put(rc.getCourseName(), rc);
					if (rc.getStatus() == RequestedCourseStatus.OVERRIDE_NEEDED && "TBD".equals(rc.getOverrideExternalId())) {
						request.addConfirmationMessage(
								rc.getCourseId(), rc.getCourseName(), "NOT_REQUESTED", 
								ApplicationProperties.getProperty("purdue.specreg.messages.waitlist.notRequested", "Overrides not requested, wait-list inactive."),
								RequestedCourseStatus.OVERRIDE_NEEDED, 1
								);
					}
				}
		}
		
		if (request.getMaxCreditOverrideStatus() == null) {
			request.setMaxCreditOverrideStatus(RequestedCourseStatus.SAVED);
		}
		
		if (rcs.isEmpty() && !request.hasMaxCreditOverride()) return;
		
		Integer ORD_BANNER = 1;
		
		ClientResource resource = null;
		try {
			resource = new ClientResource(getSpecialRegistrationApiSiteCheckSpecialRegistrationStatus());
			resource.setNext(iClient);
			
			AcademicSessionInfo session = server.getAcademicSession();
			String term = getBannerTerm(session);
			String campus = getBannerCampus(session);
			resource.addQueryParameter("term", term);
			resource.addQueryParameter("campus", campus);
			resource.addQueryParameter("studentId", getBannerId(original));
			resource.addQueryParameter("mode", getSpecialRegistrationApiMode().name());
			helper.getAction().addOptionBuilder().setKey("term").setValue(term);
			helper.getAction().addOptionBuilder().setKey("campus").setValue(campus);
			helper.getAction().addOptionBuilder().setKey("studentId").setValue(getBannerId(original));
			resource.addQueryParameter("apiKey", getSpecialRegistrationApiKey());
			
			long t0 = System.currentTimeMillis();
			
			resource.get(MediaType.APPLICATION_JSON);
			
			helper.getAction().setApiGetTime(System.currentTimeMillis() - t0);
			
			SpecialRegistrationStatusResponse status = (SpecialRegistrationStatusResponse)new GsonRepresentation<SpecialRegistrationStatusResponse>(resource.getResponseEntity(), SpecialRegistrationStatusResponse.class).getObject();
			Gson gson = getGson(helper);
			
			if (helper.isDebugEnabled())
				helper.debug("Status: " + gson.toJson(status));
			helper.getAction().addOptionBuilder().setKey("wl-status").setValue(gson.toJson(status));
			
			Float maxCredit = null;
			if (status != null && status.data != null) {
				maxCredit = status.data.maxCredit;
				request.setMaxCredit(status.data.maxCredit);
			}
			if (maxCredit == null) maxCredit = Float.parseFloat(ApplicationProperties.getProperty("purdue.specreg.maxCreditDefault", "18"));

			if (status != null && status.data != null && status.data.requests != null) {
				// Retrieve max credit request
				SpecialRegistration maxCreditReq = null;
				// Try matching request first
				if (request.getMaxCreditOverrideExternalId() != null)
					for (SpecialRegistration r: status.data.requests) {
						if (r.maxCredit != null && request.getMaxCreditOverrideExternalId().equals(r.regRequestId)) {
							maxCreditReq = r; break;
						}
					}
				// Get latest not-cancelled max credit override request otherwise
				if (maxCreditReq == null)
					for (SpecialRegistration r: status.data.requests) {
						if (r.maxCredit != null && status(r, true) != RequestedCourseStatus.OVERRIDE_CANCELLED && (maxCreditReq == null || r.dateCreated.isAfter(maxCreditReq.dateCreated))) {
							maxCreditReq = r;
						}
					}
				
				if (maxCreditReq != null) {
					request.setMaxCreditOverrideExternalId(maxCreditReq.regRequestId);
					request.setMaxCreditOverrideStatus(status(maxCreditReq, true));
					request.setMaxCreditOverride(maxCreditReq.maxCredit);
					request.setMaxCreditOverrideTimeStamp(maxCreditReq.dateCreated == null ? null : maxCreditReq.dateCreated.toDate());
					request.setCreditNote(SpecialRegistrationHelper.note(maxCreditReq, true));
					String warning = null;
					if (maxCreditReq.changes != null)
						for (Change ch: maxCreditReq.changes)
							if (ch.subject == null && ch.courseNbr == null)
								if (ch.errors != null)
									for (ChangeError er: ch.errors)
										if ("MAXI".equals(er.code) && er.message != null)
											warning = (warning == null ? "" : warning + "\n") + er.message;
					request.setCreditWarning(warning);
					request.setRequestorNote(SpecialRegistrationHelper.maxCreditRequestorNotes(maxCreditReq));
					request.setRequestId(maxCreditReq.regRequestId);
					for (String suggestion: ApplicationProperties.getProperty("purdue.specreg.waitlist.requestorNoteSuggestions", "").split("[\r\n]+"))
						if (!suggestion.isEmpty()) request.addRequestorNoteSuggestion(suggestion);
				}
				
				for (SpecialRegistration r: status.data.requests) {
					if (r.regRequestId == null) continue;
					RequestedCourse rc = rcs.get(r.regRequestId);
					if (rc == null) {
						if (r.changes != null)
							for (Change ch: r.changes)
								if (ch.status == ChangeStatus.approved) {
									rc = rcs.get(ch.subject + " " + ch.courseNbr);
									if (rc != null) {
										for (ChangeError er: ch.errors)
											request.addConfirmationMessage(rc.getCourseId(), rc.getCourseName(), er.code, "Approved " + er.message, status(ch.status), ORD_BANNER);
										if (rc.getRequestId() == null) {
											rc.setStatusNote(SpecialRegistrationHelper.note(r, false));
											rc.setRequestorNote(SpecialRegistrationHelper.requestorNotes(r, ch.subject, ch.courseNbr));
											if (rc.getStatus() != RequestedCourseStatus.ENROLLED)
												rc.setStatus(RequestedCourseStatus.OVERRIDE_APPROVED);
										}
									}
								}
						continue;
					}
					if (rc.getStatus() != RequestedCourseStatus.ENROLLED) {
						rc.setStatus(status(r, false));
					}
					if (r.changes != null)
						for (Change ch: r.changes)
							if (ch.errors != null && ch.courseNbr != null && ch.subject != null && ch.status != null)
								for (ChangeError er: ch.errors) {
									if (ch.status == ChangeStatus.denied) {
										request.addConfirmationError(rc.getCourseId(), rc.getCourseName(), er.code, "Denied " + er.message, status(ch.status), ORD_BANNER);
										request.setErrorMessage(ApplicationProperties.getProperty("purdue.specreg.messages.waitlist.deniedOverrideError",
												"One or more wait-listed courses require registration overrides which have been denied.\nYou cannot wait-list these courses."));
									} else if (ch.status == ChangeStatus.approved) {
										request.addConfirmationMessage(rc.getCourseId(), rc.getCourseName(), er.code, "Approved " + er.message, status(ch.status), ORD_BANNER);
									} else {
										request.addConfirmationMessage(rc.getCourseId(), rc.getCourseName(), er.code, er.message, status(ch.status), ORD_BANNER);
									}
								}
					rc.setStatusNote(SpecialRegistrationHelper.note(r, false));
					rc.setRequestorNote(SpecialRegistrationHelper.requestorNotes(r, rc.getCourseName()));
					rc.setRequestId(r.regRequestId);
					for (String suggestion: ApplicationProperties.getProperty("purdue.specreg.waitlist.requestorNoteSuggestions", "").split("[\r\n]+"))
						if (!suggestion.isEmpty()) rc.addRequestorNoteSuggestion(suggestion);
				}
			}
		} catch (SectioningException e) {
			helper.getAction().setApiException(e.getMessage());
			throw (SectioningException)e;
		} catch (Exception e) {
			helper.getAction().setApiException(e.getMessage() == null ? "Null" : e.getMessage());
			sLog.error(e.getMessage(), e);
			throw new SectioningException(e.getMessage());
		} finally {
			if (resource != null) {
				if (resource.getResponse() != null) resource.getResponse().release();
				resource.release();
			}
		}
	}

	@Override
	public void checkEligibility(OnlineSectioningServer server, OnlineSectioningHelper helper, EligibilityCheck check, XStudent student) throws SectioningException {
		if (student == null) return;
		// Do not check eligibility when validation is disabled
		if (!isValidationEnabled(server, helper, student)) return;
		if (!check.hasFlag(EligibilityCheck.EligibilityFlag.CAN_ENROLL)) return;
		ClientResource resource = null;
		try {
			resource = new ClientResource(getSpecialRegistrationApiSiteCheckEligibility());
			resource.setNext(iClient);
			
			AcademicSessionInfo session = server.getAcademicSession();
			String term = getBannerTerm(session);
			String campus = getBannerCampus(session);
			resource.addQueryParameter("term", term);
			resource.addQueryParameter("campus", campus);
			resource.addQueryParameter("studentId", getBannerId(student));
			resource.addQueryParameter("mode", getSpecialRegistrationApiMode().name());
			helper.getAction().addOptionBuilder().setKey("term").setValue(term);
			helper.getAction().addOptionBuilder().setKey("campus").setValue(campus);
			helper.getAction().addOptionBuilder().setKey("studentId").setValue(getBannerId(student));
			resource.addQueryParameter("apiKey", getSpecialRegistrationApiKey());
			
			long t0 = System.currentTimeMillis();
			
			resource.get(MediaType.APPLICATION_JSON);
			
			helper.getAction().setApiGetTime(
					(helper.getAction().hasApiGetTime() ? helper.getAction().getApiGetTime() : 0l) +
					System.currentTimeMillis() - t0);
			
			CheckEligibilityResponse eligibility = (CheckEligibilityResponse)new GsonRepresentation<CheckEligibilityResponse>(resource.getResponseEntity(), CheckEligibilityResponse.class).getObject();
			Gson gson = getGson(helper);
			
			if (helper.isDebugEnabled())
				helper.debug("Eligibility: " + gson.toJson(eligibility));
			helper.getAction().addOptionBuilder().setKey("wl-eligibility").setValue(gson.toJson(eligibility));
			
			if (ResponseStatus.success != eligibility.status)
				throw new SectioningException(eligibility.message == null || eligibility.message.isEmpty() ? "Failed to check wait-list eligibility (" + eligibility.status + ")." : eligibility.message);
			
			if (eligibility.data != null && eligibility.data.eligible != null && eligibility.data.eligible.booleanValue()) {
				check.setFlag(EligibilityFlag.WAIT_LIST_VALIDATION, true);
			}
			if (eligibility.data != null && eligibility.data.eligibilityProblems != null) {
				String m = null;
				for (EligibilityProblem p: eligibility.data.eligibilityProblems)
					if (m == null)
						m = p.message;
					else
						m += "\n" + p.message;
				if (m != null)
					check.setMessage(MESSAGES.exceptionFailedEligibilityCheck(m));
			}
		} catch (SectioningException e) {
			helper.getAction().setApiException(e.getMessage());
			throw (SectioningException)e;
		} catch (Exception e) {
			helper.getAction().setApiException(e.getMessage() == null ? "Null" : e.getMessage());
			sLog.error(e.getMessage(), e);
			throw new SectioningException(e.getMessage());
		} finally {
			if (resource != null) {
				if (resource.getResponse() != null) resource.getResponse().release();
				resource.release();
			}
		}
	}

	protected boolean hasOverride(org.unitime.timetable.model.Student student) {
		if (student.getOverrideExternalId() != null) return true;
		if (student.getMaxCredit() == null) return true;
		for (CourseDemand cd: student.getCourseDemands()) {
			for (org.unitime.timetable.model.CourseRequest cr: cd.getCourseRequests()) {
				if (cr.getOverrideExternalId() != null && ((cr.getCourseRequestOverrideIntent() == CourseRequestOverrideIntent.WAITLIST) ||
						(Boolean.TRUE.equals(cd.isWaitlist()) && Boolean.FALSE.equals(cd.isAlternative()) && !cd.isEnrolledExceptForWaitListSwap())))
					return true;
			}
		}
		if (student.getOverrideExternalId() != null && student.getMaxCreditOverrideIntent() == CourseRequestOverrideIntent.WAITLIST)
			return true;
		return false;
	}

	public boolean updateStudent(OnlineSectioningServer server, OnlineSectioningHelper helper, Student student, Builder action) throws SectioningException {
		// No pending overrides -> nothing to do
		if (student == null || !hasOverride(student)) return false;
		
		ClientResource resource = null;
		try {
			resource = new ClientResource(getSpecialRegistrationApiSiteCheckSpecialRegistrationStatus());
			resource.setNext(iClient);
			
			AcademicSessionInfo session = (server == null ? new AcademicSessionInfo(student.getSession()) : server.getAcademicSession());
			String term = getBannerTerm(session);
			String campus = getBannerCampus(session);
			resource.addQueryParameter("term", term);
			resource.addQueryParameter("campus", campus);
			resource.addQueryParameter("studentId", getBannerId(student));
			resource.addQueryParameter("mode", getSpecialRegistrationApiMode().name());
			action.addOptionBuilder().setKey("term").setValue(term);
			action.addOptionBuilder().setKey("campus").setValue(campus);
			action.addOptionBuilder().setKey("studentId").setValue(getBannerId(student));
			resource.addQueryParameter("apiKey", getSpecialRegistrationApiKey());
			
			long t0 = System.currentTimeMillis();
			
			resource.get(MediaType.APPLICATION_JSON);
			
			action.setApiGetTime(System.currentTimeMillis() - t0);
			
			SpecialRegistrationStatusResponse status = (SpecialRegistrationStatusResponse)new GsonRepresentation<SpecialRegistrationStatusResponse>(resource.getResponseEntity(), SpecialRegistrationStatusResponse.class).getObject();
			Gson gson = getGson(helper);
			
			if (helper.isDebugEnabled())
				helper.debug("Status: " + gson.toJson(status));
			action.addOptionBuilder().setKey("wl-status").setValue(gson.toJson(status));
			
			boolean changed = false;
			for (CourseDemand cd: student.getCourseDemands()) {
					for (org.unitime.timetable.model.CourseRequest cr: cd.getCourseRequests()) {
						if (cr.getOverrideExternalId() != null && !"TBD".equals(cr.getOverrideExternalId()) &&
								((cr.getCourseRequestOverrideIntent() == CourseRequestOverrideIntent.WAITLIST) ||
								(Boolean.TRUE.equals(cd.isWaitlist()) && Boolean.FALSE.equals(cd.isAlternative()) && !cd.isEnrolledExceptForWaitListSwap()))) {
							SpecialRegistration req = null;
							for (SpecialRegistration r: status.data.requests) {
								if (cr.getOverrideExternalId().equals(r.regRequestId)) { req = r; break; }
							}
							if (req == null) {
								if (cr.getCourseRequestOverrideStatus() != CourseRequestOverrideStatus.CANCELLED) {
									cr.setCourseRequestOverrideStatus(CourseRequestOverrideStatus.CANCELLED);
									helper.getHibSession().update(cr);
									changed = true;
								}
							} else {
								Integer oldStatus = cr.getOverrideStatus();
								switch (status(req, false)) {
								case OVERRIDE_REJECTED: 
									cr.setCourseRequestOverrideStatus(CourseRequestOverrideStatus.REJECTED);
									break;
								case OVERRIDE_APPROVED:
									cr.setCourseRequestOverrideStatus(CourseRequestOverrideStatus.APPROVED);
									break;
								case OVERRIDE_CANCELLED:
									cr.setCourseRequestOverrideStatus(CourseRequestOverrideStatus.CANCELLED);
									break;
								case OVERRIDE_PENDING:
									cr.setCourseRequestOverrideStatus(CourseRequestOverrideStatus.PENDING);
									break;
								}
								if (oldStatus == null || !oldStatus.equals(cr.getOverrideStatus())) {
									helper.getHibSession().update(cr);
									changed = true;
								}
							}
						}
					}
			}
			
			boolean studentChanged = false;
			if (status.data.maxCredit != null && !status.data.maxCredit.equals(student.getMaxCredit())) {
				student.setMaxCredit(status.data.maxCredit);
				studentChanged = true;
			}
			if (student.getOverrideExternalId() != null) {
				SpecialRegistration req = null;
				for (SpecialRegistration r: status.data.requests) {
					if (student.getOverrideExternalId().equals(r.regRequestId)) { req = r; break; }
				}
				if (req == null && student.getMaxCreditOverrideIntent() == CourseRequestOverrideIntent.WAITLIST) {
					student.setOverrideExternalId(null);
					student.setOverrideMaxCredit(null);
					student.setOverrideStatus(null);
					student.setOverrideTimeStamp(null);
					student.setOverrideIntent(null);
					studentChanged = true;
				} else if (req != null) {
					Integer oldStatus = student.getOverrideStatus();
					switch (status(req, true)) {
					case OVERRIDE_REJECTED: 
						student.setMaxCreditOverrideStatus(CourseRequestOverrideStatus.REJECTED);
						break;
					case OVERRIDE_APPROVED:
						student.setMaxCreditOverrideStatus(CourseRequestOverrideStatus.APPROVED);
						break;
					case OVERRIDE_CANCELLED:
						student.setMaxCreditOverrideStatus(CourseRequestOverrideStatus.CANCELLED);
						break;
					case OVERRIDE_PENDING:
						student.setMaxCreditOverrideStatus(CourseRequestOverrideStatus.PENDING);
						break;
					}
					if (oldStatus == null || !oldStatus.equals(student.getOverrideStatus()))
						studentChanged = true;
				}
			}
			if (studentChanged) helper.getHibSession().update(student);
			
			if (changed || studentChanged) helper.getHibSession().flush();
						
			return changed || studentChanged;
		} catch (SectioningException e) {
			action.setApiException(e.getMessage());
			throw (SectioningException)e;
		} catch (Exception e) {
			action.setApiException(e.getMessage() == null ? "Null" : e.getMessage());
			sLog.error(e.getMessage(), e);
			throw new SectioningException(e.getMessage());
		} finally {
			if (resource != null) {
				if (resource.getResponse() != null) resource.getResponse().release();
				resource.release();
			}
		}
	}
	
	protected boolean hasNotApprovedCourseRequestOverride(org.unitime.timetable.model.Student student) {
		for (CourseDemand cd: student.getCourseDemands()) {
			for (org.unitime.timetable.model.CourseRequest cr: cd.getCourseRequests()) {
				if (cr.getOverrideExternalId() != null && cr.getCourseRequestOverrideStatus() != CourseRequestOverrideStatus.APPROVED &&
						((cr.getCourseRequestOverrideIntent() == CourseRequestOverrideIntent.WAITLIST) ||
						(Boolean.TRUE.equals(cd.isWaitlist()) && Boolean.FALSE.equals(cd.isAlternative()) && !cd.isEnrolledExceptForWaitListSwap())))
					return true;
			}
		}
		if (student.getOverrideExternalId() != null && student.getMaxCreditOverrideIntent() == CourseRequestOverrideIntent.WAITLIST && student.getMaxCreditOverrideStatus() != CourseRequestOverrideStatus.APPROVED)
			return true;
		return false;
	}


	@Override
	public boolean revalidateStudent(OnlineSectioningServer server, OnlineSectioningHelper helper, Student student, Builder action) throws SectioningException {
		// Do not re-validate when validation is disabled
		if (!isValidationEnabled(student)) return false;

		// When there is a pending override, try to update student first
		boolean studentUpdated = false;
		if (hasOverride(student))
			studentUpdated = updateStudent(server, helper, student, action);

		// All course requests are approved -> nothing to do
		if (!hasNotApprovedCourseRequestOverride(student) && !"true".equalsIgnoreCase(ApplicationProperties.getProperty("purdue.specreg.forceRevalidation", "false"))) return false;
		
		XStudent original = server.getStudent(student.getUniqueId());
		if (original == null) return false;
		
		boolean changed = false;
		
		SpecialRegistrationRequest submitRequest = new SpecialRegistrationRequest();
		submitRequest.studentId = getBannerId(original);
		submitRequest.pgrmcode = SpecialRegistrationHelper.getProgramCode(original);
		submitRequest.term = getBannerTerm(server.getAcademicSession());
		submitRequest.campus = getBannerCampus(server.getAcademicSession());
		submitRequest.mode = getSpecialRegistrationApiMode();
		submitRequest.changes = new ArrayList<Change>();
		if (helper.getUser() != null) {
			submitRequest.requestorId = getRequestorId(helper.getUser());
			submitRequest.requestorRole = getRequestorType(helper.getUser(), original);
		}
		
		Float maxCredit = null;
		Float maxCreditNeeded = null;
		for (CourseDemand cd: student.getCourseDemands()) {
			XCourseId dropCourse = null;
			Set<String> dropCrns = null;
			if (cd.getWaitListSwapWithCourseOffering() != null) {
				dropCourse = new XCourseId(cd.getWaitListSwapWithCourseOffering());
				dropCrns = new TreeSet<String>();
				for (StudentClassEnrollment enrl: student.getClassEnrollments()) {
					if (cd.getWaitListSwapWithCourseOffering().equals(enrl.getCourseOffering())) {
						dropCrns.add(enrl.getClazz().getExternalId(cd.getWaitListSwapWithCourseOffering()));
					}
				}
			}
			
			if (Boolean.TRUE.equals(cd.isWaitlist()) && Boolean.FALSE.equals(cd.isAlternative()) && !cd.isEnrolledExceptForWaitListSwap()) {
				XCourseId enrolledCourse = null;
				Integer enrolledOrder = null;
				if (dropCourse != null && !dropCrns.isEmpty()) {
					for (org.unitime.timetable.model.CourseRequest cr: cd.getCourseRequests())
						if (cr.getCourseOffering().getUniqueId().equals(dropCourse.getCourseId())) {
							enrolledCourse = dropCourse;
	    					enrolledOrder = cr.getOrder();
						}
				}
					
				for (org.unitime.timetable.model.CourseRequest cr: cd.getCourseRequests()) {
					// skip courses that cannot be wait-listed
					if (!cr.getCourseOffering().getInstructionalOffering().effectiveWaitList()) continue;
					
					// skip cases where the wait-list request was cancelled
					if ("TBD".equals(cr.getOverrideExternalId())) continue;

					// when enrolled (section swap), check if active
					if (enrolledCourse != null) {
						if (cr.getOrder() > enrolledOrder) continue;
						if (cr.getOrder() == enrolledOrder) {
							XCourseRequest rq = original.getRequestForCourse(enrolledCourse.getCourseId());
							XOffering offering = server.getOffering(enrolledCourse.getOfferingId());
							// when enrolled, skip the enrolled course if the enrollment matches the requirements
							if (rq != null && rq.getEnrollment() != null && offering != null && rq.isRequired(rq.getEnrollment(), offering)) continue;
						}
					}
					
					// get possible enrollments into the course
					Assignment<Request, Enrollment> assignment = new AssignmentMap<Request, Enrollment>();
					CourseRequest courseRequest = SectioningRequest.convert(assignment, new XCourseRequest(cr, helper, null), dropCourse, server, WaitListMode.WaitList);
					Collection<Enrollment> enrls = courseRequest.getEnrollmentsSkipSameTime(assignment);
					
					// get a test enrollment (preferably a non-conflicting one)
					Enrollment testEnrollment = null;
					for (Iterator<Enrollment> e = enrls.iterator(); e.hasNext();) {
						testEnrollment = e.next();
						boolean overlaps = false;
						for (Request q: testEnrollment.getStudent().getRequests()) {
							if (q.equals(courseRequest)) continue;
							Enrollment x = assignment.getValue(q);
							if (x == null || x.getAssignments() == null || x.getAssignments().isEmpty()) continue;
							for (Iterator<SctAssignment> i = x.getAssignments().iterator(); i.hasNext();) {
					        	SctAssignment a = i.next();
								if (a.isOverlapping(testEnrollment.getAssignments())) {
									overlaps = true;
								}
					        }
						}
						if (!overlaps) break;
					}
					// no test enrollment, take first possible enrollment
					if (testEnrollment == null) {
						Course c = courseRequest.getCourses().get(0);
						for (Config config: c.getOffering().getConfigs()) {
							if (courseRequest.isNotAllowed(c, config)) continue;
							testEnrollment = firstEnrollment(courseRequest, assignment, c, config, new HashSet<Section>(), 0);
						}
					}
					// still no test enrollment -> ignore
					if (testEnrollment == null) continue;
					
					// create request
					CheckRestrictionsRequest req = new CheckRestrictionsRequest();
					req.studentId = getBannerId(original);
					req.term = getBannerTerm(server.getAcademicSession());
					req.campus = getBannerCampus(server.getAcademicSession());
					req.mode = getSpecialRegistrationApiMode();
					
					Set<String> crns = new HashSet<String>();
					Set<String> keep = new HashSet<String>();
					for (Section section: testEnrollment.getSections()) {
						String crn = getCRN(section, testEnrollment.getCourse());
						if (dropCrns != null && dropCrns.contains(crn)) {
							keep.add(crn);
						} else {
							SpecialRegistrationHelper.addWaitListCrn(req, crn);
							crns.add(crn);
						}
					}
					if (dropCrns != null)
						for (String crn: dropCrns) {
							if (!keep.contains(crn)) {
								SpecialRegistrationHelper.dropWaitListCrn(req, crn);
								crns.add(crn);
							}
						}
					// no CRNs to check -> continue
					if (crns.isEmpty()) continue;
					
					// call validation
					CheckRestrictionsResponse validation = null;
					ClientResource resource = null;
					try {
						resource = new ClientResource(getSpecialRegistrationApiValidationSite());
						resource.setNext(iClient);
						resource.addQueryParameter("apiKey", getSpecialRegistrationApiKey());
						
						Gson gson = getGson(helper);
						if (helper.isDebugEnabled())
							helper.debug("Request: " + gson.toJson(req));
						action.addOptionBuilder().setKey("wl-req-" + testEnrollment.getCourse().getName().replace(" ", "").toLowerCase()).setValue(gson.toJson(req));
						long t1 = System.currentTimeMillis();
						
						resource.post(new GsonRepresentation<CheckRestrictionsRequest>(req));
						
						action.setApiPostTime(
								(action.hasApiPostTime() ? action.getApiPostTime() : 0) + 
								System.currentTimeMillis() - t1);
						
						validation = (CheckRestrictionsResponse)new GsonRepresentation<CheckRestrictionsResponse>(resource.getResponseEntity(), CheckRestrictionsResponse.class).getObject();
						if (helper.isDebugEnabled())
							helper.debug("Response: " + gson.toJson(validation));
						action.addOptionBuilder().setKey("wl-resp-" + testEnrollment.getCourse().getName().replace(" ", "").toLowerCase()).setValue(gson.toJson(validation));
						
						if (ResponseStatus.success != validation.status)
							throw new SectioningException(validation.message == null || validation.message.isEmpty() ? "Failed to check student eligibility (" + validation.status + ")." : validation.message);
					} catch (SectioningException e) {
						action.setApiException(e.getMessage());
						throw (SectioningException)e;
					} catch (Exception e) {
						action.setApiException(e.getMessage());
						sLog.error(e.getMessage(), e);
						throw new SectioningException(e.getMessage());
					} finally {
						if (resource != null) {
							if (resource.getResponse() != null) resource.getResponse().release();
							resource.release();
						}
					}
					
					if (validation.outJson != null && validation.outJson.problems != null)
						problems: for (Problem problem: validation.outJson.problems) {
							if ("HOLD".equals(problem.code)) continue;
							if ("DUPL".equals(problem.code)) continue;
							if ("MAXI".equals(problem.code)) continue;
							if ("CLOS".equals(problem.code)) continue;
							if ("TIME".equals(problem.code)) continue;
							if (!crns.contains(problem.crn)) continue;
							Change change = null;
							for (Change ch: submitRequest.changes) {
								if (ch.subject.equals(cr.getCourseOffering().getSubjectAreaAbbv()) && ch.courseNbr.equals(cr.getCourseOffering().getCourseNbr())) { change = ch; break; }
							}
							if (change == null) {
								change = new Change();
								change.setCourse(cr.getCourseOffering().getSubjectAreaAbbv(), cr.getCourseOffering().getCourseNbr(), iExternalTermProvider, server.getAcademicSession());
								change.crn = "";
								change.errors = new ArrayList<ChangeError>();
								change.operation = ChangeOperation.ADD;
								submitRequest.changes.add(change);
							}  else {
								for (ChangeError err: change.errors)
									if (problem.code.equals(err.code)) continue problems;
							}
							ChangeError err = new ChangeError();
							err.code = problem.code;
							err.message = problem.message;
							if (err.message != null && err.message.indexOf("this section") >= 0)
								err.message = err.message.replace("this section", cr.getCourseOffering().getCourseName());
							if (err.message != null && err.message.indexOf(" (CRN ") >= 0)
								err.message = err.message.replaceFirst(" \\(CRN [0-9][0-9][0-9][0-9][0-9]\\) ", " ");
							change.errors.add(err);
						}
					
					if (maxCredit == null && validation.maxCredit != null)
						maxCredit = validation.maxCredit;
					
					if (validation.outJson.maxHoursCalc != null) {
						if (maxCreditNeeded == null || maxCreditNeeded < validation.outJson.maxHoursCalc)
							maxCreditNeeded = validation.outJson.maxHoursCalc;
					}
				}
			}
		}
		
		if (maxCredit == null) maxCredit = Float.parseFloat(ApplicationProperties.getProperty("purdue.specreg.maxCreditDefault", "18"));
		if (maxCreditNeeded != null && maxCreditNeeded > maxCredit)
			submitRequest.maxCredit = maxCreditNeeded;
		
		submitRequest.courseCreditHrs = new ArrayList<CourseCredit>();
		for (XRequest r: original.getRequests()) {
			CourseCredit cc = null;
			if (r instanceof XCourseRequest) {
				XCourseRequest cr = (XCourseRequest)r;
				if (!cr.isWaitlist() || cr.getEnrollment() != null) continue;
				for (XCourseId cid: cr.getCourseIds()) {
					XCourse course = (cid instanceof XCourse ? (XCourse)cid : server.getCourse(cid.getCourseId()));
					if (course == null) continue;
					if (cc == null) {
						cc = new CourseCredit();
						cc.setCourse(course.getSubjectArea(), course.getCourseNumber(), iExternalTermProvider, server.getAcademicSession());
						cc.title = course.getTitle();
						cc.creditHrs = (course.hasCredit() ? course.getMinCredit() : 0f);
					} else {
						if (cc.alternatives == null) cc.alternatives = new ArrayList<CourseCredit>();
						CourseCredit acc = new CourseCredit();
						acc.setCourse(course.getSubjectArea(), course.getCourseNumber(), iExternalTermProvider, server.getAcademicSession());
						acc.title = course.getTitle();
						acc.creditHrs = (course.hasCredit() ? course.getMinCredit() : 0f);
						cc.alternatives.add(acc);
					}
				}
			}
			if (cc != null)
				submitRequest.courseCreditHrs.add(cc);
		}
		
		SpecialRegistrationResponseList response = null;
		ClientResource resource = null;
		try {
			resource = new ClientResource(getSpecialRegistrationApiSiteSubmitRegistration());
			resource.setNext(iClient);
			resource.addQueryParameter("apiKey", getSpecialRegistrationApiKey());
			
			Gson gson = getGson(helper);
			if (helper.isDebugEnabled())
				helper.debug("Submit Request: " + gson.toJson(submitRequest));
			action.addOptionBuilder().setKey("wl-request").setValue(gson.toJson(submitRequest));
			long t1 = System.currentTimeMillis();
			
			resource.post(new GsonRepresentation<SpecialRegistrationRequest>(submitRequest));
			
			action.setApiPostTime(action.getApiPostTime() + System.currentTimeMillis() - t1);
			
			response = (SpecialRegistrationResponseList)new GsonRepresentation<SpecialRegistrationResponseList>(resource.getResponseEntity(), SpecialRegistrationResponseList.class).getObject();
			if (helper.isDebugEnabled())
				helper.debug("Submit Response: " + gson.toJson(response));
			action.addOptionBuilder().setKey("wl-response").setValue(gson.toJson(response));
			
			if (ResponseStatus.success != response.status)
				throw new SectioningException(response.message == null || response.message.isEmpty() ? "Failed to request overrides (" + response.status + ")." : response.message);
		} catch (SectioningException e) {
			action.setApiException(e.getMessage());
			throw (SectioningException)e;
		} catch (Exception e) {
			action.setApiException(e.getMessage());
			sLog.error(e.getMessage(), e);
			throw new SectioningException(e.getMessage());
		} finally {
			if (resource != null) {
				if (resource.getResponse() != null) resource.getResponse().release();
				resource.release();
			}
		}
		
		for (CourseDemand cd: student.getCourseDemands()) {
			cr: for (org.unitime.timetable.model.CourseRequest cr: cd.getCourseRequests()) {
				if ((cr.getOverrideExternalId() != null && cr.getCourseRequestOverrideIntent() == CourseRequestOverrideIntent.WAITLIST) ||
					(Boolean.TRUE.equals(cd.isWaitlist()) && Boolean.FALSE.equals(cd.isAlternative()) && !cd.isEnrolledExceptForWaitListSwap())) {
					if (response != null && response.data != null) {
						for (SpecialRegistration r: response.data)
							if (r.changes != null)
								for (Change ch: r.changes) {
									if (cr.getCourseOffering().getSubjectAreaAbbv().equals(ch.subject) && cr.getCourseOffering().getCourseNbr().equals(ch.courseNbr)) {
										Integer oldStatus = cr.getOverrideStatus();
										switch (status(r, false)) {
										case OVERRIDE_REJECTED: 
											cr.setCourseRequestOverrideStatus(CourseRequestOverrideStatus.REJECTED);
											break;
										case OVERRIDE_APPROVED:
											cr.setCourseRequestOverrideStatus(CourseRequestOverrideStatus.APPROVED);
											break;
										case OVERRIDE_CANCELLED:
											cr.setCourseRequestOverrideStatus(CourseRequestOverrideStatus.CANCELLED);
											break;
										case OVERRIDE_PENDING:
											cr.setCourseRequestOverrideStatus(CourseRequestOverrideStatus.PENDING);
											break;
										}
										if (oldStatus == null || !oldStatus.equals(cr.getOverrideStatus()))
											changed = true;
										if (cr.getOverrideExternalId() == null || !cr.getOverrideExternalId().equals(r.regRequestId))
											changed = true;
										cr.setOverrideExternalId(r.regRequestId);
										cr.setOverrideTimeStamp(r.dateCreated == null ? null : r.dateCreated.toDate());
										cr.setCourseRequestOverrideIntent(CourseRequestOverrideIntent.WAITLIST);
										helper.getHibSession().update(cr);
										continue cr;
									}
								}
					}
					if ((cr.getOverrideExternalId() != null || cr.getOverrideStatus() != null) && 
						(!"TBD".equals(cr.getOverrideExternalId()) || !Boolean.TRUE.equals(cd.isWaitlist()) || Boolean.TRUE.equals(cd.isAlternative()))) {
						cr.setOverrideExternalId(null);
						cr.setOverrideStatus(null);
						cr.setOverrideTimeStamp(null);
						cr.setOverrideIntent(null);
						helper.getHibSession().update(cr);
						changed = true;
					}
				}
			}
		}
		
		boolean studentChanged = false;
		if (submitRequest.maxCredit != null) {
			for (SpecialRegistration r: response.data) {
				if (r.maxCredit != null) {
					Integer oldStatus = student.getOverrideStatus();
					switch (status(r, true)) {
					case OVERRIDE_REJECTED: 
						student.setMaxCreditOverrideStatus(CourseRequestOverrideStatus.REJECTED);
						break;
					case OVERRIDE_APPROVED:
						student.setMaxCreditOverrideStatus(CourseRequestOverrideStatus.APPROVED);
						break;
					case OVERRIDE_CANCELLED:
						student.setMaxCreditOverrideStatus(CourseRequestOverrideStatus.CANCELLED);
						break;
					case OVERRIDE_PENDING:
						student.setMaxCreditOverrideStatus(CourseRequestOverrideStatus.PENDING);
						break;
					}
					if (oldStatus == null || !oldStatus.equals(student.getOverrideStatus()))
						studentChanged = true;
					if (student.getOverrideMaxCredit() == null || !student.getOverrideMaxCredit().equals(r.maxCredit))
						studentChanged = true;
					student.setOverrideMaxCredit(r.maxCredit);
					if (student.getOverrideExternalId() == null || !student.getOverrideExternalId().equals(r.regRequestId))
						studentChanged = true;
					student.setOverrideExternalId(r.regRequestId);
					student.setOverrideTimeStamp(r.dateCreated == null ? null : r.dateCreated.toDate());
					student.setMaxCreditOverrideIntent(CourseRequestOverrideIntent.WAITLIST);
					break;
				}
			}
		} else if (student.getMaxCreditOverrideIntent() == CourseRequestOverrideIntent.WAITLIST) {
			student.setOverrideExternalId(null);
			student.setOverrideMaxCredit(null);
			student.setOverrideStatus(null);
			student.setOverrideTimeStamp(null);
			student.setOverrideIntent(null);
			studentChanged = true;
		}
		if (studentChanged) helper.getHibSession().update(student);
		
		if (changed) helper.getHibSession().flush();
					
		if (changed || studentChanged) helper.getHibSession().flush();
		
		return changed || studentChanged || studentUpdated;
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
	public Collection<Long> updateStudents(OnlineSectioningServer server, OnlineSectioningHelper helper, List<Student> students) throws SectioningException {
		Map<String, org.unitime.timetable.model.Student> id2student = new HashMap<String, org.unitime.timetable.model.Student>();
		List<Long> reloadIds = new ArrayList<Long>();
		int batchNumber = 1;
		for (int i = 0; i < students.size(); i++) {
			org.unitime.timetable.model.Student student = students.get(i);
			if (student == null || !hasOverride(student)) continue;
			if (!isValidationEnabled(student)) continue;
			String id = getBannerId(student);
			id2student.put(id, student);
			if (id2student.size() >= 100) {
				checkStudentStatuses(server, helper, id2student, reloadIds, batchNumber++);
				id2student.clear();
			}
		}
		if (!id2student.isEmpty())
			checkStudentStatuses(server, helper, id2student, reloadIds, batchNumber++);
		if (!reloadIds.isEmpty())
			helper.getHibSession().flush();
		if (!reloadIds.isEmpty() && server != null && !(server instanceof DatabaseServer))
			server.execute(server.createAction(ReloadStudent.class).forStudents(reloadIds), helper.getUser());
		return reloadIds;
	}
	
	protected void checkStudentStatuses(OnlineSectioningServer server, OnlineSectioningHelper helper, Map<String, org.unitime.timetable.model.Student> id2student, List<Long> reloadIds, int batchNumber) throws SectioningException {
		ClientResource resource = null;
		try {
			resource = new ClientResource(getSpecialRegistrationApiSiteCheckAllSpecialRegistrationStatus());
			resource.setNext(iClient);
			
			AcademicSessionInfo session = (server == null ? null : server.getAcademicSession());
			String studentIds = null;
			List<String> ids = new ArrayList<String>();
			for (Map.Entry<String, org.unitime.timetable.model.Student> e: id2student.entrySet()) {
				if (session == null) session = new AcademicSessionInfo(e.getValue().getSession());
				if (studentIds == null) studentIds = e.getKey();
				else studentIds += "," + e.getKey();
				ids.add(e.getKey());
			}
			String term = getBannerTerm(session);
			String campus = getBannerCampus(session);
			resource.addQueryParameter("term", term);
			resource.addQueryParameter("campus", campus);
			resource.addQueryParameter("studentIds", studentIds);
			resource.addQueryParameter("mode", getSpecialRegistrationApiMode().name());
			resource.addQueryParameter("apiKey", getSpecialRegistrationApiKey());
			OnlineSectioningLog.Action.Builder action = helper.getAction();
			if (action != null) {
				action.addOptionBuilder().setKey("term").setValue(term);
				action.addOptionBuilder().setKey("campus").setValue(campus);
				action.addOptionBuilder().setKey("studentIds-" + batchNumber).setValue(studentIds);
			}
			
			long t0 = System.currentTimeMillis();
			
			resource.get(MediaType.APPLICATION_JSON);
			
			if (action != null) action.setApiGetTime(action.getApiGetTime() + System.currentTimeMillis() - t0);
			
			SpecialRegistrationMultipleStatusResponse response = (SpecialRegistrationMultipleStatusResponse)new GsonRepresentation<SpecialRegistrationMultipleStatusResponse>(resource.getResponseEntity(), SpecialRegistrationMultipleStatusResponse.class).getObject();
			Gson gson = getGson(helper);
			
			if (helper.isDebugEnabled())
				helper.debug("Response: " + gson.toJson(response));
			if (action != null) action.addOptionBuilder().setKey("wl-response-" + batchNumber).setValue(gson.toJson(response));
			
			if (ResponseStatus.success != response.status)
				throw new SectioningException(response.message == null || response.message.isEmpty() ? "Failed to check student statuses (" + response.status + ")." : response.message);
			
			if (response.data != null && response.data.students != null) {
				int index = 0;
				for (SpecialRegistrationStatus status: response.data.students) {
					String studentId = status.studentId;
					if (studentId == null && status.requests != null)
						for (SpecialRegistration req: status.requests) {
							if (req.studentId != null) { studentId = req.studentId; break; }
						}
					if (studentId == null) studentId = ids.get(index);
					index++;
					org.unitime.timetable.model.Student student = id2student.get(studentId);
					if (student == null) continue;
					
					boolean changed = false;
					for (CourseDemand cd: student.getCourseDemands()) {
						for (org.unitime.timetable.model.CourseRequest cr: cd.getCourseRequests()) {
							if (cr.getOverrideExternalId() != null && (cr.getCourseRequestOverrideIntent() == CourseRequestOverrideIntent.WAITLIST) ||
									(Boolean.TRUE.equals(cd.isWaitlist()) && Boolean.FALSE.equals(cd.isAlternative()) && !cd.isEnrolledExceptForWaitListSwap())) {
								SpecialRegistration req = null;
								for (SpecialRegistration r: status.requests) {
									if (cr.getOverrideExternalId().equals(r.regRequestId)) { req = r; break; }
								}
								if (req == null) {
									if (cr.getCourseRequestOverrideStatus() != CourseRequestOverrideStatus.CANCELLED) {
										cr.setCourseRequestOverrideStatus(CourseRequestOverrideStatus.CANCELLED);
										helper.getHibSession().update(cr);
										changed = true;
									}
								} else {
									Integer oldStatus = cr.getOverrideStatus();
									switch (status(req, false)) {
									case OVERRIDE_REJECTED: 
										cr.setCourseRequestOverrideStatus(CourseRequestOverrideStatus.REJECTED);
										break;
									case OVERRIDE_APPROVED:
										cr.setCourseRequestOverrideStatus(CourseRequestOverrideStatus.APPROVED);
										break;
									case OVERRIDE_CANCELLED:
										cr.setCourseRequestOverrideStatus(CourseRequestOverrideStatus.CANCELLED);
										break;
									case OVERRIDE_PENDING:
										cr.setCourseRequestOverrideStatus(CourseRequestOverrideStatus.PENDING);
										break;
									}
									if (oldStatus == null || !oldStatus.equals(cr.getOverrideStatus())) {
										helper.getHibSession().update(cr);
										changed = true;
									}
								}
							}
						}
					}
					
					boolean studentChanged = false;
					if (status.maxCredit != null && !status.maxCredit.equals(student.getMaxCredit())) {
						student.setMaxCredit(status.maxCredit);
						studentChanged = true;
					}
					if (student.getOverrideExternalId() != null) {
						SpecialRegistration req = null;
						for (SpecialRegistration r: status.requests) {
							if (student.getOverrideExternalId().equals(r.regRequestId)) { req = r; break; }
						}
						if (req == null && student.getMaxCreditOverrideIntent() == CourseRequestOverrideIntent.WAITLIST) {
							student.setOverrideExternalId(null);
							student.setOverrideMaxCredit(null);
							student.setOverrideStatus(null);
							student.setOverrideTimeStamp(null);
							student.setOverrideIntent(null);
							studentChanged = true;
						} else if (req != null) {
							Integer oldStatus = student.getOverrideStatus();
							switch (status(req, true)) {
							case OVERRIDE_REJECTED: 
								student.setMaxCreditOverrideStatus(CourseRequestOverrideStatus.REJECTED);
								break;
							case OVERRIDE_APPROVED:
								student.setMaxCreditOverrideStatus(CourseRequestOverrideStatus.APPROVED);
								break;
							case OVERRIDE_CANCELLED:
								student.setMaxCreditOverrideStatus(CourseRequestOverrideStatus.CANCELLED);
								break;
							case OVERRIDE_PENDING:
								student.setMaxCreditOverrideStatus(CourseRequestOverrideStatus.PENDING);
								break;
							}
							if (oldStatus == null || !oldStatus.equals(student.getOverrideStatus()))
								studentChanged = true;
						}
					}
					if (studentChanged) helper.getHibSession().update(student);
					
					if (changed || studentChanged) reloadIds.add(student.getUniqueId());
				}
			}
		} catch (SectioningException e) {
			throw (SectioningException)e;
		} catch (Exception e) {
			sLog.error(e.getMessage(), e);
			throw new SectioningException(e.getMessage());
		} finally {
			if (resource != null) {
				if (resource.getResponse() != null) resource.getResponse().release();
				resource.release();
			}
		}
	}

	@Override
	public boolean updateStudent(OnlineSectioningServer server, OnlineSectioningHelper helper, XStudent student, Builder action) throws SectioningException {
		if (student == null) return false;
		
		ClientResource resource = null;
		try {
			resource = new ClientResource(getSpecialRegistrationApiSiteCheckSpecialRegistrationStatus());
			resource.setNext(iClient);
			
			AcademicSessionInfo session = server.getAcademicSession();
			String term = getBannerTerm(session);
			String campus = getBannerCampus(session);
			resource.addQueryParameter("term", term);
			resource.addQueryParameter("campus", campus);
			resource.addQueryParameter("studentId", getBannerId(student));
			resource.addQueryParameter("mode", getSpecialRegistrationApiMode().name());
			action.addOptionBuilder().setKey("term").setValue(term);
			action.addOptionBuilder().setKey("campus").setValue(campus);
			action.addOptionBuilder().setKey("studentId").setValue(getBannerId(student));
			resource.addQueryParameter("apiKey", getSpecialRegistrationApiKey());
			
			long t0 = System.currentTimeMillis();
			
			resource.get(MediaType.APPLICATION_JSON);
			
			action.setApiGetTime(System.currentTimeMillis() - t0);
			
			SpecialRegistrationStatusResponse status = (SpecialRegistrationStatusResponse)new GsonRepresentation<SpecialRegistrationStatusResponse>(resource.getResponseEntity(), SpecialRegistrationStatusResponse.class).getObject();
			Gson gson = getGson(helper);
			
			if (helper.isDebugEnabled())
				helper.debug("Status: " + gson.toJson(status));
			action.addOptionBuilder().setKey("wl-status").setValue(gson.toJson(status));
			
			boolean studentChanged = false;
			for (XRequest r: student.getRequests()) {
				if (r instanceof XCourseRequest) {
					XCourseRequest cr = (XCourseRequest)r;
					if (!cr.hasOverrides()) continue;
					for (Map.Entry<XCourseId, XOverride> e: cr.getOverrides().entrySet()) {
						XCourseId course = e.getKey();
						XOverride override = e.getValue();
						if ("TBD".equals(override.getExternalId())) continue;
						SpecialRegistration req = null;
						for (SpecialRegistration q: status.data.requests) {
							if (override.getExternalId().equals(q.regRequestId)) { req = q; break; }
						}
						if (req != null) {
							Integer oldStatus = override.getStatus();
							Integer newStatus = null;
							switch (status(req, false)) {
							case OVERRIDE_REJECTED: 
								newStatus = CourseRequestOverrideStatus.REJECTED.ordinal();
								break;
							case OVERRIDE_APPROVED:
								newStatus = CourseRequestOverrideStatus.APPROVED.ordinal();
								break;
							case OVERRIDE_CANCELLED:
								newStatus = CourseRequestOverrideStatus.CANCELLED.ordinal();
								break;
							case OVERRIDE_PENDING:
								newStatus = CourseRequestOverrideStatus.PENDING.ordinal();
								break;
							}
							if (newStatus != null && !newStatus.equals(oldStatus)) {
								override.setStatus(newStatus);
								CourseDemand dbCourseDemand = CourseDemandDAO.getInstance().get(cr.getRequestId(), helper.getHibSession());
								if (dbCourseDemand != null) {
									for (org.unitime.timetable.model.CourseRequest dbCourseRequest: dbCourseDemand.getCourseRequests()) {
										if (dbCourseRequest.getCourseOffering().getUniqueId().equals(course.getCourseId())) {
											dbCourseRequest.setOverrideStatus(newStatus);
											helper.getHibSession().update(dbCourseRequest);
										}
									}
								}
								studentChanged = true;
							}
						}
					}
				}
			}
			
			if (status.data.maxCredit != null && !status.data.maxCredit.equals(student.getMaxCredit())) {
				student.setMaxCredit(status.data.maxCredit);
				Student dbStudent = StudentDAO.getInstance().get(student.getStudentId(), helper.getHibSession());
				if (dbStudent != null) {
					dbStudent.setMaxCredit(status.data.maxCredit);
					helper.getHibSession().update(dbStudent);
				}
				studentChanged = true;
			}
			if (student.getMaxCreditOverride() != null) {
				SpecialRegistration req = null;
				for (SpecialRegistration r: status.data.requests) {
					if (r.regRequestId != null && r.regRequestId.equals(student.getMaxCreditOverride().getExternalId())) { req = r; break; }
				}
				if (req != null) {
					Integer oldStatus = student.getMaxCreditOverride().getStatus();
					Integer newStatus = null;
					switch (status(req, true)) {
					case OVERRIDE_REJECTED: 
						newStatus = CourseRequestOverrideStatus.REJECTED.ordinal();
						break;
					case OVERRIDE_APPROVED:
						newStatus = CourseRequestOverrideStatus.APPROVED.ordinal();
						break;
					case OVERRIDE_CANCELLED:
						newStatus = CourseRequestOverrideStatus.CANCELLED.ordinal();
						break;
					case OVERRIDE_PENDING:
						newStatus = CourseRequestOverrideStatus.PENDING.ordinal();
						break;
					}
					if (newStatus == null || !newStatus.equals(oldStatus)) {
						student.getMaxCreditOverride().setStatus(newStatus);
						Student dbStudent = StudentDAO.getInstance().get(student.getStudentId(), helper.getHibSession());
						if (dbStudent != null) {
							dbStudent.setOverrideStatus(newStatus);
							helper.getHibSession().update(dbStudent);
						}
						studentChanged = true;
					}
				}
			}
			if (studentChanged) {
				server.update(student, false);
				helper.getHibSession().flush();
			}
			if (studentChanged) helper.getHibSession().flush();

			return studentChanged;
		} catch (SectioningException e) {
			action.setApiException(e.getMessage());
			throw (SectioningException)e;
		} catch (Exception e) {
			action.setApiException(e.getMessage() == null ? "Null" : e.getMessage());
			sLog.error(e.getMessage(), e);
			throw new SectioningException(e.getMessage());
		} finally {
			if (resource != null) {
				if (resource.getResponse() != null) resource.getResponse().release();
				resource.release();
			}
		}
	}

}
