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
import java.util.List;
import java.util.Set;

import org.cpsolver.coursett.model.RoomLocation;
import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.AssignmentMap;
import org.cpsolver.ifs.assignment.DefaultSingleAssignment;
import org.cpsolver.ifs.util.ToolBox;
import org.cpsolver.studentsct.extension.StudentQuality;
import org.cpsolver.studentsct.model.Config;
import org.cpsolver.studentsct.model.Course;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.FreeTimeRequest;
import org.cpsolver.studentsct.model.Instructor;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Request.RequestPriority;
import org.cpsolver.studentsct.model.Student.ModalityPreference;
import org.cpsolver.studentsct.model.Student.StudentPriority;
import org.cpsolver.studentsct.model.SctAssignment;
import org.cpsolver.studentsct.model.Section;
import org.cpsolver.studentsct.model.Student;
import org.cpsolver.studentsct.model.Subpart;
import org.cpsolver.studentsct.online.selection.ResectioningWeights;
import org.cpsolver.studentsct.online.selection.ResectioningWeights.LastSectionProvider;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.unitime.timetable.gwt.shared.OnlineSectioningInterface.WaitListMode;
import org.unitime.timetable.model.CourseDemand;
import org.unitime.timetable.onlinesectioning.OnlineSectioningLog;
import org.unitime.timetable.onlinesectioning.OnlineSectioningServer;
import org.unitime.timetable.onlinesectioning.model.XCourseId;
import org.unitime.timetable.onlinesectioning.model.XCourseRequest;
import org.unitime.timetable.onlinesectioning.model.XEnrollment;
import org.unitime.timetable.onlinesectioning.model.XFreeTimeRequest;
import org.unitime.timetable.onlinesectioning.model.XIndividualReservation;
import org.unitime.timetable.onlinesectioning.model.XOffering;
import org.unitime.timetable.onlinesectioning.model.XRequest;
import org.unitime.timetable.onlinesectioning.model.XReservation;
import org.unitime.timetable.onlinesectioning.model.XStudent;
import org.unitime.timetable.onlinesectioning.model.XTime;


/**
 * @author Tomas Muller
 */
public class SectioningRequest implements LastSectionProvider {
	private XCourseRequest iOldRequest, iRequest;
	private XStudent iOldStudent, iStudent;
	private XEnrollment iLastEnrollment, iNewEnrollment;
	private XOffering iOldOffering, iOffering;
	private boolean iHasIndividualReservation;
	private OnlineSectioningLog.Action.Builder iAction;
	private List<Section> iLastSections = new ArrayList<Section>();
	private RequestPriority iRequestPriority = RequestPriority.Normal;
	private StudentPriority iStudentPriority = StudentPriority.Normal;
	private int iAlternativity = 0;
	private XCourseId iCourseId = null;
	private XCourseId iDropCourseId = null;
	private ReschedulingReason iRescheduling;
	
	public static enum ReschedulingReason {
		CLASS_CANCELLED,
		TIME_CONFLICT,
		MISSING_CLASS,
		MULTIPLE_ENRLS,
		CLASS_LINK,
		MULTIPLE_CONFIGS,
		NO_REQUEST,
		;
	}

	public SectioningRequest(XOffering offering, XCourseRequest request, XCourseId courseId, XStudent student, ReschedulingReason rescheduling, StudentPriority priority, OnlineSectioningLog.Action.Builder action) {
		iRequest = request;
		iStudent = student;
		iOffering = offering;
		iCourseId = courseId;
		iRescheduling = rescheduling;
		if (courseId != null) {
			iAlternativity = request.getCourseIds().indexOf(courseId);
		} else {
			for (int i = 0; i < request.getCourseIds().size(); i++) {
				if (request.getCourseIds().get(i).getOfferingId().equals(offering.getOfferingId())) {
					iAlternativity = i; break;
				}
			}
		}
		iDropCourseId = request.getWaitListSwapWithCourseOffering();

		iStudentPriority = priority;
		if (action != null)
			action.addOptionBuilder().setKey("Student Priority").setValue(iStudentPriority.name());

		if (request.isCritical()) {
			if (request.getCritical() == CourseDemand.Critical.CRITICAL.ordinal())
				iRequestPriority = RequestPriority.Critical;
			else if (request.getCritical() == CourseDemand.Critical.IMPORTANT.ordinal())
				iRequestPriority = RequestPriority.Important;
			else if (request.getCritical() == CourseDemand.Critical.VITAL.ordinal())
				iRequestPriority = RequestPriority.Vital;
			else if (request.getCritical() == CourseDemand.Critical.LC.ordinal())
				iRequestPriority = RequestPriority.LC;
		}
		if (action != null)
			action.addOptionBuilder().setKey("Request Priority").setValue(iRequestPriority.name());

		iHasIndividualReservation = false;
		for (XReservation reservation: iOffering.getReservations()) {
			if (!reservation.mustBeUsed() || reservation.isExpired()) continue;
			if (reservation instanceof XIndividualReservation && ((XIndividualReservation)reservation).getStudentIds().contains(request.getStudentId())) {
				iHasIndividualReservation = true; break;
			}
		}
		
		iAction = action;
	}
	
	public XCourseId getCourseId() {
		if (iCourseId != null) return iCourseId;
		return getRequest().getCourseIdByOfferingId(getOffering().getOfferingId());
	}
	
	public boolean isRescheduling() { return iRescheduling != null; }
	public ReschedulingReason getReschedulingReason() { return iRescheduling; }
	
	public XOffering getOffering() { return (iOffering == null ? iOldOffering : iOffering); }
	public SectioningRequest setOldOffering(XOffering offering) { iOldOffering = offering; return this; }
	public XOffering getOldOffering() { return (iOldOffering != null ? iOldOffering : iOffering); }
	
	public XStudent getStudent() { return (iStudent == null ? iOldStudent : iStudent); }
	public SectioningRequest setOldStudent(XStudent student) { iOldStudent = student; return this; }
	public XStudent getOldStudent() { return (iOldStudent == null ? iStudent : iOldStudent); }
	
	public SectioningRequest setLastEnrollment(XEnrollment enrollment) { iLastEnrollment = enrollment; return this; }
	public XEnrollment getLastEnrollment() { return iLastEnrollment == null ? iNewEnrollment : iLastEnrollment; }
	public SectioningRequest setNewEnrollment(XEnrollment enrollment) { iNewEnrollment = enrollment; return this; }
	public XEnrollment getNewEnrollment() { return iNewEnrollment == null ? iLastEnrollment : iNewEnrollment; }
	
	public XCourseRequest getRequest() { return (iRequest == null ? iOldRequest : iRequest); }
	public SectioningRequest setOldRequest(XCourseRequest request) { iOldRequest = request; return this; }
	public XCourseRequest getOldRequest() { return (iOldRequest == null ? iRequest : iOldRequest); }

	public void setRequest(XCourseRequest request) { iRequest = request; }
	public boolean hasIndividualReservation() { return iHasIndividualReservation; }
	public OnlineSectioningLog.Action.Builder getAction() { return iAction; }
	
	public XCourseId getDropCourseId() {
		if (iLastEnrollment != null) return null; // re-sectioning ->> no drop
		return iDropCourseId;
	}
	public XEnrollment getDropEnrollment() {
		if (iLastEnrollment != null) return null; // re-sectioning ->> no drop
		if (iDropCourseId == null) return null;
		XCourseRequest request = getOldStudent().getRequestForCourse(iDropCourseId.getCourseId());
		if (request == null) return null;
		XEnrollment enrollment = request.getEnrollment();
		// different course enrolled ->> no drop
		if (enrollment != null && !enrollment.getCourseId().equals(iDropCourseId.getCourseId())) return null;
		return enrollment;
	}
	public XCourseRequest getDropRequest() {
		if (iLastEnrollment != null) return null; // re-sectioning ->> no drop
		if (iDropCourseId == null) return null;
		XCourseRequest request = getStudent().getRequestForCourse(iDropCourseId.getCourseId());
		if (request == null) request = getOldStudent().getRequestForCourse(iDropCourseId.getCourseId());
		return request;
	}
	
	public OnlineSectioningLog.CourseRequestOption getOriginalEnrollment() {
		return getOldRequest().getOptions(getOldOffering().getOfferingId());
	}
	
	public int hashCode() { return Long.valueOf(getRequest().getStudentId()).hashCode(); }
	
	public boolean equals(Object o) {
		if (o == null || !(o instanceof SectioningRequest)) return false;
		return getRequest().getStudentId().equals(((SectioningRequest)o).getRequest().getStudentId());
	}
	
	public RequestPriority getRequestPriority() { return iRequestPriority; }
	public StudentPriority getStudentPriority() { return iStudentPriority; }
	public int getAlternativity() { return iAlternativity; }
	
	public boolean isOverlappingFreeTime(FreeTimeRequest request, Enrollment e) {
		if (request.getTime() == null || e.isAllowOverlap()) return false;
        for (SctAssignment assignment : e.getAssignments())
            if (!assignment.isAllowOverlap() && assignment.getTime() != null && request.getTime().hasIntersection(assignment.getTime()))
            	return true;
        return false;
	}

	public XEnrollment resection(OnlineSectioningServer server, ResectioningWeights w, StudentQuality sq) {
		w.setLastSectionProvider(this);
		
		List<Enrollment> enrollments = new ArrayList<Enrollment>();
		double bestValue = 0.0;
		
		Assignment<Request, Enrollment> assignment = new AssignmentMap<Request, Enrollment>();
		CourseRequest request = convert(assignment, getRequest(), getDropCourseId(), server, WaitListMode.WaitList);
		if (request == null) return null;
		
		Integer currentDateIndex = null;
		int dayOfWeekOffset = server.getAcademicSession().getDayOfWeekOffset();
		if (server.getConfig().getPropertyBoolean("ReScheduling.AvoidPastSections", false))
			currentDateIndex = Days.daysBetween(new LocalDate(server.getAcademicSession().getDatePatternFirstDate()), new LocalDate()).getDays() + server.getConfig().getPropertyInt("ReScheduling.AvoidPastOffset", 0);
		
		if (getLastEnrollment() != null)
			for (Long sectionId: getLastEnrollment().getSectionIds()) {
				for (Course course: request.getCourses()) {
					Section section = course.getOffering().getSection(sectionId);
					if (section != null) iLastSections.add(section);
				}
			}
		
		enrollments: for (Enrollment e: request.getAvaiableEnrollments(assignment)) {
			// only consider enrollments of the offering that is being checked
			if (e.getOffering().getId() != getOffering().getOfferingId()) continue;

			// avoid past sections
			if (currentDateIndex != null)
				for (Section s: e.getSections()) {
					if (!iLastSections.contains(s) && s.getTime() != null) {
						if (s.getTime().getDayCode() != 0 && s.getTime().getFirstMeeting(dayOfWeekOffset) < currentDateIndex)
							continue enrollments;
						if (s.getTime().getDayCode() == 0 && !s.getTime().getWeekCode().isEmpty() && s.getTime().getWeekCode().nextSetBit(0) < currentDateIndex)
							continue enrollments;
					}
				}
			
			for (Request other: request.getStudent().getRequests()) {
				if (other.equals(request)) continue;
				Enrollment x = assignment.getValue(other);
				if (e.isOverlapping(x))
					continue enrollments;
				if (!w.isFreeTimeAllowOverlaps() && other instanceof FreeTimeRequest && other.getPriority() < request.getPriority() && isOverlappingFreeTime((FreeTimeRequest) other, e))
					continue enrollments;
			}
			
			for (Section s: e.getSections()) {
				if (getLastEnrollment() == null) {
					if (!server.checkDeadline(e.getCourse().getId(), s.getTime() == null ? null : new XTime(s.getTime()), OnlineSectioningServer.Deadline.NEW)) continue enrollments;
				} else {
					if (!getLastEnrollment().getSectionIds().contains(s.getId()) &&
						!server.checkDeadline(e.getCourse().getId(), s.getTime() == null ? null : new XTime(s.getTime()), OnlineSectioningServer.Deadline.CHANGE)) continue enrollments;
				}
			}
			
			double value = w.getWeight(assignment, e, sq.allConflicts(assignment, e));
			if (enrollments.isEmpty() || value > bestValue) {
				enrollments.clear();
				enrollments.add(e); bestValue = value;
			} else if (value == bestValue) {
				enrollments.add(e); 
			}
		}
		
		return (enrollments.isEmpty() ? null : new XEnrollment(ToolBox.random(enrollments)));
	}

	@Override
	public boolean sameLastChoice(Section current) {
		if (getLastEnrollment() != null)
			for (Section section: iLastSections)
				if (current.sameChoice(section)) return true;
		
		if (getOriginalEnrollment() != null)
			sections: for (OnlineSectioningLog.Section section: getOriginalEnrollment().getSectionList()) {
				
				if (!section.hasSubpart()) continue;
				if (section.getSubpart().hasExternalId()) {
					if (!section.getSubpart().getExternalId().equals(current.getSubpart().getInstructionalType())) continue;
				} else if (section.getSubpart().hasName()) {
					if (!section.getSubpart().getName().equals(current.getSubpart().getName())) continue;
				} else if (section.getSubpart().hasUniqueId()) {
					if (section.getSubpart().getUniqueId() != current.getSubpart().getId()) continue;
				}
				
				if (section.hasTime()) {
					if (current.getTime() == null) continue;
					if (section.getTime().getDays() != current.getTime().getDayCode()) continue;
					if (section.getTime().getStart() != current.getTime().getStartSlot()) continue;
					if (section.getTime().getLength() != current.getTime().getLength()) continue;
					if (section.getTime().hasPattern() && !ToolBox.equals(section.getTime().getPattern(), current.getTime().getDatePatternName())) continue;
				} else {
					if (current.getTime() != null) continue;
				}
				
				if (current.nrInstructors() != section.getInstructorCount()) continue;
				for (OnlineSectioningLog.Entity instructor: section.getInstructorList())
					if (!instructor.hasUniqueId() || !current.getInstructors().contains(new Instructor(instructor.getUniqueId()))) continue sections;

				return true;
			}
		return false;
	}

	@Override
	public boolean sameLastTime(Section current) {
		if (getLastEnrollment() != null)
			for (Section section: iLastSections)
				if (section.getSubpart().getInstructionalType().equals(current.getSubpart().getInstructionalType()))
					if (ResectioningWeights.sameTime(current, section.getTime()))
						return true;
		
		if (getOriginalEnrollment() != null)
			for (OnlineSectioningLog.Section section: getOriginalEnrollment().getSectionList()) {
				
				if (!section.hasSubpart()) continue;
				if (section.getSubpart().hasExternalId()) {
					if (!section.getSubpart().getExternalId().equals(current.getSubpart().getInstructionalType())) continue;
				} else if (section.getSubpart().hasName()) {
					if (!section.getSubpart().getName().equals(current.getSubpart().getName())) continue;
				} else if (section.getSubpart().hasUniqueId()) {
					if (section.getSubpart().getUniqueId() != current.getSubpart().getId()) continue;
				}
				
				if (section.hasTime()) {
					if (current.getTime() == null) continue;
					if (section.getTime().getDays() != current.getTime().getDayCode()) continue;
					if (section.getTime().getStart() != current.getTime().getStartSlot()) continue;
					if (section.getTime().getLength() != current.getTime().getLength()) continue;
					if (section.getTime().hasPattern() && !ToolBox.equals(section.getTime().getPattern(), current.getTime().getDatePatternName())) continue;
				} else {
					if (current.getTime() != null) continue;
				}
				
				return true;
			}
		return false;
	}

	@Override
	public boolean sameLastRoom(Section current) {
		if (getLastEnrollment() != null)
			for (Section section: iLastSections)
				if (section.getSubpart().getInstructionalType().equals(current.getSubpart().getInstructionalType()))
					if (ResectioningWeights.sameRooms(current, section.getRooms()))
						return true;

		if (getOriginalEnrollment() != null)
			sections: for (OnlineSectioningLog.Section section: getOriginalEnrollment().getSectionList()) {
				
				if (!section.hasSubpart()) continue;
				if (section.getSubpart().hasExternalId()) {
					if (!section.getSubpart().getExternalId().equals(current.getSubpart().getInstructionalType())) continue;
				} else if (section.getSubpart().hasName()) {
					if (!section.getSubpart().getName().equals(current.getSubpart().getName())) continue;
				} else if (section.getSubpart().hasUniqueId()) {
					if (section.getSubpart().getUniqueId() != current.getSubpart().getId()) continue;
				}
				
				if (section.getLocationCount() > 0) {
					if (current.getRooms() == null || current.getRooms().isEmpty()) continue;
					rooms: for (OnlineSectioningLog.Entity room: section.getLocationList()) {
						for (RoomLocation loc: current.getRooms()) {
							if (room.hasUniqueId() && room.getUniqueId() == loc.getId()) continue rooms;
							if (room.hasName() && room.getName().equals(loc.getName())) continue rooms;
						}
						continue sections;
					}
				} else {
					if (current.getRooms() != null && !current.getRooms().isEmpty()) continue;
				}
				
				return true;
			}
		return false;
	}

	@Override
	public boolean sameLastName(Section current, Course course) {
		if (getLastEnrollment() != null)
			for (Section section: iLastSections)
				if (section.getSubpart().getId() == current.getSubpart().getId())
					return ResectioningWeights.sameName(course.getId(), current, section);
		
		if (getOriginalEnrollment() != null)
			for (OnlineSectioningLog.Section section: getOriginalEnrollment().getSectionList()) {
				
				if (!section.hasSubpart()) continue;
				if (section.getSubpart().hasExternalId()) {
					if (!section.getSubpart().getExternalId().equals(current.getSubpart().getInstructionalType())) continue;
				} else if (section.getSubpart().hasName()) {
					if (!section.getSubpart().getName().equals(current.getSubpart().getName())) continue;
				} else if (section.getSubpart().hasUniqueId()) {
					if (section.getSubpart().getUniqueId() != current.getSubpart().getId()) continue;
				}

				if (!section.hasClazz()) continue;
				
				if (section.getClazz().hasName() && !section.getClazz().getName().equals(current.getName(-1l))) continue;

				if (section.getClazz().hasExternalId() && !section.getClazz().getExternalId().equals(current.getName(course.getId()))) continue;
				
				return false;
			}
		return false;
	}
	
	public static CourseRequest convert(Assignment<Request, Enrollment> assignment, XCourseRequest request, OnlineSectioningServer server, WaitListMode wlMode) {
		XCourseId dropCourse = (request.isWaitlist(wlMode) ? request.getWaitListSwapWithCourseOffering() : null);
		return convert(assignment, request, dropCourse, server, wlMode);
	}
	
	public static CourseRequest convert(Assignment<Request, Enrollment> assignment, XCourseRequest request, XCourseId dropCourse, OnlineSectioningServer server, WaitListMode wlMode) {
		return convert(assignment, server.getStudent(request.getStudentId()), request, server, null, null, dropCourse, wlMode);
	}
	
	public static Enrollment convert(XCourseRequest request, OnlineSectioningServer server, WaitListMode wlMode) {
		Assignment<Request, Enrollment> assignment = new DefaultSingleAssignment<Request, Enrollment>();
		CourseRequest cr = convert(assignment, server.getStudent(request.getStudentId()), request, server, null, null, null, wlMode);
		return assignment.getValue(cr);
	}
	
	public static boolean hasRequirements(CourseRequest cr) {
		if (cr.getStudent().getClassFirstDate() != null) return true;
		if (cr.getStudent().getClassLastDate() != null) return true;
		if (cr.getStudent().getModalityPreference() == ModalityPreference.ONLINE_REQUIRED) return true;
		return !cr.getRequiredChoices().isEmpty();
	}
	
	private static boolean check(CourseRequest cr, Course course, Config config, HashSet<Section> sections, int idx) {
		if (idx == 0) {
			if (cr.isNotAllowed(course, config)) return false;
		}
		if (config.getSubparts().size() == idx) {
			Enrollment e = new Enrollment(cr, 0, course, config, new HashSet<SctAssignment>(sections), null);
            if (cr.isNotAllowed(e)) return false;
            return true;
		} else {
			Subpart subpart = config.getSubparts().get(idx);
			for (Section section : subpart.getSections()) {
                if (section.isCancelled()) continue;
                if (!cr.isRequired(section)) continue;
                if (section.getParent() != null && !sections.contains(section.getParent())) continue;
                if (section.isOverlapping(sections)) continue;
                if (cr.isNotAllowed(course, section)) continue;
                if (!cr.getStudent().isAllowDisabled() && !section.isEnabled(cr.getStudent())) continue;
                sections.add(section);
                if (check(cr, course, config, sections, idx + 1))
                	return true;
                sections.remove(section);
			}
		}
		return false;
	}
	
	public static boolean hasInconsistentRequirements(CourseRequest cr, Long courseId) {
		if (!hasRequirements(cr)) return false;
		for (Course course: cr.getCourses()) {
			if (courseId != null && course.getId() != courseId) continue;
			for (Config config: course.getOffering().getConfigs()) {
				if (check(cr, course, config, new HashSet<Section>(), 0)) return false;
			}
		}
		return true;
	}
	
	public static CourseRequest convert(Assignment<Request, Enrollment> assignment, XStudent student, XCourseRequest request, OnlineSectioningServer server, XOffering oldOffering, XEnrollment oldEnrollment, XCourseId dropCourse, WaitListMode wlMode) {
		Student clonnedStudent = new Student(request.getStudentId());
		clonnedStudent.setExternalId(student.getExternalId());
		clonnedStudent.setName(student.getName());
		clonnedStudent.setNeedShortDistances(student.hasAccomodation(server.getDistanceMetric().getShortDistanceAccommodationReference()));
		clonnedStudent.setAllowDisabled(student.isAllowDisabled());
		clonnedStudent.setClassFirstDate(student.getClassStartDate());
		clonnedStudent.setClassLastDate(student.getClassEndDate());
		clonnedStudent.setBackToBackPreference(student.getBackToBackPreference());
		clonnedStudent.setModalityPreference(student.getModalityPreference());
		CourseRequest ret = null;
		for (XRequest r: student.getRequests()) {
			if (r instanceof XFreeTimeRequest) {
				XFreeTimeRequest ft = (XFreeTimeRequest)r;
				new FreeTimeRequest(r.getRequestId(), r.getPriority(), r.isAlternative(), clonnedStudent,
						new TimeLocation(ft.getTime().getDays(), ft.getTime().getSlot(), ft.getTime().getLength(), 0, 0.0,
								-1l, "Free Time", server.getAcademicSession().getFreeTimePattern(), 0));
			} else {
				XCourseRequest cr = (XCourseRequest)r;
				List<Course> courses = new ArrayList<Course>();
				for (XCourseId c: cr.getCourseIds()) {
					XOffering offering = server.getOffering(c.getOfferingId());
					if (oldOffering != null && oldOffering.getOfferingId().equals(c.getOfferingId()))
						offering = oldOffering;
					courses.add(offering.toCourse(c.getCourseId(), student, server));
				}
				CourseRequest clonnedRequest = new CourseRequest(r.getRequestId(), r.getPriority(), r.isAlternative(), clonnedStudent, courses, cr.isWaitListOrNoSub(wlMode), cr.getTimeStamp() == null ? null : cr.getTimeStamp().getTime());
				cr.fillChoicesIn(clonnedRequest);
				XEnrollment enrollment = cr.getEnrollment();
				if (oldEnrollment != null && cr.getCourseIdByOfferingId(oldOffering.getOfferingId()) != null)
					enrollment = oldEnrollment;
				if (enrollment != null && (dropCourse == null || !dropCourse.getCourseId().equals(enrollment.getCourseId()))) {
					Config config = null;
					Set<Section> assignments = new HashSet<Section>();
					for (Course c: clonnedRequest.getCourses()) {
						if (enrollment.getCourseId().equals(c.getId()))
							for (Config g: c.getOffering().getConfigs()) {
								if (enrollment.getConfigId().equals(g.getId())) {
									config = g;
									for (Subpart s: g.getSubparts())
										for (Section x: s.getSections())
											if (enrollment.getSectionIds().contains(x.getId()))
												assignments.add(x);
								}
							}
					}
					if (config != null)
						assignment.assign(0, new Enrollment(clonnedRequest, 0, config, assignments, assignment));
				}
					
				if (request.equals(r)) ret = clonnedRequest;
			}
		}
		if (ret == null) {
			List<Course> courses = new ArrayList<Course>();
			for (XCourseId c: request.getCourseIds()) {
				XOffering offering = server.getOffering(c.getOfferingId());
				if (oldOffering != null && oldOffering.getOfferingId().equals(c.getOfferingId()))
					offering = oldOffering;
				courses.add(offering.toCourse(c.getCourseId(), student, server));
			}
			ret = new CourseRequest(request.getRequestId(), request.getPriority(), request.isAlternative(), clonnedStudent, courses, request.isWaitListOrNoSub(wlMode), request.getTimeStamp() == null ? null : request.getTimeStamp().getTime());
			request.fillChoicesIn(ret);
		}
		if (clonnedStudent.getExternalId() != null && !clonnedStudent.getExternalId().isEmpty()) {
			Collection<Long> offerings = server.getInstructedOfferings(clonnedStudent.getExternalId());
			if (offerings != null)
				for (Long offeringId: offerings) {
					XOffering offering = server.getOffering(offeringId);
					if (offering != null)
						offering.fillInUnavailabilities(clonnedStudent);
				}
		}
		return ret;
	}
	
	public static Enrollment convert(XStudent student, XCourseRequest request, OnlineSectioningServer server, XOffering oldOffering, XEnrollment oldEnrollment, WaitListMode wlMode) {
		Assignment<Request, Enrollment> assignment = new DefaultSingleAssignment<Request, Enrollment>();
		CourseRequest cr = convert(assignment, student, request, server, oldOffering, oldEnrollment, null, wlMode);
		return assignment.getValue(cr);
	}
}
