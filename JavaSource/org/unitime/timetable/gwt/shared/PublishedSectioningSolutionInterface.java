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
package org.unitime.timetable.gwt.shared;

import java.util.Date;
import java.util.Map;

import org.unitime.timetable.gwt.command.client.GwtRpcRequest;
import org.unitime.timetable.gwt.command.client.GwtRpcResponseList;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * @author Tomas Muller
 */
public class PublishedSectioningSolutionInterface implements IsSerializable {
	private Long iUniqueId;
	private Date iTimeStamp;
	private String iOwner;
	private Map<String, String> iInfo;
	private boolean iLoaded, iClonned, iSelected;
	private boolean iCanSelect, iCanClone, iCanLoad, iCanChangeNote;
	private String iConfig, iNote;
	
	public PublishedSectioningSolutionInterface() {}
	
	public Long getUniqueId() { return iUniqueId; }
	public void setUniqueId(Long uniqueId) { iUniqueId = uniqueId; }
	public Date getTimeStamp() { return iTimeStamp; }
	public void setTimeStamp(Date ts) { iTimeStamp = ts; }
	public String getOwner() { return iOwner; }
	public void setOwner(String owner) { iOwner = owner; }
	public Map<String, String> getInfo() { return iInfo; }
	public void setInfo(Map<String,String> info) { iInfo = info; }
	public boolean isLoaded() { return iLoaded; }
	public void setLoaded(boolean loaded) { iLoaded = loaded; }
	public boolean isClonned() { return iClonned; }
	public void setClonned(boolean clonned) { iClonned = clonned; }
	public boolean isSelected() { return iSelected; }
	public void setSelected(boolean selected) { iSelected = selected; }
	
	public boolean isCanLoad() { return iCanLoad; }
	public void setCanLoad(boolean canLoad) { iCanLoad = canLoad; }
	public boolean isCanSelect() { return iCanSelect; }
	public void setCanSelect(boolean canSelect) { iCanSelect = canSelect; }
	public boolean isCanClone() { return iCanClone; }
	public void setCanClone(boolean canClone) { iCanClone = canClone; }
	
	public boolean hasConfig() { return iConfig != null && !iConfig.isEmpty(); }
	public String getConfig() { return iConfig; }
	public void setConfig(String config) { iConfig = config; }
	
	public boolean hasNote() { return iNote != null && !iNote.isEmpty(); }
	public String getNote() { return iNote; }
	public void setNote(String note) { iNote = note; }
	public boolean isCanChangeNote() { return iCanChangeNote; }
	public void setCanChangeNote(boolean canChangeNote) { iCanChangeNote = canChangeNote; }
	
	public String getValue(String attribute) { 
		if (iInfo == null) return "";
		String value = iInfo.get(attribute);
		return (value == null ? "" : value.replace(" (", "\n("));
	}
	
	public boolean hasValue(String attribute) {
		if (iInfo == null) return false;
		String value = iInfo.get(attribute);
		return value != null && !value.isEmpty();
	}
	
	public static enum Operation implements IsSerializable {
		LIST, REMOVE, LOAD, UNLOAD, PUBLISH, UNPUBLISH, SELECT, DESELECT, NOTE,
	}
	
	public static class PublishedSectioningSolutionsRequest implements GwtRpcRequest<GwtRpcResponseList<PublishedSectioningSolutionInterface>> {
		private Operation iOperation;
		private Long iUniqueId;
		private String iNote;
		
		public PublishedSectioningSolutionsRequest() {}
		public PublishedSectioningSolutionsRequest(Operation operation) {
			iOperation = operation;
		}
		public PublishedSectioningSolutionsRequest(Operation operation, Long uniqueId) {
			iOperation = operation; iUniqueId = uniqueId;
		}
		public PublishedSectioningSolutionsRequest(Long uniqueId, String note) {
			iOperation = Operation.NOTE; iUniqueId = uniqueId; iNote = note;
		}
		
		public Long getUniqueId() { return iUniqueId; }
		public void setUniqueId(Long uniqueId) { iUniqueId = uniqueId; }
		public Operation getOperation() { return iOperation; }
		public void setOperation(Operation operation) { iOperation = operation; }
		public void setNote(String note) { iNote = note; }
		public String getNote() { return iNote; }
	}
	
	public static enum TableColumn {
		DATE_TIME,
		OWNER,
		CONFIG,
		COURSE_REQUESTS("Assigned course requests"),
		PRIORITY_REQUESTS("Assigned priority course requests"),
		LC("Assigned lc course requests"),
		CRITICAL("Assigned critical course requests"),
		IMPORTANT("Assigned important course requests"),
		VITAL("Assigned vital course requests"),
		COMPLETE("Students with complete schedule"),
		SELECTION("Selection"),
		DISTANCE("Student distance conflicts"),
		TIME("Time overlapping conflicts"),
		DISBALANCED("Sections disbalanced by 10% or more"),
		NO_TIME("Using classes w/o time"),
		NOTE,
		OPERATIONS,
		;
		String iProperty;
		TableColumn() {}
		TableColumn(String property) { iProperty = property; }
		
		public String getAttribute() { return iProperty; }
	}
}
