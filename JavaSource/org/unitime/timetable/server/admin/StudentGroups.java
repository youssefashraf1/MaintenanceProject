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
package org.unitime.timetable.server.admin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;


import org.cpsolver.ifs.util.ToolBox;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.unitime.localization.impl.Localization;
import org.unitime.timetable.defaults.ApplicationProperty;
import org.unitime.timetable.gwt.command.client.GwtRpcException;
import org.unitime.timetable.gwt.resources.GwtMessages;
import org.unitime.timetable.gwt.shared.SimpleEditInterface;
import org.unitime.timetable.gwt.shared.SimpleEditInterface.Field;
import org.unitime.timetable.gwt.shared.SimpleEditInterface.FieldType;
import org.unitime.timetable.gwt.shared.SimpleEditInterface.Flag;
import org.unitime.timetable.gwt.shared.SimpleEditInterface.ListItem;
import org.unitime.timetable.gwt.shared.SimpleEditInterface.PageName;
import org.unitime.timetable.gwt.shared.SimpleEditInterface.Record;
import org.unitime.timetable.model.ChangeLog;
import org.unitime.timetable.model.DepartmentalInstructor;
import org.unitime.timetable.model.Student;
import org.unitime.timetable.model.StudentGroup;
import org.unitime.timetable.model.StudentGroupType;
import org.unitime.timetable.model.ChangeLog.Operation;
import org.unitime.timetable.model.ChangeLog.Source;
import org.unitime.timetable.model.StudentSectioningQueue;
import org.unitime.timetable.model.dao.SessionDAO;
import org.unitime.timetable.model.dao.StudentGroupDAO;
import org.unitime.timetable.model.dao.StudentGroupTypeDAO;
import org.unitime.timetable.security.SessionContext;
import org.unitime.timetable.security.rights.Right;
import org.unitime.timetable.server.admin.AdminTable.HasFilter;
import org.unitime.timetable.server.admin.AdminTable.HasLazyFields;

/**
 * @author Tomas Muller
 */
@Service("gwtAdminTable[type=group]")
public class StudentGroups implements AdminTable, HasLazyFields, HasFilter {
	protected static final GwtMessages MESSAGES = Localization.create(GwtMessages.class);
	
	@Override
	public PageName name() {
		return new PageName(MESSAGES.pageStudentGroup(), MESSAGES.pageStudentGroups());
	}

	@Override
	@PreAuthorize("checkPermission('StudentGroups')")
	public SimpleEditInterface load(SessionContext context, Session hibSession) {
		return load((String[])null, context, hibSession);
	}
	
	protected List<StudentGroup> getGroups(String[] filter, SessionContext context, Session hibSession) {
		if (filter == null || filter[0] == null || filter[0].isEmpty()) {
			return StudentGroupDAO.getInstance().findBySession(hibSession, context.getUser().getCurrentAcademicSessionId());
		} else if ("null".equals(filter[0])) {
			context.getUser().setProperty("Admin.StudentGroups.LastGroupTypeId", "null");
			return StudentGroup.findByType(hibSession, context.getUser().getCurrentAcademicSessionId(), null);
		} else {
			context.getUser().setProperty("Admin.StudentGroups.LastGroupTypeId", filter[0]);
			return StudentGroup.findByType(hibSession, context.getUser().getCurrentAcademicSessionId(), Long.valueOf(filter[0]));
		}
	}
	
	@Override
	@PreAuthorize("checkPermission('StudentGroups')")
	public SimpleEditInterface load(String[] filter, SessionContext context, Session hibSession) {
		List<ListItem> types = new ArrayList<ListItem>();
		types.add(new ListItem("", MESSAGES.itemNoStudentGroupType()));
		for (StudentGroupType type: StudentGroupTypeDAO.getInstance().findAll(Order.asc("label"))) {
			types.add(new ListItem(type.getUniqueId().toString(), type.getLabel()));
		}
		String defaultType = null;
		if (filter != null && filter[0] != null && !filter[0].isEmpty() && !filter[0].equals("null")) {
			defaultType = filter[0];
		}
		boolean lazy = ApplicationProperty.AdminStudentGroupsLazyStudents.isTrue();
		SimpleEditInterface data = new SimpleEditInterface(
				new Field(MESSAGES.fieldExternalId(), FieldType.text, 120, 40, Flag.READ_ONLY),
				new Field(MESSAGES.fieldCode(), FieldType.text, 200, 30, Flag.UNIQUE),
				new Field(MESSAGES.fieldName(), FieldType.text, 500, 90, Flag.UNIQUE),
				new Field(MESSAGES.fieldStudentGroupType(), FieldType.list, 100, types).withDefault(defaultType),
				new Field(MESSAGES.fieldExpectedSize(), FieldType.number, 80, 10),
				(lazy ? new Field(MESSAGES.fieldStudents(), FieldType.students, 200, Flag.LAZY) : new Field(MESSAGES.fieldStudents(), FieldType.students, 200)));
		data.setSortBy(1,2);
		for (StudentGroup group: getGroups(filter, context, hibSession)) {
			Record r = data.addRecord(group.getUniqueId());
			r.setField(0, group.getExternalUniqueId());
			r.setField(1, group.getGroupAbbreviation());
			r.setField(2, group.getGroupName());
			r.setField(3, group.getType() == null ? "" : group.getType().getUniqueId().toString());
			r.setField(4, group.getExpectedSize() == null ? "" : group.getExpectedSize().toString());
			if (lazy) {
				r.setField(5, String.valueOf(group.getStudents().size()));
			} else {
				String students = "";
				for (Student student: new TreeSet<Student>(group.getStudents())) {
					if (!students.isEmpty()) students += "\n";
					students += student.getExternalUniqueId() + " " + student.getName(DepartmentalInstructor.sNameFormatLastFirstMiddle);
				}
				r.setField(5, students, group.getExternalUniqueId() == null);
			}
			r.setDeletable(group.getExternalUniqueId() == null);
		}
		data.setEditable(context.hasPermission(Right.StudentGroupEdit));
		return data;
	}

	@Override
	@PreAuthorize("checkPermission('StudentGroupEdit')")
	public void save(SimpleEditInterface data, SessionContext context, Session hibSession) {
		save(null, data, context, hibSession);
	}
	
	@Override
	@PreAuthorize("checkPermission('StudentGroupEdit')")
	public void save(String[] filter, SimpleEditInterface data, SessionContext context, Session hibSession) {
		Set<Long> studentIds = new HashSet<Long>();
		for (StudentGroup group: getGroups(filter, context, hibSession)) {
			Record r = data.getRecord(group.getUniqueId());
			if (r == null)
				delete(group, context, hibSession, studentIds);
			else 
				update(group, r, context, hibSession, studentIds);
		}
		for (Record r: data.getNewRecords())
			save(r, context, hibSession, studentIds);
		if (!studentIds.isEmpty())
			StudentSectioningQueue.studentChanged(hibSession, context.getUser(), context.getUser().getCurrentAcademicSessionId(), studentIds);
	}

	protected void save(Record record, SessionContext context, Session hibSession, Set<Long> studentIds) {
		StudentGroup g1 = (StudentGroup)hibSession.createQuery(
                "select a from StudentGroup a where a.session.uniqueId=:sessionId and a.groupAbbreviation=:abbv").
				setLong("sessionId", context.getUser().getCurrentAcademicSessionId()).
				setString("abbv", record.getField(1)).
				setMaxResults(1).uniqueResult();
		if (g1 != null)
			throw new GwtRpcException(MESSAGES.errorMustBeUnique(MESSAGES.fieldCode()));
		StudentGroup g2 = (StudentGroup)hibSession.createQuery(
                "select a from StudentGroup a where a.session.uniqueId=:sessionId and a.groupName=:name").
				setLong("sessionId", context.getUser().getCurrentAcademicSessionId()).
				setString("name", record.getField(2)).
				setMaxResults(1).uniqueResult();
		if (g2 != null)
			throw new GwtRpcException(MESSAGES.errorMustBeUnique(MESSAGES.fieldName()));
		StudentGroup group = new StudentGroup();
		group.setExternalUniqueId(record.getField(0));
		group.setGroupAbbreviation(record.getField(1));
		group.setGroupName(record.getField(2));
		group.setType(record.getField(3) == null || record.getField(3).isEmpty() ? null : StudentGroupTypeDAO.getInstance().get(Long.valueOf(record.getField(3))));
		try {
			group.setExpectedSize(record.getField(4) == null || record.getField(4).isEmpty() ? null : Integer.valueOf(record.getField(4)));
		} catch (NumberFormatException e) {
			group.setExpectedSize(null);
		}
		group.setSession(SessionDAO.getInstance().get(context.getUser().getCurrentAcademicSessionId(), hibSession));
		group.setStudents(new HashSet<Student>());
		if (record.getField(5) != null) {
			String students = "";
			for (String s: record.getField(5).split("\\n")) {
				if (s.indexOf(' ') >= 0) s = s.substring(0, s.indexOf(' '));
				if (s.trim().isEmpty()) continue;
				Student student = Student.findByExternalId(context.getUser().getCurrentAcademicSessionId(), s.trim());
				if (student != null) {
					group.getStudents().add(student);
					student.getGroups().add(group);
					if (!students.isEmpty()) students += "\n";
					students += student.getExternalUniqueId() + " " + student.getName(DepartmentalInstructor.sNameFormatLastFirstMiddle);
					studentIds.add(student.getUniqueId());
				}
			}
			record.setField(5, students, true);
		}
		record.setUniqueId((Long)hibSession.save(group));
		ChangeLog.addChange(hibSession,
				context,
				group,
				group.getGroupAbbreviation() + " " + group.getGroupName(),
				Source.SIMPLE_EDIT, 
				Operation.CREATE,
				null,
				null);
	}
	
	@Override
	@PreAuthorize("checkPermission('StudentGroupEdit')")
	public void save(Record record, SessionContext context, Session hibSession) {
		Set<Long> studentIds = new HashSet<Long>();
		save(record, context, hibSession, studentIds);
		if (!studentIds.isEmpty())
			StudentSectioningQueue.studentChanged(hibSession, context.getUser(), context.getUser().getCurrentAcademicSessionId(), studentIds);
	}

	
	protected void update(StudentGroup group, Record record, SessionContext context, Session hibSession, Set<Long> studentIds) {
		if (group == null) return;
		StudentGroup g1 = (StudentGroup)hibSession.createQuery(
                "select a from StudentGroup a where a.session.uniqueId=:sessionId and a.groupAbbreviation=:abbv and a.uniqueId!=:gid").
				setLong("sessionId", context.getUser().getCurrentAcademicSessionId()).
				setString("abbv", record.getField(1)).
				setLong("gid", group.getUniqueId()).
				setMaxResults(1).uniqueResult();
		if (g1 != null)
			throw new GwtRpcException(MESSAGES.errorMustBeUnique(MESSAGES.fieldCode()));
		StudentGroup g2 = (StudentGroup)hibSession.createQuery(
                "select a from StudentGroup a where a.session.uniqueId=:sessionId and a.groupName=:name and a.uniqueId!=:gid").
				setLong("sessionId", context.getUser().getCurrentAcademicSessionId()).
				setString("name", record.getField(2)).
				setLong("gid", group.getUniqueId()).
				setMaxResults(1).uniqueResult();
		if (g2 != null)
			throw new GwtRpcException(MESSAGES.errorMustBeUnique(MESSAGES.fieldName()));		
		boolean changed = 
				!ToolBox.equals(group.getExternalUniqueId(), record.getField(0)) ||
				!ToolBox.equals(group.getGroupAbbreviation(), record.getField(1)) ||
				!ToolBox.equals(group.getGroupName(), record.getField(2)) ||
				!ToolBox.equals(group.getType() == null ? "" : group.getType().getUniqueId().toString(), record.getField(3)) ||
				!ToolBox.equals(group.getExpectedSize() == null ? "" : group.getExpectedSize().toString(), record.getField(4));
		boolean codeOrTypeChange =
				!ToolBox.equals(group.getGroupAbbreviation(), record.getField(1)) ||
				!ToolBox.equals(group.getType() == null ? "" : group.getType().getUniqueId().toString(), record.getField(3));
		group.setExternalUniqueId(record.getField(0));
		group.setGroupAbbreviation(record.getField(1));
		group.setGroupName(record.getField(2));
		group.setType(record.getField(3) == null || record.getField(3).isEmpty() ? null : StudentGroupTypeDAO.getInstance().get(Long.valueOf(record.getField(3))));
		try {
			group.setExpectedSize(record.getField(4) == null || record.getField(4).isEmpty() ? null : Integer.valueOf(record.getField(4)));
		} catch (NumberFormatException e) {
			group.setExpectedSize(null);
		}
		if (group.getExternalUniqueId() == null && record.getField(5) != null) {
			Hashtable<String, Student> students = new Hashtable<String, Student>();
			for (Student s: group.getStudents()) {
				students.put(s.getExternalUniqueId(), s);
			}
			for (String line: record.getField(5).split("\\n")) {
				String extId = (line.indexOf(' ') >= 0 ? line.substring(0, line.indexOf(' ')) : line).trim();
				if (extId.isEmpty() || students.remove(extId) != null) continue;
				Student student = Student.findByExternalId(context.getUser().getCurrentAcademicSessionId(), extId);
				if (student != null) {
					group.getStudents().add(student);
					student.getGroups().add(group);
					changed = true;
					studentIds.add(student.getUniqueId());
				}
			}
			if (!students.isEmpty()) {
				for (Student student: students.values()) {
					studentIds.add(student.getUniqueId());
					student.getGroups().remove(group);
				}
				group.getStudents().removeAll(students.values());
				changed = true;
			}
			String newStudents = "";
			for (Student student: new TreeSet<Student>(group.getStudents())) {
				if (!newStudents.isEmpty()) newStudents += "\n";
				newStudents += student.getExternalUniqueId() + " " + student.getName(DepartmentalInstructor.sNameFormatLastFirstMiddle);
				if (codeOrTypeChange) studentIds.add(student.getUniqueId());
			}
			record.setField(5, newStudents, group.getExternalUniqueId() == null);
		}
		hibSession.saveOrUpdate(group);
		if (changed)
			ChangeLog.addChange(hibSession,
					context,
					group,
					group.getGroupAbbreviation() + " " + group.getGroupName(),
					Source.SIMPLE_EDIT, 
					Operation.UPDATE,
					null,
					null);
	}

	@Override
	@PreAuthorize("checkPermission('StudentGroupEdit')")
	public void update(Record record, SessionContext context, Session hibSession) {
		Set<Long> studentIds = new HashSet<Long>();
		update(StudentGroupDAO.getInstance().get(record.getUniqueId()), record, context, hibSession, studentIds);
		if (!studentIds.isEmpty())
			StudentSectioningQueue.studentChanged(hibSession, context.getUser(), context.getUser().getCurrentAcademicSessionId(), studentIds);
	}

	protected void delete(StudentGroup group, SessionContext context, Session hibSession, Set<Long> studentIds) {
		if (group == null) return;
		if (group.getStudents() != null)
			for (Student student: group.getStudents()) {
				studentIds.add(student.getUniqueId());
				student.getGroups().remove(group);
			}
		ChangeLog.addChange(hibSession,
				context,
				group,
				group.getGroupAbbreviation() + " " + group.getGroupName(),
				Source.SIMPLE_EDIT, 
				Operation.DELETE,
				null,
				null);
		hibSession.delete(group);
	}
	
	@Override
	@PreAuthorize("checkPermission('StudentGroupEdit')")
	public void delete(Record record, SessionContext context, Session hibSession) {
		Set<Long> studentIds = new HashSet<Long>();
		delete(StudentGroupDAO.getInstance().get(record.getUniqueId()), context, hibSession, studentIds);		
		if (!studentIds.isEmpty())
			StudentSectioningQueue.studentChanged(hibSession, context.getUser(), context.getUser().getCurrentAcademicSessionId(), studentIds);
	}

	@Override
	@PreAuthorize("checkPermission('StudentGroupEdit')")
	public void load(Record record, SessionContext context, Session hibSession) {
		if (record.getUniqueId() != null) {
			StudentGroup group = StudentGroupDAO.getInstance().get(record.getUniqueId());
			if (group != null) {
				String students = "";
				for (Student student: new TreeSet<Student>(group.getStudents())) {
					if (!students.isEmpty()) students += "\n";
					students += student.getExternalUniqueId() + " " + student.getName(DepartmentalInstructor.sNameFormatLastFirstMiddle);
				}
				record.setField(5, students, group.getExternalUniqueId() == null);
			}
		}
	}

	@Override
	public SimpleEditInterface.Filter getFilter(SessionContext context, Session hibSession) {
		List<ListItem> types = new ArrayList<ListItem>();
		types.add(new ListItem("", MESSAGES.itemAll()));
		types.add(new ListItem("null", MESSAGES.itemNoStudentGroupType()));
		for (StudentGroupType type: StudentGroupTypeDAO.getInstance().findAll(Order.asc("label"))) {
			types.add(new ListItem(type.getUniqueId().toString(), type.getLabel()));
		}
		SimpleEditInterface.Filter filter = new SimpleEditInterface.Filter(new Field(MESSAGES.fieldStudentGroupType(), FieldType.list, 100, types));
		String lastId = context.getUser().getProperty("Admin.StudentGroups.LastGroupTypeId");
		if (lastId != null)
			filter.getDefaultValue().setField(0, lastId);
		return filter;
	}
}
