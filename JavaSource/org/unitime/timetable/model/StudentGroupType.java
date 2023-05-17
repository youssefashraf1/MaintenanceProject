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

import org.unitime.timetable.model.base.BaseStudentGroupType;
import org.unitime.timetable.model.dao.StudentGroupTypeDAO;

public class StudentGroupType extends BaseStudentGroupType {
	private static final long serialVersionUID = 1L;

	public StudentGroupType() {
		super();
	}
	
	public static StudentGroupType findByReference(String reference, org.hibernate.Session hibSession) {
		return (StudentGroupType)(hibSession == null ? StudentGroupTypeDAO.getInstance().getSession() : hibSession).createQuery(
				"from StudentGroupType where reference = :reference"
				).setString("reference", reference).setMaxResults(1).setCacheable(true).uniqueResult();
	}
	
	public static enum AllowDisabledSection {
		NotAllowed,
		WithGroupReservation,
		AlwaysAllowed,
	}
	
	public AllowDisabledSection getAllowDisabledSection() {
		if (getAllowDisabled() == null)
			return AllowDisabledSection.NotAllowed;
		return AllowDisabledSection.values()[getAllowDisabled()];
	}
	
	public void setAllowDisabledSection(AllowDisabledSection allow) {
		if (allow == null)
			setAllowDisabled((short)AllowDisabledSection.NotAllowed.ordinal());
		else
			setAllowDisabled((short)allow.ordinal());
	}

}
