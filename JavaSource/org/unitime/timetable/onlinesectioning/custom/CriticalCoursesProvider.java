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
package org.unitime.timetable.onlinesectioning.custom;

import org.unitime.timetable.model.AdvisorCourseRequest;
import org.unitime.timetable.model.CourseOffering;
import org.unitime.timetable.onlinesectioning.OnlineSectioningHelper;
import org.unitime.timetable.onlinesectioning.OnlineSectioningLog;
import org.unitime.timetable.onlinesectioning.OnlineSectioningServer;
import org.unitime.timetable.onlinesectioning.model.XAdvisorRequest;
import org.unitime.timetable.onlinesectioning.model.XCourseId;
import org.unitime.timetable.onlinesectioning.model.XStudentId;

/**
 * @author Tomas Muller
 */
public interface CriticalCoursesProvider {
	
	public CriticalCourses getCriticalCourses(OnlineSectioningServer server, OnlineSectioningHelper helper, XStudentId student);
	public CriticalCourses getCriticalCourses(OnlineSectioningServer server, OnlineSectioningHelper helper, XStudentId student, OnlineSectioningLog.Action.Builder action);
	
	public void dispose();
	
	public static interface CriticalCourses {
		public boolean isEmpty();
		public int isCritical(CourseOffering course);
		public int isCritical(XCourseId course);
	}
	
	public static interface AdvisorCriticalCourses {
		public int isCritical(AdvisorCourseRequest request);
		public int isCritical(XAdvisorRequest request);
	}
}
