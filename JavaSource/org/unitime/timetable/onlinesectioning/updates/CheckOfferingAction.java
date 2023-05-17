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
package org.unitime.timetable.onlinesectioning.updates;

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

import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.studentsct.extension.StudentQuality;
import org.cpsolver.studentsct.online.selection.ResectioningWeights;
import org.hibernate.CacheMode;
import org.unitime.localization.impl.Localization;
import org.unitime.timetable.defaults.ApplicationProperty;
import org.unitime.timetable.gwt.resources.StudentSectioningMessages;
import org.unitime.timetable.gwt.shared.OnlineSectioningInterface.WaitListMode;
import org.unitime.timetable.gwt.shared.SectioningException;
import org.unitime.timetable.model.Class_;
import org.unitime.timetable.model.CourseDemand;
import org.unitime.timetable.model.CourseOffering;
import org.unitime.timetable.model.CourseRequest;
import org.unitime.timetable.model.StudentClassEnrollment;
import org.unitime.timetable.model.WaitList;
import org.unitime.timetable.model.dao.Class_DAO;
import org.unitime.timetable.model.dao.CourseOfferingDAO;
import org.unitime.timetable.model.dao.StudentDAO;
import org.unitime.timetable.onlinesectioning.HasCacheMode;
import org.unitime.timetable.onlinesectioning.OnlineSectioningHelper;
import org.unitime.timetable.onlinesectioning.OnlineSectioningLog;
import org.unitime.timetable.onlinesectioning.OnlineSectioningServer;
import org.unitime.timetable.onlinesectioning.OnlineSectioningServer.Lock;
import org.unitime.timetable.onlinesectioning.custom.CustomStudentEnrollmentHolder;
import org.unitime.timetable.onlinesectioning.custom.Customization;
import org.unitime.timetable.onlinesectioning.custom.WaitListComparatorProvider;
import org.unitime.timetable.onlinesectioning.model.XConfig;
import org.unitime.timetable.onlinesectioning.model.XCourse;
import org.unitime.timetable.onlinesectioning.model.XCourseId;
import org.unitime.timetable.onlinesectioning.model.XCourseRequest;
import org.unitime.timetable.onlinesectioning.model.XEnrollment;
import org.unitime.timetable.onlinesectioning.model.XEnrollments;
import org.unitime.timetable.onlinesectioning.model.XOffering;
import org.unitime.timetable.onlinesectioning.model.XRequest;
import org.unitime.timetable.onlinesectioning.model.XReservation;
import org.unitime.timetable.onlinesectioning.model.XSection;
import org.unitime.timetable.onlinesectioning.model.XStudent;
import org.unitime.timetable.onlinesectioning.server.CheckMaster;
import org.unitime.timetable.onlinesectioning.server.CheckMaster.Master;
import org.unitime.timetable.onlinesectioning.solver.SectioningRequest;
import org.unitime.timetable.onlinesectioning.solver.SectioningRequest.ReschedulingReason;
import org.unitime.timetable.onlinesectioning.solver.SectioningRequestComparator;

/**
 * @author Tomas Muller
 */
@CheckMaster(Master.REQUIRED)
public class CheckOfferingAction extends WaitlistedOnlineSectioningAction<Boolean> implements HasCacheMode {
	private static final long serialVersionUID = 1L;
	private static StudentSectioningMessages MSG = Localization.create(StudentSectioningMessages.class);
	private Collection<Long> iOfferingIds;
	private Collection<Long> iSkipStudentIds;
	private Collection<Long> iStudentIds;
	
	public CheckOfferingAction forOfferings(Long... offeringIds) {
		iOfferingIds = new ArrayList<Long>();
		for (Long offeringId: offeringIds)
			iOfferingIds.add(offeringId);
		return this;
	}
	
	public CheckOfferingAction forOfferings(Collection<Long> offeringIds) {
		iOfferingIds = offeringIds;
		return this;
	}
	
	public CheckOfferingAction skipStudents(Long... studentIds) {
		iSkipStudentIds = new HashSet<Long>();
		for (Long studentId: studentIds)
			if (studentId != null)
				iSkipStudentIds.add(studentId);
		return this;
	}
	
	public CheckOfferingAction skipStudents(Collection<Long> studentIds) {
		iSkipStudentIds = studentIds;
		return this;
	}
	
	public CheckOfferingAction forStudents(Long... studentIds) {
		iStudentIds = new HashSet<Long>();
		for (Long studentId: studentIds)
			if (studentId != null)
				iStudentIds.add(studentId);
		return this;
	}
	
	public CheckOfferingAction forStudents(Collection<Long> studentIds) {
		iStudentIds = studentIds;
		return this;
	}
	
	public Collection<Long> getOfferingIds() { return iOfferingIds; }

	@Override
	public Boolean execute(OnlineSectioningServer server, OnlineSectioningHelper helper) {
		if (!server.getAcademicSession().isSectioningEnabled())
			throw new SectioningException(MSG.exceptionNotSupportedFeature());
		
		if (!CustomStudentEnrollmentHolder.isAllowWaitListing())
			return true;
		
		boolean result = true;
		
		Set<Long> recheck = new HashSet<Long>();
		for (Long offeringId: getOfferingIds()) {
			try {
				// offering is locked -> assuming that the offering will get checked when it is unlocked
				if (server.isOfferingLocked(offeringId)) continue;
				
				// skip non-existing offerings and offerings that do not wait-list
				XOffering offering = server.getOffering(offeringId);
				if (offering == null || !offering.isWaitList()) continue;
				
				// lock and check the offering
				Lock lock = server.lockOffering(offeringId, null, name());
				try {
					helper.beginTransaction();

					helper.getAction().addOther(OnlineSectioningLog.Entity.newBuilder()
							.setUniqueId(offeringId)
							.setName(offering.getName())
							.setType(OnlineSectioningLog.Entity.EntityType.OFFERING));
					
					checkOffering(server, helper, offering, recheck);

					helper.commitTransaction();
				} finally {
					lock.release();
				}
				
			} catch (Exception e) {
				helper.rollbackTransaction();
				helper.fatal("Unable to check offering " + offeringId + ", reason: " + e.getMessage(), e);
				result = false;
			}
		}
		
		if (result && !recheck.isEmpty()) {
			helper.info("Re-checking " + recheck.size() + " offerings...");
			result = server.execute(server.createAction(CheckOfferingAction.class).forOfferings(recheck).skipStudents(iSkipStudentIds).forStudents(iStudentIds), helper.getUser());
		}
		
		return result;
	}
	
	public static boolean isCheckNeeded(OnlineSectioningServer server, OnlineSectioningHelper helper, XEnrollment oldEnrollment) {
		if (oldEnrollment == null) return false;
		
		XStudent newStudent = server.getStudent(oldEnrollment.getStudentId());
		XEnrollment newEnrollment = null;
		if (newStudent != null)
			for (XRequest r: newStudent.getRequests()) {
				XEnrollment e = (r instanceof XCourseRequest ? ((XCourseRequest)r).getEnrollment() : null);
				if (e != null && e.getOfferingId().equals(oldEnrollment.getOfferingId())) {
					newEnrollment = e; break;
				}
			}
		
		return isCheckNeeded(server, helper, oldEnrollment, newEnrollment);
	}
	
	public static boolean isCheckNeeded(OnlineSectioningServer server, OnlineSectioningHelper helper, XEnrollment oldEnrollment, XEnrollment newEnrollment) {
		if (oldEnrollment == null) return false;
		if (!server.getAcademicSession().isSectioningEnabled()) return false;
		if (!CustomStudentEnrollmentHolder.isAllowWaitListing()) return false;

		XOffering offering = server.getOffering(oldEnrollment.getOfferingId());
		if (offering == null || !offering.isWaitList()) return false;

		Set<Long> oldSections;
		if (newEnrollment == null) {
			oldSections = oldEnrollment.getSectionIds();
		} else {
			oldSections = new HashSet<Long>();
			for (Long sectionId: oldEnrollment.getSectionIds())
				if (!newEnrollment.getSectionIds().contains(sectionId))
					oldSections.add(sectionId);
		}
		
		if (oldSections.isEmpty())
			return false; // no drop section detected
		
		if (offering.hasReservations()) {
			for (XReservation r: offering.getReservations()) {
				// ignore overrides or expired reservations
				if (r.isOverride() || r.isExpired()) continue;
				// there is a reservation -> check offering
				helper.debug("Check offering for " + oldEnrollment.getCourseName() + ": there are reservations.");
				return true;
			}
		}
		
		XEnrollments enrollments = server.getEnrollments(oldEnrollment.getOfferingId());
		for (Long sectionId: oldSections) {
			XSection section = offering.getSection(sectionId);
			if (section != null && section.getLimit() >= 0 && section.getLimit() - enrollments.countEnrollmentsForSection(sectionId) == 1) {
				helper.debug("Check offering for " + oldEnrollment.getCourseName() + ": section " + section + " became available.");
				return true;
			}
		}
		if (newEnrollment == null || !newEnrollment.getConfigId().equals(oldEnrollment.getConfigId())) {
			XConfig config = offering.getConfig(oldEnrollment.getConfigId());
			if (config != null && config.getLimit() >= 0 && config.getLimit() - enrollments.countEnrollmentsForConfig(config.getConfigId()) == 1) {
				helper.debug("Check offering for " + oldEnrollment.getCourseName() + ": config " + config + " became available.");
				return true;
			}
		}
		if (newEnrollment == null || !newEnrollment.getCourseId().equals(oldEnrollment.getCourseId())) {
			XCourse course = offering.getCourse(oldEnrollment.getCourseId());
			if (course != null && course.getLimit() >= 0 && course.getLimit() - enrollments.countEnrollmentsForCourse(course.getCourseId()) == 1) {
				helper.debug("Check offering for " + oldEnrollment.getCourseName() + ": course " + course + " became available.");
				return true;
			}
		}
		return false;
	}
	
	public void checkOffering(OnlineSectioningServer server, OnlineSectioningHelper helper, XOffering offering, Set<Long> recheckOfferingIds) {
		if (!server.getAcademicSession().isSectioningEnabled() || offering == null || !offering.isReSchedule()) return;
		if (recheckOfferingIds != null) recheckOfferingIds.remove(offering.getOfferingId());
		
		if (!CustomStudentEnrollmentHolder.isAllowWaitListing()) return;
		
		WaitListComparatorProvider cmp = Customization.WaitListComparatorProvider.getProvider();
		Set<SectioningRequest> queue = new TreeSet<SectioningRequest>(cmp == null ? new SectioningRequestComparator() : cmp.getComparator(server, helper));
		
		XEnrollments enrollments = server.getEnrollments(offering.getOfferingId());
		
		for (XCourseRequest request: enrollments.getRequests()) {
			if (iSkipStudentIds != null && iSkipStudentIds.contains(request.getStudentId())) continue;
			if (iStudentIds != null && !iStudentIds.contains(request.getStudentId())) continue;
			XStudent student = server.getStudent(request.getStudentId());
			ReschedulingReason check = check(server, student, offering, request);
			if (isWaitListed(student, request, offering, server, helper) || check != null) {
				OnlineSectioningLog.Action.Builder action = helper.addAction(this, server.getAcademicSession());
				action.setStudent(
						OnlineSectioningLog.Entity.newBuilder()
						.setUniqueId(student.getStudentId())
						.setExternalId(student.getExternalId())
						.setName(student.getName()));
				action.addOther(OnlineSectioningLog.Entity.newBuilder()
						.setUniqueId(offering.getOfferingId())
						.setName(offering.getName())
						.setType(OnlineSectioningLog.Entity.EntityType.OFFERING));
				if (request.getEnrollment() != null) {
					OnlineSectioningLog.Enrollment.Builder enrollment = OnlineSectioningLog.Enrollment.newBuilder();
					enrollment.setType(OnlineSectioningLog.Enrollment.EnrollmentType.PREVIOUS);
					for (XSection section: offering.getSections(request.getEnrollment()))
						enrollment.addSection(OnlineSectioningHelper.toProto(section, request.getEnrollment()));
					action.addEnrollment(enrollment);
				}
				action.addRequest(OnlineSectioningHelper.toProto(request));
				queue.add(new SectioningRequest(offering, request, request.getCourseIdByOfferingId(offering.getOfferingId()), student, check, getStudentPriority(student, server, helper), action).setNewEnrollment(request.getEnrollment()));
			}
		}
		
		if (!queue.isEmpty()) {
			DataProperties properties = server.getConfig();
			ResectioningWeights w = new ResectioningWeights(properties);
			StudentQuality sq = new StudentQuality(server.getDistanceMetric(), properties);
			Date ts = new Date();
			int index = 1;
			for (SectioningRequest r: queue) {
				helper.debug("Resectioning " + r.getRequest() + " (was " + (r.getLastEnrollment() == null ? "not assigned" : r.getLastEnrollment()) + ")", r.getAction());
				r.getAction().setStartTime(System.currentTimeMillis());
				long c0 = OnlineSectioningHelper.getCpuTime();
				r.getAction().addOptionBuilder().setKey("Index").setValue(index + " of " + queue.size()); index++;
				if (r.isRescheduling())
					r.getAction().addOptionBuilder().setKey("Issue").setValue(r.getReschedulingReason().name());
				XEnrollment dropEnrollment = r.getDropEnrollment();
				XEnrollment enrollment = r.resection(server, w, sq);
				
				if (dropEnrollment != null) {
					XOffering dropOffering = server.getOffering(dropEnrollment.getOfferingId());
					if (dropOffering != null) {
						OnlineSectioningLog.Enrollment.Builder e = OnlineSectioningLog.Enrollment.newBuilder();
						e.setType(OnlineSectioningLog.Enrollment.EnrollmentType.STORED);
						for (Long sectionId: dropEnrollment.getSectionIds())
							e.addSection(OnlineSectioningHelper.toProto(dropOffering.getSection(sectionId), dropEnrollment));
						r.getAction().addEnrollment(e);
					}
				}
				
				if (enrollment != null) {
					enrollment.setTimeStamp(ts);
					OnlineSectioningLog.Enrollment.Builder e = OnlineSectioningLog.Enrollment.newBuilder();
					e.setType(OnlineSectioningLog.Enrollment.EnrollmentType.COMPUTED);
					for (Long sectionId: enrollment.getSectionIds())
						e.addSection(OnlineSectioningHelper.toProto(offering.getSection(sectionId), enrollment));
					r.getAction().addEnrollment(e);
				}
				
				if (enrollment == null && !r.isRescheduling()) {
					// wait-listing only
					r.getAction().setResult(OnlineSectioningLog.Action.ResultType.FALSE);
					r.getAction().setCpuTime(OnlineSectioningHelper.getCpuTime() - c0);
					r.getAction().setEndTime(System.currentTimeMillis());
					continue;
				}
				
				if (CustomStudentEnrollmentHolder.hasProvider()) {
					try {
						enrollment = CustomStudentEnrollmentHolder.getProvider().resection(server, helper, r, enrollment);
					} catch (Exception e) {
						r.getAction().setResult(OnlineSectioningLog.Action.ResultType.FAILURE);
						helper.warn((r.getCourseId() == null ? offering.getName() : r.getCourseId().getCourseName()) + ": " + (e.getMessage() == null ? "Unable to resection student." : e.getMessage()), r.getAction());
						r.getAction().setCpuTime(OnlineSectioningHelper.getCpuTime() - c0);
						r.getAction().setEndTime(System.currentTimeMillis());
						if (ApplicationProperty.OnlineSchedulingEmailConfirmationWhenFailed.isTrue())
							server.execute(server.createAction(NotifyStudentAction.class)
									.forStudent(r.getRequest().getStudentId()).fromAction(name())
									.oldEnrollment(offering, r.getCourseId(), r.getLastEnrollment())
									.failedEnrollment(offering, r.getCourseId(), enrollment, e)
									.dropEnrollment(dropEnrollment)
									.rescheduling(r.getReschedulingReason()),
									helper.getUser());
						continue;
					}
				}
				
				XCourseRequest prev = r.getRequest();
				Long studentId = prev.getStudentId();
				if (dropEnrollment != null && enrollment != null)
					server.assign(r.getDropRequest(), null);
				if (enrollment != null) {
					r.setRequest(server.assign(r.getRequest(), enrollment));
				} else if (r.getRequest() != null) {
					r.setRequest(server.assign(r.getRequest(), null));
				}
				if (r.getRequest() == null) {
					helper.fatal("Failed to assign " + studentId + ": " + (enrollment == null ? prev.toString() : enrollment.toString()));
					r.getAction().setResult(OnlineSectioningLog.Action.ResultType.FAILURE);
					r.getAction().setCpuTime(OnlineSectioningHelper.getCpuTime() - c0);
					r.getAction().setEndTime(System.currentTimeMillis());
					continue;
				}
				helper.debug("New: " + (r.getRequest().getEnrollment() == null ? "not assigned" : r.getRequest().getEnrollment()), r.getAction());
				if (r.getLastEnrollment() == null && r.getRequest().getEnrollment() == null) {
					r.getAction().setResult(OnlineSectioningLog.Action.ResultType.FALSE);
					r.getAction().setCpuTime(OnlineSectioningHelper.getCpuTime() - c0);
					r.getAction().setEndTime(System.currentTimeMillis());
					continue;
				}
				if (r.getLastEnrollment() != null && r.getLastEnrollment().equals(r.getRequest().getEnrollment())) {
					r.getAction().setResult(OnlineSectioningLog.Action.ResultType.FALSE);
					r.getAction().setCpuTime(OnlineSectioningHelper.getCpuTime() - c0);
					r.getAction().setEndTime(System.currentTimeMillis());
					continue;
				}
				
				boolean tx = helper.beginTransaction();
				try {
					Date dropTS = null;
					org.unitime.timetable.model.Student student = StudentDAO.getInstance().get(r.getRequest().getStudentId(), helper.getHibSession());
					Map<Long, StudentClassEnrollment> oldEnrollments = new HashMap<Long, StudentClassEnrollment>();
					String approvedBy = null; Date approvedDate = null;
					for (Iterator<StudentClassEnrollment> i = student.getClassEnrollments().iterator(); i.hasNext();) {
						StudentClassEnrollment enrl = i.next();
						if ((enrl.getCourseRequest() != null && enrl.getCourseRequest().getCourseDemand().getUniqueId().equals(r.getRequest().getRequestId())) ||
							(r.getLastEnrollment() != null && enrl.getCourseOffering() != null && enrl.getCourseOffering().getUniqueId().equals(r.getLastEnrollment().getCourseId()))) {
							helper.debug("Deleting " + enrl.getClazz().getClassLabel(), r.getAction());
							if (dropTS == null || (enrl.getTimestamp() != null && enrl.getTimestamp().before(dropTS)))
								dropTS = enrl.getTimestamp();
							oldEnrollments.put(enrl.getClazz().getUniqueId(), enrl);
							if (approvedBy == null && enrl.getApprovedBy() != null) {
								approvedBy = enrl.getApprovedBy();
								approvedDate = enrl.getApprovedDate();
							}
							enrl.getClazz().getStudentEnrollments().remove(enrl);
							helper.getHibSession().delete(enrl);
							i.remove();
						} else if (dropEnrollment != null && dropEnrollment.getCourseId().equals(enrl.getCourseOffering().getUniqueId())) {
							helper.debug("Deleting " + enrl.getClazz().getClassLabel(), r.getAction());
							enrl.getClazz().getStudentEnrollments().remove(enrl);
							helper.getHibSession().delete(enrl);
							i.remove();	
						}
					}
					CourseDemand cd = null;
					for (CourseDemand x: student.getCourseDemands())
						if (x.getUniqueId().equals(r.getRequest().getRequestId())) {
							cd = x;
							break;
						}
					if (r.getRequest().getEnrollment() != null) { // save enrollment
						org.unitime.timetable.model.CourseRequest cr = null;
						CourseOffering co = null;
						if (co == null) 
							co = CourseOfferingDAO.getInstance().get(r.getRequest().getEnrollment().getCourseId(), helper.getHibSession());
						for (Long sectionId: r.getRequest().getEnrollment().getSectionIds()) {
							Class_ clazz = Class_DAO.getInstance().get(sectionId, helper.getHibSession());
							if (cd != null && cr == null) {
								for (org.unitime.timetable.model.CourseRequest x: cd.getCourseRequests())
									if (x.getCourseOffering().getUniqueId().equals(co.getUniqueId())) {
										cr = x; break;
									}
							}
							if (co == null)
								co = clazz.getSchedulingSubpart().getInstrOfferingConfig().getInstructionalOffering().getControllingCourseOffering();
							StudentClassEnrollment enrl = new StudentClassEnrollment();
							enrl.setClazz(clazz);
							StudentClassEnrollment old = oldEnrollments.get(sectionId);
							enrl.setChangedBy(old != null ? old.getChangedBy() : helper.getUser() == null ? StudentClassEnrollment.SystemChange.WAITLIST.toString() : helper.getUser().getExternalId());
							clazz.getStudentEnrollments().add(enrl);
							enrl.setCourseOffering(co);
							enrl.setCourseRequest(cr);
							enrl.setTimestamp(old != null ? old.getTimestamp() : ts);
							enrl.setStudent(student);
							enrl.setApprovedBy(approvedBy);
							enrl.setApprovedDate(approvedDate);
							student.getClassEnrollments().add(enrl);
							helper.debug("Adding " + enrl.getClazz().getClassLabel(), r.getAction());
						}
						if (cd != null && cd.isWaitlist()) {
							cd.setWaitlist(false);
							helper.getHibSession().saveOrUpdate(cd);
							student.addWaitList(co, WaitList.WaitListType.WAIT_LIST_PORCESSING, false, helper.getUser().getExternalId(), ts, helper.getHibSession());
						} else if (cd != null) {
							student.addWaitList(co, WaitList.WaitListType.RE_BATCH_ON_CHECK, false, helper.getUser().getExternalId(), ts, helper.getHibSession());
						}
						if (r.getRequest().isWaitlist())
							server.waitlist(r.getRequest(), false);
					} else if (r.getOffering().isWaitList() && hasWaitListingStatus(r.getStudent(), server)) { // enable wait-list
						if (cd != null && !cd.isWaitlist()) { // course demand is not wait-listed > enable wait-listing for the course
							// ensure that the dropped course is the first choice 
							CourseRequest cr = (r.getCourseId() == null ? null : cd.getCourseRequest(r.getCourseId().getCourseId()));
							if (cr != null && cr.getOrder() > 0) {
								for (CourseRequest x: cd.getCourseRequests()) {
									if (x.getOrder() < cr.getOrder()) {
										x.setOrder(x.getOrder() + 1);
										helper.getHibSession().update(x);
									}
								}
								cr.setOrder(0);
								helper.getHibSession().update(cr);
							}
							// ensure that the course request is not a substitute 
							if (cd.isAlternative()) {
								int priority = cd.getPriority();
								for (CourseDemand x: cd.getStudent().getCourseDemands()) {
									if (x.isAlternative() && x.getPriority() < cd.getPriority()) {
										if (priority > x.getPriority()) priority = x.getPriority();
										x.setPriority(1 + x.getPriority());
										helper.getHibSession().update(x);
									}
								}
								cd.setPriority(priority);
								cd.setAlternative(false);
							}
							cd.setWaitlist(true);
							cd.setWaitlistedTimeStamp(dropTS == null ? ts : dropTS);
							cd.setWaitListSwapWithCourseOffering(null);
							helper.getHibSession().saveOrUpdate(cd);
							student.addWaitList(
									CourseOfferingDAO.getInstance().get(r.getCourseId().getCourseId(), helper.getHibSession()),
									WaitList.WaitListType.WAIT_LIST_PORCESSING, true, helper.getUser().getExternalId(), ts, helper.getHibSession());
						}
						if (!r.getRequest().isWaitlist()) {
							r.getRequest().setWaitListedTimeStamp(dropTS == null ? ts : dropTS);
							r.getRequest().setWaitListSwapWithCourseOffering(null);
							boolean otherRequestsChanged = false;
							XStudent xs = r.getStudent();
							// ensure that the dropped course is the first choice
							XCourseId course = r.getCourseId();
							int idx = (course == null ? -1 : r.getRequest().getCourseIds().indexOf(course));
							if (idx > 0) {
								r.getRequest().getCourseIds().remove(idx);
								r.getRequest().getCourseIds().add(0, course);
								otherRequestsChanged = true;
							}
							// ensure that the course request is not a substitute
							if (r.getRequest().isAlternative()) {
								int priority = r.getRequest().getPriority();
								for (XRequest x: xs.getRequests()) {
									if (x.isAlternative() && x.getPriority() < cd.getPriority()) {
										if (priority > x.getPriority()) priority = x.getPriority();
										x.setPriority(1 + x.getPriority());
									}
								}
								r.getRequest().setPriority(priority);
								r.getRequest().setAlternative(false);
								otherRequestsChanged = true;
							}
							if (otherRequestsChanged) {
								r.getRequest().setWaitlist(true);
								server.update(xs, true);
							} else
								r.setRequest(server.waitlist(r.getRequest(), true));
						}
					} else if (r.getLastEnrollment() != null) {
						CourseOffering co = CourseOfferingDAO.getInstance().get(r.getLastEnrollment().getCourseId(), helper.getHibSession());
						if (co != null)
							student.addWaitList(co, WaitList.WaitListType.RE_BATCH_ON_CHECK, false, helper.getUser().getExternalId(), ts, helper.getHibSession());
					}
					
					helper.getHibSession().save(student);
		
					EnrollStudent.updateSpace(server,
							r.getRequest().getEnrollment() == null ? null : SectioningRequest.convert(r.getStudent(), r.getRequest(), server, offering, r.getRequest().getEnrollment(), WaitListMode.WaitList),
							r.getLastEnrollment() == null ? null : SectioningRequest.convert(r.getOldStudent(), r.getRequest(), server, offering, r.getLastEnrollment(), WaitListMode.WaitList),
							offering);
					server.persistExpectedSpaces(offering.getOfferingId());

					server.execute(server.createAction(NotifyStudentAction.class)
							.forStudent(r.getRequest().getStudentId())
							.fromAction(name())
							.oldEnrollment(offering, r.getCourseId(), r.getLastEnrollment())
							.dropEnrollment(dropEnrollment)
							.rescheduling(r.getReschedulingReason()), helper.getUser());
					
					if (tx) helper.commitTransaction();
					r.getAction().setResult(enrollment == null ? OnlineSectioningLog.Action.ResultType.NULL : OnlineSectioningLog.Action.ResultType.SUCCESS);
					
					if (recheckOfferingIds != null && dropEnrollment != null && isCheckNeeded(server, helper, dropEnrollment,
							enrollment != null && enrollment.getOfferingId().equals(dropEnrollment.getOfferingId()) ? enrollment : null)) 
						recheckOfferingIds.add(dropEnrollment.getOfferingId());
				} catch (Exception e) {
					if (dropEnrollment != null)
						server.assign(r.getDropRequest(), dropEnrollment);
					server.assign(r.getRequest(), r.getLastEnrollment());
					r.getAction().setResult(OnlineSectioningLog.Action.ResultType.FAILURE);
					helper.error((r.getCourseId() == null ? offering.getName() : r.getCourseId().getCourseName()) + ": " + (e.getMessage() == null ? "Unable to resection student." : e.getMessage()), e, r.getAction());
					if (tx) helper.rollbackTransaction();
				}
				
				r.getAction().setCpuTime(OnlineSectioningHelper.getCpuTime() - c0);
				r.getAction().setEndTime(System.currentTimeMillis());
			}
		}
	}
	
	public ReschedulingReason check(OnlineSectioningServer server, XStudent student, XOffering offering, XCourseRequest request) {
		if (request.getEnrollment() == null) return null;
		if (!offering.getOfferingId().equals(request.getEnrollment().getOfferingId())) return null;
		if (!server.getConfig().getPropertyBoolean("Enrollment.ReSchedulingEnabled", false)) return null;
		if (!hasReSchedulingStatus(student, server)) return null; // no changes for students that cannot be re-scheduled
		List<XSection> sections = offering.getSections(request.getEnrollment());
		XConfig config = offering.getConfig(request.getEnrollment().getConfigId());
		if (config == null || sections.size() != config.getSubparts().size()) {
			for (XSection s1: sections) {
				if (!offering.getSubpart(s1.getSubpartId()).getConfigId().equals(config.getConfigId())) {
					return ReschedulingReason.MULTIPLE_CONFIGS;
				}
			}
			return (sections.size() < config.getSubparts().size() ? ReschedulingReason.MISSING_CLASS : ReschedulingReason.MULTIPLE_ENRLS);
		}
		boolean ignoreBreakTime = server.getConfig().getPropertyBoolean("ReScheduling.IgnoreBreakTimeConflicts", false);
		for (XSection s1: sections) {
			for (XSection s2: sections) {
				if (s1.getSectionId() < s2.getSectionId() && s1.isOverlapping(offering.getDistributions(), s2, ignoreBreakTime)) {
					return ReschedulingReason.TIME_CONFLICT;
				}
				if (!s1.getSectionId().equals(s2.getSectionId()) && s1.getSubpartId().equals(s2.getSubpartId())) {
					return ReschedulingReason.CLASS_LINK;
				}
			}
			if (!offering.getSubpart(s1.getSubpartId()).getConfigId().equals(config.getConfigId())) {
				return ReschedulingReason.MULTIPLE_CONFIGS;
			}
		}
		if (!offering.isAllowOverlap(student, request.getEnrollment().getConfigId(), request.getEnrollment(), sections) &&
			!server.getConfig().getPropertyBoolean("Enrollment.CanKeepTimeConflict", false)) {
			for (XRequest r: student.getRequests()) {
				if (request.getPriority() <= r.getPriority()) continue; // only check time conflicts with courses of higher priority
				if (r instanceof XCourseRequest && !r.getRequestId().equals(request.getRequestId()) && ((XCourseRequest)r).getEnrollment() != null) {
					XEnrollment e = ((XCourseRequest)r).getEnrollment();
					XOffering other = server.getOffering(e.getOfferingId());
					if (other != null) {
						List<XSection> assignment = other.getSections(e);
						if (!other.isAllowOverlap(student, e.getConfigId(), e, assignment))
							for (XSection section: sections)
								if (section.isOverlapping(offering.getDistributions(), assignment, ignoreBreakTime)) {
									return ReschedulingReason.TIME_CONFLICT;
								}
					}
				}
			}
		}
		if (!server.getConfig().getPropertyBoolean("Enrollment.CanKeepCancelledClass", false))
			for (XSection section: sections)
				if (section.isCancelled())
					return ReschedulingReason.CLASS_CANCELLED;
		return null;
	}

	@Override
	public String name() {
		return "check-offering";
	}
	
	@Override
	public CacheMode getCacheMode() {
		return CacheMode.IGNORE;
	}
}
