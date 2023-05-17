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
package org.unitime.timetable.model;

import java.util.List;

import org.unitime.timetable.model.base.BaseInstructorCourseRequirementType;
import org.unitime.timetable.model.dao.InstructorCourseRequirementTypeDAO;

public class InstructorCourseRequirementType extends BaseInstructorCourseRequirementType {
	private static final long serialVersionUID = -2083877056369018586L;

	public InstructorCourseRequirementType() {
		super();
	}
	
	public static List<InstructorCourseRequirementType> getInstructorCourseRequirementTypes() {
		return (List<InstructorCourseRequirementType>)InstructorCourseRequirementTypeDAO.getInstance().getSession().createQuery(
				"from InstructorCourseRequirementType order by sortOrder")
				.setCacheable(true).list();
	}

}
