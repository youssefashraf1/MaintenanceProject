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
package org.unitime.timetable.gwt.client.sectioning;

import java.util.ArrayList;
import java.util.Iterator;

import org.unitime.timetable.gwt.client.ToolBox;
import org.unitime.timetable.gwt.client.sectioning.CourseRequestLine.CourseSelectionBox;
import org.unitime.timetable.gwt.client.widgets.CourseSelection;
import org.unitime.timetable.gwt.client.widgets.LoadingWidget;
import org.unitime.timetable.gwt.client.widgets.P;
import org.unitime.timetable.gwt.client.widgets.Validator;
import org.unitime.timetable.gwt.resources.GwtAriaMessages;
import org.unitime.timetable.gwt.resources.StudentSectioningConstants;
import org.unitime.timetable.gwt.resources.StudentSectioningMessages;
import org.unitime.timetable.gwt.resources.StudentSectioningResources;
import org.unitime.timetable.gwt.services.SectioningService;
import org.unitime.timetable.gwt.services.SectioningServiceAsync;
import org.unitime.timetable.gwt.shared.ClassAssignmentInterface;
import org.unitime.timetable.gwt.shared.ClassAssignmentInterface.CourseAssignment;
import org.unitime.timetable.gwt.shared.CourseRequestInterface;
import org.unitime.timetable.gwt.shared.CourseRequestInterface.CheckCoursesResponse;
import org.unitime.timetable.gwt.shared.CourseRequestInterface.CourseMessage;
import org.unitime.timetable.gwt.shared.CourseRequestInterface.FreeTime;
import org.unitime.timetable.gwt.shared.CourseRequestInterface.Request;
import org.unitime.timetable.gwt.shared.CourseRequestInterface.RequestedCourse;
import org.unitime.timetable.gwt.shared.CourseRequestInterface.RequestedCourseStatus;
import org.unitime.timetable.gwt.shared.OnlineSectioningInterface.StudentSectioningContext;
import org.unitime.timetable.gwt.shared.OnlineSectioningInterface.WaitListMode;
import org.unitime.timetable.gwt.shared.SectioningException;
import org.unitime.timetable.gwt.shared.SpecialRegistrationInterface.SpecialRegistrationContext;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;

/**
 * @author Tomas Muller
 */
public class CourseRequestsTable extends P implements HasValue<CourseRequestInterface> {
	public static final StudentSectioningResources RESOURCES =  GWT.create(StudentSectioningResources.class);
	public static final StudentSectioningMessages MESSAGES = GWT.create(StudentSectioningMessages.class);
	public static final StudentSectioningConstants CONSTANTS = GWT.create(StudentSectioningConstants.class);
	public static final GwtAriaMessages ARIA = GWT.create(GwtAriaMessages.class);
	
	private final SectioningServiceAsync iSectioningService = GWT.create(SectioningService.class);
	
	private StudentSectioningContext iContext;
	private ArrayList<CourseRequestLine> iCourses;
	private ArrayList<CourseRequestLine> iAlternatives;
	private Label iTip;
	private SpecialRegistrationContext iSpecReg;
	private CheckCoursesResponse iLastCheck;
	
	Validator<CourseSelection> iCheckForDuplicities;
	private WaitListMode iWaitListMode = WaitListMode.WaitList;
	private P iHeader, iHeaderTitle, iHeaderWaitlist;
	private P iAltHeader, iAltHeaderTitle, iAltHeaderNote;
	private boolean iArrowsVisible = true;
	private Image iCreditStatusIcon = null;

	public CourseRequestsTable(StudentSectioningContext context, SpecialRegistrationContext specreg) {
		super("unitime-CourseRequests");
		iContext = context;
		iSpecReg = specreg;
		
		iHeader = new P("header");
		iHeaderTitle = new P("title"); iHeaderTitle.setText(MESSAGES.courseRequestsCourses());
		iHeaderWaitlist = new P("waitlist"); iHeaderWaitlist.setHTML(MESSAGES.courseRequestsWaitList());
		iHeader.add(iHeaderTitle);
		iHeader.add(iHeaderWaitlist);
		add(iHeader);

		iCourses = new ArrayList<CourseRequestLine>();
		iAlternatives = new ArrayList<CourseRequestLine>();
		
		iCheckForDuplicities = new Validator<CourseSelection>() {
			public String validate(CourseSelection source) {
				RequestedCourse course = source.getValue();
				if (course == null || course.isFreeTime()) return null;
				for (CourseRequestLine line: iCourses)
					for (CourseSelectionBox c: line.getCourses()) {
						if (c == source) continue;
						if (course.equals(c.getValue()) && !c.isInactive() && !course.isInactive()) return MESSAGES.validationMultiple(course.getCourseName());
					}
				for (CourseRequestLine line: iAlternatives)
					for (CourseSelectionBox c: line.getCourses()) {
						if (c == source) continue;
						if (course.equals(c.getValue()) && !c.isInactive() && !course.isInactive()) return MESSAGES.validationMultiple(course.getCourseName());
					}
				return null;
			}
		};

		for (int i = 0; i < CONSTANTS.numberOfCourses(); i++) {
			final CourseRequestLine line = new CourseRequestLine(iContext, i, false, iCheckForDuplicities, iSpecReg);
			iCourses.add(line);
			if (i > 0) {
				CourseRequestLine prev = iCourses.get(i - 1);
				line.setPrevious(prev); prev.setNext(line);
			}
			line.addValueChangeHandler(new ValueChangeHandler<CourseRequestInterface.Request>() {
				@Override
				public void onValueChange(ValueChangeEvent<Request> event) {
					if (iLastCheck != null)
						for (CourseSelectionBox box: line.getCourses()) setErrors(box, iLastCheck);
					ValueChangeEvent.fire(CourseRequestsTable.this, getValue());
					if (event.getValue() != null && iCourses.indexOf(line) + 1 == iCourses.size())
						addCourseLine();
				}
			});
			add(line);
		}
		
		iCourses.get(1).getCourses().get(0).setHint(MESSAGES.courseRequestsHint1());
		iCourses.get(3).getCourses().get(0).setHint(MESSAGES.courseRequestsHint3());
		iCourses.get(4).getCourses().get(0).setHint(MESSAGES.courseRequestsHint4());
		iCourses.get(CONSTANTS.numberOfCourses()-1).getCourses().get(0).setHint(MESSAGES.courseRequestsHint8());

		iTip = new Label(CONSTANTS.tips()[(int)(Math.random() * CONSTANTS.tips().length)]);
		ToolBox.disableTextSelectInternal(iTip.getElement());
		iTip.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				String oldText = iTip.getText();
				do {
					iTip.setText(CONSTANTS.tips()[(int)(Math.random() * CONSTANTS.tips().length)]);
				} while (oldText.equals(iTip.getText()));
			}
		});
		iTip.addStyleName("tip");
		iTip.addStyleName("unitime-NoPrint");
		add(iTip);
		
		if (CONSTANTS.numberOfAlternatives() > 0) {
			iAltHeader = new P("alt-header");
			iAltHeaderTitle = new P("title"); iAltHeaderTitle.setText(MESSAGES.courseRequestsAlternatives());
			iAltHeaderNote = new P("note"); iAltHeaderNote.setText(MESSAGES.courseRequestsAlternativesNote());
			iAltHeader.add(iAltHeaderTitle);
			iAltHeader.add(iAltHeaderNote);
			add(iAltHeader);
		}

		for (int i=0; i<CONSTANTS.numberOfAlternatives(); i++) {
			final CourseRequestLine line = new CourseRequestLine(iContext, i, true, iCheckForDuplicities, iSpecReg);
			iAlternatives.add(line);
			if (i == 0) {
				CourseRequestLine prev = iCourses.get(iCourses.size() - 1);
				line.setPrevious(prev); prev.setNext(line);
			} else {
				CourseRequestLine prev = iAlternatives.get(i - 1);
				line.setPrevious(prev); prev.setNext(line);
			}
			line.addValueChangeHandler(new ValueChangeHandler<CourseRequestInterface.Request>() {
				@Override
				public void onValueChange(ValueChangeEvent<Request> event) {
					if (iLastCheck != null)
						for (CourseSelectionBox box: line.getCourses()) setErrors(box, iLastCheck);
					ValueChangeEvent.fire(CourseRequestsTable.this, getValue());
					if (event.getValue() != null && iAlternatives.indexOf(line) + 1 == iAlternatives.size())
						addAlternativeLine();
				}
			});
			add(line);
		}
		if (CONSTANTS.numberOfAlternatives() > 0)
			iAlternatives.get(0).getCourses().get(0).setHint(MESSAGES.courseRequestsHintA0());
	}
	
	private void addCourseLine() {
		int i = iCourses.size();
		final CourseRequestLine line = new CourseRequestLine(iContext, i, false, iCheckForDuplicities, iSpecReg);
		iCourses.add(line);
		CourseRequestLine prev = iCourses.get(i - 1);
		prev.getCourses().get(0).setHint("");
		line.getCourses().get(0).setHint(MESSAGES.courseRequestsHint8());
		CourseRequestLine next = (iAlternatives.isEmpty() ? null : iAlternatives.get(0));
		line.setPrevious(prev); prev.setNext(line);
		if (next != null) {
			line.setNext(next); next.setPrevious(line);
		}
		line.setArrowsVisible(iArrowsVisible);
		line.setWaitListMode(iWaitListMode);
		insert(line, 1 + i);
		line.addValueChangeHandler(new ValueChangeHandler<CourseRequestInterface.Request>() {
			@Override
			public void onValueChange(ValueChangeEvent<Request> event) {
				if (iLastCheck != null)
					for (CourseSelectionBox box: line.getCourses()) setErrors(box, iLastCheck);
				ValueChangeEvent.fire(CourseRequestsTable.this, getValue());
				if (event.getValue() != null && iCourses.indexOf(line) + 1 == iCourses.size())
					addCourseLine();
			}
		});
	}
	
	private void addAlternativeLine() {
		if (iAlternatives.isEmpty()) {
			iAltHeader = new P("alt-header");
			iAltHeaderTitle = new P("title"); iAltHeaderTitle.setText(MESSAGES.courseRequestsAlternatives());
			iAltHeaderNote = new P("note"); iAltHeaderNote.setText(MESSAGES.courseRequestsAlternativesNote());
			iAltHeader.add(iAltHeaderTitle);
			iAltHeader.add(iAltHeaderNote);
			add(iAltHeader);
		}
		int i = iAlternatives.size();
		final CourseRequestLine line = new CourseRequestLine(iContext, i, true, iCheckForDuplicities, iSpecReg);
		iAlternatives.add(line);
		CourseRequestLine prev = (i == 0 ? iCourses.get(iCourses.size() - 1) : iAlternatives.get(i - 1));
		if (prev != null) {
			line.setPrevious(prev); prev.setNext(line);
		}
		line.setArrowsVisible(iArrowsVisible);
		insert(line, 3 + iCourses.size() + i);
		line.addValueChangeHandler(new ValueChangeHandler<CourseRequestInterface.Request>() {
			@Override
			public void onValueChange(ValueChangeEvent<Request> event) {
				if (iLastCheck != null)
					for (CourseSelectionBox box: line.getCourses()) setErrors(box, iLastCheck);
				ValueChangeEvent.fire(CourseRequestsTable.this, getValue());
				if (event.getValue() != null && iAlternatives.indexOf(line) + 1 == iAlternatives.size())
					addAlternativeLine();
			}
		});
	}
	
	public void setWaitListMode(WaitListMode waitListMode) {
		iWaitListMode = waitListMode;
		iHeaderWaitlist.setVisible(iWaitListMode == WaitListMode.WaitList || iWaitListMode == WaitListMode.NoSubs);
		for (CourseRequestLine line: iCourses)
			line.setWaitListMode(iWaitListMode);
	}
	
	public void setArrowsVisible(boolean arrowsVisible, boolean noSubs) {
		iArrowsVisible = arrowsVisible;
		if (noSubs)
			iHeaderWaitlist.setHTML(arrowsVisible ? MESSAGES.courseRequestsNoSubstitutions() : MESSAGES.courseRequestsNoSubstitutionsNoArrows());
		else
			iHeaderWaitlist.setHTML(arrowsVisible ? MESSAGES.courseRequestsWaitList() : MESSAGES.courseRequestsWaitListNoArrows());
		iHeader.setStyleName("noarrows", !arrowsVisible);
		for (CourseRequestLine line: iCourses)
			line.setArrowsVisible(arrowsVisible);
		for (CourseRequestLine line: iAlternatives)
			line.setArrowsVisible(arrowsVisible);
	}

	public void validate(final AsyncCallback<Boolean> callback) {
		validate(null, callback);
	}

	public void validate(Boolean updateLastRequest, final AsyncCallback<Boolean> callback) {
		try {
			iLastCheck = null;
			String failed = null;
			LoadingWidget.getInstance().show(MESSAGES.courseRequestsValidating());
			for (CourseRequestLine line: iCourses) {
				String message = line.validate();
				if (message != null) failed = message;
			}
			for (CourseRequestLine line: iAlternatives) {
				String message = line.validate();
				if (message != null) failed = message;
			}
			final CourseRequestInterface cr = new CourseRequestInterface(iContext);
			if (cr.getAcademicSessionId() == null)
				throw new SectioningException(MESSAGES.sessionSelectorNoSession());
			fillInCourses(cr); fillInAlternatives(cr);
			if (updateLastRequest != null)
				cr.setUpdateLastRequest(updateLastRequest);
			final boolean success = (failed == null);
			iSectioningService.checkCourses(cr,
					new AsyncCallback<CheckCoursesResponse>() {
						public void onSuccess(final CheckCoursesResponse result) {
							setLastCheck(result);
							LoadingWidget.getInstance().hide();
							if (result.isError()) {
								callback.onFailure(null);
								return;
							}
							if (success && result.isConfirm()) {
								final Iterator<Integer> it = result.getConfirms().iterator();
								new AsyncCallback<Boolean>() {
									@Override
									public void onFailure(Throwable caught) {}
									@Override
									public void onSuccess(Boolean accept) {
										if (accept && it.hasNext()) {
											CourseRequestsConfirmationDialog.confirm(result, it.next(), this);
										} else {
											callback.onSuccess(accept);
										}
									}
								}.onSuccess(true);
							} else {
								if (success)
									callback.onSuccess(true);
								else
									callback.onFailure(null);
							}
						}
						public void onFailure(Throwable caught) {
							LoadingWidget.getInstance().hide();
							callback.onFailure(caught);
						}
					});
		} catch (Exception e) {
			callback.onFailure(e);
		}
	}
	
	public void setLastCheck(CheckCoursesResponse result) {
		iLastCheck = result;
		if (iCreditStatusIcon != null) {
			if (result != null && result.hasCreditWarning()) {
				String warning = result.getCreditWarning();
				if (result.getMaxCreditOverrideStatus() != null) {
					switch (result.getMaxCreditOverrideStatus()) {
					case CREDIT_HIGH:
						iCreditStatusIcon.setResource(RESOURCES.requestNeeded());
						warning += "\n" + MESSAGES.creditStatusDeniedShort();
						break;
					case OVERRIDE_REJECTED:
						iCreditStatusIcon.setResource(RESOURCES.requestError());
						warning += "\n" + MESSAGES.creditStatusDenied();
						break;
					default:
						iCreditStatusIcon.setResource(RESOURCES.requestNeeded());
						break;
					}
				} else {
					iCreditStatusIcon.setResource(RESOURCES.requestNeeded());
				}
				iCreditStatusIcon.setAltText(warning);
				iCreditStatusIcon.setTitle(warning);
				iCreditStatusIcon.setVisible(true);
			} else {
				iCreditStatusIcon.setVisible(false);
			}
		}
		for (CourseRequestLine line: iCourses) {
			for (CourseSelectionBox box: line.getCourses()) {
				String message = box.validate();
				if (message != null) {
					RequestedCourse rc = box.getValue();
					iLastCheck.addError(rc.getCourseId(), rc.getCourseName(), "ERROR", message);
				}
			}
		}
		for (CourseRequestLine line: iAlternatives) {
			for (CourseSelectionBox box: line.getCourses()) {
				String message = box.validate();
				if (message != null) {
					RequestedCourse rc = box.getValue();
					iLastCheck.addError(rc.getCourseId(), rc.getCourseName(), "ERROR", message);
				}
			}
		}
		setErrors(result);
	}
	
	public void setError(String course, String error) {
		GWT.log(error);
		for (CourseRequestLine line: iCourses) {
			for (CourseSelectionBox box: line.getCourses()) {
				if (course.equals(box.getText())) box.setError(error);
			}
		}
		for (CourseRequestLine line: iAlternatives) {
			for (CourseSelectionBox box: line.getCourses()) {
				if (course.equals(box.getText())) box.setError(error);
			}
		}
	}
	
	public void setErrors(CheckCoursesResponse response) {
		for (CourseRequestLine line: iCourses)
			for (CourseSelectionBox box: line.getCourses()) setErrors(box, response);
		for (CourseRequestLine line: iAlternatives)
			for (CourseSelectionBox box: line.getCourses()) setErrors(box, response);
	}
	
	protected void setErrors(CourseSelectionBox box, CheckCoursesResponse messages) {
		if (iContext.isSectioning()) {
			String message = null;
			for (CourseMessage m: messages.getMessages(box.getText())) {
				if (m.getStatus() == RequestedCourseStatus.OVERRIDE_APPROVED && !m.isError()) continue;
				if (message == null)
					message = m.getMessage();
				else
					message += "\n" + m.getMessage();
			}
			if (message != null) {
				if (messages.isError(box.getText()) || messages.isConfirm(box.getText()))
					box.setError(message);
				else
					box.setWarning(message);
			}
		} else {
			String message = null;
			String itemized = null;
			for (CourseMessage m: messages.getMessages(box.getText())) {
				if (m.getStatus() == RequestedCourseStatus.OVERRIDE_APPROVED && !m.isError()) continue;
				if ("REQUEST_NOTE".equals(m.getCode())) continue;
				if (message == null) {
					message = m.getMessage();
					itemized = MESSAGES.courseMessage(m.getMessage());
				} else {
					message += "\n" + m.getMessage();
					itemized += "\n" + MESSAGES.courseMessage(m.getMessage());
				}
			}
			RequestedCourseStatus status = messages.getStatus(box.getText());
			if (status == null) status = box.getValue().getStatus();
			if (status == null && !box.isCanDelete()) status = RequestedCourseStatus.ENROLLED;
			if (message != null) {
				String note = "";
				if (box.getValue().hasRequestorNote()) note += "\n<span class='status-note'>" + box.getValue().getRequestorNote() + "</span>";
				else if (iSpecReg != null && iSpecReg.isAllowChangeRequestNote() && box.getValue().hasRequestId() && status == RequestedCourseStatus.OVERRIDE_PENDING)
					note += "\n<span class='status-note'>" + MESSAGES.noRequestNoteClickToChange() + "</span>";
				if (box.getValue().hasStatusNote()) note += "\n<span class='status-note'>" + box.getValue().getStatusNote() + "</span>";
				if (messages.isError(box.getText()) || messages.isConfirm(box.getText()))
					box.setError(message + note);
				else
					box.setWarning(message + note);
			}
			String note = "";
			if (box.getValue().hasRequestorNote())
				note += "\n\n" + MESSAGES.requestNote(box.getValue().getRequestorNote());
			if (box.getValue().hasStatusNote()) note = "\n\n" + MESSAGES.overrideNote(box.getValue().getStatusNote());
			if (messages.isError(box.getText()) && (status == null || status != RequestedCourseStatus.OVERRIDE_REJECTED)) {
				box.setStatus(RESOURCES.requestError(), itemized);
			} else if (status != null) {
				switch (status) {
				case ENROLLED:
					box.setStatus(RESOURCES.requestEnrolled(), MESSAGES.enrolled(box.getText()) + note);
					break;
				case OVERRIDE_NEEDED:
					box.setStatus(RESOURCES.requestNeeded(), (itemized == null ? "" : MESSAGES.overrideNeeded(itemized)) + note);
					break;
				case SAVED:
					box.setStatus(RESOURCES.requestSaved(), (itemized == null ? "" : MESSAGES.requestWarnings(itemized) + "\n\n") + MESSAGES.requested(box.getText()) + note);
					break;				
				case OVERRIDE_REJECTED:
					box.setStatus(RESOURCES.requestRejected(), (itemized == null ? "" : MESSAGES.requestWarnings(itemized) + "\n\n") + MESSAGES.overrideRejected(box.getText()) + note);
					break;
				case OVERRIDE_PENDING:
					box.setStatus(RESOURCES.requestPending(), (itemized == null ? "" : MESSAGES.requestWarnings(itemized) + "\n\n") + MESSAGES.overridePending(box.getText()) + note);
					break;
				case OVERRIDE_CANCELLED:
					box.setStatus(RESOURCES.requestCancelled(), (itemized == null ? "" : MESSAGES.requestWarnings(itemized) + "\n\n") + MESSAGES.overrideCancelled(box.getText()) + note);
					break;
				case OVERRIDE_APPROVED:
					box.setStatus(RESOURCES.requestSaved(), (itemized == null ? "" : MESSAGES.requestWarnings(itemized) + "\n\n") + MESSAGES.overrideApproved(box.getText()) + note);
					break;
				case OVERRIDE_NOT_NEEDED:
					box.setStatus(RESOURCES.requestNotNeeded(), (itemized == null ? "" : MESSAGES.requestWarnings(itemized) + "\n\n") + MESSAGES.overrideNotNeeded(box.getText()) + note);
					break;
				default:
					if (messages.isError(box.getText()))
						box.setStatus(RESOURCES.requestError(), (itemized == null ? "" : itemized) + note);
					else
						box.clearStatus();
					break;
				}
			}
		}
	}
	
	public void changeTip() {
		iTip.setText(CONSTANTS.tips()[(int)(Math.random() * CONSTANTS.tips().length)]);
	}
	
	public void fillInCourses(CourseRequestInterface cr) {
		for (CourseRequestLine line: iCourses) {
			CourseRequestInterface.Request req = line.getValue();
			if (req != null) cr.getCourses().add(req);
		}
	}
	
	public void fillInAlternatives(CourseRequestInterface cr) {
		for (CourseRequestLine line: iAlternatives) {
			CourseRequestInterface.Request req = line.getValue();
			if (req != null) cr.getAlternatives().add(req);
		}
	}
	
	public CourseRequestInterface getRequest() {
		CourseRequestInterface cr = new CourseRequestInterface(iContext);
		cr.setWaitListMode(iWaitListMode);
		fillInCourses(cr);
		fillInAlternatives(cr);
		cr.setTimeConflictsAllowed(iSpecReg.isEnabled() && iSpecReg.isDisclaimerAccepted() && iSpecReg.areTimeConflictsAllowed());
		cr.setSpaceConflictsAllowed(iSpecReg.isEnabled() && iSpecReg.isDisclaimerAccepted() && iSpecReg.areSpaceConflictsAllowed());
		cr.setLinkedConflictsAllowed(iSpecReg.isEnabled() && iSpecReg.isDisclaimerAccepted() && iSpecReg.areLinkedConflictsAllowed());
		cr.setDeadlineConflictsAllowed(iSpecReg.isEnabled() && iSpecReg.isDisclaimerAccepted() && iSpecReg.areDeadlineConflictsAllowed());
		if (iLastCheck != null) cr.setConfirmations(iLastCheck.getMessages());
		cr.removeInactiveDuplicates();
		return cr;
	}

	public void activate(RequestedCourse course) {
		if (course == null) return;
		for (CourseRequestLine line: iCourses) {
			for (CourseSelectionBox box: line.getCourses())
				if (box.isActive(course)) return;
		}
		for (CourseRequestLine line: iAlternatives) {
			for (CourseSelectionBox box: line.getCourses())
				if (box.isActive(course)) return;
		}
		for (CourseRequestLine line: iCourses) {
			for (CourseSelectionBox box: line.getCourses())
				box.activate(course);
		}
		for (CourseRequestLine line: iAlternatives) {
			for (CourseSelectionBox box: line.getCourses())
				box.activate(course);
		}
	}
	
	public void activate(CourseAssignment course) {
		if (course == null) return;
		activate(new RequestedCourse(course, CONSTANTS.showCourseTitle()));
	}
	
	public boolean isActive(RequestedCourse course) {
		if (course == null) return true;
		for (CourseRequestLine line: iCourses) {
			for (CourseSelectionBox box: line.getCourses())
				if (box.isActive(course)) return true;
		}
		for (CourseRequestLine line: iAlternatives) {
			for (CourseSelectionBox box: line.getCourses())
				if (box.isActive(course)) return true;
		}
		for (CourseRequestLine line: iCourses) {
			for (CourseSelectionBox box: line.getCourses())
				if (box.isInactive(course)) return false;
		}
		for (CourseRequestLine line: iAlternatives) {
			for (CourseSelectionBox box: line.getCourses())
				if (box.isInactive(course)) return false;
		}
		return true;
	}
	
	public boolean isActive(CourseAssignment course) {
		if (course == null) return true;
		return isActive(new RequestedCourse(course, CONSTANTS.showCourseTitle()));
	}
	
	public void setRequest(CourseRequestInterface request) {
		clear();
		while (iCourses.size() < request.getCourses().size()) addCourseLine();
		for (int idx = 0; idx < request.getCourses().size(); idx++)
			iCourses.get(idx).setValue(request.getCourses().get(idx), true);
		while (iAlternatives.size() < request.getAlternatives().size()) addAlternativeLine();;
		for (int idx = 0; idx < request.getAlternatives().size(); idx++)
			iAlternatives.get(idx).setValue(request.getAlternatives().get(idx), true);
		if (request.hasWaitListChecks()) {
			iLastCheck = request.getWaitListChecks();
			setErrors(iLastCheck);
		} else if (request.hasConfirmations()) {
			iLastCheck = new CheckCoursesResponse(request.getConfirmations());
			setErrors(iLastCheck);
		} else {
			iLastCheck = null;
		}
	}
	
	public void disableArrowsWhereNeeded() {
		CourseRequestLine prev = null;
		for (Iterator<CourseRequestLine> i = iCourses.iterator(); i.hasNext(); ) {
			CourseRequestLine line = i.next();
			if (prev != null) {
				prev.setDownArrowEnabled(prev.getValue().isCanChangePriority() && line.getValue().isCanChangePriority());
				line.setUpArrowEnabled(prev.getValue().isCanChangePriority() && line.getValue().isCanChangePriority());
			}
			prev = line;
		}
		for (Iterator<CourseRequestLine> i = iAlternatives.iterator(); i.hasNext(); ) {
			CourseRequestLine line = i.next();
			if (prev != null) {
				prev.setDownArrowEnabled(prev.getValue().isCanChangePriority() && line.getValue().isCanChangePriority());
				line.setUpArrowEnabled(prev.getValue().isCanChangePriority() && line.getValue().isCanChangePriority());
			}
			prev = line;
		}
	}
	
	public void notifySaveSucceeded() {
		if (iLastCheck != null && iLastCheck.hasMessages()) {
			for (CourseMessage m: iLastCheck.getMessages())
				if (m.isConfirm()) m.setConfirm(null);
			setErrors(iLastCheck);
		}
	}
	
	public RequestedCourse getRequestedCourse(Long course) {
		// skip inactive first
		for (CourseRequestLine line: iCourses)
			for (CourseSelectionBox box: line.getCourses()) {
				RequestedCourse rc = box.getValue();
				if (rc != null && course.equals(rc.getCourseId()) && !rc.isInactive())
					return rc;
			}
		for (CourseRequestLine line: iAlternatives)
			for (CourseSelectionBox box: line.getCourses()) {
				RequestedCourse rc = box.getValue();
				if (rc != null && course.equals(rc.getCourseId()) && !rc.isInactive())
					return rc;
			}
		// all courses next
		for (CourseRequestLine line: iCourses)
			for (CourseSelectionBox box: line.getCourses()) {
				RequestedCourse rc = box.getValue();
				if (rc != null && course.equals(rc.getCourseId()))
					return rc;
			}
		for (CourseRequestLine line: iAlternatives)
			for (CourseSelectionBox box: line.getCourses()) {
				RequestedCourse rc = box.getValue();
				if (rc != null && course.equals(rc.getCourseId()))
					return rc;
			}
		return null;
	}
	
	public Boolean getWaitList(Long course) {
		if ((iWaitListMode == WaitListMode.WaitList || iWaitListMode == WaitListMode.NoSubs) && course != null) {
			// skip inactive first
			for (CourseRequestLine line: iCourses)
				for (CourseSelectionBox box: line.getCourses()) {
					RequestedCourse rc = box.getValue();
					if (rc != null && course.equals(rc.getCourseId()) && !rc.isInactive())
						return line.getWaitList();
				}
			// all courses next
			for (CourseRequestLine line: iCourses)
				for (CourseSelectionBox box: line.getCourses()) {
					RequestedCourse rc = box.getValue();
					if (rc != null && course.equals(rc.getCourseId()))
						return line.getWaitList();
				}
		}
		return null;
	}
	
	public CourseRequestLine getWaitListedLine(Long course) {
		if ((iWaitListMode == WaitListMode.WaitList || iWaitListMode == WaitListMode.NoSubs) && course != null) {
			// skip inactive first
			for (CourseRequestLine line: iCourses)
				for (CourseSelectionBox box: line.getCourses()) {
					RequestedCourse rc = box.getValue();
					if (rc != null && course.equals(rc.getCourseId()) && !rc.isInactive())
						return line;
				}
			// all courses next
			for (CourseRequestLine line: iCourses)
				for (CourseSelectionBox box: line.getCourses()) {
					RequestedCourse rc = box.getValue();
					if (rc != null && course.equals(rc.getCourseId()))
						return line;
				}
		}
		return null;
	}
	
	public void setWaitList(Long course, boolean waitList) {
		// skip inactive first
		for (CourseRequestLine line: iCourses) {
			for (CourseSelectionBox box: line.getCourses()) {
				RequestedCourse rc = box.getValue();
				if (rc != null && course.equals(rc.getCourseId()) && !rc.isInactive()) {
					line.setWaitList(waitList);
					return;
				}
			}
		}
		// all courses next
		for (CourseRequestLine line: iCourses) {
			for (CourseSelectionBox box: line.getCourses()) {
				RequestedCourse rc = box.getValue();
				if (rc != null && course.equals(rc.getCourseId())) {
					line.setWaitList(waitList);
					return;
				}
			}
		}
	}
	
	public boolean isWaitListed(Long course) {
		// skip inactive first
		for (CourseRequestLine line: iCourses) {
			for (CourseSelectionBox box: line.getCourses()) {
				RequestedCourse rc = box.getValue();
				if (rc != null && course.equals(rc.getCourseId()) && !rc.isInactive()) {
					return line.getWaitList();
				}
			}
		}
		// all courses next
		for (CourseRequestLine line: iCourses) {
			for (CourseSelectionBox box: line.getCourses()) {
				RequestedCourse rc = box.getValue();
				if (rc != null && course.equals(rc.getCourseId())) {
					return line.getWaitList();
				}
			}
		}
		return false;
	}
	
	public void clear() {
		iTip.setText(CONSTANTS.tips()[(int)(Math.random() * CONSTANTS.tips().length)]);
		for (CourseRequestLine line: iCourses)
			line.setValue(null);
		for (CourseRequestLine line: iAlternatives)
			line.setValue(null);
	}
	
	public String getFirstError() {
		if (iLastCheck != null && iLastCheck.hasErrorMessage())
			return iLastCheck.getErrorMessage();
		if (iLastCheck != null && iLastCheck.hasMessages()) {
			for (CourseRequestLine line: iCourses)
				for (CourseSelectionBox box: line.getCourses())
					for (CourseMessage m: iLastCheck.getMessages(box.getText()))
						if (m.isError()) return m.getMessage();
			for (CourseRequestLine line: iAlternatives)
				for (CourseSelectionBox box: line.getCourses())
					for (CourseMessage m: iLastCheck.getMessages(box.getText()))
						if (m.isError()) return m.getMessage();
			for (CourseMessage m: iLastCheck.getMessages())
				if (m.isError()) return m.getMessage();
		}
		for (CourseRequestLine line: iCourses)
			for (CourseSelectionBox box: line.getCourses())
				if (box.getError() != null) return box.getError();
		for (CourseRequestLine line: iAlternatives)
			for (CourseSelectionBox box: line.getCourses())
				if (box.getError() != null) return box.getError();
		return null;
	}

	@Override
	public HandlerRegistration addValueChangeHandler(ValueChangeHandler<CourseRequestInterface> handler) {
		return addHandler(handler, ValueChangeEvent.getType());
	}

	@Override
	public CourseRequestInterface getValue() {
		return getRequest();
	}

	@Override
	public void setValue(CourseRequestInterface value) {
		setValue(value, false);
	}

	@Override
	public void setValue(CourseRequestInterface value, boolean fireEvents) {
		setRequest(value);
		if (fireEvents)
			ValueChangeEvent.fire(this, value);
	}

	protected void clearErrors() {
		for (CourseRequestLine line: iCourses)
			for (CourseSelectionBox box: line.getCourses())
				box.setError(null);
		for (CourseRequestLine line: iAlternatives)
			for (CourseSelectionBox box: line.getCourses())
				box.setError(null);
	}

	public Command addCourse(RequestedCourse rc) {
		Request request = new Request(); request.addRequestedCourse(rc);
		for (final CourseRequestLine line: iCourses)
			if (line.getValue() == null) {
				line.setValue(request, true);
				return new Command() {
					@Override
					public void execute() {
						line.setValue(null, true);
						clearErrors();
					}
				};
			}
		addCourseLine();
		final CourseRequestLine line = iCourses.get(iCourses.size() - 1);
		line.setValue(request, true);
		return new Command() {
			@Override
			public void execute() {
				line.setValue(null, true);
				clearErrors();
			}
		};
	}
	
	public void addRequest(Request request) {
		for (final CourseRequestLine line: iCourses)
			if (line.getValue() == null) {
				line.setValue(request, true);
				return;
			}		
		addCourseLine();
		final CourseRequestLine line = iCourses.get(iCourses.size() - 1);
		line.setValue(request, true);
	}

	public boolean hasCourse(RequestedCourse rc) {
		for (final CourseRequestLine line: iCourses) {
			Request request = line.getValue();
			if (request != null && request.hasRequestedCourse(rc)) return true;
		}
		for (final CourseRequestLine line: iAlternatives) {
			Request request = line.getValue();
			if (request != null && request.hasRequestedCourse(rc)) return true;
		}
		return false;
	}
	
	public boolean hasCourseActive(RequestedCourse rc) {
		for (final CourseRequestLine line: iCourses) {
			Request request = line.getValue();
			if (request != null && request.hasRequestedCourseActive(rc)) return true;
		}
		for (final CourseRequestLine line: iAlternatives) {
			Request request = line.getValue();
			if (request != null && request.hasRequestedCourseActive(rc)) return true;
		}
		return false;
	}

	public void dropCourse(RequestedCourse rc) {
		for (final CourseRequestLine line: iCourses) {
			Request request = line.getValue();
			if (request != null && request.hasRequestedCourseActive(rc)) {
				line.delete();
				return;
			}
		}
		for (final CourseRequestLine line: iAlternatives) {
			Request request = line.getValue();
			if (request != null && request.hasRequestedCourseActive(rc)) {
				line.delete();
				return;
			}
		}
		for (final CourseRequestLine line: iCourses) {
			Request request = line.getValue();
			if (request != null && request.hasRequestedCourse(rc)) {
				line.delete();
				return;
			}
		}
		for (final CourseRequestLine line: iAlternatives) {
			Request request = line.getValue();
			if (request != null && request.hasRequestedCourse(rc)) {
				line.delete();
				return;
			}
		}
	}
	
	public boolean updateCourse(RequestedCourse rc) {
		for (final CourseRequestLine line: iCourses) {
			Request request = line.getValue();
			if (request != null && request.hasRequestedCourse(rc)) {
				request.update(rc);
				line.setValue(request);
				return true;
			}
		}
		for (final CourseRequestLine line: iAlternatives) {
			Request request = line.getValue();
			if (request != null && request.hasRequestedCourse(rc)) {
				request.update(rc);
				line.setValue(request);
				return true;
			}
		}
		return false;
	}
	
	public void dropCourse(ClassAssignmentInterface.ClassAssignment assignment) {
		if (assignment.isFreeTime() && assignment.isAssigned()) {
			FreeTime ft = new FreeTime(assignment.getDays(), assignment.getStart(), assignment.getLength());
			for (final CourseRequestLine line: iCourses) {
				Request request = line.getValue();
				if (request != null && request.hasRequestedCourse() && request.getRequestedCourse(0).isFreeTime() && request.getRequestedCourse(0).getFreeTime().contains(ft)) {
					request.getRequestedCourse(0).getFreeTime().remove(ft);
					if (request.getRequestedCourse(0).isEmpty())
						line.delete();
					else
						line.setValue(request, true);
					return;
				}
			}
		} else if (!assignment.isFreeTime()) {
			for (final CourseRequestLine line: iCourses)
				for (CourseSelectionBox box: line.getCourses())
					if (assignment.equalsIgnoreCase(box.getValue()) && !box.isInactive()) {
						line.delete(); return;
					}
			for (final CourseRequestLine line: iAlternatives)
				for (CourseSelectionBox box: line.getCourses())
					if (assignment.equalsIgnoreCase(box.getValue()) && !box.isInactive()) {
						line.delete(); return;
					}
			for (final CourseRequestLine line: iCourses)
				for (CourseSelectionBox box: line.getCourses())
					if (assignment.equalsIgnoreCase(box.getValue())) {
						line.delete(); return;
					}
			for (final CourseRequestLine line: iAlternatives)
				for (CourseSelectionBox box: line.getCourses())
					if (assignment.equalsIgnoreCase(box.getValue())) {
						line.delete(); return;
					}
		}
	}
	
	public void setCreditStatusIcon(Image image) { iCreditStatusIcon = image; }
	
	public CheckCoursesResponse getLastCheck() { return iLastCheck; }
}
