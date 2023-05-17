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
package org.unitime.timetable.form;

import java.util.ArrayList;
import java.util.Collection;

import org.hibernate.type.StringType;
import org.unitime.localization.impl.Localization;
import org.unitime.localization.messages.CourseMessages;
import org.unitime.timetable.action.RollForwardSessionAction.RollForwardErrors;
import org.unitime.timetable.action.UniTimeAction;
import org.unitime.timetable.model.Building;
import org.unitime.timetable.model.ClassInstructor;
import org.unitime.timetable.model.CourseOffering;
import org.unitime.timetable.model.DatePattern;
import org.unitime.timetable.model.Department;
import org.unitime.timetable.model.ExamType;
import org.unitime.timetable.model.LearningManagementSystemInfo;
import org.unitime.timetable.model.Location;
import org.unitime.timetable.model.OfferingCoordinator;
import org.unitime.timetable.model.PointInTimeData;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.SubjectArea;
import org.unitime.timetable.model.TimePattern;
import org.unitime.timetable.model.dao.ClassInstructorDAO;
import org.unitime.timetable.model.dao.CourseOfferingDAO;
import org.unitime.timetable.model.dao.CourseRequestDAO;
import org.unitime.timetable.model.dao.CurriculumDAO;
import org.unitime.timetable.model.dao.DepartmentalInstructorDAO;
import org.unitime.timetable.model.dao.ExamDAO;
import org.unitime.timetable.model.dao.ExamPeriodDAO;
import org.unitime.timetable.model.dao.LastLikeCourseDemandDAO;
import org.unitime.timetable.model.dao.OfferingCoordinatorDAO;
import org.unitime.timetable.model.dao.RoomFeatureDAO;
import org.unitime.timetable.model.dao.RoomGroupDAO;
import org.unitime.timetable.model.dao.StudentClassEnrollmentDAO;
import org.unitime.timetable.model.dao.TeachingRequestDAO;
import org.unitime.timetable.model.dao.TimetableManagerDAO;
import org.unitime.timetable.util.SessionRollForward;


/** 
 * @author Stephanie Schluttenhofer, Tomas Muller
 */
public class RollForwardSessionForm implements UniTimeForm {
	private static final long serialVersionUID = 7553214589949959977L;
	protected static final CourseMessages MSG = Localization.create(CourseMessages.class);
	
	private Collection<SubjectArea> subjectAreas;
	private String[] subjectAreaIds; 
	private String buttonAction;
	private Collection<Session> toSessions;
	private Collection<Session> fromSessions;
	private Collection<PointInTimeData> fromPointInTimeDataSnapshots;
	private Long sessionToRollForwardTo;
	private Boolean rollForwardDatePatterns;
	private Long sessionToRollDatePatternsForwardFrom;
	private Boolean rollForwardTimePatterns;
	private Long sessionToRollTimePatternsForwardFrom;
	private Boolean rollForwardDepartments;
	private Long sessionToRollDeptsFowardFrom;
	private Boolean rollForwardManagers;
	private Long sessionToRollManagersForwardFrom;
	private Boolean rollForwardRoomData;
	private Long sessionToRollRoomDataForwardFrom;
	private Collection<Department> departments;
	private String[] rollForwardDepartmentIds;
	private Boolean rollForwardSubjectAreas;
	private Long sessionToRollSubjectAreasForwardFrom;
	private Boolean rollForwardInstructorData;
	private Long sessionToRollInstructorDataForwardFrom;
	private Boolean rollForwardCourseOfferings;
	private Long sessionToRollCourseOfferingsForwardFrom;
	private String[] rollForwardSubjectAreaIds;
	private Boolean rollForwardClassInstructors;
	private String[] rollForwardClassInstrSubjectIds;
	private Boolean addNewCourseOfferings;
	private String[] addNewCourseOfferingsSubjectIds;
	private Boolean rollForwardExamConfiguration;
	private Long sessionToRollExamConfigurationForwardFrom;
	private Boolean rollForwardMidtermExams;
	private Boolean rollForwardFinalExams;
	private Boolean rollForwardStudents;
	private String rollForwardStudentsMode;
	private Long pointInTimeSnapshotToRollCourseEnrollmentsForwardFrom;
	private String subpartLocationPrefsAction;
	private String subpartTimePrefsAction;
	private String classPrefsAction;
	private String rollForwardDistributions;
	private String cancelledClassAction;
	private Boolean rollForwardCurricula;
	private Long sessionToRollCurriculaForwardFrom;
	private String midtermExamsPrefsAction, finalExamsPrefsAction;
	private Boolean rollForwardSessionConfig;
	private Long sessionToRollSessionConfigForwardFrom;
	private Boolean rollForwardLearningManagementSystems;
	private Long sessionToRollLearningManagementSystemsForwardFrom;
	private Boolean rollForwardWaitListsProhibitedOverrides;
	
	private Boolean rollForwardReservations;
	private Long sessionToRollReservationsForwardFrom;
	private String[] rollForwardReservationsSubjectIds;
	private Boolean rollForwardCourseReservations;
	private Boolean rollForwardCurriculumReservations;
	private Boolean rollForwardGroupReservations;
	private String expirationCourseReservations;
	private String expirationCurriculumReservations;
	private String expirationGroupReservations;
	private Boolean createStudentGroupsIfNeeded;
	private Boolean rollForwardOfferingCoordinators;
	private String[] rollForwardOfferingCoordinatorsSubjectIds;
	private Boolean rollForwardTeachingRequests;
	private String[] rollForwardTeachingRequestsSubjectIds;
	private Boolean rollForwardPeriodicTasks;
	private Long sessionToRollPeriodicTasksFrom;
	private String startDateCourseReservations;
	private String startDateCurriculumReservations;
	private String startDateGroupReservations;
	
	public RollForwardSessionForm() {
		reset();
	}
	
	@Override
	public void validate(UniTimeAction action) {
	}
	
		
	@SuppressWarnings("rawtypes")
	private void validateRollForwardSessionHasNoDataOfType(RollForwardErrors action, Session sessionToRollForwardTo, String rollForwardType, Collection checkCollection){
		if (checkCollection != null && !checkCollection.isEmpty()){
			action.addFieldError("sessionHasData", MSG.errorRollForwardNoData(rollForwardType, sessionToRollForwardTo.getLabel()));			
		}		
	}

	@SuppressWarnings("rawtypes")
	protected void validateRollForward(RollForwardErrors action, Session sessionToRollForwardTo, Long sessionIdToRollForwardFrom, String rollForwardType, Collection checkCollection){
		validateRollForwardSessionHasNoDataOfType(action, sessionToRollForwardTo, rollForwardType,  checkCollection);
		Session sessionToRollForwardFrom = Session.getSessionById(sessionIdToRollForwardFrom);
		if (sessionToRollForwardFrom == null){
			action.addFieldError("mustSelectSession", MSG.errorRollForwardMissingFromSession(rollForwardType));			
		}
		if (sessionToRollForwardFrom.equals(sessionToRollForwardTo)){
			action.addFieldError("sessionsMustBeDifferent", MSG.errorRollForwardSessionsMustBeDifferent(rollForwardType, sessionToRollForwardTo.getLabel()));
		}	
	}

	public void validateLearningManagementSystemRollForward(Session toAcadSession, RollForwardErrors action){
		if (getRollForwardLearningManagementSystems().booleanValue()){
			validateRollForward(action, toAcadSession, getSessionToRollLearningManagementSystemsForwardFrom(), MSG.rollForwardLMSInfo(), LearningManagementSystemInfo.findAll(toAcadSession.getUniqueId()));			
 		}
	}
	
	public void validateDatePatternRollForward(Session toAcadSession, RollForwardErrors action){
		if (getRollForwardDatePatterns().booleanValue()){
			validateRollForward(action, toAcadSession, getSessionToRollDatePatternsForwardFrom(), MSG.rollForwardDatePatterns(), DatePattern.findAll(toAcadSession, true, null, null));			
 		}
	}
	
	public void validateTimePatternRollForward(Session toAcadSession, RollForwardErrors action){
		if (getRollForwardTimePatterns().booleanValue()){
			validateRollForward(action, toAcadSession, getSessionToRollTimePatternsForwardFrom(), MSG.rollForwardTimePatterns(), TimePattern.findAll(toAcadSession, null));			
 		}
	}

	public void validateDepartmentRollForward(Session toAcadSession, RollForwardErrors action){
		if (getRollForwardDepartments().booleanValue()){
			validateRollForward(action, toAcadSession, getSessionToRollDeptsFowardFrom(), MSG.rollForwardDepartments(), Department.findAll(toAcadSession.getUniqueId()));			
		}
	}

	public void validateManagerRollForward(Session toAcadSession, RollForwardErrors action){
		if (getRollForwardManagers().booleanValue()){
			TimetableManagerDAO tmDao = new TimetableManagerDAO();
			validateRollForward(action, toAcadSession, getSessionToRollManagersForwardFrom(), MSG.rollForwardManagers(), tmDao.getQuery("from TimetableManager tm inner join tm.departments d where d.session.uniqueId =" + toAcadSession.getUniqueId().toString()).list());
		}
	}
	
	public void validateBuildingAndRoomRollForward(Session toAcadSession, RollForwardErrors action){
		if (getRollForwardRoomData().booleanValue()){
			validateRollForward(action, toAcadSession, getSessionToRollRoomDataForwardFrom(), MSG.rollForwardBuildings(), new ArrayList<Building>());
			validateRollForwardSessionHasNoDataOfType(action, toAcadSession, MSG.rollForwardBuildings(), Building.findAll(toAcadSession.getUniqueId()));
			validateRollForwardSessionHasNoDataOfType(action, toAcadSession, MSG.rollForwardRooms(), Location.findAll(toAcadSession.getUniqueId()));
			RoomFeatureDAO rfDao = new RoomFeatureDAO();
			validateRollForwardSessionHasNoDataOfType(action, toAcadSession, MSG.rollForwardRoomsFeatures(), rfDao.getQuery("from RoomFeature rf where rf.department.session.uniqueId = " + toAcadSession.getUniqueId().toString()).list());
			RoomGroupDAO rgDao = new RoomGroupDAO();
			validateRollForwardSessionHasNoDataOfType(action, toAcadSession, MSG.rollForwardRoomsGroups(), rgDao.getQuery("from RoomGroup rg where rg.session.uniqueId = " + toAcadSession.getUniqueId().toString() + " and rg.global = false").list());
		}		
	}

	public void validateSubjectAreaRollForward(Session toAcadSession, RollForwardErrors action){
		if (getRollForwardSubjectAreas().booleanValue()){
			validateRollForward(action, toAcadSession, getSessionToRollSubjectAreasForwardFrom(), MSG.rollForwardSubjectAreas(), SubjectArea.getSubjectAreaList(toAcadSession.getUniqueId()));			
		}		
	}
		
	public void validateInstructorDataRollForward(Session toAcadSession, RollForwardErrors action){
		if (getRollForwardInstructorData().booleanValue()){
			DepartmentalInstructorDAO diDao = new DepartmentalInstructorDAO();
			validateRollForward(action, toAcadSession, getSessionToRollInstructorDataForwardFrom(), MSG.rollForwardInstructors(), diDao.getQuery("from DepartmentalInstructor di where di.department.session.uniqueId = " + toAcadSession.getUniqueId().toString()).list());			
		}		
	}

	public void validateCourseOfferingRollForward(Session toAcadSession, RollForwardErrors action){
		if (getRollForwardCourseOfferings().booleanValue()){
			if (getSubpartLocationPrefsAction() != null 
					&& !getSubpartLocationPrefsAction().equalsIgnoreCase(SessionRollForward.DO_NOT_ROLL_ACTION)
					&& !getSubpartLocationPrefsAction().equalsIgnoreCase(SessionRollForward.ROLL_PREFS_ACTION)){
				action.addFieldError("invalidSubpartLocationAction", MSG.errorRollForwardInvalidSubpartLocationAction(getSubpartLocationPrefsAction()));			
			}
			if (getSubpartTimePrefsAction() != null 
					&& !getSubpartTimePrefsAction().equalsIgnoreCase(SessionRollForward.DO_NOT_ROLL_ACTION)
					&& !getSubpartTimePrefsAction().equalsIgnoreCase(SessionRollForward.ROLL_PREFS_ACTION)){
				action.addFieldError("invalidSubpartTimeAction", MSG.errorRollForwardInvalidSubpartTimeAction(getSubpartLocationPrefsAction()));
			}
			if (getClassPrefsAction() != null 
					&& !getClassPrefsAction().equalsIgnoreCase(SessionRollForward.DO_NOT_ROLL_ACTION)
					&& !getClassPrefsAction().equalsIgnoreCase(SessionRollForward.PUSH_UP_ACTION)
					&& !getClassPrefsAction().equalsIgnoreCase(SessionRollForward.ROLL_PREFS_ACTION)){
				action.addFieldError("invalidClassAction", MSG.errorRollForwardInvalidClassAction(getClassPrefsAction()));
			}
			if (getRollForwardDistributions() != null
					&& !getRollForwardDistributions().equalsIgnoreCase(SessionRollForward.DistributionMode.ALL.name())
					&& !getRollForwardDistributions().equalsIgnoreCase(SessionRollForward.DistributionMode.MIXED.name())
					&& !getRollForwardDistributions().equalsIgnoreCase(SessionRollForward.DistributionMode.SUBPART.name())
					&& !getRollForwardDistributions().equalsIgnoreCase(SessionRollForward.DistributionMode.NONE.name())){
				action.addFieldError("invalidDistributionAction", MSG.errorRollForwardInvalidDistributionAction(getRollForwardDistributions()));
			}
			if (getCancelledClassAction() != null
					&& !getCancelledClassAction().equalsIgnoreCase(SessionRollForward.CancelledClassAction.KEEP.name())
					&& !getCancelledClassAction().equalsIgnoreCase(SessionRollForward.CancelledClassAction.REOPEN.name())
					&& !getCancelledClassAction().equalsIgnoreCase(SessionRollForward.CancelledClassAction.SKIP.name())){
				action.addFieldError("invalidCancelAction", MSG.errorRollForwardInvalidCancelAction(getCancelledClassAction()));
			}
			validateRollForward(action, toAcadSession, getSessionToRollCourseOfferingsForwardFrom(), MSG.rollForwardCourseOfferings(), new ArrayList<CourseOffering>());
			CourseOfferingDAO coDao = new CourseOfferingDAO();
			for (int i = 0; i < getRollForwardSubjectAreaIds().length; i++){
				String queryStr = "from CourseOffering co where co.subjectArea.session.uniqueId = "
					+ toAcadSession.getUniqueId().toString()
					+ " and co.isControl = true and co.subjectArea.uniqueId  = '"
				    + getRollForwardSubjectAreaIds()[i] + "'";
				validateRollForwardSessionHasNoDataOfType(action, toAcadSession, (MSG.rollForwardCourseOfferings() + ": " + getRollForwardSubjectAreaIds()[i]), coDao.getQuery(queryStr).list());
			}			
		}
	}
	
	public void validateClassInstructorRollForward(Session toAcadSession, RollForwardErrors action){
		if (getRollForwardClassInstructors().booleanValue()){
			validateRollForward(action, toAcadSession, getSessionToRollCourseOfferingsForwardFrom(), MSG.rollForwardClassInstructors(), new ArrayList<ClassInstructor>());
			ClassInstructorDAO ciDao = new ClassInstructorDAO();
			for (int i = 0; i < getRollForwardClassInstrSubjectIds().length; i++){
				String queryStr = "from ClassInstructor c  inner join c.classInstructing.schedulingSubpart.instrOfferingConfig.instructionalOffering.courseOfferings as co where c.classInstructing.schedulingSubpart.instrOfferingConfig.instructionalOffering.session.uniqueId = "
					+ toAcadSession.getUniqueId().toString()
					+ " and co.isControl = true and co.subjectArea.uniqueId  = '"
				    + getRollForwardClassInstrSubjectIds()[i] + "'";
				validateRollForwardSessionHasNoDataOfType(action, toAcadSession, (MSG.rollForwardClassInstructors() + ": " + getRollForwardClassInstrSubjectIds()[i]), ciDao.getQuery(queryStr).list());
			}			
		}
	}
	
	public void validateOfferingCoordinatorsRollForward(Session toAcadSession, RollForwardErrors action){
		if (getRollForwardOfferingCoordinators().booleanValue()){
			validateRollForward(action, toAcadSession, getSessionToRollCourseOfferingsForwardFrom(), MSG.rollForwardOfferingCoordinators(), new ArrayList<OfferingCoordinator>());
			OfferingCoordinatorDAO ocDao = OfferingCoordinatorDAO.getInstance();
			for (int i = 0; i < getRollForwardOfferingCoordinatorsSubjectIds().length; i++){
				String queryStr = "from OfferingCoordinator c inner join c.offering.courseOfferings as co where c.offering.session.uniqueId = "
					+ toAcadSession.getUniqueId().toString()
					+ " and co.isControl = true and co.subjectArea.uniqueId  = '"
				    + getRollForwardOfferingCoordinatorsSubjectIds()[i] + "'";
				validateRollForwardSessionHasNoDataOfType(action, toAcadSession, (MSG.rollForwardOfferingCoordinators() + ": " + getRollForwardOfferingCoordinatorsSubjectIds()[i]), ocDao.getQuery(queryStr).list());
			}			
		}
	}

	
	public void validateExamConfigurationRollForward(Session toAcadSession, RollForwardErrors action){
		if (getRollForwardExamConfiguration().booleanValue()){
			ExamPeriodDAO epDao = new ExamPeriodDAO();
			validateRollForward(action, toAcadSession, getSessionToRollExamConfigurationForwardFrom(), MSG.rollForwardExamConfiguration(), epDao.getQuery("from ExamPeriod ep where ep.session.uniqueId = " + toAcadSession.getUniqueId().toString()).list());			
		}
	}

	public void validateMidtermExamRollForward(Session toAcadSession, RollForwardErrors action){
		if (getRollForwardMidtermExams().booleanValue()){
			ExamDAO eDao = new ExamDAO();
			validateRollForwardSessionHasNoDataOfType(action, toAcadSession, MSG.rollForwardMidtermExams(), eDao.getQuery("from Exam e where e.session.uniqueId = " + toAcadSession.getUniqueId().toString() +" and e.examType.type = " + ExamType.sExamTypeMidterm).list());			
		}
	}

	public void validateFinalExamRollForward(Session toAcadSession, RollForwardErrors action){
		if (getRollForwardFinalExams().booleanValue()){
			ExamDAO epDao = new ExamDAO();
			validateRollForwardSessionHasNoDataOfType(action, toAcadSession, MSG.rollForwardFinalExams(), epDao.getQuery("from Exam e where e.session.uniqueId = " + toAcadSession.getUniqueId().toString() +" and e.examType.type = " + ExamType.sExamTypeFinal).list());			
		}
	}

	public void validateLastLikeDemandRollForward(Session toAcadSession, RollForwardErrors action){
		if (getRollForwardStudents().booleanValue()) {
		    if (getRollForwardStudentsMode().equals(SessionRollForward.StudentEnrollmentMode.LAST_LIKE.name())) {
		        validateRollForwardSessionHasNoDataOfType(action, toAcadSession, MSG.rollForwardLastLikeStudentCourseRequests(), 
		                LastLikeCourseDemandDAO.getInstance().getQuery("from LastLikeCourseDemand d where d.subjectArea.session.uniqueId = " + toAcadSession.getUniqueId().toString()).list());
		    } else if (getRollForwardStudentsMode().equals(SessionRollForward.StudentEnrollmentMode.STUDENT_CLASS_ENROLLMENTS.name())) {
		        validateRollForwardSessionHasNoDataOfType(action, toAcadSession, MSG.rollForwardStudentClassEnrollments(), 
		                StudentClassEnrollmentDAO.getInstance().getQuery("from StudentClassEnrollment d where d.courseOffering.subjectArea.session.uniqueId = " + toAcadSession.getUniqueId().toString()).list());
		    } else if (getRollForwardStudentsMode().equals(SessionRollForward.StudentEnrollmentMode.STUDENT_COURSE_REQUESTS.name())) {
                validateRollForwardSessionHasNoDataOfType(action, toAcadSession, MSG.rollForwardCourseRequests(), 
                        CourseRequestDAO.getInstance().getQuery("from CourseRequest r where r.courseOffering.subjectArea.session.uniqueId = " + toAcadSession.getUniqueId().toString()).list());
		    } else if (getRollForwardStudentsMode().equals(SessionRollForward.StudentEnrollmentMode.POINT_IN_TIME_CLASS_ENROLLMENTS.name())) {
		        validateRollForwardSessionHasNoDataOfType(action, toAcadSession, MSG.rollForwardPITStudentClassEnrollments(), 
		                StudentClassEnrollmentDAO.getInstance().getQuery("from PitStudentClassEnrollment d where d.pitCourseOffering.subjectArea.session.uniqueId = " + toAcadSession.getUniqueId().toString()).list());
		    } else {
				action.addFieldError("invalidCancelAction", MSG.errorRollForwardInvalidCourseDemandAction(getRollForwardStudentsMode()));
		    }
		}
	}

	public void validateCurriculaRollForward(Session toAcadSession, RollForwardErrors action){
		if (getRollForwardCurricula().booleanValue()){
			CurriculumDAO curDao = new CurriculumDAO();
			validateRollForward(action, toAcadSession, getSessionToRollCurriculaForwardFrom(), MSG.rollForwardCurricula(), curDao.getQuery("from Curriculum c where c.department.session.uniqueId = " + toAcadSession.getUniqueId().toString()).list());			
		}
	}
	
	public void validatePeriodicTasksForward(Session toAcadSession, RollForwardErrors action){
		if (getRollForwardPeriodicTasks()){
			validateRollForward(action, toAcadSession, getSessionToRollCurriculaForwardFrom(), MSG.rollForwardScheduledTasks(), null);			
		}
	}

	public void validateSessionToRollForwardTo(RollForwardErrors action){
		Session toAcadSession = Session.getSessionById(getSessionToRollForwardTo());
		if (toAcadSession == null){
   			action.addFieldError("mustSelectSession", MSG.errorRollForwardMissingToSession());
   			return;
		}
		
		validateDepartmentRollForward(toAcadSession, action);
		validateManagerRollForward(toAcadSession, action);
		validateBuildingAndRoomRollForward(toAcadSession, action);
		validateDatePatternRollForward(toAcadSession, action);
		validateTimePatternRollForward(toAcadSession, action);
		validateLearningManagementSystemRollForward(toAcadSession, action);
		validateSubjectAreaRollForward(toAcadSession, action);
		validateCourseOfferingRollForward(toAcadSession, action);
		validateTeachingRequestsRollForward(toAcadSession, action);
		validateClassInstructorRollForward(toAcadSession, action);
		validateOfferingCoordinatorsRollForward(toAcadSession, action);
		validateExamConfigurationRollForward(toAcadSession, action);
		validateMidtermExamRollForward(toAcadSession, action);
		validateFinalExamRollForward(toAcadSession, action);
		validateLastLikeDemandRollForward(toAcadSession, action);
		validateCurriculaRollForward(toAcadSession, action);
		validatePeriodicTasksForward(toAcadSession, action);
	}
	
	@Override
	public void reset() {
		subjectAreas = new ArrayList<SubjectArea>();
		subjectAreaIds = new String[0];
		fromSessions = null;
		toSessions = null;
		sessionToRollForwardTo = null;
		rollForwardDatePatterns = Boolean.valueOf(false);
		sessionToRollDatePatternsForwardFrom = null;
		rollForwardTimePatterns = Boolean.valueOf(false);
		sessionToRollTimePatternsForwardFrom = null;
		rollForwardDepartments = Boolean.valueOf(false);
		sessionToRollDeptsFowardFrom = null;
		rollForwardManagers = Boolean.valueOf(false);
		sessionToRollManagersForwardFrom = null;
		rollForwardRoomData = Boolean.valueOf(false);
		sessionToRollRoomDataForwardFrom = null;
		setDepartments(new ArrayList<Department>());
		setRollForwardDepartmentIds(new String[0]);
		rollForwardSubjectAreas = Boolean.valueOf(false);
		sessionToRollSubjectAreasForwardFrom = null;
		rollForwardInstructorData = Boolean.valueOf(false);
		sessionToRollInstructorDataForwardFrom = null;
		rollForwardCourseOfferings = Boolean.valueOf(false);
		sessionToRollCourseOfferingsForwardFrom = null;
		rollForwardSubjectAreaIds = new String[0];
		rollForwardClassInstructors = Boolean.valueOf(false);
		rollForwardClassInstrSubjectIds = new String[0];
		addNewCourseOfferings = Boolean.valueOf(false);
		addNewCourseOfferingsSubjectIds = new String[0];
		rollForwardExamConfiguration = Boolean.valueOf(false);
		sessionToRollExamConfigurationForwardFrom = null;
		rollForwardMidtermExams = Boolean.valueOf(false);
		rollForwardFinalExams = Boolean.valueOf(false);
		rollForwardStudents = Boolean.valueOf(false);
		rollForwardStudentsMode = null;
		pointInTimeSnapshotToRollCourseEnrollmentsForwardFrom = null;
		setFromPointInTimeDataSnapshots(new ArrayList<PointInTimeData>());
		subpartLocationPrefsAction = null;
		subpartTimePrefsAction = null;
		classPrefsAction = null;
		rollForwardDistributions = null;
		cancelledClassAction = null;
		rollForwardCurricula = false;
		sessionToRollCurriculaForwardFrom = null;
		finalExamsPrefsAction = null;
		midtermExamsPrefsAction = null;
		rollForwardSessionConfig = false;
		sessionToRollSessionConfigForwardFrom = null;
		rollForwardReservations = false;
		sessionToRollReservationsForwardFrom = null;
		rollForwardReservationsSubjectIds = new String[0];
		rollForwardCurriculumReservations = false;
		rollForwardCourseReservations = false;
		rollForwardGroupReservations = false;
		expirationCourseReservations = null;
		expirationCurriculumReservations = null;
		expirationGroupReservations = null;
		createStudentGroupsIfNeeded = false;
		rollForwardTeachingRequests = false;
		rollForwardTeachingRequestsSubjectIds = new String[0];
		rollForwardOfferingCoordinators = Boolean.valueOf(false);
		rollForwardOfferingCoordinatorsSubjectIds = new String[0];
		rollForwardPeriodicTasks = false;
		sessionToRollPeriodicTasksFrom = null;
		startDateCourseReservations = null;
		startDateCurriculumReservations = null;
		startDateGroupReservations = null;
		rollForwardLearningManagementSystems = Boolean.valueOf(false);
		sessionToRollLearningManagementSystemsForwardFrom = null;
		rollForwardWaitListsProhibitedOverrides = false;
	}

	public String getButtonAction() {
		return buttonAction;
	}

	public void setButtonAction(String buttonAction) {
		this.buttonAction = buttonAction;
	}

	public String[] getSubjectAreaIds() {
		return subjectAreaIds;
	}

	public void setSubjectAreaIds(String[] subjectAreaIds) {
		this.subjectAreaIds = subjectAreaIds;
	}

	public Collection<SubjectArea> getSubjectAreas() {
		return subjectAreas;
	}

	public void setSubjectAreas(Collection<SubjectArea> subjectAreas) {
		this.subjectAreas = subjectAreas;
	}

	public Boolean getRollForwardCourseOfferings() {
		return rollForwardCourseOfferings;
	}

	public void setRollForwardCourseOfferings(Boolean rollForwardCourseOfferings) {
		this.rollForwardCourseOfferings = rollForwardCourseOfferings;
	}

	public Boolean getRollForwardDatePatterns() {
		return rollForwardDatePatterns;
	}

	public void setRollForwardDatePatterns(Boolean rollForwardDatePatterns) {
		this.rollForwardDatePatterns = rollForwardDatePatterns;
	}

	public Boolean getRollForwardDepartments() {
		return rollForwardDepartments;
	}

	public void setRollForwardDepartments(Boolean rollForwardDepartments) {
		this.rollForwardDepartments = rollForwardDepartments;
	}

	public Boolean getRollForwardInstructorData() {
		return rollForwardInstructorData;
	}

	public void setRollForwardInstructorData(Boolean rollForwardInstructorData) {
		this.rollForwardInstructorData = rollForwardInstructorData;
	}

	public Boolean getRollForwardManagers() {
		return rollForwardManagers;
	}

	public void setRollForwardManagers(Boolean rollForwardManagers) {
		this.rollForwardManagers = rollForwardManagers;
	}

	public Boolean getRollForwardRoomData() {
		return rollForwardRoomData;
	}

	public void setRollForwardRoomData(Boolean rollForwardRoomData) {
		this.rollForwardRoomData = rollForwardRoomData;
	}

	public String[] getRollForwardSubjectAreaIds() {
		return rollForwardSubjectAreaIds;
	}

	public void setRollForwardSubjectAreaIds(String[] rollForwardSubjectAreaIds) {
		this.rollForwardSubjectAreaIds = rollForwardSubjectAreaIds;
	}

	public Boolean getRollForwardSubjectAreas() {
		return rollForwardSubjectAreas;
	}

	public void setRollForwardSubjectAreas(Boolean rollForwardSubjectAreas) {
		this.rollForwardSubjectAreas = rollForwardSubjectAreas;
	}

	public Long getSessionToRollCourseOfferingsForwardFrom() {
		return sessionToRollCourseOfferingsForwardFrom;
	}

	public void setSessionToRollCourseOfferingsForwardFrom(
			Long sessionToRollCourseOfferingsForwardFrom) {
		this.sessionToRollCourseOfferingsForwardFrom = sessionToRollCourseOfferingsForwardFrom;
	}

	public Long getSessionToRollDatePatternsForwardFrom() {
		return sessionToRollDatePatternsForwardFrom;
	}

	public void setSessionToRollDatePatternsForwardFrom(
			Long sessionToRollDatePatternsForwardFrom) {
		this.sessionToRollDatePatternsForwardFrom = sessionToRollDatePatternsForwardFrom;
	}

	public Long getSessionToRollDeptsFowardFrom() {
		return sessionToRollDeptsFowardFrom;
	}

	public void setSessionToRollDeptsFowardFrom(Long sessionToRollDeptsFowardFrom) {
		this.sessionToRollDeptsFowardFrom = sessionToRollDeptsFowardFrom;
	}

	public Long getSessionToRollForwardTo() {
		return sessionToRollForwardTo;
	}

	public void setSessionToRollForwardTo(Long sessionToRollForwardTo) {
		this.sessionToRollForwardTo = sessionToRollForwardTo;
	}

	public Long getSessionToRollInstructorDataForwardFrom() {
		return sessionToRollInstructorDataForwardFrom;
	}

	public void setSessionToRollInstructorDataForwardFrom(
			Long sessionToRollInstructorDataForwardFrom) {
		this.sessionToRollInstructorDataForwardFrom = sessionToRollInstructorDataForwardFrom;
	}

	public Long getSessionToRollManagersForwardFrom() {
		return sessionToRollManagersForwardFrom;
	}

	public void setSessionToRollManagersForwardFrom(
			Long sessionToRollManagersForwardFrom) {
		this.sessionToRollManagersForwardFrom = sessionToRollManagersForwardFrom;
	}

	public Long getSessionToRollRoomDataForwardFrom() {
		return sessionToRollRoomDataForwardFrom;
	}

	public void setSessionToRollRoomDataForwardFrom(
			Long sessionToRollRoomDataForwardFrom) {
		this.sessionToRollRoomDataForwardFrom = sessionToRollRoomDataForwardFrom;
	}

	public Collection<Department> getDepartments() {
		return departments;
	}

	public void setFromPointInTimeDataSnapshots(
			Collection<PointInTimeData> fromPointInTimeDataSnapshots) {
		this.fromPointInTimeDataSnapshots = fromPointInTimeDataSnapshots;
	}

	public Collection<PointInTimeData> getFromPointInTimeDataSnapshots() {
		return fromPointInTimeDataSnapshots;
	}

	public void setDepartments(
			Collection<Department> departments) {
		this.departments = departments;
	}

	public String[] getRollForwardDepartmentIds() {
		return rollForwardDepartmentIds;
	}

	public void setRollForwardDepartmentIds(String[] rollForwardDepartmentIds) {
		this.rollForwardDepartmentIds = rollForwardDepartmentIds;
	}

	public Long getSessionToRollSubjectAreasForwardFrom() {
		return sessionToRollSubjectAreasForwardFrom;
	}

	public void setSessionToRollSubjectAreasForwardFrom(
			Long sessionToRollSubjectAreasForwardFrom) {
		this.sessionToRollSubjectAreasForwardFrom = sessionToRollSubjectAreasForwardFrom;
	}

	public Collection<Session> getFromSessions() {
		return fromSessions;
	}

	public void setFromSessions(Collection<Session> fromSessions) {
		this.fromSessions = fromSessions;
	}


	public Boolean getRollForwardTimePatterns() {
		return rollForwardTimePatterns;
	}


	public void setRollForwardTimePatterns(Boolean rollForwardTimePatterns) {
		this.rollForwardTimePatterns = rollForwardTimePatterns;
	}


	public Long getSessionToRollTimePatternsForwardFrom() {
		return sessionToRollTimePatternsForwardFrom;
	}


	public void setSessionToRollTimePatternsForwardFrom(
			Long sessionToRollTimePatternsForwardFrom) {
		this.sessionToRollTimePatternsForwardFrom = sessionToRollTimePatternsForwardFrom;
	}


	public Boolean getRollForwardClassInstructors() {
		return rollForwardClassInstructors;
	}


	public void setRollForwardClassInstructors(Boolean rollForwardClassInstructors) {
		this.rollForwardClassInstructors = rollForwardClassInstructors;
	}


	public String[] getRollForwardClassInstrSubjectIds() {
		return rollForwardClassInstrSubjectIds;
	}


	public void setRollForwardClassInstrSubjectIds(
			String[] rollForwardClassInstrSubjectIds) {
		this.rollForwardClassInstrSubjectIds = rollForwardClassInstrSubjectIds;
	}
	
	public Boolean getRollForwardOfferingCoordinators() {
		return rollForwardOfferingCoordinators;
	}


	public void setRollForwardOfferingCoordinators(Boolean rollForwardOfferingCoordinators) {
		this.rollForwardOfferingCoordinators = rollForwardOfferingCoordinators;
	}


	public String[] getRollForwardOfferingCoordinatorsSubjectIds() {
		return rollForwardOfferingCoordinatorsSubjectIds;
	}


	public void setRollForwardOfferingCoordinatorsSubjectIds(
			String[] rollForwardOfferingCoordinatorsSubjectIds) {
		this.rollForwardOfferingCoordinatorsSubjectIds = rollForwardOfferingCoordinatorsSubjectIds;
	}


	public Collection<Session> getToSessions() {
		return toSessions;
	}


	public void setToSessions(Collection<Session> toSessions) {
		this.toSessions = toSessions;
	}


	public Boolean getAddNewCourseOfferings() {
		return addNewCourseOfferings;
	}


	public void setAddNewCourseOfferings(Boolean addNewCourseOfferings) {
		this.addNewCourseOfferings = addNewCourseOfferings;
	}


	public String[] getAddNewCourseOfferingsSubjectIds() {
		return addNewCourseOfferingsSubjectIds;
	}


	public void setAddNewCourseOfferingsSubjectIds(
			String[] addNewCourseOfferingsSubjectIds) {
		this.addNewCourseOfferingsSubjectIds = addNewCourseOfferingsSubjectIds;
	}


	public Boolean getRollForwardExamConfiguration() {
		return rollForwardExamConfiguration;
	}


	public void setRollForwardExamConfiguration(Boolean rollForwardExamConfiguration) {
		this.rollForwardExamConfiguration = rollForwardExamConfiguration;
	}


	public Boolean getRollForwardMidtermExams() {
		return rollForwardMidtermExams;
	}


	public void setRollForwardMidtermExams(Boolean rollForwardMidtermExams) {
		this.rollForwardMidtermExams = rollForwardMidtermExams;
	}


	public Boolean getRollForwardFinalExams() {
		return rollForwardFinalExams;
	}


	public void setRollForwardFinalExams(Boolean rollForwardFinalExams) {
		this.rollForwardFinalExams = rollForwardFinalExams;
	}


	public Long getSessionToRollExamConfigurationForwardFrom() {
		return sessionToRollExamConfigurationForwardFrom;
	}


	public void setSessionToRollExamConfigurationForwardFrom(
			Long sessionToRollExamConfigurationForwardFrom) {
		this.sessionToRollExamConfigurationForwardFrom = sessionToRollExamConfigurationForwardFrom;
	}
	
	public Boolean getRollForwardStudents() {
	    return rollForwardStudents;
	}
	
	public void setRollForwardStudents(Boolean rollForwardStudents) {
	    this.rollForwardStudents = rollForwardStudents;
	}
	
    public String getRollForwardStudentsMode() {
        return rollForwardStudentsMode;
    }
    
    public void setRollForwardStudentsMode(String rollForwardStudentsMode) {
        this.rollForwardStudentsMode = rollForwardStudentsMode;
    }
    
    public Long getPointInTimeSnapshotToRollCourseEnrollmentsForwardFrom() {
    	return(this.pointInTimeSnapshotToRollCourseEnrollmentsForwardFrom);
    }
    
    public void setPointInTimeSnapshotToRollCourseEnrollmentsForwardFrom(Long pointInTimeSnapshotToRollCourseEnrollmentsForwardFrom) {
    	this.pointInTimeSnapshotToRollCourseEnrollmentsForwardFrom = pointInTimeSnapshotToRollCourseEnrollmentsForwardFrom;
    }
    
    public Boolean getRollForwardCurricula() {
    	return rollForwardCurricula;
    }
    
    public void setRollForwardCurricula(Boolean rollForwardCurricula) {
    	this.rollForwardCurricula = rollForwardCurricula;
    }
    
    public Long getSessionToRollCurriculaForwardFrom() {
    	return sessionToRollCurriculaForwardFrom;
    }
    
    public void setSessionToRollCurriculaForwardFrom(Long sessionToRollCurriculaForwardFrom) {
    	this.sessionToRollCurriculaForwardFrom = sessionToRollCurriculaForwardFrom;
    }
    
    public Boolean getRollForwardSessionConfig() {
    	return rollForwardSessionConfig;
    }
    
    public void setRollForwardSessionConfig(Boolean rollForwardSessionConfig) {
    	this.rollForwardSessionConfig = rollForwardSessionConfig;
    }
    
    public Long getSessionToRollSessionConfigForwardFrom() {
    	return sessionToRollSessionConfigForwardFrom;
    }
    
    public void setSessionToRollSessionConfigForwardFrom(Long sessionToRollSessionConfigForwardFrom) {
    	this.sessionToRollSessionConfigForwardFrom = sessionToRollSessionConfigForwardFrom;
    }


	/**
	 * @return the subpartLocationPrefsAction
	 */
	public String getSubpartLocationPrefsAction() {
		return subpartLocationPrefsAction;
	}


	/**
	 * @param subpartLocationPrefsAction the subpartLocationPrefsAction to set
	 */
	public void setSubpartLocationPrefsAction(String subpartLocationPrefsAction) {
		this.subpartLocationPrefsAction = subpartLocationPrefsAction;
	}


	/**
	 * @return the subpartTimePrefsAction
	 */
	public String getSubpartTimePrefsAction() {
		return subpartTimePrefsAction;
	}


	/**
	 * @param subpartTimePrefsAction the subpartTimePrefsAction to set
	 */
	public void setSubpartTimePrefsAction(String subpartTimePrefsAction) {
		this.subpartTimePrefsAction = subpartTimePrefsAction;
	}


	/**
	 * @return the classPrefsAction
	 */
	public String getClassPrefsAction() {
		return classPrefsAction;
	}


	/**
	 * @param classPrefsAction the classPrefsAction to set
	 */
	public void setClassPrefsAction(String classPrefsAction) {
		this.classPrefsAction = classPrefsAction;
	}
	
	public String getRollForwardDistributions() { return rollForwardDistributions; }
	public void setRollForwardDistributions(String rollForwardDistributions) { this.rollForwardDistributions = rollForwardDistributions; }

	public String getCancelledClassAction() { return cancelledClassAction; }
	public void setCancelledClassAction(String cancelledClassAction) { this.cancelledClassAction = cancelledClassAction; }

	public String getMidtermExamsPrefsAction() { return midtermExamsPrefsAction; }
	public void setMidtermExamsPrefsAction(String midtermExamsPrefsAction) { this.midtermExamsPrefsAction = midtermExamsPrefsAction; }

	public String getFinalExamsPrefsAction() { return finalExamsPrefsAction; }
	public void setFinalExamsPrefsAction(String finalExamsPrefsAction) { this.finalExamsPrefsAction = finalExamsPrefsAction; }
	
	public boolean getRollForwardReservations() { return rollForwardReservations; }
	public void setRollForwardReservations(boolean rollForwardReservations) { this.rollForwardReservations = rollForwardReservations; }
	
	public Long getSessionToRollReservationsForwardFrom() { return sessionToRollReservationsForwardFrom; }
	public void setSessionToRollReservationsForwardFrom(Long sessionToRollReservationsForwardFrom) { this.sessionToRollReservationsForwardFrom = sessionToRollReservationsForwardFrom; }
	
	public String[] getRollForwardReservationsSubjectIds() { return rollForwardReservationsSubjectIds; }
	public void setRollForwardReservationsSubjectIds(String[] rollForwardReservationsSubjectIds) { this.rollForwardReservationsSubjectIds = rollForwardReservationsSubjectIds; }

	public boolean getRollForwardCourseReservations() { return rollForwardCourseReservations; }
	public void setRollForwardCourseReservations(boolean rollForwardCourseReservations) { this.rollForwardCourseReservations = rollForwardCourseReservations; }
	
	public boolean getRollForwardCurriculumReservations() { return rollForwardCurriculumReservations; }
	public void setRollForwardCurriculumReservations(boolean rollForwardCurriculumReservations) { this.rollForwardCurriculumReservations = rollForwardCurriculumReservations; }
	
	public boolean getRollForwardGroupReservations() { return rollForwardGroupReservations; }
	public void setRollForwardGroupReservations(boolean rollForwardGroupReservations) { this.rollForwardGroupReservations = rollForwardGroupReservations; }
	
	public String getExpirationCourseReservations() { return expirationCourseReservations; }
	public void setExpirationCourseReservations(String expirationCourseReservations) { this.expirationCourseReservations = expirationCourseReservations; }
	
	public String getExpirationCurriculumReservations() { return expirationCurriculumReservations; }
	public void setExpirationCurriculumReservations(String expirationCurriculumReservations) { this.expirationCurriculumReservations = expirationCurriculumReservations; }
	
	public String getExpirationGroupReservations() { return expirationGroupReservations; }
	public void setExpirationGroupReservations(String expirationGroupReservations) { this.expirationGroupReservations = expirationGroupReservations; }
	
	public boolean getCreateStudentGroupsIfNeeded() { return createStudentGroupsIfNeeded; }
	public void setCreateStudentGroupsIfNeeded(boolean createStudentGroupsIfNeeded) { this.createStudentGroupsIfNeeded = createStudentGroupsIfNeeded; }
	
	public String getStartDateCourseReservations() { return startDateCourseReservations; }
	public void setStartDateCourseReservations(String startDateCourseReservations) { this.startDateCourseReservations = startDateCourseReservations; }
	
	public String getStartDateCurriculumReservations() { return startDateCurriculumReservations; }
	public void setStartDateCurriculumReservations(String startDateCurriculumReservations) { this.startDateCurriculumReservations = startDateCurriculumReservations; }
	
	public String getStartDateGroupReservations() { return startDateGroupReservations; }
	public void setStartDateGroupReservations(String startDateGroupReservations) { this.startDateGroupReservations = startDateGroupReservations; }


	public void copyTo(RollForwardSessionForm form) {
		// form.subjectAreas = subjectAreas;
		form.subjectAreaIds = subjectAreaIds;
		form.buttonAction = buttonAction;
		// form.toSessions = toSessions;
		// form.fromSessions = fromSessions;
		form.sessionToRollForwardTo = sessionToRollForwardTo;
		form.rollForwardDatePatterns = rollForwardDatePatterns;
		form.sessionToRollDatePatternsForwardFrom = sessionToRollDatePatternsForwardFrom;
		form.rollForwardTimePatterns = rollForwardTimePatterns;
		form.sessionToRollTimePatternsForwardFrom = sessionToRollTimePatternsForwardFrom;
		form.rollForwardDepartments = rollForwardDepartments;
		form.sessionToRollDeptsFowardFrom = sessionToRollDeptsFowardFrom;
		form.rollForwardManagers = rollForwardManagers;
		form.sessionToRollManagersForwardFrom = sessionToRollManagersForwardFrom;
		form.rollForwardRoomData = rollForwardRoomData;
		// form.departments = departments;
		form.rollForwardDepartmentIds = rollForwardDepartmentIds;
		form.sessionToRollRoomDataForwardFrom = sessionToRollRoomDataForwardFrom;
		form.rollForwardSubjectAreas = rollForwardSubjectAreas;
		form.sessionToRollSubjectAreasForwardFrom = sessionToRollSubjectAreasForwardFrom;
		form.rollForwardInstructorData = rollForwardInstructorData;
		form.sessionToRollInstructorDataForwardFrom = sessionToRollInstructorDataForwardFrom;
		form.rollForwardCourseOfferings = rollForwardCourseOfferings;
		form.sessionToRollCourseOfferingsForwardFrom = sessionToRollCourseOfferingsForwardFrom;
		form.rollForwardSubjectAreaIds = rollForwardSubjectAreaIds;
		form.rollForwardClassInstructors = rollForwardClassInstructors;
		form.rollForwardClassInstrSubjectIds = rollForwardClassInstrSubjectIds;
		form.addNewCourseOfferings = addNewCourseOfferings;
		form.addNewCourseOfferingsSubjectIds = addNewCourseOfferingsSubjectIds;
		form.rollForwardExamConfiguration = rollForwardExamConfiguration;
		form.sessionToRollExamConfigurationForwardFrom = sessionToRollExamConfigurationForwardFrom;
		form.rollForwardMidtermExams = rollForwardMidtermExams;
		form.rollForwardFinalExams = rollForwardFinalExams;
		form.rollForwardStudents = rollForwardStudents;
		form.rollForwardStudentsMode = rollForwardStudentsMode;
		form.pointInTimeSnapshotToRollCourseEnrollmentsForwardFrom = pointInTimeSnapshotToRollCourseEnrollmentsForwardFrom;
		// form.fromPointInTimeDataSnapshots = fromPointInTimeDataSnapshots;
		form.subpartLocationPrefsAction = subpartLocationPrefsAction;
		form.subpartTimePrefsAction = subpartTimePrefsAction;
		form.classPrefsAction = classPrefsAction;
		form.cancelledClassAction = cancelledClassAction;
		form.rollForwardCurricula = rollForwardCurricula;
		form.sessionToRollCurriculaForwardFrom = sessionToRollCurriculaForwardFrom;
		form.midtermExamsPrefsAction = midtermExamsPrefsAction;
		form.finalExamsPrefsAction = finalExamsPrefsAction;
		form.rollForwardSessionConfig = rollForwardSessionConfig;
		form.sessionToRollSessionConfigForwardFrom = sessionToRollSessionConfigForwardFrom;
		form.rollForwardReservations = rollForwardReservations;
		form.sessionToRollReservationsForwardFrom = sessionToRollReservationsForwardFrom;
		form.rollForwardReservationsSubjectIds = rollForwardReservationsSubjectIds;
		form.rollForwardCurriculumReservations = rollForwardCurriculumReservations;
		form.rollForwardCourseReservations = rollForwardCourseReservations;
		form.rollForwardGroupReservations = rollForwardGroupReservations;
		form.expirationCourseReservations = expirationCourseReservations;
		form.expirationCurriculumReservations = expirationCurriculumReservations;
		form.expirationGroupReservations = expirationGroupReservations;
		form.createStudentGroupsIfNeeded = createStudentGroupsIfNeeded;
		form.rollForwardOfferingCoordinators = rollForwardOfferingCoordinators;
		form.rollForwardOfferingCoordinatorsSubjectIds = rollForwardOfferingCoordinatorsSubjectIds; 
		form.rollForwardTeachingRequests = rollForwardTeachingRequests;
		form.rollForwardTeachingRequestsSubjectIds = rollForwardTeachingRequestsSubjectIds;
		form.rollForwardDistributions = rollForwardDistributions;
		form.rollForwardPeriodicTasks = rollForwardPeriodicTasks;
		form.sessionToRollPeriodicTasksFrom = sessionToRollPeriodicTasksFrom;
		form.startDateCourseReservations = startDateCourseReservations;
		form.startDateCurriculumReservations = startDateCurriculumReservations;
		form.startDateGroupReservations = startDateGroupReservations;
		form.rollForwardLearningManagementSystems = rollForwardLearningManagementSystems;
		form.sessionToRollLearningManagementSystemsForwardFrom = sessionToRollLearningManagementSystemsForwardFrom;
		form.rollForwardWaitListsProhibitedOverrides = rollForwardWaitListsProhibitedOverrides;
	}
	
	public Boolean getRollForwardTeachingRequests() {
		return rollForwardTeachingRequests;
	}

	public void setRollForwardTeachingRequests(Boolean rollForwardTeachingRequests) {
		this.rollForwardTeachingRequests = rollForwardTeachingRequests;
	}
	
	public String[] getRollForwardTeachingRequestsSubjectIds() {
		return rollForwardTeachingRequestsSubjectIds;
	}

	public void setRollForwardTeachingRequestsSubjectIds(String[] rollForwardTeachingRequestsSubjectIds) {
		this.rollForwardTeachingRequestsSubjectIds = rollForwardTeachingRequestsSubjectIds;
	}
	
	public void validateTeachingRequestsRollForward(Session toAcadSession, RollForwardErrors action){
		if (getRollForwardTeachingRequests().booleanValue()) {
			if (getRollForwardOfferingCoordinatorsSubjectIds() == null || getRollForwardOfferingCoordinatorsSubjectIds().length == 0) {
				action.addFieldError("mustSelectDepartment", MSG.errorRollForwardGeneric(MSG.rollForwardTeachingRequests(), MSG.infoNoSubjectAreaSelected()));
			} else {
				validateRollForward(action, toAcadSession, getSessionToRollInstructorDataForwardFrom(), MSG.rollForwardTeachingRequests(),
						TeachingRequestDAO.getInstance().getQuery("select tr from TeachingRequest tr inner join tr.offering.courseOfferings co where co.isControl = true and cast(co.subjectArea.uniqueId as string) in :subjectIds")
					.setParameterList("subjectIds", getRollForwardOfferingCoordinatorsSubjectIds(), new StringType()).list());
			}
		}
	}
	
	public Boolean getRollForwardPeriodicTasks() { return rollForwardPeriodicTasks; }
	public void setRollForwardPeriodicTasks(Boolean rollForwardPeriodicTasks) { this.rollForwardPeriodicTasks = rollForwardPeriodicTasks; }
	
	public Long getSessionToRollPeriodicTasksFrom() { return sessionToRollPeriodicTasksFrom; }
	public void setSessionToRollPeriodicTasksFrom(Long sessionToRollPeriodicTasksFrom) { this.sessionToRollPeriodicTasksFrom = sessionToRollPeriodicTasksFrom; }
	
	public Boolean getRollForwardLearningManagementSystems() {
		return rollForwardLearningManagementSystems;
	}


	public void setRollForwardLearningManagementSystems(Boolean rollForwardLearningManagementSystems) {
		this.rollForwardLearningManagementSystems = rollForwardLearningManagementSystems;
	}


	public Long getSessionToRollLearningManagementSystemsForwardFrom() {
		return sessionToRollLearningManagementSystemsForwardFrom;
	}


	public void setSessionToRollLearningManagementSystemsForwardFrom(
			Long sessionToRollLearningManagementSystemsForwardFrom) {
		this.sessionToRollLearningManagementSystemsForwardFrom = sessionToRollLearningManagementSystemsForwardFrom;
	}
	
	public Boolean getRollForwardWaitListsProhibitedOverrides() { return rollForwardWaitListsProhibitedOverrides; }
	public void setRollForwardWaitListsProhibitedOverrides(Boolean rollForwardWaitListsProhibitedOverrides) { this.rollForwardWaitListsProhibitedOverrides = rollForwardWaitListsProhibitedOverrides; }


	public Object clone() {
		RollForwardSessionForm form = new RollForwardSessionForm();
		copyTo(form);
		return form;
	}
	
	public int getDepartmentsListSize() {
		return Math.min(7,getDepartments().size());
	}
	
	public int getSubjectAreasListSize() {
		return Math.min(7,getSubjectAreas().size());
	}
}
