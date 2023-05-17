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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.unitime.timetable.gwt.command.client.GwtRpcRequest;
import org.unitime.timetable.gwt.command.client.GwtRpcResponse;
import org.unitime.timetable.gwt.shared.ClassAssignmentInterface.ClassAssignment;
import org.unitime.timetable.gwt.shared.ClassAssignmentInterface.ErrorMessage;
import org.unitime.timetable.gwt.shared.CourseRequestInterface.RequestedCourse;
import org.unitime.timetable.gwt.shared.OnlineSectioningInterface.EligibilityCheck;
import org.unitime.timetable.gwt.shared.OnlineSectioningInterface.GradeMode;
import org.unitime.timetable.gwt.shared.OnlineSectioningInterface.GradeModes;
import org.unitime.timetable.gwt.shared.OnlineSectioningInterface.StudentSectioningContext;
import org.unitime.timetable.gwt.shared.OnlineSectioningInterface.EligibilityCheck.EligibilityFlag;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;

/**
 * @author Tomas Muller
 */
public class SpecialRegistrationInterface implements IsSerializable, Serializable {
	private static final long serialVersionUID = 1L;
	
	public static class SpecialRegistrationContext implements IsSerializable, Serializable {
		private static final long serialVersionUID = 1L;
		private boolean iSpecReg = false;
		private String iSpecRegRequestId = null;
		private boolean iSpecRegDisclaimerAccepted = false;
		private boolean iSpecRegTimeConfs = false;
		private boolean iSpecRegSpaceConfs = false;
		private boolean iSpecRegLinkedConfs = false;
		private boolean iSpecRegDeadlineConfs = false;
		private boolean iSpecRegChangeRequestNote = false;
		private SpecialRegistrationStatus iSpecRegStatus = null;
		private String iDisclaimer;
		private boolean iCanRequire = true;
		private ChangeRequestorNoteInterface iChangeRequestorNote = null;

		public SpecialRegistrationContext() {}
		public SpecialRegistrationContext(SpecialRegistrationContext cx) {
			copy(cx);
		}
		public void copy(SpecialRegistrationContext cx) {
			iSpecReg = cx.iSpecReg;
			iSpecRegRequestId = cx.iSpecRegRequestId;
			iSpecRegDisclaimerAccepted = cx.iSpecRegDisclaimerAccepted;
			iSpecRegTimeConfs = cx.iSpecRegTimeConfs;
			iSpecRegSpaceConfs = cx.iSpecRegSpaceConfs;
			iSpecRegLinkedConfs = cx.iSpecRegLinkedConfs;
			iSpecRegDeadlineConfs = cx.iSpecRegDeadlineConfs;
			iSpecRegStatus = cx.iSpecRegStatus;
			iCanRequire = cx.iCanRequire;
			iSpecRegChangeRequestNote = cx.iSpecRegChangeRequestNote;
		}
		
		public boolean isEnabled() { return iSpecReg; }
		public void setEnabled(boolean specReg) { iSpecReg = specReg; }
		public boolean hasRequestId() { return iSpecRegRequestId != null; }
		public String getRequestId() { return iSpecRegRequestId; }
		public void setRequestId(String id) { iSpecRegRequestId = id; }
		public boolean isDisclaimerAccepted() { return iSpecRegDisclaimerAccepted; }
		public void setDisclaimerAccepted(boolean accepted) { iSpecRegDisclaimerAccepted = accepted; }
		public boolean areTimeConflictsAllowed() { return iSpecRegTimeConfs; }
		public void setTimeConflictsAllowed(boolean allow) { iSpecRegTimeConfs = allow; }
		public boolean areSpaceConflictsAllowed() { return iSpecRegSpaceConfs; }
		public void setSpaceConflictsAllowed(boolean allow) { iSpecRegSpaceConfs = allow; }
		public boolean areLinkedConflictsAllowed() { return iSpecRegLinkedConfs; }
		public void setLinkedConflictsAllowed(boolean allow) { iSpecRegLinkedConfs = allow; }
		public boolean areDeadlineConflictsAllowed() { return iSpecRegDeadlineConfs; }
		public void setDeadlineConflictsAllowed(boolean allow) { iSpecRegDeadlineConfs = allow; }

		public SpecialRegistrationStatus getStatus() { return iSpecRegStatus; }
		public void setStatus(SpecialRegistrationStatus status) { iSpecRegStatus = status; }
		public String getDisclaimer() { return iDisclaimer; }
		public void setDisclaimer(String disclaimer) { iDisclaimer = disclaimer; }
		public boolean hasDisclaimer() { return iDisclaimer != null && !iDisclaimer.isEmpty(); }
		public boolean isCanRequire() { return iCanRequire; }
		public void setCanRequire(boolean canRequire) { iCanRequire = canRequire; }
		public boolean isAllowChangeRequestNote() { return iSpecRegChangeRequestNote; }
		public void setAllowChangeRequestNote(boolean changeRequestNote) { iSpecRegChangeRequestNote = changeRequestNote; } 
		public void update(EligibilityCheck check) {
			iSpecRegTimeConfs = check != null && check.hasFlag(EligibilityFlag.SR_TIME_CONF);
			iSpecRegSpaceConfs = check != null && check.hasFlag(EligibilityFlag.SR_LIMIT_CONF);
			iSpecRegLinkedConfs = check != null && check.hasFlag(EligibilityFlag.SR_LINK_CONF);
			iSpecRegDeadlineConfs = check != null && check.hasFlag(EligibilityFlag.SR_EXTENDED);
			iSpecReg = check != null && check.hasFlag(EligibilityFlag.CAN_SPECREG);
			iDisclaimer = (check != null ? check.getOverrideRequestDisclaimer() : null);
			iCanRequire = check == null || check.hasFlag(EligibilityFlag.CAN_REQUIRE);
			iSpecRegChangeRequestNote = check != null && check.hasFlag(EligibilityFlag.SR_CHANGE_NOTE);
		}
		public void reset() {
			iSpecReg = false;
			iSpecRegRequestId = null;
			iSpecRegDisclaimerAccepted = false;
			iSpecRegTimeConfs = false;
			iSpecRegSpaceConfs = false;
			iSpecRegLinkedConfs = false;
			iSpecRegDeadlineConfs = false;
			iSpecRegStatus = null;
			iDisclaimer = null;
			iCanRequire = true;
			iSpecRegChangeRequestNote = false;
		}
		public void reset(EligibilityCheck check) {
			reset();
			if (check != null) update(check);
		}
		
		public void setChangeRequestorNote(ChangeRequestorNoteInterface changeRequestorNote) { iChangeRequestorNote = changeRequestorNote; }
		public ChangeRequestorNoteInterface getChangeRequestorNoteInterface() { return iChangeRequestorNote; }
	}
	
	public static class SpecialRegistrationEligibilityRequest extends StudentSectioningContext implements IsSerializable, Serializable {
		private static final long serialVersionUID = 1L;
		private String iRequestId;
		private Collection<ClassAssignmentInterface.ClassAssignment> iClassAssignments;
		private ArrayList<ErrorMessage> iErrors = null;
		
		public SpecialRegistrationEligibilityRequest() {}
		public SpecialRegistrationEligibilityRequest(StudentSectioningContext cx, String requestId, Collection<ClassAssignmentInterface.ClassAssignment> assignments, Collection<ErrorMessage> errors) {
			super(cx);
			iClassAssignments = assignments;
			iRequestId = requestId;
			if (errors != null)
				iErrors = new ArrayList<ErrorMessage>(errors);
		}
		
		public String getRequestId() { return iRequestId; }
		public boolean hasRequestId() { return iRequestId != null && !iRequestId.isEmpty(); }
		public void setRequestId(String requestId) { iRequestId = requestId; }
		public Collection<ClassAssignmentInterface.ClassAssignment> getClassAssignments() { return iClassAssignments; }
		public void setClassAssignments(Collection<ClassAssignmentInterface.ClassAssignment> assignments) { iClassAssignments = assignments; }
		public void addError(ErrorMessage error) {
			if (iErrors == null) iErrors = new ArrayList<ErrorMessage>();
			iErrors.add(error);
		}
		public boolean hasErrors() {
			return iErrors != null && !iErrors.isEmpty();
		}
		public ArrayList<ErrorMessage> getErrors() { return iErrors; }
	}
	
	public static class SpecialRegistrationEligibilityResponse implements IsSerializable, Serializable {
		private static final long serialVersionUID = 1L;
		private String iMessage;
		private boolean iCanSubmit;
		private List<ErrorMessage> iErrors = null;
		private List<ErrorMessage> iDeniedErrors = null;
		private List<ErrorMessage> iCancelErrors = null;
		private Set<String> iCancelRequestIds = null;
		private Float iCredit = null;
		private List<String> iSuggestions = null;
		
		public SpecialRegistrationEligibilityResponse() {}
		public SpecialRegistrationEligibilityResponse(boolean canSubmit, String message) {
			iCanSubmit = canSubmit; iMessage = message;
		}
	
		public boolean isCanSubmit() { return iCanSubmit; }
		public void setCanSubmit(boolean canSubmit) { iCanSubmit = canSubmit; }
		
		public boolean hasMessage() { return iMessage != null && !iMessage.isEmpty(); }
		public String getMessage() { return iMessage; }
		public void setMessage(String message) { iMessage = message; }
		
		public void addError(ErrorMessage error) {
			if (iErrors == null) iErrors = new ArrayList<ErrorMessage>();
			iErrors.add(error);
		}
		public boolean hasErrors() {
			return iErrors != null && !iErrors.isEmpty();
		}
		public List<ErrorMessage> getErrors() { return iErrors; }
		public void setErrors(Collection<ErrorMessage> messages) {
			if (messages == null)
				iErrors = null;
			else
				iErrors = new ArrayList<ErrorMessage>(messages);
		}
		
		public void addCancelError(ErrorMessage error) {
			if (iCancelErrors == null) iCancelErrors = new ArrayList<ErrorMessage>();
			iCancelErrors.add(error);
		}
		public boolean hasCancelErrors() {
			return iCancelErrors != null && !iCancelErrors.isEmpty();
		}
		public List<ErrorMessage> getCancelErrors() { return iCancelErrors; }
		public void setCancelErrors(Collection<ErrorMessage> messages) {
			if (messages == null)
				iCancelErrors = null;
			else
				iCancelErrors = new ArrayList<ErrorMessage>(messages);
		}
		public void addCancelRequestId(String id) {
			if (iCancelRequestIds == null) iCancelRequestIds = new HashSet<String>();
			iCancelRequestIds.add(id);
		}
		public boolean hasCancelRequestIds() { return iCancelRequestIds != null && !iCancelRequestIds.isEmpty(); }
		public Set<String> getCancelRequestIds() { return iCancelRequestIds; }
		public boolean isToBeCancelled(String requestId) { return iCancelRequestIds != null && iCancelRequestIds.contains(requestId); }
		
		public void addDeniedError(ErrorMessage error) {
			if (iDeniedErrors == null) iDeniedErrors = new ArrayList<ErrorMessage>();
			iDeniedErrors.add(error);
		}
		public boolean hasDeniedErrors() {
			return iDeniedErrors != null && !iDeniedErrors.isEmpty();
		}
		public List<ErrorMessage> getDeniedErrors() { return iDeniedErrors; }
		public void setDeniedErrors(Collection<ErrorMessage> messages) {
			if (messages == null)
				iDeniedErrors = null;
			else
				iDeniedErrors = new ArrayList<ErrorMessage>(messages);
		}
		
		public void setCredit(Float credit) { iCredit = credit; }
		public boolean hasCredit() { return iCredit != null; }
		public Float getCredit() { return iCredit; }
		
		public boolean hasSuggestions() { return iSuggestions != null && !iSuggestions.isEmpty(); }
		public List<String> getSuggestions() { return iSuggestions; }
		public void addSuggestion(String suggestion) {
			if (iSuggestions == null)
				iSuggestions = new ArrayList<String>();
			iSuggestions.add(suggestion);
		}
	}
	
	public static enum SpecialRegistrationStatus implements IsSerializable, Serializable {
		Draft, Pending, Approved, Rejected, Cancelled,
		;
	}
	
	public static enum SpecialRegistrationOperation implements IsSerializable, Serializable {
		Add, Drop, Keep,
		;
	}
	
	public static class RetrieveSpecialRegistrationResponse implements IsSerializable, Serializable, Comparable<RetrieveSpecialRegistrationResponse> {
		private static final long serialVersionUID = 1L;
		private SpecialRegistrationStatus iStatus;
		private Date iSubmitDate;
		private String iRequestId;
		private String iDescription;
		private Map<String, String> iNotes;
		private List<ClassAssignmentInterface.ClassAssignment> iChanges;
		private boolean iCanCancel = false;
		private boolean iHasTimeConflict, iHasSpaceConflict, iExtended, iHasLinkedConflict;
		private ArrayList<ErrorMessage> iErrors = null;
		private Float iMaxCredit = null;
		private List<String> iSuggestions = null;
		
		
		public RetrieveSpecialRegistrationResponse() {}
		
		public Date getSubmitDate() { return iSubmitDate; }
		public void setSubmitDate(Date date) { iSubmitDate = date; }
		
		public String getRequestId() { return iRequestId; }
		public void setRequestId(String requestId) { iRequestId = requestId; }
		
		public String getDescription() { return iDescription; }
		public void setDescription(String description) { iDescription = description; }
		
		public String getNote(String course) {
			if (iNotes == null) return null;
			String note = iNotes.get(course);
			if (note != null) return note;
			return iNotes.get("");
		}
		public void setNote(String course, String note) {
			if (note == null || note.isEmpty()) return;
			if (iNotes == null) iNotes = new HashMap<String, String>();
			iNotes.put(course, note);
		}
		
		public void setNote(Long courseId, String note) {
			if (courseId == null) setNote("MAXI", note);
			if (iChanges != null)
				for (ClassAssignmentInterface.ClassAssignment ca: iChanges)
					if (courseId.equals(ca.getCourseId())) {
						setNote(ca.getCourseName(), note);
						break;
					}
		}
		
		public SpecialRegistrationStatus getStatus() { return iStatus; }
		public void setStatus(SpecialRegistrationStatus status) { iStatus = status; }
		
		public boolean hasChanges() { return iChanges != null && !iChanges.isEmpty(); }
		public List<ClassAssignmentInterface.ClassAssignment> getChanges() { return iChanges; }
		public void addChange(ClassAssignmentInterface.ClassAssignment ca) {
			if (iChanges == null) iChanges = new ArrayList<ClassAssignmentInterface.ClassAssignment>();
			iChanges.add(ca);
		}
		
		public boolean isGradeModeChange() {
			if (iChanges == null) return false;
			for (ClassAssignmentInterface.ClassAssignment ca: iChanges)
				if (ca.getGradeMode() != null) return true;
			return false;
		}
		
		public boolean isCreditChange() {
			if (iChanges == null) return false;
			for (ClassAssignmentInterface.ClassAssignment ca: iChanges)
				if (ca.getCreditHour() != null) return true;
			return false;
		}

		public boolean isVariableTitleCourseChange() {
			if (iChanges == null) return false;
			for (ClassAssignmentInterface.ClassAssignment ca: iChanges) {
				if (ca.getGradeMode() != null && ca.getCredit() != null && ca.getSpecRegOperation() == SpecialRegistrationOperation.Add && ca.getClassId() != null && ca.getClassId() >= 0l)
					return true;
			}
			return false;
		}
		
		public boolean isAdd(Long courseId) {
			boolean hasDrop = false, hasAdd = false;
			for (ClassAssignmentInterface.ClassAssignment ca: iChanges)
				if (courseId.equals(ca.getCourseId())) {
					switch (ca.getSpecRegOperation()) {
					case Add: hasAdd = true; break;
					case Drop: hasDrop = true; break;
					}
				}
			return hasAdd && !hasDrop;
		}
		
		public boolean isDrop(Long courseId) {
			boolean hasDrop = false, hasAdd = false;
			for (ClassAssignmentInterface.ClassAssignment ca: iChanges)
				if (courseId.equals(ca.getCourseId())) {
					switch (ca.getSpecRegOperation()) {
					case Add: hasAdd = true; break;
					case Drop: hasDrop = true; break;
					}
				}
			return hasDrop && !hasAdd;
		}
		
		public boolean isChange(Long courseId) {
			boolean hasDrop = false, hasAdd = false, hasKeep = false;
			for (ClassAssignmentInterface.ClassAssignment ca: iChanges)
				if (courseId.equals(ca.getCourseId())) {
					switch (ca.getSpecRegOperation()) {
					case Add: hasAdd = true; break;
					case Drop: hasDrop = true; break;
					case Keep: hasKeep = true; break;
					}
				}
			return hasKeep || (hasDrop && hasAdd);
		}
		
		public boolean hasErrors(Long courseId) {
			for (ClassAssignmentInterface.ClassAssignment ca: iChanges)
				if (courseId.equals(ca.getCourseId()) && ca.hasError()) return true;
			return false;
		}
		
		public boolean isApproved(Long courseId) {
			boolean approved = false;
			for (ClassAssignmentInterface.ClassAssignment ca: iChanges)
				if (!ca.hasError()) {
					if (ca.getSpecRegStatus() == SpecialRegistrationStatus.Approved) approved = true;
					else return false;
				}
			return approved;
		}
		
		public boolean isHonorsGradeModeNotFullyMatching(ClassAssignmentInterface saved) {
			if (!hasChanges()) return false;
			for (ClassAssignmentInterface.ClassAssignment ch: iChanges) {
				if (ch.getGradeMode() != null && ch.getGradeMode().isHonor()) {
					boolean found = false;
					for (ClassAssignmentInterface.ClassAssignment ca: saved.getClassAssignments())
						if (ca.isSaved() && ch.getClassId().equals(ca.getClassId())) {
							found = true; break;
						}
					if (!found) return true;
				}
			}
			return false;
		}
		
		public boolean isFullyApplied(ClassAssignmentInterface saved) {
			if (!hasChanges() || isGradeModeChange() || isCreditChange() || isExtended()) return getStatus() == SpecialRegistrationStatus.Approved;
			if (saved == null) return false;
			Set<Long> courseIds = new HashSet<Long>();
			boolean enrolled = true, gmChange = false;
			changes: for (ClassAssignmentInterface.ClassAssignment ch: iChanges) {
				if (ch.getSpecRegOperation() == SpecialRegistrationOperation.Keep) {
					if (ch.getGradeMode() != null) {
						if (ch.getGradeMode().isHonor()) {
							boolean found = false;
							for (ClassAssignmentInterface.ClassAssignment ca: saved.getClassAssignments())
								if (ca.isSaved() && ch.getClassId().equals(ca.getClassId())) {
									found = true; break;
								}
							if (!found) enrolled = false;
						}
						for (ClassAssignmentInterface.ClassAssignment ca: saved.getClassAssignments())
							if (ca.isSaved() && ch.getCourseId().equals(ca.getCourseId()) && !ch.getGradeMode().equals(ca.getGradeMode())) {
								gmChange = true; break;
							}
					}
					if (ch.getCreditHour() != null) {
						for (ClassAssignmentInterface.ClassAssignment ca: saved.getClassAssignments())
							if (ca.isSaved() && ch.getCourseId().equals(ca.getCourseId()) && ca.getCreditHour() != null && ca.getCreditHour().equals(ch.getCreditHour())) {
								return false;
							}
					}
					continue;
				}
				Long courseId = ch.getCourseId();
				if (courseIds.add(courseId)) {
					boolean hasDrop = false, hasAdd = false;
					for (ClassAssignmentInterface.ClassAssignment ca: iChanges)
						if (courseId.equals(ca.getCourseId())) {
							switch (ca.getSpecRegOperation()) {
							case Add: hasAdd = true; break;
							case Drop: hasDrop = true; break;
							}
						}
					if (hasAdd && !hasDrop) {
						// continue, if the course is already added (ignore sections)
						for (ClassAssignmentInterface.ClassAssignment ca: saved.getClassAssignments())
							if (ca.isSaved() && courseId.equals(ca.getCourseId())) continue changes;
						return false;
					} else if (hasDrop && !hasAdd) {
						// continue, if the course is already dropped (ignore sections)
						for (ClassAssignmentInterface.ClassAssignment ca: saved.getClassAssignments())
							if (ca.isSaved() && courseId.equals(ca.getCourseId())) return false;
					} else {
						// check sections with an error
						for (ClassAssignmentInterface.ClassAssignment ca: iChanges) {
							if (courseId.equals(ca.getCourseId()) && ca.hasError()) {
								boolean match = false;
								for (ClassAssignmentInterface.ClassAssignment x: saved.getClassAssignments()) {
									if (x.isSaved() && ca.getClassId().equals(x.getClassId())) { match = true; break; }
								}
								// drop operation but section was found
								if (match && ca.getSpecRegOperation() == SpecialRegistrationOperation.Drop) return false;
								// add operation but section was NOT found
								if (!match && ca.getSpecRegOperation() == SpecialRegistrationOperation.Add) return false;
							}
						}
					}
				}
			}
			if (gmChange && enrolled) return false;
			return true;
		}
		
		public boolean isApplied(Long courseId, ClassAssignmentInterface saved) {
			if (courseId == null || saved == null) return false;
			boolean hasDrop = false, hasAdd = false, hasKeep = false;
			GradeMode gm = null;
			Float vc = null;
			for (ClassAssignmentInterface.ClassAssignment ca: iChanges)
				if (courseId.equals(ca.getCourseId())) {
					switch (ca.getSpecRegOperation()) {
					case Add: hasAdd = true; break;
					case Drop: hasDrop = true; break;
					case Keep:
						if (ca.getGradeMode() != null)
							gm = ca.getGradeMode();
						if (ca.getCreditHour() != null)
							vc = ca.getCreditHour();
						if (ca.getGradeMode() == null && ca.getCreditHour() == null)
							hasKeep = true;
						break;
					}
				}
			if (gm != null) {
				for (ClassAssignmentInterface.ClassAssignment ca: saved.getClassAssignments())
					if (ca.isSaved() && courseId.equals(ca.getCourseId())) {
						if (ca.getGradeMode() != null && !ca.getGradeMode().equals(gm)) return false;
						if (vc != null && ca.getCreditHour() != null && !ca.getCreditHour().equals(vc)) return false;
					}
				return true;
			} else if (vc != null) {
				for (ClassAssignmentInterface.ClassAssignment ca: saved.getClassAssignments())
					if (ca.isSaved() && courseId.equals(ca.getCourseId())) {
						if (ca.getCreditHour() != null && !ca.getCreditHour().equals(vc)) return false;
					}
				return true;
			} else if (hasKeep) {
				return false;
			} else if (hasAdd && !hasDrop) {
				// course is already added (ignore sections)
				for (ClassAssignmentInterface.ClassAssignment ca: saved.getClassAssignments())
					if (ca.isSaved() && courseId.equals(ca.getCourseId())) return true;
				return false;
			} else if (hasDrop && !hasAdd) {
				// course is already dropped (ignore sections)
				for (ClassAssignmentInterface.ClassAssignment ca: saved.getClassAssignments())
					if (ca.isSaved() && courseId.equals(ca.getCourseId())) return false;
				return true;
			} else {
				// course is changed, check sections with errors
				for (ClassAssignmentInterface.ClassAssignment ca: iChanges) 
					if (courseId.equals(ca.getCourseId()) && ca.hasError()) {
						boolean match = false;
						for (ClassAssignmentInterface.ClassAssignment x: saved.getClassAssignments()) {
							if (x.isSaved() && ca.getClassId().equals(x.getClassId())) {
								match = true; break;
							}
						}
						if (match && ca.getSpecRegOperation() == SpecialRegistrationOperation.Drop) return false;
						if (!match && ca.getSpecRegOperation() == SpecialRegistrationOperation.Add) return false;
					}
				return true;
			}
		}
		
		public boolean canCancel() { return iCanCancel; }
		public void setCanCancel(boolean canCancel) { iCanCancel = canCancel; }
		
		public boolean hasTimeConflict() { return iHasTimeConflict; }
		public void setHasTimeConflict(boolean hasTimeConflict) { iHasTimeConflict = hasTimeConflict; }
		
		public boolean hasSpaceConflict() { return iHasSpaceConflict; }
		public void setHasSpaceConflict(boolean hasSpaceConflict) { iHasSpaceConflict = hasSpaceConflict; }
		
		public boolean hasLinkedConflict() { return iHasLinkedConflict; }
		public void setHasLinkedConflict(boolean hasLinkedConflict) { iHasLinkedConflict = hasLinkedConflict; }
		
		public boolean isExtended() { return iExtended; }
		public void setExtended(boolean extended) { iExtended = extended; }
		
		public void addError(ErrorMessage error) {
			if (iErrors == null) iErrors = new ArrayList<ErrorMessage>();
			iErrors.add(error);
		}
		public boolean hasErrors() {
			return iErrors != null && !iErrors.isEmpty();
		}
		public ArrayList<ErrorMessage> getErrors() { return iErrors; }
		public boolean hasErrors(String course) {
			if (iErrors == null) return false;
			for (ErrorMessage em: iErrors) {
				if (course.equals(em.getCourse())) return true;
			}
			return false;
		}
		public boolean hasErrorCode(String code) {
			if (iErrors == null) return false;
			for (ErrorMessage em: iErrors) {
				if (code.equals(em.getCode())) return true;
			}
			return false;
		}
		
		public void setMaxCredit(Float maxCredit) { iMaxCredit = maxCredit; }
		public Float getMaxCredit() { return iMaxCredit; }
		public boolean hasMaxCredit() { return iMaxCredit != null; }
		
		public boolean hasSuggestions() { return iSuggestions != null && !iSuggestions.isEmpty(); }
		public List<String> getSuggestions() { return iSuggestions; }
		public void addSuggestion(String suggestion) {
			if (iSuggestions == null)
				iSuggestions = new ArrayList<String>();
			iSuggestions.add(suggestion);
		}
		
		@Override
		public int compareTo(RetrieveSpecialRegistrationResponse o) {
			int cmp = getSubmitDate().compareTo(o.getSubmitDate());
			if (cmp != 0) return -cmp;
			return getRequestId().compareTo(o.getRequestId());
		}
		
		public int hashCode() {
			return getRequestId().hashCode();
		}
		
		@Override
		public boolean equals(Object o) {
			if (o == null || !(o instanceof RetrieveSpecialRegistrationResponse)) return false;
			return getRequestId().equals(((RetrieveSpecialRegistrationResponse)o).getRequestId());
		}
	}
	
	public static class SubmitSpecialRegistrationRequest extends StudentSectioningContext implements IsSerializable, Serializable {
		private static final long serialVersionUID = 1L;
		private String iRequestId;
		private CourseRequestInterface iCourses;
		private Collection<ClassAssignmentInterface.ClassAssignment> iClassAssignments;
		private ArrayList<ErrorMessage> iErrors = null;
		private Map<String, String> iNote;
		private Float iCredit;
		
		public SubmitSpecialRegistrationRequest() {}
		public SubmitSpecialRegistrationRequest(StudentSectioningContext cx, String requestId, CourseRequestInterface courses, Collection<ClassAssignmentInterface.ClassAssignment> assignments, Collection<ErrorMessage> errors, Map<String, String> note, Float credit) {
			super(cx);
			iRequestId = requestId;
			iCourses = courses;
			iClassAssignments = assignments;
			if (errors != null)
				iErrors = new ArrayList<ErrorMessage>(errors);
			if (note != null)
				iNote = new HashMap<String, String>(note);
			iCredit = credit;
		}
		
		public Collection<ClassAssignmentInterface.ClassAssignment> getClassAssignments() { return iClassAssignments; }
		public void setClassAssignments(Collection<ClassAssignmentInterface.ClassAssignment> assignments) { iClassAssignments = assignments; }
		public CourseRequestInterface getCourses() { return iCourses; }
		public void setCourses(CourseRequestInterface courses) { iCourses = courses; }
		public String getRequestId() { return iRequestId; }
		public void setRequestId(String requestId) { iRequestId = requestId; }
		public void addError(ErrorMessage error) {
			if (iErrors == null) iErrors = new ArrayList<ErrorMessage>();
			iErrors.add(error);
		}
		public boolean hasErrors() {
			return iErrors != null && !iErrors.isEmpty();
		}
		public ArrayList<ErrorMessage> getErrors() { return iErrors; }
		public String getNote(String course) {
			if (iNote == null) return null;
			return iNote.get(course);
		}
		public void setNote(String course, String note) {
			if (iNote == null) iNote = new HashMap<String, String>();
			iNote.put(course, note);
		}
		public Map<String, String> getNotes() { return iNote; }
		public void setCredit(Float credit) { iCredit = credit; }
		public boolean hasCredit() { return iCredit != null; }
		public Float getCredit() { return iCredit; }
	}
	
	public static class SubmitSpecialRegistrationResponse implements IsSerializable, Serializable {
		private static final long serialVersionUID = 1L;
		private String iRequestId;
		private String iMessage;
		private boolean iSuccess;
		private SpecialRegistrationStatus iStatus = null;
		private List<RetrieveSpecialRegistrationResponse> iRequests = null;
		private Set<String> iCancelledRequestIds;
		
		public SubmitSpecialRegistrationResponse() {}
		
		public String getRequestId() { return iRequestId; }
		public void setRequestId(String requestId) { iRequestId = requestId; }
		
		public boolean hasMessage() { return iMessage != null && !iMessage.isEmpty(); }
		public String getMessage() { return iMessage; }
		public void setMessage(String message) { iMessage = message; }
		
		public boolean isSuccess() { return iSuccess; }
		public boolean isFailure() { return !iSuccess; }
		public void setSuccess(boolean success) { iSuccess = success; }
		
		public SpecialRegistrationStatus getStatus() { return iStatus; }
		public void setStatus(SpecialRegistrationStatus status) { iStatus = status; }
		
		public List<RetrieveSpecialRegistrationResponse> getRequests() { return iRequests; }
		public void addRequest(RetrieveSpecialRegistrationResponse request) {
			if (iRequests == null) iRequests = new ArrayList<RetrieveSpecialRegistrationResponse>();
			iRequests.add(request);
		}
		public boolean hasRequests() { return iRequests != null && !iRequests.isEmpty(); }
		public boolean hasRequest(String requestId) {
			if (iRequests == null) return false;
			for (RetrieveSpecialRegistrationResponse r: iRequests)
				if (requestId.equals(r.getRequestId())) return true;
			return false;
		}
		
		public void addCancelledRequest(String requestId) {
			if (iCancelledRequestIds == null) iCancelledRequestIds = new HashSet<String>();
			iCancelledRequestIds.add(requestId);
		}
		public boolean hasCancelledRequestIds() { return iCancelledRequestIds != null && !iCancelledRequestIds.isEmpty(); }
		public Set<String> getCancelledRequestIds() { return iCancelledRequestIds; }
		public boolean isCancelledRequest(String requestId) { return iCancelledRequestIds != null && iCancelledRequestIds.contains(requestId); }
	}
	
	public static class RetrieveAllSpecialRegistrationsRequest extends StudentSectioningContext implements IsSerializable, Serializable {
		private static final long serialVersionUID = 1L;
		
		public RetrieveAllSpecialRegistrationsRequest() {}
		public RetrieveAllSpecialRegistrationsRequest(StudentSectioningContext cx) {
			super(cx);
		}
	}
	
	public static class CancelSpecialRegistrationRequest extends StudentSectioningContext implements IsSerializable, Serializable {
		private static final long serialVersionUID = 1L;
		private String iRequestId;
		
		public CancelSpecialRegistrationRequest() {}
		public CancelSpecialRegistrationRequest(StudentSectioningContext cx) {
			super(cx);
		}
		
		public String getRequestId() { return iRequestId; }
		public void setRequestId(String requestId) { iRequestId = requestId; }
	}
	
	public static class CancelSpecialRegistrationResponse implements IsSerializable, Serializable {
		private static final long serialVersionUID = 1L;
		private boolean iSuccess;
		private String iMessage;
		
		public CancelSpecialRegistrationResponse() {}
		
		public boolean isSuccess() { return iSuccess; }
		public boolean isFailure() { return !iSuccess; }
		public void setSuccess(boolean success) { iSuccess = success; }
		
		public boolean hasMessage() { return iMessage != null && !iMessage.isEmpty(); }
		public String getMessage() { return iMessage; }
		public void setMessage(String message) { iMessage = message; }
	}
	
	public static class RetrieveAvailableGradeModesRequest extends StudentSectioningContext implements GwtRpcRequest<RetrieveAvailableGradeModesResponse>, Serializable {
		private static final long serialVersionUID = 1L;
		
		public RetrieveAvailableGradeModesRequest() {}
		public RetrieveAvailableGradeModesRequest(StudentSectioningContext cx) {
			super(cx);
		}
	}
	
	public static class RetrieveAvailableGradeModesResponse implements GwtRpcResponse, Serializable {
		private static final long serialVersionUID = 1L;
		Map<String, SpecialRegistrationGradeModeChanges> iModes = new HashMap<String, SpecialRegistrationGradeModeChanges>();
		Map<String, SpecialRegistrationVariableCreditChange> iVarCreds = new HashMap<String, SpecialRegistrationVariableCreditChange>();
		private Float iMaxCredit, iCurrentCredit;
		private List<String> iSuggestions = null;
		
		public RetrieveAvailableGradeModesResponse() {}
		
		public boolean hasGradeModes() { return !iModes.isEmpty(); }
		
		public void add(String sectionId, SpecialRegistrationGradeModeChanges modes) {
			iModes.put(sectionId, modes);
		}
		
		public boolean hasVariableCredits() { return !iVarCreds.isEmpty(); }
		
		public void add(String sectionId, SpecialRegistrationVariableCreditChange var) {
			iVarCreds.put(sectionId,var);
		}
		
		public SpecialRegistrationGradeModeChanges get(ClassAssignment a) {
			if (a.getExternalId() == null) return null;
			if (a.getParentSection() != null && a.getParentSection().equals(a.getSection())) return null;
			return iModes.get(a.getExternalId());
		}
		
		public SpecialRegistrationVariableCreditChange getVariableCredits(ClassAssignment a) {
			if (a.getExternalId() == null) return null;
			if (a.getParentSection() != null && a.getParentSection().equals(a.getSection())) return null;
			return iVarCreds.get(a.getExternalId());
		}
		
		public Float getMaxCredit() { return iMaxCredit; }
		public void setMaxCredit(Float credit) { iMaxCredit = credit; }
		
		public Float getCurrentCredit() { return iCurrentCredit; }
		public void setCurrentCredit(Float credit) { iCurrentCredit = credit; }
		
		public boolean hasSuggestions() { return iSuggestions != null && !iSuggestions.isEmpty(); }
		public List<String> getSuggestions() { return iSuggestions; }
		public void addSuggestion(String suggestion) {
			if (iSuggestions == null)
				iSuggestions = new ArrayList<String>();
			iSuggestions.add(suggestion);
		}
	}
	
	public static class SpecialRegistrationGradeMode extends GradeMode {
		private static final long serialVersionUID = 1L;
		private List<String> iApprovals = null;
		private String iDisclaimer = null;
		private String iOriginalGradeMode = null;
		
		public SpecialRegistrationGradeMode() {
			super();
		}
		public SpecialRegistrationGradeMode(String code, String label, boolean honors) {
			super(code, label, honors);
		}
		
		public boolean hasApprovals() { return iApprovals != null && !iApprovals.isEmpty(); }
		public List<String> getApprovals() { return iApprovals; }
		public void addApproval(String approval) {
			if (iApprovals == null) iApprovals = new ArrayList<String>();
			iApprovals.add(approval);
		}
		
		public boolean hasDisclaimer() { return iDisclaimer != null && !iDisclaimer.isEmpty(); }
		public String getDisclaimer() { return iDisclaimer; }
		public void setDisclaimer(String disclaimer) { iDisclaimer = disclaimer; }
		
		public String getOriginalGradeMode() { return iOriginalGradeMode; }
		public void setOriginalGradeMode(String mode) { iOriginalGradeMode = mode; }
	}
	
	public static class SpecialRegistrationVariableCredit implements IsSerializable, Serializable {
		private static final long serialVersionUID = 1L;
		private List<String> iApprovals = null;
		private Float iCredit = null;
		private Float iOriginalCredit = null;
		
		public SpecialRegistrationVariableCredit() {
			super();
		}
		public SpecialRegistrationVariableCredit(SpecialRegistrationVariableCreditChange change) {
			super();
			if (change.hasApprovals()) iApprovals = new ArrayList<String>(change.getApprovals());
		}
		
		public boolean hasApprovals() { return iApprovals != null && !iApprovals.isEmpty(); }
		public List<String> getApprovals() { return iApprovals; }
		public void addApproval(String approval) {
			if (iApprovals == null) iApprovals = new ArrayList<String>();
			iApprovals.add(approval);
		}
		
		public Float getOriginalCredit() { return iOriginalCredit; }
		public void setOriginalCredit(Float credit) { iOriginalCredit = credit; }
		
		public Float getCredit() { return iCredit; }
		public void setCredit(Float credit) { iCredit = credit; }
		
		public float getCreditChange() { return (iCredit == null ? 0f : iCredit.floatValue()) - (iOriginalCredit == null ? 0f : iOriginalCredit.floatValue()); }
	}
	
	public static class SpecialRegistrationGradeModeChanges implements IsSerializable, Serializable {
		private static final long serialVersionUID = 1L;
		private SpecialRegistrationGradeMode iCurrentGradeMode;
		private Set<SpecialRegistrationGradeMode> iAvailableChanges;
		
		public SpecialRegistrationGradeModeChanges() {}
		
		public SpecialRegistrationGradeMode getCurrentGradeMode() { return iCurrentGradeMode; }
		public void setCurrentGradeMode(SpecialRegistrationGradeMode mode) { iCurrentGradeMode = mode; }
		public boolean isCurrentGradeMode(String code) {
			return iCurrentGradeMode != null && iCurrentGradeMode.getCode().equals(code);
		}
		
		public void addAvailableChange(SpecialRegistrationGradeMode mode) {
			if (iAvailableChanges == null) iAvailableChanges = new TreeSet<SpecialRegistrationGradeMode>();
			iAvailableChanges.add(mode);
		}
		public boolean hasAvailableChanges() { return iAvailableChanges != null && !iAvailableChanges.isEmpty(); }
		public Set<SpecialRegistrationGradeMode> getAvailableChanges() { return iAvailableChanges ;}
		public SpecialRegistrationGradeMode getAvailableChange(String code) {
			if (iAvailableChanges == null) return null;
			for (SpecialRegistrationGradeMode m: iAvailableChanges)
				if (m.getCode().equals(code)) return m;
			return null;
		}
	}
	
	public static class SpecialRegistrationGradeModeChange implements IsSerializable, Serializable {
		private static final long serialVersionUID = 1L;
		private String iSubject, iCourse, iCredit;
		private Set<String> iCrn;
		private Set<String> iApprovals = null;
		private String iOriginalGradeMode = null;
		private String iSelectedGradeMode = null;
		private String iSelectedGradeModeDescription = null;
		private String iNote;
		
		public SpecialRegistrationGradeModeChange() {}
		
		public String getSubject() { return iSubject; }
		public void setSubject(String subject) { iSubject = subject; }
		
		public String getCourse() { return iCourse; }
		public void setCourse(String course) { iCourse = course; }
		
		public String getCredit() { return iCredit; }
		public void setCredit(String credit) { iCredit = credit; }
		
		public String getOriginalGradeMode() { return iOriginalGradeMode; }
		public void setOriginalGradeMode(String gm) { iOriginalGradeMode = gm; }
		
		public String getSelectedGradeMode() { return iSelectedGradeMode; }
		public void setSelectedGradeMode(String gm) { iSelectedGradeMode = gm; }
		
		public String getSelectedGradeModeDescription() { return iSelectedGradeModeDescription; }
		public void setSelectedGradeModeDescription(String desc) { iSelectedGradeModeDescription = desc; }
		
		public boolean hasCRNs() { return iCrn != null && iCrn.isEmpty(); }
		public void addCrn(String crn) {
			if (iCrn == null) iCrn = new TreeSet<String>();
			iCrn.add(crn);
		}
		public Set<String> getCRNs() { return iCrn; }
		public boolean hasCRN(String extId) { return iCrn != null && iCrn.contains(extId); }
		
		public boolean hasApprovals() { return iApprovals != null && !iApprovals.isEmpty(); }
		public void addApproval(String app) {
			if (iApprovals == null) iApprovals = new TreeSet<String>();
			iApprovals.add(app);
		}
		public Set<String> getApprovals() { return iApprovals; }
		
		public void setNote(String note) { iNote = note; }
		public String getNote() { return iNote; }
		public boolean hasNote() { return iNote != null && !iNote.isEmpty(); }
	}
	
	public static class SpecialRegistrationCreditChange implements IsSerializable, Serializable {
		private static final long serialVersionUID = 1L;
		private String iSubject, iCourse, iCrn;
		private Float iCredit, iOriginalCredit;
		private Set<String> iApprovals = null;
		private String iNote;
		
		public SpecialRegistrationCreditChange() {}
		
		public String getSubject() { return iSubject; }
		public void setSubject(String subject) { iSubject = subject; }
		
		public String getCourse() { return iCourse; }
		public void setCourse(String course) { iCourse = course; }

		public String getCrn() { return iCrn; }
		public void setCrn(String crn) { iCrn = crn; }
		
		public Float getOriginalCredit() { return iOriginalCredit; }
		public void setOriginalCredit(Float credit) { iOriginalCredit = credit; }
		
		public Float getCredit() { return iCredit; }
		public void setCredit(Float credit) { iCredit = credit; }

		public boolean hasApprovals() { return iApprovals != null && !iApprovals.isEmpty(); }
		public void addApproval(String app) {
			if (iApprovals == null) iApprovals = new TreeSet<String>();
			iApprovals.add(app);
		}
		public Set<String> getApprovals() { return iApprovals; }
		
		public void setNote(String note) { iNote = note; }
		public String getNote() { return iNote; }
		public boolean hasNote() { return iNote != null && !iNote.isEmpty(); }
	}
	
	public static class SpecialRegistrationVariableCreditChange implements IsSerializable, Serializable {
		private static final long serialVersionUID = 1L;
		private Set<String> iApprovals = null;
		private Set<Float> iAvailableCredits = null;
		
		public SpecialRegistrationVariableCreditChange() {}

		public boolean hasApprovals() { return iApprovals != null && !iApprovals.isEmpty(); }
		public void addApproval(String app) {
			if (iApprovals == null) iApprovals = new TreeSet<String>();
			iApprovals.add(app);
		}
		public Set<String> getApprovals() { return iApprovals; }
		
		public boolean hasAvailableCredits() { return iAvailableCredits != null && !iAvailableCredits.isEmpty(); }
		public void addAvailableCredit(Float credit) {
			if (iAvailableCredits == null) iAvailableCredits = new TreeSet<Float>();
			iAvailableCredits.add(credit);
		}
		public Set<Float> getAvailableCredits() { return iAvailableCredits; }
	}
	
	public static class ChangeGradeModesRequest extends StudentSectioningContext implements GwtRpcRequest<ChangeGradeModesResponse>, Serializable {
		private static final long serialVersionUID = 1L;
		List<SpecialRegistrationGradeModeChange> iChanges = new ArrayList<SpecialRegistrationGradeModeChange>();
		List<SpecialRegistrationCreditChange> iCreditChanges = new ArrayList<SpecialRegistrationCreditChange>();
		private Float iMaxCredit, iCurrentCredit;
		private String iNote;
		
		public ChangeGradeModesRequest() {}
		public ChangeGradeModesRequest(StudentSectioningContext cx) {
			super(cx);
		}
		
		public boolean hasGradeModeChanges() { return !iChanges.isEmpty(); }
		
		public void addChange(SpecialRegistrationGradeModeChange change) {
			iChanges.add(change);
		}
		
		public SpecialRegistrationGradeModeChange getChange(String sectionId) {
			for (SpecialRegistrationGradeModeChange ch: iChanges)
				if (ch.hasCRN(sectionId)) return ch;
			return null;
		}
		
		public List<SpecialRegistrationGradeModeChange> getChanges() {
			return iChanges;
		}
		
		public boolean hasGradeModeChanges(boolean approval) {
			for (SpecialRegistrationGradeModeChange change: iChanges) {
				if (approval && change.hasApprovals()) return true;
				if (!approval && !change.hasApprovals()) return true;
			}
			return false;
		}
		
		public boolean hasCreditChanges() { return !iCreditChanges.isEmpty(); }
		
		public void addChange(SpecialRegistrationCreditChange change) {
			iCreditChanges.add(change);
		}
		
		public SpecialRegistrationCreditChange getCreditChange(String sectionId) {
			for (SpecialRegistrationCreditChange ch: iCreditChanges)
				if (ch.getCrn().equals(sectionId)) return ch;
			return null;
		}
		
		public List<SpecialRegistrationCreditChange> getCreditChanges() {
			return iCreditChanges;
		}
		
		public boolean hasCreditChanges(boolean approval) {
			for (SpecialRegistrationCreditChange change: iCreditChanges) {
				if (approval && change.hasApprovals()) return true;
				if (!approval && !change.hasApprovals()) return true;
			}
			return false;
		}
		
		public Float getMaxCredit() { return iMaxCredit; }
		public void setMaxCredit(Float credit) { iMaxCredit = credit; }
		
		public Float getCurrentCredit() { return iCurrentCredit; }
		public void setCurrentCredit(Float credit) { iCurrentCredit = credit; }
		
		public void setNote(String note) { iNote = note; }
		public String getNote() { return iNote; }
		public boolean hasNote() { return iNote != null && !iNote.isEmpty(); }
	}
	
	public static class ChangeGradeModesResponse implements GwtRpcResponse, Serializable {
		private static final long serialVersionUID = 1L;
		private GradeModes iGradeModes = null;
		private List<RetrieveSpecialRegistrationResponse> iRequests = null;
		private Set<String> iCancelRequestIds = null;
		
		public ChangeGradeModesResponse() {}
		
		public boolean hasGradeModes() {
			return iGradeModes != null && iGradeModes.hasGradeModes();
		}
		public void addGradeMode(String sectionId, String code, String label, boolean honors) {
			if (iGradeModes == null) iGradeModes = new GradeModes();
			iGradeModes.addGradeMode(sectionId, new GradeMode(code, label, honors));
		}
		public GradeMode getGradeMode(ClassAssignment section) {
			if (iGradeModes == null) return null;
			return iGradeModes.getGradeMode(section);
		}
		public GradeModes getGradeModes() { return iGradeModes; }
		
		public boolean hasCreditHours() {
			return iGradeModes != null && iGradeModes.hasCreditHours();
		}
		public void addCreditHour(String sectionId, Float creditHour) {
			if (iGradeModes == null) iGradeModes = new GradeModes();
			iGradeModes.addCreditHour(sectionId, creditHour);
		}
		public Float getCreditHour(ClassAssignment section) {
			if (iGradeModes == null) return null;
			return iGradeModes.getCreditHour(section);
		}
		
		public boolean hasRequests() { return iRequests != null && !iRequests.isEmpty(); }
		public void addRequest(RetrieveSpecialRegistrationResponse request) {
			if (iRequests == null) iRequests = new ArrayList<RetrieveSpecialRegistrationResponse>();
			iRequests.add(request);
		}
		public List<RetrieveSpecialRegistrationResponse> getRequests() { return iRequests; }
		public boolean hasRequest(String requestId) {
			if (iRequests == null) return false;
			for (RetrieveSpecialRegistrationResponse r: iRequests)
				if (r.getRequestId().equals(requestId)) return true;
			return false;
		}
		
		public void addCancelRequestId(String id) {
			if (iCancelRequestIds == null) iCancelRequestIds = new HashSet<String>();
			iCancelRequestIds.add(id);
		}
		public boolean hasCancelRequestIds() { return iCancelRequestIds != null && !iCancelRequestIds.isEmpty(); }
		public Set<String> getCancelRequestIds() { return iCancelRequestIds; }
		public boolean isToBeCancelled(String requestId) { return iCancelRequestIds != null && iCancelRequestIds.contains(requestId); }
	}
	
	public static interface ChangeRequestorNoteInterface {
		public boolean changeRequestorNote(RequestedCourse request);
		public boolean changeRequestorCreditNote(CourseRequestInterface request);
		public boolean changeRequestorNote(RetrieveSpecialRegistrationResponse registration, String course, Long courseId);
	}
	
	public static class UpdateSpecialRegistrationRequest extends StudentSectioningContext implements IsSerializable, Serializable {
		private static final long serialVersionUID = 1L;
		private String iRequestId;
		private Long iCourseId;
		private String iNote;
		private boolean iPreReg = false;
		
		public UpdateSpecialRegistrationRequest() {}
		public UpdateSpecialRegistrationRequest(StudentSectioningContext cx, String requestId, Long courseId, String note, boolean preReg) {
			super(cx);
			iCourseId = courseId;
			iRequestId = requestId;
			iNote = note;
			iPreReg = preReg;
		}
		
		public String getRequestId() { return iRequestId; }
		public void setRequestId(String requestId) { iRequestId = requestId; }
		public Long getCourseId() { return iCourseId; }
		public void setCourseId(Long courseId) { iCourseId = courseId; }
		public String getNote() { return iNote; }
		public void setNote(String note) { iNote = note; }
		public boolean isPreReg() { return iPreReg; }
		public void setPreReg(boolean preReg) { iPreReg = preReg; }
	}
	
	public static class UpdateSpecialRegistrationResponse implements IsSerializable, Serializable {
		private static final long serialVersionUID = 1L;
		private boolean iSuccess;
		private String iMessage;
		
		public UpdateSpecialRegistrationResponse() {}
		
		public boolean isSuccess() { return iSuccess; }
		public boolean isFailure() { return !iSuccess; }
		public void setSuccess(boolean success) { iSuccess = success; }
		
		public boolean hasMessage() { return iMessage != null && !iMessage.isEmpty(); }
		public String getMessage() { return iMessage; }
		public void setMessage(String message) { iMessage = message; }
	}	public static class InstructorInfo implements IsSerializable, Serializable, Comparable<InstructorInfo>  {
		private static final long serialVersionUID = 1L;
		private Long iInstructorId;
		private String iInstructorName;
		
		public InstructorInfo() {}
		public InstructorInfo(Long id, String name) {
			iInstructorId = id;
			iInstructorName = name;
		}
		
		public Long getId() { return iInstructorId; }
		public void setId(Long id) { iInstructorId = id; }
		
		public String getName() { return iInstructorName; }
		public void setName(String name) { iInstructorName = name; }
		
		@Override
		public int hashCode() { return getId().hashCode(); }
		
		@Override
		public boolean equals(Object o) {
			if (o == null || !(o instanceof InstructorInfo)) return false;
			return getId().equals(((InstructorInfo)o).getId());
		}
		
		@Override
		public int compareTo(InstructorInfo i) {
			int cmp = getName().compareTo(i.getName());
			if (cmp != 0) return cmp;
			return getId().compareTo(i.getId());
		}
		
		@Override
		public String toString() {
			return getName();
		}
	}
	
	public static class VariableTitleCourseInfo implements IsSerializable, Serializable, Suggestion, Comparable<VariableTitleCourseInfo>  {
		private static final long serialVersionUID = 1L;
		private String iTitle;
		private String iSubject, iCourseNbr;
		private String iDefaultGradeModeCode;
		private Set<GradeMode> iGradeModes;
		private Set<Float> iAvailableCredits;
		private Date iStartDate, iEndDate;
		private Set<InstructorInfo> iInstructors;
		private String iDetails;
		private String iDisclaimer;
		private List<String> iSuggestions = null;
		
		public VariableTitleCourseInfo() {}
		
		public String getTitle() { return iTitle; }
		public void setTitle(String title) { iTitle = title; }
		public boolean hasTitle() { return iTitle != null && !iTitle.isEmpty(); }
		
		public String getSubject() { return iSubject; }
		public void setSubject(String subject) { iSubject = subject; }
		public String getCourseNbr() { return iCourseNbr; }
		public void setCourseNbr(String courseNbr) { iCourseNbr = courseNbr; }
		public String getCourseName() { return getSubject() + " " + getCourseNbr(); }
		
		public String getDefaultGradeModeCode() { return iDefaultGradeModeCode; }
		public void setDefaultGradeModeCode(String gmCode) { iDefaultGradeModeCode = gmCode; }
		public GradeMode getDefaultGradeMode() {
			if (iDefaultGradeModeCode == null || !hasGradeModes()) return null;
			for (GradeMode gm: iGradeModes)
				if (iDefaultGradeModeCode.equals(gm.getCode())) return gm;
			return null;
		}
		
		public boolean hasGradeModes() { return iGradeModes != null && !iGradeModes.isEmpty(); }
		public void addGradeMode(GradeMode gradeMode) {
			if (iGradeModes == null) iGradeModes = new TreeSet<GradeMode>();
			iGradeModes.add(gradeMode);
		}
		public Set<GradeMode> getGradeModes() { return iGradeModes; }
		
		public boolean hasAvailableCredits() { return iAvailableCredits != null && !iAvailableCredits.isEmpty(); }
		public void addAvailableCredit(Float credit) {
			if (iAvailableCredits == null) iAvailableCredits = new TreeSet<Float>();
			iAvailableCredits.add(credit);
		}
		public Set<Float> getAvailableCredits() { return iAvailableCredits; }
		
		public Date getStartDate() { return iStartDate; }
		public void setStartDate(Date date) { iStartDate = date; }
		public Date getEndDate() { return iEndDate; }
		public void setEndDate(Date date) { iEndDate = date; }
		
		public boolean hasInstructors() { return iInstructors != null && !iInstructors.isEmpty(); }
		public void addInstructor(Long id, String name) {
			if (iInstructors == null) iInstructors = new TreeSet<InstructorInfo>();
			iInstructors.add(new InstructorInfo(id, name));
		}
		public Set<InstructorInfo> getInstructors() { return iInstructors; }
		public void setInstructors(Set<InstructorInfo> instructors) { iInstructors = instructors; }
		
		public String getDetails() { return iDetails; }
		public void setDetails(String details) { iDetails = details; }
		public boolean hasDetails() { return iDetails != null && !iDetails.isEmpty(); }
		
		public String getDisclaimer() { return iDisclaimer; }
		public void setDisclaimer(String disclaimer) { iDisclaimer = disclaimer; }
		public boolean hasDisclaimer() { return iDisclaimer != null && !iDisclaimer.isEmpty(); }
		
		@Override
		public int hashCode() { return getCourseName().hashCode(); }
		
		@Override
		public boolean equals(Object o) {
			if (o == null || !(o instanceof VariableTitleCourseInfo)) return false;
			return getCourseName().equals(((VariableTitleCourseInfo)o).getCourseName());
		}
		
		@Override
		public int compareTo(VariableTitleCourseInfo c) {
			return getCourseName().compareTo(c.getCourseName());
		}
		
		@Override
		public String toString() {
			return getCourseName();
		}

		@Override
		public String getDisplayString() {
			return getCourseName() + (hasTitle() ? " - " + getTitle() : "");
		}

		@Override
		public String getReplacementString() {
			return getCourseName() + (hasTitle() ? " - " + getTitle() : "");
		}
		
		public boolean hasSuggestions() { return iSuggestions != null && !iSuggestions.isEmpty(); }
		public List<String> getSuggestions() { return iSuggestions; }
		public void addSuggestion(String suggestion) {
			if (iSuggestions == null)
				iSuggestions = new ArrayList<String>();
			iSuggestions.add(suggestion);
		}
	}
	
	public static class VariableTitleCourseRequest extends StudentSectioningContext {
		private static final long serialVersionUID = 1L;
		private VariableTitleCourseInfo iCourse;
		private String iTitle;
		private InstructorInfo iInstructor;
		private Float iCredit;
		private String iNote;
		private Date iStartDate, iEndDate;
		private boolean iCheckIfExists = true;
		private String iGradeMode;
		private Float iMaxCredit;
		private String iSection;
		
		public VariableTitleCourseRequest() {}
		public VariableTitleCourseRequest(StudentSectioningContext cx) {
			super(cx);
		}
		
		public VariableTitleCourseInfo getCourse() { return iCourse; }
		public void setCourse(VariableTitleCourseInfo course) { iCourse = course; }
		
		public String getTitle() { return iTitle; }
		public void setTitle(String title) { iTitle = title; }
		public boolean hasTitle() { return iTitle != null && !iTitle.isEmpty(); }
		
		public InstructorInfo getInstructor() { return iInstructor; }
		public void setInstructor(InstructorInfo instructor) { iInstructor = instructor; }
		
		public String getGradeModeCode() { return iGradeMode; }
		public void setGradeModeCode(String code) { iGradeMode = code; }
		public boolean hasGradeMode() { return iGradeMode != null && !iGradeMode.isEmpty(); }
		
		public String getGradeModeLabel() {
			if (iGradeMode == null) return null;
			if (getCourse().hasGradeModes())
				for (GradeMode gm: getCourse().getGradeModes())
					if (iGradeMode.equals(gm.getCode()))
						return gm.getLabel();
			return null;
		}
		
		public Float getCredit() { return iCredit; }
		public void setCredit(Float credit) { iCredit = credit; }
		
		public Float getMaxCredit() { return iMaxCredit; }
		public void setMaxCredit(Float maxCredit) { iMaxCredit = maxCredit; }
		
		public String getNote() { return iNote; }
		public void setNote(String note) { iNote = note; }
		public boolean hasNote() { return iNote != null && !iNote.isEmpty(); }
		
		public boolean hasSection() { return iSection != null && !iSection.isEmpty(); }
		public void setSection(String section) { iSection = section; }
		public String getSection() { return iSection; }
		
		public Date getStartDate() {
			if (iStartDate == null) return getCourse().getStartDate();
			return iStartDate;
		}
		public void setStartDate(Date date) { iStartDate = date; }
		
		public Date getEndDate() {
			if (iEndDate == null) return getCourse().getEndDate();
			return iEndDate;
		}
		public void setEndDate(Date date) { iEndDate = date; }
		
		public boolean isCheckIfExists() { return iCheckIfExists; }
		public void setCheckIfExists(boolean check) { iCheckIfExists = check; }
	}
	
	public static class VariableTitleCourseResponse implements IsSerializable, Serializable {
		private static final long serialVersionUID = 1L;
		private RequestedCourse iCourse = null;
		private List<RetrieveSpecialRegistrationResponse> iRequests = null;
		private Set<String> iCancelRequestIds = null;
		
		public VariableTitleCourseResponse() {}
		public VariableTitleCourseResponse(RequestedCourse rc) {
			iCourse = rc;
		}
		
		public RequestedCourse getCourse() { return iCourse; }
		public void setCourse(RequestedCourse course) { iCourse = course; }
		
		public boolean hasRequests() { return iRequests != null && !iRequests.isEmpty(); }
		public void addRequest(RetrieveSpecialRegistrationResponse request) {
			if (iRequests == null) iRequests = new ArrayList<RetrieveSpecialRegistrationResponse>();
			iRequests.add(request);
		}
		public List<RetrieveSpecialRegistrationResponse> getRequests() { return iRequests; }
		public boolean hasRequest(String requestId) {
			if (iRequests == null) return false;
			for (RetrieveSpecialRegistrationResponse r: iRequests)
				if (r.getRequestId().equals(requestId)) return true;
			return false;
		}
		
		public void addCancelRequestId(String id) {
			if (iCancelRequestIds == null) iCancelRequestIds = new HashSet<String>();
			iCancelRequestIds.add(id);
		}
		public boolean hasCancelRequestIds() { return iCancelRequestIds != null && !iCancelRequestIds.isEmpty(); }
		public Set<String> getCancelRequestIds() { return iCancelRequestIds; }
		public boolean isToBeCancelled(String requestId) { return iCancelRequestIds != null && iCancelRequestIds.contains(requestId); }
	}
}
