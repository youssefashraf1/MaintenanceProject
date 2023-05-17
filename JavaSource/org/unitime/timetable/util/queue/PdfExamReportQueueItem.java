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
package org.unitime.timetable.util.queue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;

import org.unitime.commons.Email;
import org.unitime.localization.impl.Localization;
import org.unitime.localization.messages.ExaminationMessages;
import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.defaults.ApplicationProperty;
import org.unitime.timetable.form.ExamPdfReportForm;
import org.unitime.timetable.form.ExamPdfReportForm.RegisteredReport;
import org.unitime.timetable.model.DepartmentalInstructor;
import org.unitime.timetable.model.ExamOwner;
import org.unitime.timetable.model.ExamType;
import org.unitime.timetable.model.ManagerRole;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.Student;
import org.unitime.timetable.model.SubjectArea;
import org.unitime.timetable.model.TimetableManager;
import org.unitime.timetable.model.dao.ExamDAO;
import org.unitime.timetable.model.dao.ExamTypeDAO;
import org.unitime.timetable.model.dao.SessionDAO;
import org.unitime.timetable.model.dao.SubjectAreaDAO;
import org.unitime.timetable.reports.exam.InstructorExamReport;
import org.unitime.timetable.reports.exam.PdfLegacyExamReport;
import org.unitime.timetable.reports.exam.StudentExamReport;
import org.unitime.timetable.security.UserContext;
import org.unitime.timetable.security.rights.Right;
import org.unitime.timetable.solver.exam.ExamSolverProxy;
import org.unitime.timetable.solver.exam.ui.ExamAssignmentInfo;
import org.unitime.timetable.solver.exam.ui.ExamInfo.ExamInstructorInfo;
import org.unitime.timetable.util.Constants;
import org.unitime.timetable.util.Formats;

/**
 * 
 * @author Tomas Muller
 *
 */
public class PdfExamReportQueueItem extends QueueItem {
	private static final long serialVersionUID = 1L;
	protected static final ExaminationMessages MSG = Localization.create(ExaminationMessages.class);

	public static String TYPE = "PDF Exam Report";

	private ExamPdfReportForm iForm;
	private String iUrl = null;
	private transient ExamSolverProxy iExamSolver;
	private String iName = null;
	private double iProgress = 0;
	private boolean iSubjectIndependent = false;
	
	public PdfExamReportQueueItem(Session session, UserContext owner, ExamPdfReportForm form, HttpServletRequest request, ExamSolverProxy examSolver) {
		super(session, owner);
		iForm = form;
		iUrl = request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+request.getContextPath();
		iExamSolver = examSolver;
		iName = ExamTypeDAO.getInstance().get(iForm.getExamType()).getLabel() + " ";
        for (int i=0;i<iForm.getReports().length;i++) {
        	if (i > 0) iName += ", ";
        	iName += iForm.getReportName(iForm.getReports()[i]);
        }
        if (!iForm.getAll()) {
        	iName += " (";
            for (int i=0;i<iForm.getSubjects().length;i++) {
                SubjectArea subject = new SubjectAreaDAO().get(Long.valueOf(iForm.getSubjects()[i]));
                if (i > 0) iName += ", ";
                iName += subject.getSubjectAreaAbbreviation();
            }
            iName += ")";
        }
        iSubjectIndependent = (owner == null || owner.getCurrentAuthority() == null ? false : owner.getCurrentAuthority().hasRight(Right.DepartmentIndependent));
        iForm.setSubjectAreas(SubjectArea.getUserSubjectAreas(owner, false));
	}

	@Override
	public void execute() {
		org.hibernate.Session hibSession = ExamDAO.getInstance().getSession();
		createReports(hibSession);
		if (hibSession.isOpen()) hibSession.close();
	}
	
	private void createReports(org.hibernate.Session hibSession) {
        try {
        	iProgress = 0;
            setStatus(MSG.statusLoadingExams());
            TreeSet<ExamAssignmentInfo> exams = null;
            if (iExamSolver!=null && iExamSolver.getExamTypeId().equals(iForm.getExamType()) && ApplicationProperty.ExaminationPdfReportsCanUseSolution.isTrue()) {
                    exams = new TreeSet(iExamSolver.getAssignedExams());
                    if (iForm.getIgnoreEmptyExams()) for (Iterator<ExamAssignmentInfo> i=exams.iterator();i.hasNext();) {
                        if (i.next().getStudentIds().isEmpty()) i.remove();
                    }
                    if (ApplicationProperty.ExaminationPdfReportsPreloadCrosslistedExams.isTrue()) {
                		setStatus("  " + MSG.statusFetchingExams());
                		hibSession.createQuery(
                                "select o from Exam x inner join x.owners o where x.session.uniqueId=:sessionId and x.examType.uniqueId=:examTypeId"
                                ).setLong("sessionId", iExamSolver.getSessionId()).setLong("examTypeId", iExamSolver.getExamTypeId()).setCacheable(true).list();
                		setStatus("  " + MSG.statusFetchingRelatedClasses());
                        hibSession.createQuery(
                                "select c from Class_ c, ExamOwner o where o.exam.session.uniqueId=:sessionId and o.exam.examType.uniqueId=:examTypeId and o.ownerType=:classType and c.uniqueId=o.ownerId")
                                .setLong("sessionId", iExamSolver.getSessionId())
                                .setLong("examTypeId", iExamSolver.getExamTypeId())
                                .setInteger("classType", ExamOwner.sOwnerTypeClass).setCacheable(true).list();
                        setStatus("  " + MSG.statusFetchingRelatedConfigs());
                        hibSession.createQuery(
                                "select c from InstrOfferingConfig c, ExamOwner o where o.exam.session.uniqueId=:sessionId and o.exam.examType.uniqueId=:examTypeId and o.ownerType=:configType and c.uniqueId=o.ownerId")
                                .setLong("sessionId", iExamSolver.getSessionId())
                                .setLong("examTypeId", iExamSolver.getExamTypeId())
                                .setInteger("configType", ExamOwner.sOwnerTypeConfig).setCacheable(true).list();
                        setStatus("  " + MSG.statusFetchingRelatedCourses());
                        hibSession.createQuery(
                                "select c from CourseOffering c, ExamOwner o where o.exam.session.uniqueId=:sessionId and o.exam.examType.uniqueId=:examTypeId and o.ownerType=:courseType and c.uniqueId=o.ownerId")
                                .setLong("sessionId", iExamSolver.getSessionId())
                                .setLong("examTypeId", iExamSolver.getExamTypeId())
                                .setInteger("courseType", ExamOwner.sOwnerTypeCourse).setCacheable(true).list();
                        setStatus("  " + MSG.statusFetchingRelatedOfferings());
                        hibSession.createQuery(
                                "select c from InstructionalOffering c, ExamOwner o where o.exam.session.uniqueId=:sessionId and o.exam.examType.uniqueId=:examTypeId and o.ownerType=:offeringType and c.uniqueId=o.ownerId")
                                .setLong("sessionId", iExamSolver.getSessionId())
                                .setLong("examTypeId", iExamSolver.getExamTypeId())
                                .setInteger("offeringType", ExamOwner.sOwnerTypeOffering).setCacheable(true).list();
                        Hashtable<Long,Hashtable<Long,Set<Long>>> owner2course2students = new Hashtable();
                        setStatus("  " + MSG.statusLoadingStudentsFromClasses());
                        for (Iterator i=
                            hibSession.createQuery(
                            "select o.uniqueId, e.student.uniqueId, e.courseOffering.uniqueId from "+
                            "Exam x inner join x.owners o, "+
                            "StudentClassEnrollment e inner join e.clazz c "+
                            "where x.session.uniqueId=:sessionId and x.examType.uniqueId=:examTypeId and "+
                            "o.ownerType="+org.unitime.timetable.model.ExamOwner.sOwnerTypeClass+" and "+
                            "o.ownerId=c.uniqueId").setLong("sessionId", iExamSolver.getSessionId()).setLong("examTypeId", iExamSolver.getExamTypeId()).setCacheable(true).list().iterator();i.hasNext();) {
                                Object[] o = (Object[])i.next();
                                Long ownerId = (Long)o[0];
                                Long studentId = (Long)o[1];
                                Long courseId = (Long)o[2];
                                Hashtable<Long, Set<Long>> course2students = owner2course2students.get(ownerId);
                                if (course2students == null) {
                                	course2students = new Hashtable<Long, Set<Long>>();
                                	owner2course2students.put(ownerId, course2students);
                                }
                                Set<Long> studentsOfCourse = course2students.get(courseId);
                                if (studentsOfCourse == null) {
                                	studentsOfCourse = new HashSet<Long>();
                                	course2students.put(courseId, studentsOfCourse);
                                }
                                studentsOfCourse.add(studentId);
                        }
                        setStatus("  " + MSG.statusLoadingStudentsFromConfigs());
                        for (Iterator i=
                            hibSession.createQuery(
                                    "select o.uniqueId, e.student.uniqueId, e.courseOffering.uniqueId from "+
                                    "Exam x inner join x.owners o, "+
                                    "StudentClassEnrollment e inner join e.clazz c " +
                                    "inner join c.schedulingSubpart.instrOfferingConfig ioc " +
                                    "where x.session.uniqueId=:sessionId and x.examType.uniqueId=:examTypeId and "+
                                    "o.ownerType="+org.unitime.timetable.model.ExamOwner.sOwnerTypeConfig+" and "+
                                    "o.ownerId=ioc.uniqueId").setLong("sessionId", iExamSolver.getSessionId()).setLong("examTypeId", iExamSolver.getExamTypeId()).setCacheable(true).list().iterator();i.hasNext();) {
                            Object[] o = (Object[])i.next();
                            Long ownerId = (Long)o[0];
                            Long studentId = (Long)o[1];
                            Long courseId = (Long)o[2];
                            Hashtable<Long, Set<Long>> course2students = owner2course2students.get(ownerId);
                            if (course2students == null) {
                            	course2students = new Hashtable<Long, Set<Long>>();
                            	owner2course2students.put(ownerId, course2students);
                            }
                            Set<Long> studentsOfCourse = course2students.get(courseId);
                            if (studentsOfCourse == null) {
                            	studentsOfCourse = new HashSet<Long>();
                            	course2students.put(courseId, studentsOfCourse);
                            }
                            studentsOfCourse.add(studentId);
                        }
                        setStatus("  " + MSG.statusLoadingStudentsFromCourses());
                        for (Iterator i=
                            hibSession.createQuery(
                                    "select o.uniqueId, e.student.uniqueId, e.courseOffering.uniqueId from "+
                                    "Exam x inner join x.owners o, "+
                                    "StudentClassEnrollment e inner join e.courseOffering co " +
                                    "where x.session.uniqueId=:sessionId and x.examType.uniqueId=:examTypeId and "+
                                    "o.ownerType="+org.unitime.timetable.model.ExamOwner.sOwnerTypeCourse+" and "+
                                    "o.ownerId=co.uniqueId").setLong("sessionId", iExamSolver.getSessionId()).setLong("examTypeId", iExamSolver.getExamTypeId()).setCacheable(true).list().iterator();i.hasNext();) {
                            Object[] o = (Object[])i.next();
                            Long ownerId = (Long)o[0];
                            Long studentId = (Long)o[1];
                            Long courseId = (Long)o[2];
                            Hashtable<Long, Set<Long>> course2students = owner2course2students.get(ownerId);
                            if (course2students == null) {
                            	course2students = new Hashtable<Long, Set<Long>>();
                            	owner2course2students.put(ownerId, course2students);
                            }
                            Set<Long> studentsOfCourse = course2students.get(courseId);
                            if (studentsOfCourse == null) {
                            	studentsOfCourse = new HashSet<Long>();
                            	course2students.put(courseId, studentsOfCourse);
                            }
                            studentsOfCourse.add(studentId);
                        }
                        setStatus("  " + MSG.statusLoadingStudentsFromOfferings());
                        for (Iterator i=
                            hibSession.createQuery(
                                    "select o.uniqueId, e.student.uniqueId, e.courseOffering.uniqueId from "+
                                    "Exam x inner join x.owners o, "+
                                    "StudentClassEnrollment e inner join e.courseOffering.instructionalOffering io " +
                                    "where x.session.uniqueId=:sessionId and x.examType.uniqueId=:examTypeId and "+
                                    "o.ownerType="+org.unitime.timetable.model.ExamOwner.sOwnerTypeOffering+" and "+
                                    "o.ownerId=io.uniqueId").setLong("sessionId", iExamSolver.getSessionId()).setLong("examTypeId", iExamSolver.getExamTypeId()).setCacheable(true).list().iterator();i.hasNext();) {
                            Object[] o = (Object[])i.next();
                            Long ownerId = (Long)o[0];
                            Long studentId = (Long)o[1];
                            Long courseId = (Long)o[2];
                            Hashtable<Long, Set<Long>> course2students = owner2course2students.get(ownerId);
                            if (course2students == null) {
                            	course2students = new Hashtable<Long, Set<Long>>();
                            	owner2course2students.put(ownerId, course2students);
                            }
                            Set<Long> studentsOfCourse = course2students.get(courseId);
                            if (studentsOfCourse == null) {
                            	studentsOfCourse = new HashSet<Long>();
                            	course2students.put(courseId, studentsOfCourse);
                            }
                            studentsOfCourse.add(studentId);
                        }
                        for (ExamAssignmentInfo exam: exams) {
                        	exam.createSectionsIncludeCrosslistedDummies(owner2course2students);
                        }
                    }
            } else {
                    exams = PdfLegacyExamReport.loadExams(getSessionId(), iForm.getExamType(), true, iForm.getIgnoreEmptyExams(), true);
            }
        	iProgress = 0.1;
            /*
            if (iForm.getAll()) {
                for (Iterator i=Exam.findAll(session.getUniqueId(), iForm.getExamType()).iterator();i.hasNext();) {
                    exams.add(new ExamAssignmentInfo((Exam)i.next()));
                }
            } else {
                for (int i=0;i<iForm.getSubjects().length;i++) {
                    SubjectArea subject = new SubjectAreaDAO().get(Long.valueOf(iForm.getSubjects()[i]));
                    TreeSet<ExamAssignmentInfo> examsThisSubject = new TreeSet();
                    for (Iterator j=Exam.findExamsOfSubjectArea(subject.getUniqueId(), iForm.getExamType()).iterator();j.hasNext();) {
                        examsThisSubject.add(new ExamAssignmentInfo((Exam)j.next()));
                    }
                    examsPerSubject.put(subject, examsThisSubject);
                }
            }
            */
            Hashtable<String,File> output = new Hashtable();
            Hashtable<SubjectArea,Hashtable<String,File>> outputPerSubject = new Hashtable();
            Hashtable<ExamInstructorInfo,File> ireports = null;
            Hashtable<Student,File> sreports = null;
            Session session = getSession();
            for (int i=0;i<iForm.getReports().length;i++) {
            	iProgress = 0.1 + (0.8 / iForm.getReports().length) * i;
            	RegisteredReport regReport = ExamPdfReportForm.RegisteredReport.valueOf(iForm.getReports()[i]);
                setStatus(MSG.statusGeneratingReport(iForm.getReportName(regReport)));
                Class reportClass = regReport.getImplementation();
                String reportName = null;
                for (Map.Entry<String, Class> entry : PdfLegacyExamReport.sRegisteredReports.entrySet())
                    if (entry.getValue().equals(reportClass)) reportName = entry.getKey();
                if (reportName==null) reportName = "r"+(i+1);
                String name = session.getAcademicTerm()+session.getSessionStartYear()+ExamTypeDAO.getInstance().get(iForm.getExamType()).getReference()+"_"+reportName;
                if (iForm.getAll()) {
                    File file = ApplicationProperties.getTempFile(name, PdfLegacyExamReport.getExtension(iForm.getReportMode()).substring(1));
                    log("&nbsp;&nbsp;" + MSG.statusWritingReport("<a href='temp/"+file.getName()+"'>"+reportName+PdfLegacyExamReport.getExtension(iForm.getReportMode())+"</a>") + (iSubjectIndependent ? " " + MSG.hintNbrExams(exams.size()) : ""));
                    PdfLegacyExamReport report = (PdfLegacyExamReport)reportClass.
                        getConstructor(int.class, File.class, Session.class, ExamType.class, Collection.class, Collection.class).
                        newInstance(iForm.getReportMode().ordinal(), file, new SessionDAO().get(session.getUniqueId()), ExamTypeDAO.getInstance().get(iForm.getExamType()), iSubjectIndependent ? null : iForm.getSubjectAreas(), exams);
                    report.setDirect(iForm.getDirect());
                    report.setM2d(iForm.getM2d());
                    report.setBtb(iForm.getBtb());
                    report.setDispRooms(iForm.getDispRooms());
                    report.setNoRoom(iForm.getNoRoom());
                    report.setTotals(iForm.getTotals());
                    report.setLimit(iForm.getLimit()==null || iForm.getLimit().length()==0?-1:Integer.parseInt(iForm.getLimit()));
                    report.setRoomCode(iForm.getRoomCodes());
                    report.setDispLimits(iForm.getDispLimit());
                    report.setSince(iForm.getSince()==null || iForm.getSince().length()==0?null:Formats.getDateFormat(Formats.Pattern.DATE_ENTRY_FORMAT).parse(iForm.getSince()));
                    report.setItype(iForm.getItype());
                    report.setClassSchedule(iForm.getClassSchedule());
                    report.setDispNote(iForm.getDispNote());
                    report.setCompact(iForm.getCompact());
                    report.setUseRoomDisplayNames(iForm.getRoomDispNames());
                    report.printReport();
                    report.close();
                    output.put(reportName+PdfLegacyExamReport.getExtension(iForm.getReportMode()),file);
                    if (report instanceof InstructorExamReport && iForm.getEmailInstructors()) {
                        ireports = ((InstructorExamReport)report).printInstructorReports(name, new FileGenerator(name));
                    } else if (report instanceof StudentExamReport && iForm.getEmailStudents()) {
                        sreports = ((StudentExamReport)report).printStudentReports(name, new FileGenerator(name));
                    }
                } else {
                    for (int j=0;j<iForm.getSubjects().length;j++) {
                        SubjectArea subject = new SubjectAreaDAO().get(Long.valueOf(iForm.getSubjects()[j]));
                        File file = ApplicationProperties.getTempFile(name+"_"+subject.getSubjectAreaAbbreviation(), PdfLegacyExamReport.getExtension(iForm.getReportMode()).substring(1));
                        int nrExams = 0;
                        for (ExamAssignmentInfo exam : exams) {
                            if (exam.isOfSubjectArea(subject)) nrExams++;
                        }
                        log("&nbsp;&nbsp;" + MSG.statusWritingReport("<a href='temp/"+file.getName()+"'>"+subject.getSubjectAreaAbbreviation()+"_"+reportName+PdfLegacyExamReport.getExtension(iForm.getReportMode())+"</a>") + " " + MSG.hintNbrExams(nrExams));
                        List<SubjectArea> subjects = new ArrayList<SubjectArea>(); subjects.add(subject);
                        PdfLegacyExamReport report = (PdfLegacyExamReport)reportClass.
                            getConstructor(int.class, File.class, Session.class, ExamType.class, Collection.class, Collection.class).
                            newInstance(iForm.getReportMode().ordinal(), file, new SessionDAO().get(session.getUniqueId()), ExamTypeDAO.getInstance().get(iForm.getExamType()), subjects, exams);
                        report.setDirect(iForm.getDirect());
                        report.setM2d(iForm.getM2d());
                        report.setBtb(iForm.getBtb());
                        report.setDispRooms(iForm.getDispRooms());
                        report.setNoRoom(iForm.getNoRoom());
                        report.setTotals(iForm.getTotals());
                        report.setLimit(iForm.getLimit()==null || iForm.getLimit().length()==0?-1:Integer.parseInt(iForm.getLimit()));
                        report.setRoomCode(iForm.getRoomCodes());
                        report.setDispLimits(iForm.getDispLimit());
                        report.setItype(iForm.getItype());
                        report.setClassSchedule(iForm.getClassSchedule());
                        report.setDispNote(iForm.getDispNote());
                        report.setCompact(iForm.getCompact());
                        report.setUseRoomDisplayNames(iForm.getRoomDispNames());
                        report.printReport();
                        report.close();
                        output.put(subject.getSubjectAreaAbbreviation()+"_"+reportName+PdfLegacyExamReport.getExtension(iForm.getReportMode()),file);
                        Hashtable<String,File> files = outputPerSubject.get(subject);
                        if (files==null) {
                            files = new Hashtable(); outputPerSubject.put(subject,files);
                        }
                        files.put(subject.getSubjectAreaAbbreviation()+"_"+reportName+PdfLegacyExamReport.getExtension(iForm.getReportMode()),file);
                        if (report instanceof InstructorExamReport && iForm.getEmailInstructors()) {
                            ireports = ((InstructorExamReport)report).printInstructorReports( name, new FileGenerator(name));
                        } else if (report instanceof StudentExamReport && iForm.getEmailStudents()) {
                            sreports = ((StudentExamReport)report).printStudentReports(name, new FileGenerator(name));
                        }
                    }
                }
            }
        	iProgress = 0.9;
            byte[] buffer = new byte[32*1024];
            int len = 0;
            if (output.isEmpty())
                log("<font color='orange'>" + MSG.warnNoReportGenerated() + "</font>");
            else if (iForm.getEmail()) {
                setStatus(MSG.statusSendingEmails());
                if (iForm.getEmailDeputies()) {
                    Hashtable<TimetableManager,Hashtable<String,File>> files2send = new Hashtable();
                    for (Map.Entry<SubjectArea, Hashtable<String,File>> entry : outputPerSubject.entrySet()) {
                        if (entry.getKey().getDepartment().getTimetableManagers().isEmpty())
                            log("<font color='orange'>&nbsp;&nbsp;" + MSG.warnNoManagerForSubject(entry.getKey().getSubjectAreaAbbreviation(), entry.getKey().getDepartment().getLabel())+"</font>");
                        for (Iterator i=entry.getKey().getDepartment().getTimetableManagers().iterator();i.hasNext();) {
                            TimetableManager g = (TimetableManager)i.next();
                            boolean receiveEmail = false;
                            for (ManagerRole mr : (Set<ManagerRole>)g.getManagerRoles()) {
                            	if (Boolean.TRUE.equals(mr.isReceiveEmails()) && !mr.getRole().hasRight(Right.DepartmentIndependent) && mr.getRole().hasRight(Right.ExaminationPdfReports))
                            		receiveEmail = true;
                            }
                            if (receiveEmail){
                                if (g.getEmailAddress()==null || g.getEmailAddress().length()==0) {
                                    log("<font color='orange'>&nbsp;&nbsp;" + MSG.warnManagerHasNoEmail(g.getName()) + "</font>");
                                } else {
                                    Hashtable<String,File> files = files2send.get(g);
                                    if (files==null) { files = new Hashtable<String,File>(); files2send.put(g, files); }
                                    files.putAll(entry.getValue());
                                }
                            }
                        }
                    }
                    if (files2send.isEmpty()) {
                        log("<font color='red'>" + MSG.warnNothingToSend() + "</font>");
                    } else {
                        Set<TimetableManager> managers = files2send.keySet();
                        while (!managers.isEmpty()) {
                            TimetableManager manager = managers.iterator().next();
                            Hashtable<String,File> files = files2send.get(manager);
                            managers.remove(manager);
                            log(MSG.infoSendingEmail(manager.getName(), manager.getEmailAddress()));
                            try {
                                Email mail = Email.createEmail();
                                mail.setSubject(iForm.getSubject()==null?MSG.emailSubjectExaminationReport():iForm.getSubject());
                                mail.setText((iForm.getMessage()==null?"":iForm.getMessage()+"\r\n\r\n")+
                                		MSG.emailForUpToDateReportVisit(iUrl)+"\r\n\r\n"+
                                		MSG.emailFooter(Constants.getVersion())
                                		);
                                mail.addRecipient(manager.getEmailAddress(), manager.getName());
                                for (Iterator<TimetableManager> i=managers.iterator();i.hasNext();) {
                                    TimetableManager m = (TimetableManager)i.next();
                                    if (files.equals(files2send.get(m))) {
                                        log("&nbsp;&nbsp;" + MSG.infoIncluding(m.getName(), m.getEmailAddress()));
                                        mail.addRecipient(m.getEmailAddress(),m.getName());
                                        i.remove();
                                    }
                                }
                                if (iForm.getAddress()!=null) for (StringTokenizer s=new StringTokenizer(iForm.getAddress(),";,\n\r ");s.hasMoreTokens();) 
                                    mail.addRecipient(s.nextToken(), null);
                                if (iForm.getCc()!=null) for (StringTokenizer s=new StringTokenizer(iForm.getCc(),";,\n\r ");s.hasMoreTokens();) 
                                    mail.addRecipientCC(s.nextToken(), null);
                                if (iForm.getBcc()!=null) for (StringTokenizer s=new StringTokenizer(iForm.getBcc(),";,\n\r ");s.hasMoreTokens();) 
                                    mail.addRecipientBCC(s.nextToken(), null);
                                for (Map.Entry<String, File> entry : files.entrySet()) {
                                	mail.addAttachment(entry.getValue(), session.getAcademicTerm()+session.getSessionStartYear()+ExamTypeDAO.getInstance().get(iForm.getExamType()).getReference()+"_"+entry.getKey());
                                    log("&nbsp;&nbsp;" + MSG.infoAttaching("<a href='temp/"+entry.getValue().getName()+"'>"+entry.getKey()+"</a>"));
                                }
                                mail.send();
                                log(MSG.infoEmailSent());
                            } catch (Exception e) {
                                log("<font color='red'>" + MSG.errorUnableToSendEmail(e.getMessage())+"</font>");
                                setError(e);
                            }
                        }
                    }
                } else {
                    try {
                    	Email mail = Email.createEmail();
                        mail.setSubject(iForm.getSubject()==null?MSG.emailSubjectExaminationReport():iForm.getSubject());
                        mail.setText((iForm.getMessage()==null?"":iForm.getMessage()+"\r\n\r\n")+
                        		MSG.emailForUpToDateReportVisit(iUrl)+"\r\n\r\n"+
                        		MSG.emailFooter(Constants.getVersion()));
                        if (iForm.getAddress()!=null) for (StringTokenizer s=new StringTokenizer(iForm.getAddress(),";,\n\r ");s.hasMoreTokens();) 
                            mail.addRecipient(s.nextToken(), null);
                        if (iForm.getCc()!=null) for (StringTokenizer s=new StringTokenizer(iForm.getCc(),";,\n\r ");s.hasMoreTokens();) 
                            mail.addRecipientCC(s.nextToken(), null);
                        if (iForm.getBcc()!=null) for (StringTokenizer s=new StringTokenizer(iForm.getBcc(),";,\n\r ");s.hasMoreTokens();) 
                            mail.addRecipientBCC(s.nextToken(), null);
                        for (Map.Entry<String, File> entry : output.entrySet()) {
                        	mail.addAttachment(entry.getValue(), session.getAcademicTerm()+session.getSessionStartYear()+ExamTypeDAO.getInstance().get(iForm.getExamType()).getReference()+"_"+entry.getKey());
                        }
                        mail.send();
                        log(MSG.infoEmailSent());
                    } catch (Exception e) {
                    	log("<font color='red'>" + MSG.errorUnableToSendEmail(e.getMessage())+"</font>");
                        setError(e);
                    }
                }
                if (iForm.getEmailInstructors() && ireports!=null && !ireports.isEmpty()) {
                    setStatus(MSG.statusEmailingInstructors());
                    for (ExamInstructorInfo instructor : new TreeSet<ExamInstructorInfo>(ireports.keySet())) {
                        File report = ireports.get(instructor);
                        String email = instructor.getInstructor().getEmail();
                        if (email==null || email.length()==0) {
                            log("&nbsp;&nbsp;<font color='orange'>" + MSG.errorUnableToSentInstructorNoEmail("<a href='temp/"+report.getName()+"'>"+instructor.getName()+"</a>") + "</font>");
                            continue;
                        }
                        try {
                        	Email mail = Email.createEmail();
                            mail.setSubject(iForm.getSubject()==null?MSG.emailSubjectExaminationReport():iForm.getSubject());
                            mail.setText((iForm.getMessage()==null?"":iForm.getMessage()+"\r\n\r\n")+
                            		MSG.emailForUpToDateReportVisit(iUrl)+"\r\n\r\n"+
                            		MSG.emailFooter(Constants.getVersion()));
                            mail.addRecipient(email, null);
                            if (iForm.getCc()!=null) for (StringTokenizer s=new StringTokenizer(iForm.getCc(),";,\n\r ");s.hasMoreTokens();) 
                                mail.addRecipientCC(s.nextToken(), null);
                            if (iForm.getBcc()!=null) for (StringTokenizer s=new StringTokenizer(iForm.getBcc(),";,\n\r ");s.hasMoreTokens();) 
                                mail.addRecipientBCC(s.nextToken(), null);
                            mail.addAttachment(report, session.getAcademicTerm()+session.getSessionStartYear()+ExamTypeDAO.getInstance().get(iForm.getExamType()).getReference()+PdfLegacyExamReport.getExtension(iForm.getReportMode()));
                            mail.send();
                            log("&nbsp;&nbsp;" + MSG.infoEmailSentTo("<a href='temp/"+report.getName()+"'>"+instructor.getName()+"</a>"));
                        } catch (Exception e) {
                            log("&nbsp;&nbsp;<font color='orange'>" + MSG.errorUnableToSendEmailTo("<a href='temp/"+report.getName()+"'>"+instructor.getName()+"</a>", e.getMessage())+"</font>");
                            setError(e);
                        }
                    }
                    log(MSG.infoEmailsSent());
                }
                if (iForm.getEmailStudents() && sreports!=null && !sreports.isEmpty()) {
                    setStatus(MSG.statusEmailingStudents());
                    for (Student student : new TreeSet<Student>(sreports.keySet())) {
                        File report = sreports.get(student);
                        String email = student.getEmail();
                        if (email==null || email.length()==0) {
                            log("&nbsp;&nbsp;<font color='orange'>" + MSG.errorUnableToSentStudentNoEmail("<a href='temp/"+report.getName()+"'>"+student.getName(DepartmentalInstructor.sNameFormatLastFist)+"</a>") + "</font>");
                            continue;
                        }
                        try {
                            Email mail = Email.createEmail();
                            mail.setSubject(iForm.getSubject()==null?MSG.emailSubjectExaminationReport():iForm.getSubject());
                            mail.setText((iForm.getMessage()==null?"":iForm.getMessage()+"\r\n\r\n")+
                            		MSG.emailForUpToDateReportVisit(iUrl)+"\r\n\r\n"+
                            		MSG.emailFooter(Constants.getVersion()));
                            mail.addRecipient(email, null);
                            if (iForm.getCc()!=null) for (StringTokenizer s=new StringTokenizer(iForm.getCc(),";,\n\r ");s.hasMoreTokens();) 
                                mail.addRecipientCC(s.nextToken(), null);
                            if (iForm.getBcc()!=null) for (StringTokenizer s=new StringTokenizer(iForm.getBcc(),";,\n\r ");s.hasMoreTokens();) 
                                mail.addRecipientBCC(s.nextToken(), null);
                            mail.addAttachment(report, session.getAcademicTerm()+session.getSessionStartYear()+ExamTypeDAO.getInstance().get(iForm.getExamType()).getReference()+PdfLegacyExamReport.getExtension(iForm.getReportMode()));
                            mail.send();
                            log("&nbsp;&nbsp;" + MSG.infoEmailSentTo("<a href='temp/"+report.getName()+"'>"+student.getName(DepartmentalInstructor.sNameFormatLastFist)+"</a>"));
                        } catch (Exception e) {
                        	log("&nbsp;&nbsp;<font color='orange'>" + MSG.errorUnableToSendEmailTo("<a href='temp/"+report.getName()+"'>"+student.getName(DepartmentalInstructor.sNameFormatLastFist)+"</a>", e.getMessage())+"</font>");
                            setError(e);
                        }
                    }
                    log(MSG.infoEmailsSent());
                }
            }
            if (output.isEmpty()) {
                throw new Exception(MSG.errorNoReportGenerated());
            } else if (output.size()==1) {
            	setOutput(output.elements().nextElement());
            } else {
                FileInputStream fis = null;
                ZipOutputStream zip = null;
                try {
                    File zipFile = ApplicationProperties.getTempFile(session.getAcademicTerm()+session.getSessionStartYear()+ExamTypeDAO.getInstance().get(iForm.getExamType()).getReference(), "zip");
                    log(MSG.statusWritingReport("<a href='temp/"+zipFile.getName()+"'>"+session.getAcademicTerm()+session.getSessionStartYear()+ExamTypeDAO.getInstance().get(iForm.getExamType()).getReference()+".zip</a>"));
                    zip = new ZipOutputStream(new FileOutputStream(zipFile));
                    for (Map.Entry<String, File> entry : output.entrySet()) {
                        zip.putNextEntry(new ZipEntry(entry.getKey()));
                        fis = new FileInputStream(entry.getValue());
                        while ((len=fis.read(buffer))>0) zip.write(buffer, 0, len);
                        fis.close(); fis = null;
                        zip.closeEntry();
                    }
                    zip.flush(); zip.close();
                    setOutput(zipFile);
                } catch (IOException e) {
                    if (fis!=null) fis.close();
                    if (zip!=null) zip.close();
                    setError(e);
                }
            }
        	iProgress = 1.0;
            setStatus(MSG.statusAllDone());
        } catch (Exception e) {
            fatal(MSG.errorTaskFailed(), e);
        }
	}

	@Override
	public String name() {
		return iName;
	}

	@Override
	public double progress() {
		return iProgress;
	}
	
	@Override
	public String type() {
		return TYPE;
	}
	
	public static class FileGenerator implements InstructorExamReport.FileGenerator {
        String iName;
        public FileGenerator(String name) {
            iName = name;
        }
        public File generate(String prefix, String ext) {
            return ApplicationProperties.getTempFile(iName+"_"+prefix, ext);
        }
    }

}
