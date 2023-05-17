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
package org.unitime.timetable.model.base;

import java.io.Serializable;

import org.unitime.timetable.model.LearningManagementSystemInfo;
import org.unitime.timetable.model.RefTableEntry;
import org.unitime.timetable.model.Session;

/**
 * Do not change this class. It has been automatically generated using ant create-model.
 * @see org.unitime.commons.ant.CreateBaseModelFromXml
 */
public abstract class BaseLearningManagementSystemInfo extends RefTableEntry implements Serializable {
	private static final long serialVersionUID = 1L;

	private String iExternalUniqueId;
	private Boolean iDefaultLms;

	private Session iSession;

	public static String PROP_EXTERNAL_UID = "externalUniqueId";
	public static String PROP_DEFAULT_LMS = "defaultLms";

	public BaseLearningManagementSystemInfo() {
		initialize();
	}

	public BaseLearningManagementSystemInfo(Long uniqueId) {
		setUniqueId(uniqueId);
		initialize();
	}

	protected void initialize() {}

	public String getExternalUniqueId() { return iExternalUniqueId; }
	public void setExternalUniqueId(String externalUniqueId) { iExternalUniqueId = externalUniqueId; }

	public Boolean isDefaultLms() { return iDefaultLms; }
	public Boolean getDefaultLms() { return iDefaultLms; }
	public void setDefaultLms(Boolean defaultLms) { iDefaultLms = defaultLms; }

	public Session getSession() { return iSession; }
	public void setSession(Session session) { iSession = session; }

	public boolean equals(Object o) {
		if (o == null || !(o instanceof LearningManagementSystemInfo)) return false;
		if (getUniqueId() == null || ((LearningManagementSystemInfo)o).getUniqueId() == null) return false;
		return getUniqueId().equals(((LearningManagementSystemInfo)o).getUniqueId());
	}

	public int hashCode() {
		if (getUniqueId() == null) return super.hashCode();
		return getUniqueId().hashCode();
	}

	public String toString() {
		return "LearningManagementSystemInfo["+getUniqueId()+" "+getLabel()+"]";
	}

	public String toDebugString() {
		return "LearningManagementSystemInfo[" +
			"\n	DefaultLms: " + getDefaultLms() +
			"\n	ExternalUniqueId: " + getExternalUniqueId() +
			"\n	Label: " + getLabel() +
			"\n	Reference: " + getReference() +
			"\n	Session: " + getSession() +
			"\n	UniqueId: " + getUniqueId() +
			"]";
	}
}
