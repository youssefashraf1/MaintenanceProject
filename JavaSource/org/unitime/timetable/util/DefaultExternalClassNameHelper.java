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
package org.unitime.timetable.util;

import org.unitime.timetable.interfaces.ExternalClassNameHelperInterface;
import org.unitime.timetable.model.Class_;
import org.unitime.timetable.model.CourseOffering;
import org.unitime.timetable.model.SchedulingSubpart;
import org.unitime.timetable.model.comparators.SchedulingSubpartComparator;

/**
 * @author Stephanie Schluttenhofer, Tomas Muller
 *
 */
public class DefaultExternalClassNameHelper implements
	ExternalClassNameHelperInterface, ExternalClassNameHelperInterface.HasGradableSubpart {

	/**
	 * 
	 */
	public DefaultExternalClassNameHelper() {
		// do nothing
	}

	/* (non-Javadoc)
	 * @see org.unitime.timetable.interfaces.ExternalClassNameHelperInterface#getClassLabel(org.unitime.timetable.model.Class_, org.unitime.timetable.model.CourseOffering)
	 */
	public String getClassLabel(Class_ clazz, CourseOffering courseOffering) {
    	return courseOffering.getCourseName()+" "+clazz.getItypeDesc().trim()+" "+clazz.getSectionNumberString();
	}

	/* (non-Javadoc)
	 * @see org.unitime.timetable.interfaces.ExternalClassNameHelperInterface#getClassSuffix(org.unitime.timetable.model.Class_, org.unitime.timetable.model.CourseOffering)
	 */
	public String getClassSuffix(Class_ clazz, CourseOffering courseOffering) {
		return(clazz.getClassSuffix());
	}

	/* (non-Javadoc)
	 * @see org.unitime.timetable.interfaces.ExternalClassNameHelperInterface#getClassLabelWithTitle(org.unitime.timetable.model.Class_, org.unitime.timetable.model.CourseOffering)
	 */
	public String getClassLabelWithTitle(Class_ clazz, CourseOffering courseOffering) {
    	return courseOffering.getCourseNameWithTitle()+" "+clazz.getItypeDesc().trim()+" "+clazz.getSectionNumberString();
	}

	/* (non-Javadoc)
	 * @see org.unitime.timetable.interfaces.ExternalClassNameHelperInterface#getClassLabelWithTitle(org.unitime.timetable.model.Class_, org.unitime.timetable.model.CourseOffering)
	 */
	public String getExternalId(Class_ clazz, CourseOffering courseOffering) {
		return(clazz.getExternalUniqueId());
	}

	@Override
	public Float getClassCredit(Class_ clazz, CourseOffering courseOffering) {
		return null;
	}

	@Override
	public boolean isGradableSubpart(SchedulingSubpart subpart, CourseOffering courseOffering, org.hibernate.Session hibSession) {
		// there is only one subpart -> true
		if (subpart.getInstrOfferingConfig().getSchedulingSubparts().size() == 1) return true;
		// has a parent -> false
		if (subpart.getParentSubpart() != null) return false;
		// otherwise, check that this subpart is the first one
		SchedulingSubpartComparator cmp = new SchedulingSubpartComparator();
		for (SchedulingSubpart s: subpart.getInstrOfferingConfig().getSchedulingSubparts()) {
			if (cmp.compare(s, subpart) < 0) return false;
		}
		return true;
	}
}
