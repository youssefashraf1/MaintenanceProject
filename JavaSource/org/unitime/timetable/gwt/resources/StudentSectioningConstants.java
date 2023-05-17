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
package org.unitime.timetable.gwt.resources;

/**
 * @author Tomas Muller, Stephanie Schluttenhofer
 */
public interface StudentSectioningConstants extends Constants {
	@DefaultStringArrayValue({
		"Tip: Use Ctrl+1 (or Ctrl+Alt+1) to navigate to the first course, Ctrl+2 to the second, Ctrl+A to the first alternative, Ctr+B to the second alternative, etc.",
		"Tip: Use Ctrl+Arrow to navigate, Ctrl+Shift+Up and Ctrl+Shith+Down to move a line around.",
		"Tip: Use Ctrl+F (or Ctrl+Alt+F in some browsers) to open the Course Finder dialog.",
		"Tip: Use Ctrl+N (or Ctrl+Alt+N in some browsers) to validate the screen and go next.",
		"Tip: Start entering the name (e.g., ENGL 10600) of the course or a part of its title (e.g., History) to see suggestions.",
		"Tip: The Substitute Course Requests below can be used to ensure that the desired number of courses are scheduled even when a Course Request (and its alternatives) are not available.",
		"Tip: Enter a free time to aviod getting classes in time you need for something else.",
		"Tip: All courses above a free time should not overlap with the free time (you will get the course even when the only possibility is to break the free time).",
		"Tip: All courses below a free time can not overlap with the free time (you will only get the course if there are sections that do not break the free time).",
		"Tip: Click this tip to see another tip.",
		"Tip: There are no alternative free times.",
		"Tip: Try not to break too many standard time patterns with a free time (see the numbers in the Course Finder dialog).",
		"Tip: Use Esc to hide suggestions, Ctrl+L (or Ctrl+Alt+L in some browsers) to show suggestions."
	})
	String[] tips();
	
	@DefaultStringArrayValue({
		"Tip: Use Up and Down to navigate through courses, Enter to select one.",
		"Tip: Start entering the name of a course (e.g., ENGL 10600) or a part of its title (e.g., History) to see suggestions.",
		"Tip: Click on a course to see its details.",
		"Tip: Doubleclik on a course to select it.",
		"Tip: Press Esc to close the dialog, Enter to select the inputed text or the selected course."})
	String[] courseTips();

	@DefaultStringArrayValue({
		"Tip: Enter a free time (e.g., Monday 8am - 10am) or use the mouse to select it.",
		"Tip: The numbers in the selected times counts the number of overlapping standard time patterns (3x50, 2x75, 1x150), try to avoid overlapping too many of those."})
	String[] freeTimeTips();

	@DefaultStringValue("Free ")
	String freePrefix();

	@DefaultIntValue(25)
	@DoNotTranslate
	int freeTimePeriods();
	
	@DefaultStringArrayValue({"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"})
	String[] freeTimeDays();
	
	@DefaultStringArrayValue({"M", "T", "W", "R", "F", "S", "U"})
	String[] freeTimeShortDays();
	
	@DefaultStringArrayValue({"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"})
	String[] freeTimeLongDays();

	@DefaultStringArrayValue({"0", "2", "6", "8", "12", "14", "15", "16", "17", "18", "19", "20"})
	@DoNotTranslate
	String[] freeTimeOneDay150();
	
	@DefaultStringArrayValue({"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"})
	String[] longDays();

	@DefaultStringArrayValue({"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"})
	String[] days();

	@DefaultStringArrayValue({"M", "T", "W", "R", "F", "S", "U"})
	String[] shortDays();

	@DefaultStringArrayValue({
		"blue", "green", "orange", "yellow", "pink",
		"purple", "teal", "darkpurple", "steelblue", "lightblue",
		"lightgreen", "yellowgreen", "redorange", "lightbrown", "lightpurple",
		"grey", "bluegrey", "lightteal", "yellowgrey", "brown"})
	@DoNotTranslate
	String[] meetingColors();
	
	@DefaultStringValue("red")
	@DoNotTranslate
	String freeTimeColor();
	
	@DefaultStringValue("steelblue")
	@DoNotTranslate
	String teachingAssignmentColor();
	
	@DefaultBooleanValue(false)
	@DoNotTranslate
	boolean printReportShowUserName();
	
	@DefaultIntValue(12)
	@DoNotTranslate
	int numberOfCourses();
	
	@DefaultIntValue(3)
	@DoNotTranslate
	int numberOfAlternatives();
	
	@DefaultStringValue("MM/dd/yyyy")
	String requestDateFormat();
	
	@DefaultStringValue("MM/dd/yyyy HH:mm:ss")
	String timeStampFormat();
	
	@DefaultStringValue("MM/dd/yyyy hh:mmaa")
	String requestWaitListedDateFormat();

	@DefaultBooleanValue(true)
	boolean useAmPm();
	
	@DefaultStringValue("MM/dd")
	String patternDateFormat();
	
	@DefaultBooleanValue(false)
	@DoNotTranslate
	boolean isAuthenticationRequired();
	
	@DefaultBooleanValue(true)
	@DoNotTranslate
	boolean tryAuthenticationWhenGuest();
	
	@DefaultBooleanValue(false)
	@DoNotTranslate
	boolean hasAuthenticationPin();
	
	@DefaultBooleanValue(true)
	@DoNotTranslate
	boolean allowEmptySchedule();
	
	@DefaultBooleanValue(true)
	@DoNotTranslate
	boolean allowUserLogin();
	
	@DefaultBooleanValue(false)
	@DoNotTranslate
	boolean allowCalendarExport();
	
	@DefaultStringArrayValue({
		"All", "Enrolled", "Not Enrolled", "Wait-Listed"
	})
	String[] enrollmentFilterValues();
	
	@DefaultBooleanValue(false)
	@DoNotTranslate
	boolean showCourseTitle();
	
	@DefaultBooleanValue(false)
	@DoNotTranslate
	boolean courseFinderSuggestWhenEmpty();
	
	@DefaultBooleanValue(true)
	@DoNotTranslate
	boolean courseFinderShowRequired();
	
	@DefaultStringArrayValue({
		"Assigned",
		"Reserved",
		"Not Assigned",
		"Wait-Listed",
		"Critical",
		"Assigned Critical",
		"Not Assigned Critical",
		"Important",
		"Assigned Important",
		"Not Assigned Important",
		"No-Substitutes",
		"Assigned No-Subs",
		"Not Assigned No-Subs",
		"Vital",
		"Assigned Vital",
		"Not Assigned Vital",
		"LC",
		"Assigned LC",
		"Not Assigned LC",
	})
	String[] assignmentType();
	
	@DefaultStringArrayValue({
		"Consent",
		"No Consent",
		"Waiting",
		"Approved",
		"To Do",
	})
	String[] consentTypeAbbv();
	
	@DefaultStringArrayValue({
		"Any Consent Needed",
		"Consent Not Needed",
		"Consent Waiting Approval",
		"Consent Approved",
		"Waiting My Approval",
	})
	String[] consentTypeLabel();
	
	@DefaultStringArrayValue({
		"Pending",
		"Approved",
		"Rejected",
		"Cancelled",
		"Not-Requested",
		"Not-Needed",
	})
	String[] overrideType();
	
	@DefaultStringValue("None")
	String noOverride();
	
	@DefaultStringArrayValue({
		"[0-9]+",
	})
	@DoNotTranslate
	String[] freeTimeDoNotParse();
	
	@DefaultBooleanValue(true)
	@DoNotTranslate
	boolean startOverCanChangeView();

	@DefaultBooleanValue(true)
	@DoNotTranslate
	boolean listOfClassesUseLockIcon();
	
	@DefaultBooleanValue(false)
	@DoNotTranslate
	boolean checkLastResult();
	
	@DefaultStringValue("0.000")
	@DoNotTranslate
	String executionTimeFormat();
	
	@DefaultBooleanValue(true)
	@DoNotTranslate
	boolean courseRequestAutomaticallyAddFirstAlternative();
	
	@DefaultIntValue(500)
	@DoNotTranslate
	int dashboardMaxLines();
	
	@DefaultIntValue(10)
	@DoNotTranslate
	int degreePlanMaxAlternatives();
	
	@DefaultBooleanValue(true)
	@DoNotTranslate
	boolean advisorCourseRequestsShowCritical();
}