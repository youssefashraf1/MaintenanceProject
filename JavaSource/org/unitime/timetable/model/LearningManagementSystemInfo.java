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

import org.unitime.timetable.model.base.BaseLearningManagementSystemInfo;
import org.unitime.timetable.model.dao.LearningManagementSystemInfoDAO;
import org.unitime.timetable.security.UserContext;

public class LearningManagementSystemInfo extends BaseLearningManagementSystemInfo {

    /**
	 * 
	 */
	private static final long serialVersionUID = 45964274048126169L;
	public static String LEARNING_MANAGEMENT_SYSTEM_LIST_ATTR = "lmsList";

	public LearningManagementSystemInfo() {
		super();
	}

    public boolean isUsed(org.hibernate.Session hibSession) {
    	    if (this.isDefaultLms()) {
    	    		return(true);
    	    }
    	    return ((Number)(hibSession == null ? LearningManagementSystemInfoDAO.getInstance().getSession() : hibSession).createQuery(
    			"select count(c) from Class_ c where c.lmsInfo.uniqueId = :lmsId")
    			.setLong("lmsId", getUniqueId()).setCacheable(true).uniqueResult()).intValue() > 0;
    }

	public static List<LearningManagementSystemInfo> findAll(UserContext user) {
	   	return(findAll(user.getCurrentAcademicSessionId()));
	}
	
	public static List<LearningManagementSystemInfo> findAll(Long sessionId) {
	   	@SuppressWarnings("unchecked")
		List<LearningManagementSystemInfo> list = (List<LearningManagementSystemInfo>)LearningManagementSystemInfoDAO.getInstance().getSession().createQuery(
    			"select distinct lms from LearningManagementSystemInfo as lms where lms.session.uniqueId=:sessionId")
    			.setLong("sessionId", sessionId)
    			.setCacheable(true).list();
	   	return(list);
	}

	public static LearningManagementSystemInfo findBySessionIdAndReference(Long sessionId, String reference) {
	   	@SuppressWarnings("unchecked")
		LearningManagementSystemInfo lms = (LearningManagementSystemInfo)LearningManagementSystemInfoDAO.getInstance().getSession().createQuery(
    			"select distinct lms from LearningManagementSystemInfo as lms where lms.session.uniqueId=:sessionId and lms.reference = :ref")
    			.setLong("sessionId", sessionId).setString("ref", reference)
    			.setCacheable(true).uniqueResult();

	   	return(lms);
	}

	public static LearningManagementSystemInfo getDefaultIfExists(Long sessionId) {
		return((LearningManagementSystemInfo)LearningManagementSystemInfoDAO.getInstance().getSession().createQuery(
    			"select distinct lms from LearningManagementSystemInfo as lms where lms.session.uniqueId=:sessionId and lms.defaultLms = true")
    			.setLong("sessionId", sessionId)
    			.setCacheable(true).uniqueResult());
	}
	
    public static boolean isLmsInfoDefinedForSession(Long sessionId) {
    	LearningManagementSystemInfoDAO lmsdao = LearningManagementSystemInfoDAO.getInstance();
      return(lmsdao.findBySession(lmsdao.getSession(), sessionId).size() > 0);
    }

    public static boolean isLmsInfoDefinedForSession(Session hibSession, Long sessionId) {
      return(isLmsInfoDefinedForSession(hibSession, sessionId));
    }
    
	public Object clone(){
		LearningManagementSystemInfo newLms = new LearningManagementSystemInfo();
		newLms.setReference(getReference());
		newLms.setLabel(getLabel());
		newLms.setExternalUniqueId(getExternalUniqueId());
		newLms.setDefaultLms(getDefaultLms());
		newLms.setSession(getSession());
		return(newLms);
	}


}
