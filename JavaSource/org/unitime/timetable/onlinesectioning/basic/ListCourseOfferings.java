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
package org.unitime.timetable.onlinesectioning.basic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cpsolver.ifs.heuristics.RouletteWheelSelection;
import org.hibernate.criterion.Order;
import org.hibernate.type.LongType;
import org.unitime.timetable.defaults.ApplicationProperty;
import org.unitime.timetable.gwt.server.Query;
import org.unitime.timetable.gwt.shared.ClassAssignmentInterface;
import org.unitime.timetable.gwt.shared.ClassAssignmentInterface.CourseAssignment;
import org.unitime.timetable.gwt.shared.CourseRequestInterface;
import org.unitime.timetable.model.CourseOffering;
import org.unitime.timetable.model.OverrideType;
import org.unitime.timetable.model.dao.OverrideTypeDAO;
import org.unitime.timetable.onlinesectioning.OnlineSectioningAction;
import org.unitime.timetable.onlinesectioning.OnlineSectioningHelper;
import org.unitime.timetable.onlinesectioning.OnlineSectioningServer;
import org.unitime.timetable.onlinesectioning.OnlineSectioningServer.Lock;
import org.unitime.timetable.onlinesectioning.custom.CustomCourseLookupHolder;
import org.unitime.timetable.onlinesectioning.custom.Customization;
import org.unitime.timetable.onlinesectioning.custom.ExternalTermProvider;
import org.unitime.timetable.onlinesectioning.match.CourseMatcher;
import org.unitime.timetable.onlinesectioning.model.XAreaClassificationMajor;
import org.unitime.timetable.onlinesectioning.model.XConfig;
import org.unitime.timetable.onlinesectioning.model.XCourse;
import org.unitime.timetable.onlinesectioning.model.XCourseId;
import org.unitime.timetable.onlinesectioning.model.XCourseRequest;
import org.unitime.timetable.onlinesectioning.model.XEnrollment;
import org.unitime.timetable.onlinesectioning.model.XOffering;
import org.unitime.timetable.onlinesectioning.model.XStudent;
import org.unitime.timetable.onlinesectioning.status.StatusPageSuggestionsAction.StudentMatcher;

/**
 * @author Tomas Muller
 */
public class ListCourseOfferings implements OnlineSectioningAction<Collection<ClassAssignmentInterface.CourseAssignment>> {
	private static final long serialVersionUID = 1L;
	
	protected String iQuery = null;
	protected CourseRequestInterface.Request iRequest = null;
	protected Integer iLimit = null;
	protected CourseMatcher iMatcher = null;
	protected Long iStudentId;
	protected String iFilterIM = null;
	private transient XStudent iStudent = null;
	
	public ListCourseOfferings forQuery(String query) {
		iQuery = query; return this;
	}
	
	public ListCourseOfferings forRequest(CourseRequestInterface.Request request) {
		iRequest = request; return this;	
	}
	
	public ListCourseOfferings withLimit(Integer limit) {
		iLimit = limit; return this;
	}
	
	public ListCourseOfferings withMatcher(CourseMatcher matcher) {
		iMatcher = matcher; return this;
	}
	
	public ListCourseOfferings forStudent(Long studentId) {
		iStudentId = studentId; return this;
	}
	
	public Long getStudentId() {
		return iStudentId;
	}
	
	@Override
	public Collection<CourseAssignment> execute(OnlineSectioningServer server, OnlineSectioningHelper helper) {
		Lock lock = server.readLock();
		try {
			iStudent = (getStudentId() == null ? null : server.getStudent(getStudentId()));
			if (iStudent != null) {
				String filter = server.getConfig().getProperty("Filter.OnlineOnlyStudentFilter", null);
				if (filter != null && !filter.isEmpty()) {
					if (new Query(filter).match(new StudentMatcher(iStudent, server.getAcademicSession().getDefaultSectioningStatus(), server, false))) {
						iFilterIM = server.getConfig().getProperty("Filter.OnlineOnlyInstructionalModeRegExp");
					} else if (server.getConfig().getPropertyBoolean("Filter.OnlineOnlyExclusiveCourses", false)) {
						iFilterIM = server.getConfig().getProperty("Filter.ResidentialInstructionalModeRegExp");
					}
				}
			}
			if (iFilterIM != null) {
				if (helper.hasAdminPermission() && server.getConfig().getPropertyBoolean("Filter.OnlineOnlyAdminOverride", false))
					iFilterIM = null;
				else if (helper.hasAvisorPermission() && server.getConfig().getPropertyBoolean("Filter.OnlineOnlyAdvisorOverride", false))
					iFilterIM = null;
			}
			List<CourseAssignment> courses = null;
			if (iRequest != null) {
				courses = new ArrayList<CourseAssignment>();
				for (Long courseId: iRequest.getCourseIds()) {
					XCourse course = server.getCourse(courseId);
					if (course != null)
						courses.add(convert(course, server));
				}
			} else {
				courses = listCourses(server, helper);
			}
			if (courses != null && !courses.isEmpty() && courses.size() <= 1000) {
				List<OverrideType> overrides = OverrideTypeDAO.getInstance().findAll(helper.getHibSession(), Order.asc("label"));
				if (overrides != null && !overrides.isEmpty()) {
					Map<Long, CourseAssignment> table = new HashMap<Long, CourseAssignment>();
					for (CourseAssignment ca: courses)
						table.put(ca.getCourseId(), ca);
					for (CourseOffering co: (List<CourseOffering>)helper.getHibSession().createQuery("from CourseOffering co left join fetch co.disabledOverrides do where co.uniqueId in :courseIds")
							.setParameterList("courseIds", table.keySet(), LongType.INSTANCE).list()) {
						for (OverrideType override: overrides)
							if (!co.getDisabledOverrides().contains(override))
								table.get(co.getUniqueId()).addOverride(override.getReference(), override.getLabel());
					}
				}
			}
			if (ApplicationProperty.ListCourseOfferingsMatchingCampusFirst.isTrue() &&  iStudent != null && courses != null && !courses.isEmpty()) {
				XAreaClassificationMajor primary = iStudent.getPrimaryMajor();
				final String campus = (primary == null ? null : primary.getCampus());
				if (campus != null && !campus.equals(server.getAcademicSession().getCampus())) {
					ExternalTermProvider ext = Customization.ExternalTermProvider.getProvider();
					List<CourseAssignment> ret = new ArrayList<CourseAssignment>(courses.size());
					for (CourseAssignment ca: courses) {
						if (ext == null) {
							if (ca.getSubject().startsWith(campus + " - ")) ret.add(ca);
						} else {
							if (campus.equals(ext.getExternalCourseCampus(server.getAcademicSession(), ca.getSubject(), ca.getCourseNbr())))
								ret.add(ca);
						}
					}
					if (ret.isEmpty()) return courses;
					for (CourseAssignment ca: courses) {
						if (ext == null) {
							if (!ca.getSubject().startsWith(campus + " - ")) ret.add(ca);
						} else {
							if (!campus.equals(ext.getExternalCourseCampus(server.getAcademicSession(), ca.getSubject(), ca.getCourseNbr())))
								ret.add(ca);
						}
					}
					return ret;
				}
			}
			return courses;
		} finally {
			lock.release();
		}
	}
	
	protected List<CourseAssignment> listCourses(OnlineSectioningServer server, OnlineSectioningHelper helper) {
		List<CourseAssignment> ret = customCourseLookup(server, helper);
		if (ret != null && !ret.isEmpty()) return ret;
				
		ret = new ArrayList<CourseAssignment>();
		for (XCourseId id: server.findCourses(iQuery, iLimit, iMatcher)) {
			XCourse course = server.getCourse(id.getCourseId());
			if (course != null)
				ret.add(convert(course, server));
		}
		return ret;
	}
	
	protected List<CourseAssignment> customCourseLookup(OnlineSectioningServer server, OnlineSectioningHelper helper) {
		try {
			if (iMatcher != null) iMatcher.setServer(server);
			if (iQuery != null && !iQuery.isEmpty() && CustomCourseLookupHolder.hasProvider()) {
				List<XCourse> courses = CustomCourseLookupHolder.getProvider().getCourses(server, helper, iQuery, true);
				if (courses != null && !courses.isEmpty()) {
					List<CourseAssignment> ret = new ArrayList<CourseAssignment>();
					for (XCourse course: courses) {
						if (course != null && (iMatcher == null || iMatcher.match(course)))
							ret.add(convert(course, server));
					}
					setSelection(ret);
					return ret;
				}
			}
		} catch (Exception e) {
			helper.error("Failed to use the custom course lookup: " + e.getMessage(), e);
		}
		return null;
	}
	
	protected CourseAssignment convert(XCourse c, OnlineSectioningServer server) {
		CourseAssignment course = new CourseAssignment();
		course.setCourseId(c.getCourseId());
		course.setSubject(c.getSubjectArea());
		course.setCourseNbr(c.getCourseNumber());
		course.setTitle(c.getTitle());
		course.setNote(c.getNote());
		course.setCreditAbbv(c.getCreditAbbv());
		course.setCreditText(c.getCreditText());
		course.setTitle(c.getTitle());
		course.setHasUniqueName(c.hasUniqueName());
		course.setLimit(c.getLimit());
		course.setSnapShotLimit(c.getSnapshotLimit());
		XOffering offering = server.getOffering(c.getOfferingId());
		XEnrollment enrollment = null;
		if (iFilterIM != null && iStudent != null) {
			XCourseRequest r = iStudent.getRequestForCourse(c.getCourseId());
			enrollment = (r == null ? null : r.getEnrollment());
		}
		if (offering != null) {
			course.setAvailability(offering.getCourseAvailability(server.getRequests(c.getOfferingId()), c));
			for (XConfig config: offering.getConfigs()) {
				if (iFilterIM != null && (enrollment == null || !config.getConfigId().equals(enrollment.getConfigId()))) {
					String imRef = (config.getInstructionalMethod() == null ? null : config.getInstructionalMethod().getReference());
        			if (iFilterIM.isEmpty()) {
        				if (imRef != null && !imRef.isEmpty())
        					continue;
        			} else {
        				if (imRef == null || !imRef.matches(iFilterIM))
        					continue;
        			}
				}
				if (config.getInstructionalMethod() != null)
					course.addInstructionalMethod(config.getInstructionalMethod().getUniqueId(), config.getInstructionalMethod().getLabel());
				else
					course.setHasNoInstructionalMethod(true);
			}
			course.setHasCrossList(offering.hasCrossList());
			course.setCanWaitList(offering.isWaitList());
			
		}
		return course;
	}
	
	static interface SelectionModeInterface {
		public int getPoints(CourseAssignment ca);
	}
	
	public static enum SelectionMode implements Comparator<CourseAssignment>{
		availability(new SelectionModeInterface() {
			@Override
			public int getPoints(CourseAssignment ca) {
				int p = 0;
				if (ca.getLimit() != null)
					p += 4 * (ca.getLimit() < 0 ? 9999 : ca.getLimit());
				if (ca.getEnrollment() != null)
					p -= 3 * (ca.getEnrollment());
				if (ca.getRequested() != null)
					p -= ca.getRequested();
				return p;
			}
			
		}),
		limit(new SelectionModeInterface() {
			@Override
			public int getPoints(CourseAssignment ca) {
				return (ca.getLimit() < 0 ? 999 : ca.getLimit());
			}
			
		}),
		snapshot(new SelectionModeInterface() {
			@Override
			public int getPoints(CourseAssignment ca) {
				int snapshot = (ca.getSnapShotLimit() == null ? 0 : ca.getSnapShotLimit() < 0 ? 999 : ca.getSnapShotLimit());
				int limit = (ca.getLimit() < 0 ? 999 : ca.getLimit());
				return Math.max(snapshot, limit);
			}
			
		}),
		;
		
		SelectionModeInterface iMode;
		SelectionMode(SelectionModeInterface mode) {
			iMode = mode;
		}
		
		public int getPoints(CourseAssignment ca) {
			return iMode.getPoints(ca);
		}

		@Override
		public int compare(CourseAssignment ca1, CourseAssignment ca2) {
			int p1 = getPoints(ca1);
			int p2 = getPoints(ca2);
			if (p1 != p2) return (p1 > p2 ? -1 : 1);
			return ca1.getCourseNameWithTitle().compareTo(ca2.getCourseNameWithTitle());
		}
	}
	
	public static void setSelection(List<CourseAssignment> courses) {
		if (courses == null || courses.isEmpty()) return;
		SelectionMode mode = SelectionMode.valueOf(ApplicationProperty.ListCourseOfferingsSelectionMode.value());
		int limit = ApplicationProperty.ListCourseOfferingsSelectionLimit.intValue();
		if (ApplicationProperty.ListCourseOfferingsSelectionRandomize.isTrue()) {
			RouletteWheelSelection<CourseAssignment> roulette = new RouletteWheelSelection<CourseAssignment>();
			for (CourseAssignment ca: courses) {
				int p = mode.getPoints(ca);
				if (p > 0) roulette.add(ca, p);
			}
			int idx = 0;
			while (roulette.hasMoreElements() && idx < limit) {
				CourseAssignment ca = roulette.nextElement();
				ca.setSelection(idx++);
			}
		} else {
			List<CourseAssignment> sorted = new ArrayList<CourseAssignment>(courses);
			Collections.sort(sorted, mode);
			int idx = 0;
			for (CourseAssignment ca: sorted) {
				int p = mode.getPoints(ca);
				if (p <= 0 || idx >= limit) break;
				ca.setSelection(idx++);
			}
		}
	}

	@Override
	public String name() {
		return "list-courses";
	}
}
