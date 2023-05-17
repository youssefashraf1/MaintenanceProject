/*
< * Licensed to The Apereo Foundation under one or more contributor license
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
package org.unitime.timetable.onlinesectioning.custom.purdue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.unitime.timetable.model.Student;
import org.unitime.timetable.model.StudentAreaClassificationMajor;
import org.unitime.timetable.onlinesectioning.custom.purdue.SpecialRegistrationInterface.Change;
import org.unitime.timetable.onlinesectioning.custom.purdue.SpecialRegistrationInterface.ChangeNote;
import org.unitime.timetable.onlinesectioning.custom.purdue.SpecialRegistrationInterface.ChangeStatus;
import org.unitime.timetable.onlinesectioning.custom.purdue.SpecialRegistrationInterface.CheckRestrictionsRequest;
import org.unitime.timetable.onlinesectioning.custom.purdue.SpecialRegistrationInterface.Crn;
import org.unitime.timetable.onlinesectioning.custom.purdue.SpecialRegistrationInterface.IncludeReg;
import org.unitime.timetable.onlinesectioning.custom.purdue.SpecialRegistrationInterface.RestrictionsCheckRequest;
import org.unitime.timetable.onlinesectioning.custom.purdue.SpecialRegistrationInterface.SpecialRegistration;
import org.unitime.timetable.onlinesectioning.custom.purdue.SpecialRegistrationInterface.ValidationMode;
import org.unitime.timetable.onlinesectioning.custom.purdue.SpecialRegistrationInterface.ValidationOperation;
import org.unitime.timetable.onlinesectioning.model.XAreaClassificationMajor;
import org.unitime.timetable.onlinesectioning.model.XStudent;

/**
 * @author Tomas Muller
 */
public class SpecialRegistrationHelper {
	
	public static boolean hasLastNote(Change change) {
		if (change.notes == null || change.notes.isEmpty()) return false;
		for (ChangeNote n: change.notes)
			if (n.notes != null && !n.notes.isEmpty()) return true;
		return false;
	}
	
	public static String getLastNote(Change change) {
		if (change.notes == null || change.notes.isEmpty()) return null;
		ChangeNote note = null;
		for (ChangeNote n: change.notes)
			if (n.notes != null && !n.notes.isEmpty() && (note == null || note.dateCreated.isBefore(n.dateCreated)))
				note = n;
		return (note == null ? null : note.notes);
	}
	
	public static String note(SpecialRegistration reg, boolean credit) {
		String note = null;
		if (reg.changes != null)
			for (Change ch: reg.changes) {
				if (credit && ch.subject == null && ch.courseNbr == null && hasLastNote(ch))
					note = (note == null ? "" : note + "\n") + getLastNote(ch);
				if (!credit && ch.subject != null && ch.courseNbr != null && hasLastNote(ch) && ch.status != ChangeStatus.approved) {
					String n = getLastNote(ch);
					if (note == null)
						note = n;
					else if (!note.contains(n)) {
						note += "\n" + n;
					}
				}
			}
		return note;
	}
	
	@SuppressWarnings("deprecation")
	public static String requestorNotes(SpecialRegistration r, String subject, String courseNbr) {
		if (r.changes != null && subject != null && courseNbr != null)
			for (Change ch: r.changes)
				if (subject.equals(ch.subject) && courseNbr.equals(ch.courseNbr) && ch.requestorNotes != null && !ch.requestorNotes.isEmpty())
					return ch.requestorNotes;
		return r.requestorNotes;
	}
	
	@SuppressWarnings("deprecation")
	public static String requestorNotes(SpecialRegistration r, String course) {
		if (r.changes != null && course != null)
			for (Change ch: r.changes) {
				if (course.equals(ch.subject + " " + ch.courseNbr))
					return ch.requestorNotes;
			}
		return r.requestorNotes;
	}
	
	@SuppressWarnings("deprecation")
	public static String maxCreditRequestorNotes(SpecialRegistration r) {
		if (r.maxCreditRequestorNotes != null)
			return r.maxCreditRequestorNotes;
		return r.requestorNotes;
	}
	
	public static RestrictionsCheckRequest createValidationRequest(CheckRestrictionsRequest req, ValidationMode mode, boolean includeRegistration) {
		RestrictionsCheckRequest ret = new RestrictionsCheckRequest();
		ret.sisId = req.studentId;
		ret.term = req.term;
		ret.campus = req.campus;
		ret.mode = mode;
		ret.includeReg = (includeRegistration ? IncludeReg.Y : IncludeReg.N);
		ret.actions = new HashMap<ValidationOperation, List<Crn>>();
		ret.actions.put(ValidationOperation.ADD, new ArrayList<Crn>());
		if (includeRegistration)
			ret.actions.put(ValidationOperation.DROP, new ArrayList<Crn>());
		return ret;
	}
	
	
	public static void addOperation(RestrictionsCheckRequest request, ValidationOperation op, String crn) {
		if (request.actions == null) request.actions = new HashMap<ValidationOperation, List<Crn>>();
		List<Crn> crns = request.actions.get(op);
		if (crns == null) {
			crns = new ArrayList<Crn>();
			request.actions.put(op, crns);
		} else {
			for (Crn c: crns) {
				if (crn.equals(c.crn)) return;
			}
		}
		Crn c = new Crn(); c.crn = crn;
		crns.add(c);
	}
	public static void addCrn(RestrictionsCheckRequest request, String crn) { addOperation(request, ValidationOperation.ADD, crn); }
	public static void dropCrn(RestrictionsCheckRequest request, String crn) { addOperation(request, ValidationOperation.DROP, crn); }
	public static boolean isEmpty(RestrictionsCheckRequest request) {
		if (request.actions == null || request.actions.isEmpty()) return true;
		List<Crn> adds = request.actions.get(ValidationOperation.ADD);
		if (adds != null && !adds.isEmpty()) return false;
		List<Crn> drops = request.actions.get(ValidationOperation.DROP);
		if (drops != null && !drops.isEmpty()) return false;
		return true;
	}

	public static void addCrn(CheckRestrictionsRequest req, String crn) {
		if (req.changes == null)
			req.changes = createValidationRequest(req, ValidationMode.REG, false);
		addCrn(req.changes, crn);
	}
	
	public static void addAltCrn(CheckRestrictionsRequest req, String crn) {
		if (req.alternatives == null)
			req.alternatives = createValidationRequest(req, ValidationMode.ALT, false);
		addCrn(req.alternatives, crn);
	}
	
	public static void addWaitListCrn(CheckRestrictionsRequest req, String crn) {
		if (req.changes == null)
			req.changes = createValidationRequest(req, ValidationMode.WAITL, true);
		addCrn(req.changes, crn);
	}
	
	public static void dropWaitListCrn(CheckRestrictionsRequest req, String crn) {
		if (req.changes == null)
			req.changes = createValidationRequest(req, ValidationMode.WAITL, true);
		dropCrn(req.changes, crn);
	}
	
	public static boolean isEmpty(CheckRestrictionsRequest req) { 
		return (req.changes == null || isEmpty(req.changes)) && (req.alternatives == null || isEmpty(req.alternatives)); 
	}
	
	public static String pgrmcode(String programCode) {
		if (programCode == null || programCode.isEmpty()) return null;
		if (programCode.endsWith("-OL")) return "OL";
		if (programCode.endsWith("-HY")) return "HY";
		return "RT";
	}
	
	public static String getProgramCode(XStudent student) {
		for (XAreaClassificationMajor acm: student.getMajors()) {
			return SpecialRegistrationHelper.pgrmcode(acm.getProgram());
		}
		return null;
	}

	public static String getProgramCode(Student student) {
		StudentAreaClassificationMajor primary = student.getPrimaryAreaClasfMajor();
		if (primary == null || primary.getProgram() == null) return null;
		return SpecialRegistrationHelper.pgrmcode(primary.getProgram().getReference());
	}

}