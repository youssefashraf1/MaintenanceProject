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
package org.unitime.timetable.onlinesectioning.solver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.unitime.localization.impl.Localization;
import org.unitime.timetable.gwt.resources.StudentSectioningMessages;
import org.unitime.timetable.gwt.shared.ClassAssignmentInterface;
import org.unitime.timetable.gwt.shared.ClassAssignmentInterface.ErrorMessage;
import org.unitime.timetable.gwt.shared.SectioningException;
import org.unitime.timetable.onlinesectioning.OnlineSectioningAction;
import org.unitime.timetable.onlinesectioning.OnlineSectioningHelper;
import org.unitime.timetable.onlinesectioning.OnlineSectioningServer;
import org.unitime.timetable.onlinesectioning.OnlineSectioningServer.Lock;
import org.unitime.timetable.onlinesectioning.custom.StudentEnrollmentProvider.EnrollmentRequest;
import org.unitime.timetable.onlinesectioning.model.XConfig;
import org.unitime.timetable.onlinesectioning.model.XCourse;
import org.unitime.timetable.onlinesectioning.model.XCourseRequest;
import org.unitime.timetable.onlinesectioning.model.XEnrollment;
import org.unitime.timetable.onlinesectioning.model.XEnrollments;
import org.unitime.timetable.onlinesectioning.model.XOffering;
import org.unitime.timetable.onlinesectioning.model.XRequest;
import org.unitime.timetable.onlinesectioning.model.XReservation;
import org.unitime.timetable.onlinesectioning.model.XSection;
import org.unitime.timetable.onlinesectioning.model.XStudent;
import org.unitime.timetable.onlinesectioning.model.XSubpart;

/**
 * @author Tomas Muller
 */
public class CheckAssignmentAction implements OnlineSectioningAction<List<EnrollmentRequest>>{
	private static final long serialVersionUID = 1L;
	private static StudentSectioningMessages MSG = Localization.create(StudentSectioningMessages.class);
	private Long iStudentId;
	private Collection<ClassAssignmentInterface.ClassAssignment> iAssignment;
	
	public CheckAssignmentAction forStudent(Long studentId) {
		iStudentId = studentId;
		return this;
	}
	
	public CheckAssignmentAction withAssignment(Collection<ClassAssignmentInterface.ClassAssignment> assignment) {
		iAssignment = assignment;
		return this;
	}

	public Long getStudentId() { return iStudentId; }
	public Collection<ClassAssignmentInterface.ClassAssignment> getAssignment() { return iAssignment; }

	@Override
	public List<EnrollmentRequest> execute(OnlineSectioningServer server, OnlineSectioningHelper helper) {
		Lock readLock = server.readLock();
		try {
			Set<Long> offeringIds = new HashSet<Long>();
			for (ClassAssignmentInterface.ClassAssignment ca: getAssignment())
				if (ca != null && !ca.isFreeTime()) {
					XCourse course = server.getCourse(ca.getCourseId());
					if (course != null) offeringIds.add(course.getOfferingId());
				}
			
			Lock lock = server.lockStudent(getStudentId(), offeringIds, name());
			try {
				return check(server, helper, null);
			} finally {
				lock.release();
			}
		} finally {
			readLock.release();
		}
	}
	
	public List<EnrollmentRequest> check(OnlineSectioningServer server, OnlineSectioningHelper helper, Set<ErrorMessage> errors) {
		XStudent student = server.getStudent(getStudentId());
		if (student == null) throw new SectioningException(MSG.exceptionBadStudentId());
		List<EnrollmentRequest> requests = new ArrayList<EnrollmentRequest>();
		Hashtable<Long, EnrollmentRequest> courseId2request = new Hashtable<Long, EnrollmentRequest>();
		Hashtable<Long, XOffering> courseId2offering = new Hashtable<Long, XOffering>();
		for (ClassAssignmentInterface.ClassAssignment ca: getAssignment()) {	
			// Skip free times and dummy sections
			if (ca == null || ca.isFreeTime() || ca.getClassId() == null || ca.isDummy() || ca.isTeachingAssignment()) continue;
			
			XCourse course = server.getCourse(ca.getCourseId());
			if (course == null)
				throw new SectioningException(MSG.exceptionCourseDoesNotExist(MSG.courseName(ca.getSubject(), ca.getClassNumber())));
			XOffering offering = server.getOffering(course.getOfferingId());
			if (offering == null)
				throw new SectioningException(MSG.exceptionCourseDoesNotExist(MSG.courseName(ca.getSubject(), ca.getClassNumber())));
			
			// Check section limits
			XSection section = offering.getSection(ca.getClassId());
			if (section == null)
				throw new SectioningException(MSG.exceptionEnrollNotAvailable(MSG.clazz(ca.getSubject(), ca.getCourseNbr(), ca.getSubpart(), ca.getSection())));
			
			// Check cancelled flag
			if (section.isCancelled()) {
				if (server.getConfig().getPropertyBoolean("Enrollment.CanKeepCancelledClass", false)) {
					boolean contains = false;
					for (XRequest r: student.getRequests())
						if (r instanceof XCourseRequest) {
							XCourseRequest cr = (XCourseRequest)r;
							if (cr.getEnrollment() != null && cr.getEnrollment().getSectionIds().contains(section.getSectionId())) { contains = true; break; }
						}
					if (!contains) {
						if (errors != null)
							errors.add(new ErrorMessage(course.getCourseName(), section.getExternalId(ca.getCourseId()), ErrorMessage.UniTimeCode.UT_CANCEL, MSG.exceptionEnrollCancelled(MSG.clazz(ca.getSubject(), ca.getCourseNbr(), ca.getSubpart(), ca.getSection()))));
						else
							throw new SectioningException(MSG.exceptionEnrollCancelled(MSG.clazz(ca.getSubject(), ca.getCourseNbr(), ca.getSubpart(), ca.getSection())));
					}
				} else {
					if (errors != null)
						errors.add(new ErrorMessage(course.getCourseName(), section.getExternalId(ca.getCourseId()), ErrorMessage.UniTimeCode.UT_CANCEL, MSG.exceptionEnrollCancelled(MSG.clazz(ca.getSubject(), ca.getCourseNbr(), ca.getSubpart(), ca.getSection()))));
					else
						throw new SectioningException(MSG.exceptionEnrollCancelled(MSG.clazz(ca.getSubject(), ca.getCourseNbr(), ca.getSubpart(), ca.getSection())));
				}
			}

			
			EnrollmentRequest request = courseId2request.get(ca.getCourseId());
			if (request == null) {
				request = new EnrollmentRequest(course, new ArrayList<XSection>());
				courseId2request.put(ca.getCourseId(), request);
				requests.add(request);
			}
			request.getSections().add(section);
			courseId2offering.put(course.getCourseId(), offering);
		}
		
		// Check for NEW and CHANGE deadlines
		check: for (EnrollmentRequest request: requests) {
			XCourse course = request.getCourse();
			List<XSection> sections = request.getSections();

			for (XRequest r: student.getRequests()) {
				if (r instanceof XCourseRequest) {
					XEnrollment enrollment = ((XCourseRequest)r).getEnrollment();
					if (enrollment != null && enrollment.getCourseId().equals(course.getCourseId())) { // course change
						for (XSection s: sections)
							if (!enrollment.getSectionIds().contains(s.getSectionId()) && !server.checkDeadline(course.getCourseId(), s.getTime(), OnlineSectioningServer.Deadline.CHANGE)) {
								if (errors != null)
									errors.add(new ErrorMessage(course.getCourseName(), s.getExternalId(course.getCourseId()), ErrorMessage.UniTimeCode.UT_DEADLINE, MSG.exceptionEnrollDeadlineChange(MSG.clazz(course.getSubjectArea(), course.getCourseNumber(), s.getSubpartName(), s.getName(course.getCourseId())))));
								else
									throw new SectioningException(MSG.exceptionEnrollDeadlineChange(MSG.clazz(course.getSubjectArea(), course.getCourseNumber(), s.getSubpartName(), s.getName(course.getCourseId()))));
							}
						continue check;
					}
				}
			}
			
			// new course
			for (XSection section: sections) {
				if (!server.checkDeadline(course.getOfferingId(), section.getTime(), OnlineSectioningServer.Deadline.NEW)) {
					if (errors != null)
						errors.add(new ErrorMessage(course.getCourseName(), section.getExternalId(course.getCourseId()), ErrorMessage.UniTimeCode.UT_DEADLINE, MSG.exceptionEnrollDeadlineNew(MSG.clazz(course.getSubjectArea(), course.getCourseNumber(), section.getSubpartName(), section.getName(course.getCourseId())))));
					else
						throw new SectioningException(MSG.exceptionEnrollDeadlineNew(MSG.clazz(course.getSubjectArea(), course.getCourseNumber(), section.getSubpartName(), section.getName(course.getCourseId()))));
				}
			}
		}
		
		// Check for DROP deadlines
		for (XRequest r: student.getRequests()) {
			if (r instanceof XCourseRequest) {
				XEnrollment enrollment = ((XCourseRequest)r).getEnrollment();
				if (enrollment != null && !courseId2offering.containsKey(enrollment.getCourseId())) {
					XOffering offering = server.getOffering(enrollment.getOfferingId());
					if (offering != null)
						for (XSection section: offering.getSections(enrollment)) {
							if (!server.checkDeadline(offering.getOfferingId(), section.getTime(), OnlineSectioningServer.Deadline.DROP)) {
								if (errors != null)
									errors.add(new ErrorMessage(enrollment.getCourseName(), section.getExternalId(enrollment.getCourseId()), ErrorMessage.UniTimeCode.UT_DEADLINE, MSG.exceptionEnrollDeadlineDrop(enrollment.getCourseName())));
								else
									throw new SectioningException(MSG.exceptionEnrollDeadlineDrop(enrollment.getCourseName()));
							}
						}
				}
			}
		}
		
		Hashtable<Long, XConfig> courseId2config = new Hashtable<Long, XConfig>();
		for (EnrollmentRequest request: requests) {
			XCourse course = request.getCourse();
			XOffering offering = courseId2offering.get(course.getCourseId());
			XEnrollments enrollments = server.getEnrollments(course.getOfferingId());
			List<XSection> sections = request.getSections();
			XSubpart subpart = offering.getSubpart(sections.get(0).getSubpartId());
			XConfig config = offering.getConfig(subpart.getConfigId());
			courseId2config.put(course.getCourseId(), config);

			List<XReservation> reservations = new ArrayList<XReservation>();
			boolean canAssignOverLimit = false;
			reservations: for (XReservation r: offering.getReservations()) {
				if (!r.isApplicable(student, course)) continue;
				if (r.getLimit() >= 0 && r.getLimit() <= enrollments.countEnrollmentsForReservation(r.getReservationId())) {
					boolean contain = false;
					for (XEnrollment e: enrollments.getEnrollmentsForReservation(r.getReservationId()))
						if (e.getStudentId().equals(student.getStudentId())) { contain = true; break; }
					if (!contain) continue;
				}
				if (!r.getConfigsIds().isEmpty() && !r.getConfigsIds().contains(config.getConfigId())) continue;
				for (XSection section: sections)
					if (r.getSectionIds(section.getSubpartId()) != null && !r.getSectionIds(section.getSubpartId()).contains(section.getSectionId())) continue reservations;
				if (r.canAssignOverLimit())
					canAssignOverLimit = true;
				reservations.add(r);
			}
			
			if (!canAssignOverLimit) {
				for (XSection section: sections) {
					if (section.getLimit() >= 0 && section.getLimit() <= enrollments.countEnrollmentsForSection(section.getSectionId())) {
						boolean contain = false;
						for (XEnrollment e: enrollments.getEnrollmentsForSection(section.getSectionId()))
							if (e.getStudentId().equals(student.getStudentId())) { contain = true; break; }
						if (!contain) {
							if (errors != null)
								errors.add(new ErrorMessage(course.getCourseName(), section.getExternalId(course.getCourseId()), ErrorMessage.UniTimeCode.UT_NOT_AVAILABLE, MSG.exceptionEnrollNotAvailable(MSG.clazz(course.getSubjectArea(), course.getCourseNumber(), section.getSubpartName(), section.getName()))));
							else
								throw new SectioningException(MSG.exceptionEnrollNotAvailable(MSG.clazz(course.getSubjectArea(), course.getCourseNumber(), section.getSubpartName(), section.getName())));
						}
					}
					if (offering.getUnreservedSectionSpace(section.getSectionId(), enrollments) <= 0) {
						boolean hasReservation = false;
						if (!reservations.isEmpty()) {
							for (XReservation r: offering.getSectionReservations(section.getSectionId()))
								if (reservations.contains(r)) {
									hasReservation = true; break;
								}
						}
						boolean contain = false;
						for (XEnrollment e: enrollments.getEnrollmentsForSection(section.getSectionId()))
							if (e.getStudentId().equals(student.getStudentId())) { contain = true; break; }
						if (!contain && !hasReservation) {
							if (errors != null)
								errors.add(new ErrorMessage(course.getCourseName(), section.getExternalId(course.getCourseId()), ErrorMessage.UniTimeCode.UT_NOT_AVAILABLE, MSG.exceptionEnrollNotAvailable(MSG.clazz(course.getSubjectArea(), course.getCourseNumber(), section.getSubpartName(), section.getName()))));
							else
								throw new SectioningException(MSG.exceptionEnrollNotAvailable(MSG.clazz(course.getSubjectArea(), course.getCourseNumber(), section.getSubpartName(), section.getName())));
						}
					}
				}
				
				if (config.getLimit() >= 0 && config.getLimit() <= enrollments.countEnrollmentsForConfig(config.getConfigId())) {
					boolean contain = false;
					for (XEnrollment e: enrollments.getEnrollmentsForConfig(config.getConfigId()))
						if (e.getStudentId().equals(student.getStudentId())) { contain = true; break; }
					if (!contain) {
						if (errors != null) {
							for (XSection section: sections)
								errors.add(new ErrorMessage(course.getCourseName(), section.getExternalId(course.getCourseId()), ErrorMessage.UniTimeCode.UT_NOT_AVAILABLE, MSG.exceptionEnrollNotAvailable(MSG.clazz(course.getSubjectArea(), course.getCourseNumber(), section.getSubpartName(), section.getName()))));
						} else
							throw new SectioningException(MSG.exceptionEnrollNotAvailable(MSG.courseName(course.getSubjectArea(), course.getCourseNumber())));
					}
				}
				if (offering.getUnreservedConfigSpace(config.getConfigId(), enrollments) <= 0) {
					boolean hasReservation = false;
					if (!reservations.isEmpty()) {
						for (XReservation r: offering.getConfigReservations(config.getConfigId()))
							if (reservations.contains(r)) {
								hasReservation = true; break;
							}
					}
					boolean contain = false;
					for (XEnrollment e: enrollments.getEnrollmentsForConfig(config.getConfigId()))
						if (e.getStudentId().equals(student.getStudentId())) { contain = true; break; }
					if (!contain && !hasReservation) {
						if (errors != null) {
							for (XSection section: sections)
								errors.add(new ErrorMessage(course.getCourseName(), section.getExternalId(course.getCourseId()), ErrorMessage.UniTimeCode.UT_NOT_AVAILABLE, MSG.exceptionEnrollNotAvailable(MSG.clazz(course.getSubjectArea(), course.getCourseNumber(), section.getSubpartName(), section.getName()))));
						} else
							throw new SectioningException(MSG.exceptionEnrollNotAvailable(MSG.courseName(course.getSubjectArea(), course.getCourseNumber())));
					}
				}
				
				if (course.getLimit() >= 0 && course.getLimit() <= enrollments.countEnrollmentsForCourse(course.getCourseId())) {
					boolean contain = false;
					for (XEnrollment e: enrollments.getEnrollmentsForCourse(course.getCourseId()))
						if (e.getStudentId().equals(student.getStudentId())) { contain = true; break; }
					if (!contain) {
						if (errors != null) {
							for (XSection section: sections)
								errors.add(new ErrorMessage(course.getCourseName(), section.getExternalId(course.getCourseId()), ErrorMessage.UniTimeCode.UT_NOT_AVAILABLE, MSG.exceptionEnrollNotAvailable(MSG.clazz(course.getSubjectArea(), course.getCourseNumber(), section.getSubpartName(), section.getName()))));
						} else
							throw new SectioningException(MSG.exceptionEnrollNotAvailable(MSG.courseName(course.getSubjectArea(), course.getCourseNumber())));
					}
				}
			}
		}
		
		for (EnrollmentRequest request: requests) {
			XCourse course = request.getCourse();
			XOffering offering = courseId2offering.get(course.getCourseId());
			List<XSection> sections = request.getSections();
			XSubpart subpart = offering.getSubpart(sections.get(0).getSubpartId());
			XConfig config = offering.getConfig(subpart.getConfigId());
			if (sections.size() < config.getSubparts().size()) {
				if (errors != null) {
					for (XSection section: sections)
						errors.add(new ErrorMessage(course.getCourseName(), section.getExternalId(course.getCourseId()), ErrorMessage.UniTimeCode.UT_STRUCTURE, MSG.exceptionEnrollmentIncomplete(MSG.courseName(course.getSubjectArea(), course.getCourseNumber()))));
				} else
					throw new SectioningException(MSG.exceptionEnrollmentIncomplete(MSG.courseName(course.getSubjectArea(), course.getCourseNumber())));
			} else if (sections.size() > config.getSubparts().size()) {
				if (errors != null) {
					for (XSection section: sections)
						errors.add(new ErrorMessage(course.getCourseName(), section.getExternalId(course.getCourseId()), ErrorMessage.UniTimeCode.UT_STRUCTURE, MSG.exceptionEnrollmentInvalid(MSG.courseName(course.getSubjectArea(), course.getCourseNumber()))));
				} else
					throw new SectioningException(MSG.exceptionEnrollmentInvalid(MSG.courseName(course.getSubjectArea(), course.getCourseNumber())));
			}
			for (XSection s1: sections) {
				for (XSection s2: sections) {
					if (s1.getSectionId() < s2.getSectionId() && s1.isOverlapping(offering.getDistributions(), s2)) {
						if (errors != null) {
							errors.add(new ErrorMessage(course.getCourseName(), s1.getExternalId(course.getCourseId()), ErrorMessage.UniTimeCode.UT_TIME_CNF, MSG.exceptionEnrollmentOverlapping(MSG.courseName(course.getSubjectArea(), course.getCourseNumber()))));
							errors.add(new ErrorMessage(course.getCourseName(), s2.getExternalId(course.getCourseId()), ErrorMessage.UniTimeCode.UT_TIME_CNF, MSG.exceptionEnrollmentOverlapping(MSG.courseName(course.getSubjectArea(), course.getCourseNumber()))));
						} else
							throw new SectioningException(MSG.exceptionEnrollmentOverlapping(MSG.courseName(course.getSubjectArea(), course.getCourseNumber())));
					}
					if (!s1.getSectionId().equals(s2.getSectionId()) && s1.getSubpartId().equals(s2.getSubpartId())) {
						if (errors != null) {
							errors.add(new ErrorMessage(course.getCourseName(), s1.getExternalId(course.getCourseId()), ErrorMessage.UniTimeCode.UT_STRUCTURE, MSG.exceptionEnrollmentInvalid(MSG.courseName(course.getSubjectArea(), course.getCourseNumber()))));
							errors.add(new ErrorMessage(course.getCourseName(), s2.getExternalId(course.getCourseId()), ErrorMessage.UniTimeCode.UT_STRUCTURE, MSG.exceptionEnrollmentInvalid(MSG.courseName(course.getSubjectArea(), course.getCourseNumber()))));
						} else
							throw new SectioningException(MSG.exceptionEnrollmentInvalid(MSG.courseName(course.getSubjectArea(), course.getCourseNumber())));
					}
				}
				if (!offering.getSubpart(s1.getSubpartId()).getConfigId().equals(config.getConfigId())) {
					if (errors != null) {
						errors.add(new ErrorMessage(course.getCourseName(), s1.getExternalId(course.getCourseId()), ErrorMessage.UniTimeCode.UT_STRUCTURE, MSG.exceptionEnrollmentInvalid(MSG.courseName(course.getSubjectArea(), course.getCourseNumber()))));
					} else
						throw new SectioningException(MSG.exceptionEnrollmentInvalid(MSG.courseName(course.getSubjectArea(), course.getCourseNumber())));
				}
			}
			if (!offering.isAllowOverlap(student, config.getConfigId(), course, sections))
				for (EnrollmentRequest otherRequest: requests) {
					XOffering other = courseId2offering.get(otherRequest.getCourse().getCourseId());
					XConfig otherConfig = courseId2config.get(otherRequest.getCourse().getCourseId());
					if (!other.equals(offering) && !other.isAllowOverlap(student, otherConfig.getConfigId(), otherRequest.getCourse(), otherRequest.getSections())) {
						List<XSection> assignment = otherRequest.getSections();
						for (XSection section: sections)
							if (section.isOverlapping(offering.getDistributions(), assignment)) {
								if (errors != null) {
									errors.add(new ErrorMessage(course.getCourseName(), section.getExternalId(course.getCourseId()), ErrorMessage.UniTimeCode.UT_TIME_CNF,MSG.exceptionEnrollmentConflicting(MSG.courseName(course.getSubjectArea(), course.getCourseNumber()))));
								} else
									throw new SectioningException(MSG.exceptionEnrollmentConflicting(MSG.courseName(course.getSubjectArea(), course.getCourseNumber())));
							}
					}
				}
		}
		
		return requests;
	}

	@Override
	public String name() {
		return "check-assignment";
	}
}
