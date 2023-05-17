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
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.unitime.timetable.gwt.client.aria.AriaStatus;
import org.unitime.timetable.gwt.client.aria.ImageButton;
import org.unitime.timetable.gwt.client.widgets.OpenCloseSectionImage;
import org.unitime.timetable.gwt.client.widgets.P;
import org.unitime.timetable.gwt.client.widgets.UniTimeConfirmationDialog;
import org.unitime.timetable.gwt.client.widgets.UniTimeTable;
import org.unitime.timetable.gwt.client.widgets.UniTimeTableHeader;
import org.unitime.timetable.gwt.resources.GwtAriaMessages;
import org.unitime.timetable.gwt.resources.GwtMessages;
import org.unitime.timetable.gwt.resources.GwtResources;
import org.unitime.timetable.gwt.resources.StudentSectioningConstants;
import org.unitime.timetable.gwt.resources.StudentSectioningMessages;
import org.unitime.timetable.gwt.resources.StudentSectioningResources;
import org.unitime.timetable.gwt.shared.ClassAssignmentInterface;
import org.unitime.timetable.gwt.shared.ClassAssignmentInterface.ClassAssignment;
import org.unitime.timetable.gwt.shared.ClassAssignmentInterface.ErrorMessage;
import org.unitime.timetable.gwt.shared.CourseRequestInterface.CheckCoursesResponse;
import org.unitime.timetable.gwt.shared.OnlineSectioningInterface.GradeMode;
import org.unitime.timetable.gwt.shared.SpecialRegistrationInterface.RetrieveSpecialRegistrationResponse;
import org.unitime.timetable.gwt.shared.SpecialRegistrationInterface.SpecialRegistrationContext;
import org.unitime.timetable.gwt.shared.SpecialRegistrationInterface.SpecialRegistrationOperation;
import org.unitime.timetable.gwt.shared.SpecialRegistrationInterface.SpecialRegistrationStatus;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Tomas Muller
 */
public class SpecialRegistrationsPanel extends P {
	protected static StudentSectioningMessages MESSAGES = GWT.create(StudentSectioningMessages.class);
	protected static GwtMessages GWT_MESSAGES = GWT.create(GwtMessages.class);
	protected static StudentSectioningConstants CONSTANTS = GWT.create(StudentSectioningConstants.class);
	protected static StudentSectioningResources RESOURCES = GWT.create(StudentSectioningResources.class);
	protected static final GwtAriaMessages ARIA = GWT.create(GwtAriaMessages.class);
	private static DateTimeFormat sModifiedDateFormat = DateTimeFormat.getFormat(CONSTANTS.timeStampFormat());
	protected static final GwtResources GWT_RESOURCES =  GWT.create(GwtResources.class);

	
	private UniTimeTable<RetrieveSpecialRegistrationResponse> iTable;
	private FocusPanel iPanel;
	private SpecialRegistrationContext iSpecReg;
	private Image iWaiting = null;
	private CheckBox iShowAllChanges = null;
	private List<RetrieveSpecialRegistrationResponse> iRegistrations = new ArrayList<RetrieveSpecialRegistrationResponse>();
	private ClassAssignmentInterface iLastSaved = null;
	private OpenCloseSectionImage iOpenCloseImage;
	private boolean iHasOneOrMoreFullyApproved = false;
	private Label iAllRequestsApplied = null;
	
	public SpecialRegistrationsPanel(SpecialRegistrationContext specReg) {
		addStyleName("unitime-SpecialRegistrationsPanel");
		iSpecReg = specReg;
		
		P title = new P("registrations-header");
		iWaiting = new Image(RESOURCES.loading_small()); iWaiting.addStyleName("icon");
		iWaiting.setVisible(false);
		title.add(iWaiting);
		
		iOpenCloseImage = new OpenCloseSectionImage(true);
		iOpenCloseImage.addStyleName("open-close-icon");
		iOpenCloseImage.setVisible(true);
		title.add(iOpenCloseImage);

		P label = new P("title"); label.setText(MESSAGES.dialogSpecialRegistrations());
		title.add(label);
		add(title);
		
		iTable = new Table<RetrieveSpecialRegistrationResponse>();
		iTable.addStyleName("registrations-table");
		iTable.setAllowSelection(true);
		iTable.setAllowMultiSelect(true);
		
		iAllRequestsApplied = new Label(MESSAGES.specRegAllRequestsFullyApplied());
		iAllRequestsApplied.setVisible(false);
		iAllRequestsApplied.addStyleName("all-requests-applied");
		add(iAllRequestsApplied);
		
		iPanel = new FocusPanel(iTable);
		iPanel.addStyleName("registrations-panel");
		add(iPanel);
		
		iShowAllChanges = new CheckBox(MESSAGES.checkOverridesShowAllChanges());
		String showAllChanges = Cookies.getCookie("UniTime:ShowAllChanges");
		iShowAllChanges.setValue(showAllChanges != null && SectioningCookie.getInstance().isShowAllChanges());
		iShowAllChanges.addStyleName("registrations-toggle");
		add(iShowAllChanges);
		
		iPanel.addKeyUpHandler(new KeyUpHandler() {
			@Override
			public void onKeyUp(KeyUpEvent event) {
				if (event.getNativeKeyCode() == KeyCodes.KEY_UP) {
					RetrieveSpecialRegistrationResponse prev = null;
					RetrieveSpecialRegistrationResponse selected = null;
					RetrieveSpecialRegistrationResponse last = null;
					for (int row = 0; row < iTable.getRowCount(); row ++) {
						RetrieveSpecialRegistrationResponse d = iTable.getData(row);
						if (d == null) continue;
						if (iTable.isSelected(row)) selected = d;
						else if (selected == null) prev = d;
						last = d;
					}
					int row = setSelected(prev == null ? last : prev);
					if (row >= 0)
						iTable.getRowFormatter().getElement(row).scrollIntoView();
					updateAriaStatus();
					event.preventDefault();
					event.stopPropagation();
					iPanel.setFocus(true);
				} else if (event.getNativeKeyCode() == KeyCodes.KEY_DOWN) {
					RetrieveSpecialRegistrationResponse first = null;
					RetrieveSpecialRegistrationResponse selected = null;
					RetrieveSpecialRegistrationResponse next = null;
					for (int row = 0; row < iTable.getRowCount(); row ++) {
						RetrieveSpecialRegistrationResponse d = iTable.getData(row);
						if (d == null) continue;
						if (first == null) first = d;
						if (iTable.isSelected(row)) selected = d;
						else if (selected != null && next == null) next = d;
					}
					int row = setSelected(next == null ? first : next);
					if (row >= 0)
						iTable.getRowFormatter().getElement(row).scrollIntoView();
					updateAriaStatus();
					event.preventDefault();
					event.stopPropagation();
					iPanel.setFocus(true);
				} else if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER || event.getNativeKeyCode() == KeyCodes.KEY_SPACE) {
					if (iTable.getSelectedRow() > 0)
						doSubmit(iTable.getData(iTable.getSelectedRow()));
					event.preventDefault();
					event.stopPropagation();
					iPanel.setFocus(true);
				} else if (event.getNativeKeyCode() == KeyCodes.KEY_DELETE) {
					if (iTable.getSelectedRow() > 0) {
						cancel(iTable.getData(iTable.getSelectedRow()));
					}
				}
			}
		});
		
		iShowAllChanges.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
			@Override
			public void onValueChange(ValueChangeEvent<Boolean> event) {
				SectioningCookie.getInstance().setShowAllChanges(event.getValue());
				if (iLastSaved != null)
					populate(getRegistrations(), iLastSaved);
			}
		});
		
		iOpenCloseImage.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
			@Override
			public void onValueChange(ValueChangeEvent<Boolean> event) {
				iPanel.setVisible(event.getValue() && iTable.getRowCount() > 1);
				iAllRequestsApplied.setVisible(event.getValue() && iTable.getRowCount() <= 1);
				iShowAllChanges.setVisible(event.getValue());
				SectioningCookie.getInstance().setRequestOverridesOpened(event.getValue());
			}
		});
		label.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if (iOpenCloseImage.isVisible()) {
					iOpenCloseImage.setValue(!iOpenCloseImage.getValue(), true);
				}
			}
		});

		List<UniTimeTableHeader> header = new ArrayList<UniTimeTableHeader>();
		header.add(new UniTimeTableHeader(""));
		header.add(new UniTimeTableHeader(MESSAGES.colSpecRegSubmitted()));
		header.add(new UniTimeTableHeader(MESSAGES.colSubject()));
		header.add(new UniTimeTableHeader(MESSAGES.colCourse()));
		header.add(new UniTimeTableHeader(MESSAGES.colSubpart()));
		header.add(new UniTimeTableHeader(MESSAGES.colClass()));
		header.add(new UniTimeTableHeader(MESSAGES.colLimit()));
		header.add(new UniTimeTableHeader(MESSAGES.colCredit()));
		header.add(new UniTimeTableHeader(MESSAGES.colGradeMode()));
		header.add(new UniTimeTableHeader(MESSAGES.colSpecRegErrors()));
		header.add(new UniTimeTableHeader(""));
		header.add(new UniTimeTableHeader(""));
		iTable.addRow(null, header);
		
		iTable.addMouseClickListener(new UniTimeTable.MouseClickListener<RetrieveSpecialRegistrationResponse>() {
			@Override
			public void onMouseClick(UniTimeTable.TableEvent<RetrieveSpecialRegistrationResponse> event) {
				if (event.getData() != null)
					doSubmit(event.getData());
			}
		});
		
		iOpenCloseImage.setValue(SectioningCookie.getInstance().isRequestOverridesOpened());
		iPanel.setVisible(iOpenCloseImage.getValue());
		iShowAllChanges.setVisible(iOpenCloseImage.getValue());
		setVisible(false);
	}
	
	public void showWaiting() {
		iWaiting.setVisible(true);
		iOpenCloseImage.setVisible(false);
		iAllRequestsApplied.setVisible(false);
		iPanel.setVisible(false);
		iShowAllChanges.setVisible(false);
		setVisible(true);
	}
	
	public void hideWaiting() {
		iWaiting.setVisible(false);
		iOpenCloseImage.setVisible(true);
		iAllRequestsApplied.setVisible(iOpenCloseImage.getValue() && iTable.getRowCount() <= 1);
		iPanel.setVisible(iOpenCloseImage.getValue() && iTable.getRowCount() > 1);
		iShowAllChanges.setVisible(iOpenCloseImage.getValue());
		setVisible(!iRegistrations.isEmpty());
	}
	
	public void clearRegistrations() {
		iRegistrations = new ArrayList<RetrieveSpecialRegistrationResponse>();
		iLastSaved = null;
		iTable.clearTable(1);
		setVisible(false);
	}
	
	public List<RetrieveSpecialRegistrationResponse> getRegistrations() { return iRegistrations; }
	
	public void populate(List<RetrieveSpecialRegistrationResponse> registrations, ClassAssignmentInterface saved) {
		iRegistrations = registrations;
		iLastSaved = saved;
		iTable.clearTable(1);
		iHasOneOrMoreFullyApproved = false;
		Collections.sort(registrations);
		for (final RetrieveSpecialRegistrationResponse reg: registrations) {
			P p = new P("icons");
			if (reg.isFullyApplied(saved)) {
				p.add(new Icon(RESOURCES.specRegApplied(), MESSAGES.hintSpecRegApplied()));
			} else if (reg.getStatus() != null) {
				switch (reg.getStatus()) {
				case Approved:
					if (reg.isGradeModeChange() || reg.isVariableTitleCourseChange() || reg.isExtended())
						p.add(new Icon(RESOURCES.specRegApproved(), MESSAGES.hintSpecRegApproved()));
					else
						p.add(new Icon(RESOURCES.specRegApproved(), MESSAGES.hintSpecRegApprovedNoteApply()));
					break;
				case Cancelled:
					p.add(new Icon(RESOURCES.specRegCancelled(), MESSAGES.hintSpecRegCancelled()));
					break;
				case Pending:
					if (reg.isHonorsGradeModeNotFullyMatching(saved)) {
						p.add(new Icon(RESOURCES.specRegCancelled(), MESSAGES.hintSpecRegHonorsGradeModeNotMatchingSchedule()));
					} else {
						p.add(new Icon(RESOURCES.specRegPending(), MESSAGES.hintSpecRegPending()));
					}
					break;
				case Rejected:
					p.add(new Icon(RESOURCES.specRegRejected(), MESSAGES.hintSpecRegRejected()));
					break;
				case Draft:
					p.add(new Icon(RESOURCES.specRegDraft(), MESSAGES.hintSpecRegDraft()));
					break;
				}
			}
			ImageButton delete = null;
			if (reg.canCancel() && iSpecReg.isEnabled()) {
				delete = new ImageButton(RESOURCES.delete(), RESOURCES.delete_Down(), RESOURCES.delete_Over());
				delete.addStyleName("unitime-NoPrint");
				delete.addClickHandler(new ClickHandler() {
					public void onClick(ClickEvent event) {
						for (int row = 1; row < iTable.getRowCount(); row ++) {
							RetrieveSpecialRegistrationResponse data = iTable.getData(row);
							iTable.setSelected(row, data != null && data.equals(reg));
						}
						cancel(reg);
						event.preventDefault();
						event.stopPropagation();
					}
				});
				delete.addStyleName("delete");
				delete.setAltText(ARIA.altCancelOverrideRequest());
			}
			if (reg.hasChanges()) {
				if (!iShowAllChanges.getValue() && reg.isFullyApplied(saved)) continue;
				Long lastCourseId = null;
				List<ClassAssignment> rows = new ArrayList<ClassAssignment>();
				for (ClassAssignment ca: reg.getChanges()) {
					if (!iShowAllChanges.getValue() && (reg.isApplied(ca.getCourseId(), saved) || (!reg.hasErrors(ca.getCourseId()) && !reg.isDrop(ca.getCourseId())))) {
						// UniTimeNotifications.info(ca.getCourseName() + ": applied=" + reg.isApplied(ca.getCourseId(), saved) + ", errors:" + reg.hasErrors(ca.getCourseId()) + ", drop: " + reg.isDrop(ca.getCourseId()));
						continue;
					}
					if (!iShowAllChanges.getValue() && reg.isChange(ca.getCourseId()) && !ca.hasError()) {
						// UniTimeNotifications.info(ca.getCourseName() + ": change:" + reg.isChange(ca.getCourseId()) + ", error:" + ca.hasError());
						continue;
					}
					if (ca.getParentSection() != null && ca.getParentSection().equals(ca.getSection())) {
						// UniTimeNotifications.info(ca.getCourseName() + ": this:" + ca.getSection() + ", parent:" + ca.getParentSection());
						continue;
					}
					rows.add(ca);
				}
				
				for (int r = 0; r < rows.size(); r++) {
					ClassAssignment ca = rows.get(r);
					List<Widget> row = new ArrayList<Widget>();
					if (lastCourseId == null) {
						row.add(p);
					} else {
						row.add(new P("icons"));
					}
					Label label = new Label();
					label.addStyleName("date-and-note");
					if (lastCourseId == null || !lastCourseId.equals(ca.getCourseId())) {
						String course = rows.get(r).getCourseName();
						String note = reg.getNote(course);
						if (iSpecReg.isAllowChangeRequestNote() && reg.getStatus() == SpecialRegistrationStatus.Pending && reg.hasErrors(course) && (note == null || note.isEmpty()))
							note = MESSAGES.noRequestNoteClickToChange();
						label.setText(r > 0 || reg.getSubmitDate() == null ? note == null ? "" : note : sModifiedDateFormat.format(reg.getSubmitDate()) + (note == null || note.isEmpty() ? "" : "\n" + note));
						if (iSpecReg.isAllowChangeRequestNote() && reg.getStatus() == SpecialRegistrationStatus.Pending && reg.hasErrors(course)) {
							label.getElement().getStyle().setCursor(Cursor.POINTER);
							final String courseName = rows.get(r).getCourseName();
							final Long courseId = rows.get(r).getCourseId();
							label.addClickHandler(new ClickHandler() {
								@Override
								public void onClick(ClickEvent event) {
									iSpecReg.getChangeRequestorNoteInterface().changeRequestorNote(reg, courseName, courseId);
									event.stopPropagation();
								}
							});
						}
					}
					row.add(label);
					if (lastCourseId == null || !lastCourseId.equals(ca.getCourseId())) {
						row.add(new Label(ca.getSubject(), false));
						row.add(new Label(ca.getCourseNbr(), false));
					} else {
						row.add(new Label());
						row.add(new Label());
					}
					row.add(new Label(ca.getSubpart(), false));
					row.add(new Label(ca.getSection(), false));
					row.add(new HTML(ca.getLimitString(), false));
					if (ca.getCreditHour() != null) {
						row.add(new Label(MESSAGES.credit(ca.getCreditHour())));
					} else {
						row.add(new CreditCell(ca.getCredit()));
					}
					if (ca.getGradeMode() != null) {
						Label gm = new Label(ca.getGradeMode().getCode());
						if (ca.getGradeMode().getLabel() != null) gm.setTitle(ca.getGradeMode().getLabel());
						row.add(gm);
					} else {
						row.add(new Label());
					}
					HTML errorsLabel = new HTML(ca.hasError() ? ca.getError() : ""); errorsLabel.addStyleName("registration-errors");
					row.add(errorsLabel);
					P s = new P("icons");
					switch (ca.getSpecRegOperation()) {
					case Add:
						s.add(new Icon(RESOURCES.assignment(), MESSAGES.specRegAssignment(ca.getSubject() + " " + ca.getCourseNbr() + " " + ca.getSubpart() + " " + ca.getSection())));
						break;
					case Drop:
						s.add(new Icon(RESOURCES.unassignment(), MESSAGES.specRegRemoved(ca.getSubject() + " " + ca.getCourseNbr() + " " + ca.getSubpart() + " " + ca.getSection())));
						break;
					case Keep:
						if (ca.getGradeMode() != null && ca.getGradeMode().isHonor()) {
							boolean found = false;
							for (ClassAssignmentInterface.ClassAssignment x: saved.getClassAssignments())
								if (x.isSaved() && ca.getClassId().equals(x.getClassId())) {
									found = true; break;
								}
							if (!found)
								s.add(new Icon(RESOURCES.unassignment(), MESSAGES.specRegRemoved(ca.getSubject() + " " + ca.getCourseNbr() + " " + ca.getSubpart() + " " + ca.getSection())));
						}
						// s.add(new Icon(RESOURCES.saved(), MESSAGES.saved(ca.getSubject() + " " + ca.getCourseNbr() + " " + ca.getSubpart() + " " + ca.getSection())));
						// break;
					default:
						s.add(new Label());
					}
					row.add(s);
					if (delete != null) {
						row.add(delete); delete = null;
					} else
						row.add(new Label());
					int idx = iTable.addRow(reg, row);
					if (reg.getStatus() == SpecialRegistrationStatus.Approved) {
						iTable.setBackGroundColor(idx, "#D7FFD7");
						if (!reg.isFullyApplied(saved) && !reg.isExtended() && !reg.isGradeModeChange())
							iHasOneOrMoreFullyApproved = true;
					}
					if (reg.getRequestId().equals(iSpecReg.getRequestId()))
						iTable.setSelected(idx, true);
					if (idx > 1 && lastCourseId == null)
						for (int c = 0; c < iTable.getCellCount(idx); c++)
							iTable.getCellFormatter().addStyleName(idx, c, "top-border-solid");
					if (lastCourseId != null && !lastCourseId.equals(ca.getCourseId()))
						for (int c = 2; c < iTable.getCellCount(idx) - 1; c++)
							iTable.getCellFormatter().addStyleName(idx, c, "top-border-dashed");
					if (!ca.isCourseAssigned()) {
						for (int c = 2; c < iTable.getCellCount(idx) - 1; c++)
							iTable.getCellFormatter().addStyleName(idx, c, ca.hasError() ? "change-drop-with-errors" : "change-drop");
					} else  {
						for (int c = 2; c < iTable.getCellCount(idx) - 1; c++)
							iTable.getCellFormatter().addStyleName(idx, c, "change-add");
					}
					lastCourseId = ca.getCourseId();
				}
				String noCourseErrors = "";
				if (reg.hasErrors())
					for (ErrorMessage e: reg.getErrors())
						if (e.getCourse() == null || e.getCourse().isEmpty())
							noCourseErrors += (noCourseErrors.isEmpty() ? "" : "\n") + e.getMessage();
				if (!noCourseErrors.isEmpty()) {
					List<Widget> row = new ArrayList<Widget>();
					row.add(new P("icons"));
					String note = reg.getNote("MAXI");
					DateAndNoteCell dateAndNote = new DateAndNoteCell(null, note);
					if (iSpecReg.isAllowChangeRequestNote() && reg.getStatus() == SpecialRegistrationStatus.Pending && reg.hasErrorCode("MAXI")) {
						if (note == null || note.isEmpty())
							dateAndNote = new DateAndNoteCell(null, MESSAGES.noRequestNoteClickToChange());						
						dateAndNote.getElement().getStyle().setCursor(Cursor.POINTER);
						dateAndNote.addClickHandler(new ClickHandler() {
							@Override
							public void onClick(ClickEvent event) {
								iSpecReg.getChangeRequestorNoteInterface().changeRequestorNote(reg, null, null);
								event.stopPropagation();
							}
						});
					}
					row.add(dateAndNote);
					row.add(new DescriptionCell(null));
					HTML errorsLabel = new HTML(noCourseErrors); errorsLabel.addStyleName("registration-errors");
					row.add(errorsLabel);
					row.add(new Label());
					row.add(new Label());
					int idx = iTable.addRow(reg, row);
					if (reg.getStatus() == SpecialRegistrationStatus.Approved)
						iTable.setBackGroundColor(idx, "#D7FFD7");
					if (reg.getRequestId().equals(iSpecReg.getRequestId()))
						iTable.setSelected(idx, true);
					for (int c = 2; c < iTable.getCellCount(idx) - 1; c++)
						iTable.getCellFormatter().addStyleName(idx, c, "top-border-dashed");
				}
			} else if (reg.hasErrors()) {
				List<Widget> row = new ArrayList<Widget>();
				row.add(p);
				String note = reg.getNote("MAXI");
				DateAndNoteCell dateAndNote = new DateAndNoteCell(reg.getSubmitDate(), note);
				if (iSpecReg.isAllowChangeRequestNote() && reg.getStatus() == SpecialRegistrationStatus.Pending && reg.hasErrorCode("MAXI")) {
					if (note == null || note.isEmpty())
						dateAndNote = new DateAndNoteCell(reg.getSubmitDate(), MESSAGES.noRequestNoteClickToChange());						
					dateAndNote.getElement().getStyle().setCursor(Cursor.POINTER);
					dateAndNote.addClickHandler(new ClickHandler() {
						@Override
						public void onClick(ClickEvent event) {
							iSpecReg.getChangeRequestorNoteInterface().changeRequestorNote(reg, null, null);
							event.stopPropagation();
						}
					});
				}
				row.add(dateAndNote);
				row.add(new DescriptionCell(reg.getDescription()));
				String errors = "";
				for (ErrorMessage e: reg.getErrors())
					errors += (errors.isEmpty() ? "" : "\n") + e.getMessage();
				HTML errorsLabel = new HTML(errors); errorsLabel.addStyleName("registration-errors");
				row.add(errorsLabel);
				row.add(new Label());
				if (delete != null)
					row.add(delete);
				else
					row.add(new Label());
				int idx = iTable.addRow(reg, row);
				if (reg.getStatus() == SpecialRegistrationStatus.Approved)
					iTable.setBackGroundColor(idx, "#D7FFD7");
				if (reg.getRequestId().equals(iSpecReg.getRequestId()))
					iTable.setSelected(idx, true);
				if (idx > 1)
					for (int c = 0; c < iTable.getCellCount(idx); c++)
						iTable.getCellFormatter().addStyleName(idx, c, "top-border-solid");
			}
		}
		setVisible(!iRegistrations.isEmpty());
		iAllRequestsApplied.setVisible(iOpenCloseImage.getValue() && iTable.getRowCount() <= 1);
		iPanel.setVisible(iOpenCloseImage.getValue() && iTable.getRowCount() > 1);
	}
	
	public boolean hasOneOrMoreFullyApproved() {
		return iHasOneOrMoreFullyApproved;
	}
	
	protected int setSelected(RetrieveSpecialRegistrationResponse data) {
		int row = -1;
		for (int i = 0; i < iTable.getRowCount(); i++) {
			RetrieveSpecialRegistrationResponse d = iTable.getData(i);
			if (d == null) continue;
			if (row < 0 && d.equals(data)) row = i;
			iTable.setSelected(i, d.equals(data));
		}
		return row;
	}
	
	protected void updateAriaStatus() {
		int row = iTable.getSelectedRow();
		RetrieveSpecialRegistrationResponse reg = iTable.getData(row);
		if (row >= 0 && reg != null) {
			AriaStatus.getInstance().setText(ARIA.showingSpecReg(row, iTable.getRowCount() - 1, reg.getDescription(), reg.getSubmitDate()));
		}
	}
	
	protected class DateAndNoteCell extends Label {
		public DateAndNoteCell(Date date, String note) {
			super(date == null ? note == null ? "" : note : sModifiedDateFormat.format(date) + (note == null || note.isEmpty() ? "" : "\n" + note));
			addStyleName("date-and-note");
		}
	}
	
	protected class DescriptionCell extends Label implements UniTimeTable.HasColSpan {
		
		public DescriptionCell(String text) {
			super(text == null ? "" : text);
		}
	
		@Override
		public int getColSpan() {
			return 7;
		}
		
	}
	
	protected class Icon extends Image {
		public Icon(ImageResource image, final String text) {
			super(image);
			if (text != null && !text.isEmpty()) {
				setAltText(text);
				setTitle(text);
				addClickHandler(new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						event.preventDefault(); event.stopPropagation();
						UniTimeConfirmationDialog.info(text);
					}
				});
			}
		}
	}
	
	protected class CreditCell extends HTML {
		public CreditCell(String text) {
			if (text != null && text.indexOf('|') >= 0) {
				setHTML(text.substring(0, text.indexOf('|')));
				setTitle(text.substring(text.indexOf('|') + 1).replace("\n", "<br>"));
			} else {
				setHTML(text == null ? "" : text.replace("\n", "<br>"));
				if (text != null) setTitle(text);
			}
		}
	}
	
	public class Table<T> extends UniTimeTable<T> {
		protected Set<Integer> iLastHoverRows = new HashSet<Integer>();
		
		public Table() {
			super();
			sinkEvents(Event.ONKEYDOWN);
		}
		
		protected void updateHover() {
			// clear hover if needed
			if (!iLastHoverRows.isEmpty() && (iLastHoverRow < 0 || !iLastHoverRows.contains(iLastHoverRow))) {
				for (int row: iLastHoverRows) {
					String style = getRowFormatter().getStyleName(row);
					if ("unitime-TableRowSelected".equals(style)) {
					} else if ("unitime-TableRowHover".equals(style)) {
						getRowFormatter().setStyleName(row, null);
					} else if ("unitime-TableRowSelectedHover".equals(style)) {
						getRowFormatter().setStyleName(row, "unitime-TableRowSelected");
					}
					if (getRowFormatter().getStyleName(row).isEmpty()) {
						String color = iLastHoverBackgroundColor.remove(row);
						if (color != null && !color.isEmpty()) {
							getRowFormatter().getElement(row).getStyle().setBackgroundColor(color);
						}
					}
				}
				iLastHoverRows.clear();
			}

			// set hover if needed
			if (iLastHoverRow >= 0 && iLastHoverRows.isEmpty()) {
				T data = getData(iLastHoverRow);
				if (data != null) {
					for (int row = 0; row < getRowCount(); row++) {
						if (data.equals(getData(row))) {
							iLastHoverRows.add(row);
							String style = getRowFormatter().getStyleName(row);
							if ("unitime-TableRowSelectedHover".equals(style)) {
							} else if ("unitime-TableRowSelected".equals(style)) {
								getRowFormatter().setStyleName(row, "unitime-TableRowSelectedHover");
							} else {
								getRowFormatter().setStyleName(row, "unitime-TableRowHover");
							}
							if (style.isEmpty()) {
								String color = getRowFormatter().getElement(row).getStyle().getBackgroundColor();
								if (color != null && !color.isEmpty()) {
									getRowFormatter().getElement(row).getStyle().clearBackgroundColor();
									iLastHoverBackgroundColor.put(row, color);
								} else {
									iLastHoverBackgroundColor.remove(row);
								}
							} else if (!getRowFormatter().getElement(row).getStyle().getBackgroundColor().isEmpty()) {
								String color = getRowFormatter().getElement(row).getStyle().getBackgroundColor();
								getRowFormatter().getElement(row).getStyle().clearBackgroundColor();
								iLastHoverBackgroundColor.put(row, color);
							}
						}
					}
				}
			}
		}
		
		@Override
		public void clearHover() {
			super.clearHover();
			updateHover();
		}
		
		@Override
		public void onBrowserEvent(Event event) {
			super.onBrowserEvent(event);
			updateHover();
		}
	}

	public void doCancel(String requestId, AsyncCallback<Boolean> callback) {
	}
	
	public void doSubmit(RetrieveSpecialRegistrationResponse reg) {
		if (reg != null)
			AriaStatus.getInstance().setText(ARIA.selectedSpecReg(reg.getDescription()));
		for (int row = 1; row < iTable.getRowCount(); row ++) {
			RetrieveSpecialRegistrationResponse data = iTable.getData(row);
			iTable.setSelected(row, data != null && data.equals(reg));
		}
	}
	
	public SpecialRegistrationStatus getStatus(ClassAssignment a) {
		if (a.getSpecRegStatus() != null) return a.getSpecRegStatus();
		if (iRegistrations != null && a.getClassId() != null)
			for (RetrieveSpecialRegistrationResponse response: iRegistrations) {
				if (response.hasChanges() && !response.isFullyApplied(iLastSaved) && !response.isApplied(a.getCourseId(), iLastSaved))
					for (ClassAssignment ch: response.getChanges())
						if (ch.getGradeMode() != null) {
							if (a.getCourseId().equals(ch.getCourseId()) && a.getGradeMode() != null && !ch.getGradeMode().equals(a.getGradeMode()) && response.getRequestId().equals(iSpecReg.getRequestId()))
								return ch.getSpecRegStatus();
						} else if (ch.getCreditHour() != null) {
							if (a.getCourseId().equals(ch.getCourseId()) && a.getCreditHour() != null && !ch.getCreditHour().equals(a.getCreditHour()) && response.getRequestId().equals(iSpecReg.getRequestId()))
								return ch.getSpecRegStatus();
						} else if (a.getCourseId().equals(ch.getCourseId()) && a.getSection().equals(ch.getSection())) {
							return ch.getSpecRegStatus();
						}
			}
		return null;
	}
	
	public String getError(ClassAssignment a) {
		if (a.getSpecRegStatus() != null) return (a.hasError() ? a.getError() : null);
		if (iRegistrations != null && a.getClassId() != null)
			for (RetrieveSpecialRegistrationResponse response: iRegistrations) {
				if (response.hasChanges())
					for (ClassAssignment ch: response.getChanges())
						if (ch.getGradeMode() != null) {
							if (a.getCourseId().equals(ch.getCourseId()) && a.getGradeMode() != null && !ch.getGradeMode().equals(a.getGradeMode()) && response.getRequestId().equals(iSpecReg.getRequestId()))
								if (ch.hasError()) return ch.getError();
						} else if (ch.getCreditHour() != null) {
							if (a.getCourseId().equals(ch.getCourseId()) && a.getCreditHour() != null && !ch.getCreditHour().equals(a.getCreditHour()) && response.getRequestId().equals(iSpecReg.getRequestId()))
								if (ch.hasError()) return ch.getError();
						} else if (a.getCourseId().equals(ch.getCourseId()) && a.getSection().equals(ch.getSection()))
							if (ch.hasError()) return ch.getError();
			}
		return null;
	}
	
	public GradeMode getGradeMode(ClassAssignment a) {
		if (iRegistrations != null && a.getClassId() != null)
			for (RetrieveSpecialRegistrationResponse response: iRegistrations) {
				if (response.hasChanges())
					for (ClassAssignment ch: response.getChanges())
						if (a.getCourseId().equals(ch.getCourseId()) && ch.getGradeMode() != null)
							return ch.getGradeMode();
			}
		return null;
	}
	
	public Float getCreditHours(ClassAssignment a) {
		if (iRegistrations != null && a.getClassId() != null)
			for (RetrieveSpecialRegistrationResponse response: iRegistrations) {
				if (response.hasChanges())
					for (ClassAssignment ch: response.getChanges())
						if (a.getCourseId().equals(ch.getCourseId()) && ch.getCreditHour() != null && a.getSection().equals(ch.getSection()) && !a.getSection().equals(a.getParentSection()))
							return ch.getCreditHour();
			}
		return null;
	}
	
	public boolean isDrop(Long courseId) {
		if (courseId == null || iRegistrations == null) return false;
		boolean hasDrop = false, hasAdd = false;
		for (RetrieveSpecialRegistrationResponse response: iRegistrations) {
			if (response.hasChanges())
				for (ClassAssignment ca: response.getChanges())
					if (courseId.equals(ca.getCourseId())) {
						switch (ca.getSpecRegOperation()) {
						case Add: hasAdd = true; break;
						case Drop: hasDrop = true; break;
						}
			}
		}
		return hasDrop && !hasAdd;
	}
	
	public boolean canWaitList(Long courseId) {
		if (courseId == null) return true;
		for (RetrieveSpecialRegistrationResponse response: iRegistrations) {
			if (response.getStatus() != SpecialRegistrationStatus.Pending) continue; // not in-progress
			if (!response.hasChanges()) continue; // no changes
			for (ClassAssignment ca: response.getChanges()) {
				if (courseId.equals(ca.getCourseId()) && ca.getSpecRegOperation() == SpecialRegistrationOperation.Add && ca.getSpecRegStatus() == SpecialRegistrationStatus.Pending)
					return false;
			}
		}
		return true;
	}
	
	protected void cancel(final RetrieveSpecialRegistrationResponse reg) {
		if (reg != null && reg.canCancel()) {
			CheckCoursesResponse confirm = new CheckCoursesResponse();
			confirm.setConfirmation(0, GWT_MESSAGES.dialogConfirmation(), GWT_MESSAGES.buttonConfirmYes(), GWT_MESSAGES.buttonConfirmNo(), null, null);
			confirm.addConfirmation(MESSAGES.confirmOverrideRequestCancel(), 0, 1);
			if (reg.hasErrors()) {
				confirm.addConfirmation(MESSAGES.confirmOverrideRequestCancelCancelledErrors(), 0, 2);
				for (ErrorMessage e: reg.getErrors())
					confirm.addMessage(null, e.getCourse(), e.getCode(), e.getMessage(), 0, 3);
			}
			CourseRequestsConfirmationDialog.confirm(confirm, 0, GWT_RESOURCES.confirm(), new AsyncCallback<Boolean>() {
				@Override
				public void onFailure(Throwable caught) {
				}

				@Override
				public void onSuccess(Boolean result) {
					if (result) {
						doCancel(reg.getRequestId(), new AsyncCallback<Boolean>(){
							@Override
							public void onFailure(Throwable caught) {}
							@Override
							public void onSuccess(Boolean result) {
								if (result) {
									iTable.clearHover();
									for (int i = iTable.getRowCount() - 1; i > 0; i --) {
										if (iTable.getData(i).equals(reg)) {
											iTable.removeRow(i);
										}
									}
									iRegistrations.remove(reg);
									setVisible(!iRegistrations.isEmpty());
									iAllRequestsApplied.setVisible(iOpenCloseImage.getValue() && iTable.getRowCount() <= 1);
									iPanel.setVisible(iOpenCloseImage.getValue() && iTable.getRowCount() > 1);
								}
							}
						});
					}					
				}
			});
		}
	}
}
