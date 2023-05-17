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
package org.unitime.timetable.solver.curricula;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.cpsolver.ifs.util.IdGenerator;
import org.cpsolver.ifs.util.Progress;
import org.unitime.timetable.model.CourseOffering;
import org.unitime.timetable.model.Curriculum;
import org.unitime.timetable.model.CurriculumClassification;
import org.unitime.timetable.model.InstructionalOffering;
import org.unitime.timetable.model.PosMajor;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.Student;
import org.unitime.timetable.model.StudentAreaClassificationMajor;
import org.unitime.timetable.model.StudentAreaClassificationMinor;
import org.unitime.timetable.model.StudentGroup;
import org.unitime.timetable.model.dao.CourseOfferingDAO;

/**
 * @author Tomas Muller
 */
public interface StudentCourseDemands {
	/**
	 * Called only once
	 * @param hibSession opened hibernate session
	 * @param progress progress to print messages
	 * @param session current academic session
	 * @param offerings instructional offerings of the problem that is being loaded
	 */
	public void init(org.hibernate.Session hibSession, Progress progress, Session session, Collection<InstructionalOffering> offerings);
	
	/**
	 * Called once for each course
	 * @param course course for which demands are requested
	 * @return set of students (their unique ids, and weights) that request the course
	 */
	public Set<WeightedStudentId> getDemands(CourseOffering course);
	
	/**
	 * Returns enrollment priority, i.e., an importance of a course request to a student 
	 * @param studentId identification of a student, e.g., as returned by {@link StudentCourseDemands#getDemands(CourseOffering)}
	 * @param course one of the course offerings requested by the student
	 * @return <code>null</code> if not implemented, 0.0 no priority, 1.0 highest priority
	 */
	public Double getEnrollmentPriority(Long studentId, Long courseId);
	
	/**
	 * Return true if students are made up (i.e, it does not make any sense to save them with the solution).
	 */
	public boolean isMakingUpStudents();
	
	/**
	 * Return true if students are real students (not last-like) for which the student class enrollments
	 * apply (real student class enrollments can be loaded instead of solution's student class assignments).
	 */
	public boolean canUseStudentClassEnrollmentsAsSolution();

	/**
	 * Return true if students should be weghted so that the offering is filled in completely.
	 */
	public boolean isWeightStudentsToFillUpOffering();
	
	
	public static interface NeedsStudentIdGenerator {
		/**
		 * Set student id generator
		 */
		public void setStudentIdGenerator(IdGenerator generator);
	}
	
	/**
	 * List of courses for a student
	 */
	public Set<WeightedCourseOffering> getCourses(Long studentId);
	
	public static class AreaClasfMajor implements Comparable<AreaClasfMajor> {
		Double iWeight = 0.0;
		String iArea, iClasf, iMajor, iConcentration;
		String iDegree, iProgram;
		public AreaClasfMajor(String area, String clasf, String major) {
			iArea = area; iClasf = clasf; iMajor = major;
		}
		public AreaClasfMajor(String area, String clasf, String major, String concentration) {
			iArea = area; iClasf = clasf; iMajor = major; iConcentration = concentration;
		}
		public AreaClasfMajor(String area, String clasf, String major, String concentration, String degree, String program, Double weight) {
			iArea = area; iClasf = clasf; iMajor = major; iConcentration = concentration;
			iDegree = degree; iProgram = program; iWeight = weight;
		}
		
		public String getArea() { return iArea; }
		public String getClasf() { return iClasf; }
		public String getMajor() { return iMajor; }
		public String getConcentration() { return iConcentration; }
		public String getDegree() { return iDegree; }
		public String getProgram() { return iProgram; }
		public double getWeight() { return iWeight == null ? 0.0 : iWeight.doubleValue(); }
		
		public String toString() {
			return getArea() + 
				(getMajor().isEmpty() ? "" : "/" + getMajor() + (getConcentration() == null ? "" : "-" + getConcentration())) +
				(getClasf().isEmpty() ? "" : " " + getClasf());
		}
		
		@Override
		public int hashCode() {
			return toString().hashCode();
		}
		
		@Override
		public boolean equals(Object o) {
			if (o == null || !(o instanceof AreaClasfMajor)) return false;
			return toString().equals(o.toString());
		}
		
		@Override
		public int compareTo(AreaClasfMajor ac) {
			if (getWeight() != ac.getWeight()) return (getWeight() > ac.getWeight() ? -1 : 1);
			return toString().compareTo(ac.toString());
		}
	}
	
	public static class Group implements Comparable<Group> {
		Long iId;
		String iName;
		double iWeight;
		boolean iKeepTogether = true;
		
		public Group(Long id, String name, double weight, boolean keepTogether) {
			iId = id; iName = name; iWeight = weight; iKeepTogether = keepTogether;
		}
		
		public Group(Long id, String name) {
			this(id, name, 1.0, true);
		}
		
		public Group(Long id, String name, boolean keepTogether) {
			this(id, name, 1.0, keepTogether);
		}
		
		public Long getId() { return iId; }
		public String getName() { return iName; }
		public double getWeight() { return iWeight; }
		
		public String toString() { return getName(); }
		
		public boolean isKeepTogether() { return iKeepTogether; }
		
		@Override
		public int hashCode() {
			return toString().hashCode();
		}
		
		@Override
		public boolean equals(Object o) {
			if (o == null || !(o instanceof Group)) return false;
			return getId().equals(((Group)o).getId());
		}
		
		@Override
		public int compareTo(Group g) {
			int cmp = getName().compareTo(g.getName());
			if (cmp != 0) return cmp;
			return getId().compareTo(g.getId());
		}
	}
	
	public static class WeightedStudentId {
		private long iStudentId;
		private float iWeight;
		private TreeSet<AreaClasfMajor> iMajors = new TreeSet<AreaClasfMajor>();
		private Set<AreaClasfMajor> iMinors = new TreeSet<AreaClasfMajor>();
		private Set<String> iCurricula = new TreeSet<String>();
		private Set<Group> iGroups = new HashSet<Group>();
		private Long iPrimaryOfferingId = null;
		private Student iStudent = null;
		
		public WeightedStudentId(WeightedStudentId student, float weight) {
			iStudentId = student.iStudentId;
			iWeight = weight;
			iMajors.addAll(student.iMajors);
			iMinors.addAll(student.iMinors);
			iCurricula.addAll(student.iCurricula);
			iGroups.addAll(student.iGroups);
			iStudent = student.iStudent;
		}
		
		public WeightedStudentId(Student student, ProjectionsProvider projections, float weight) {
			iStudent = student;
			iStudentId = student.getUniqueId();
			iWeight = weight;
			float rule = 0.0f, total = 0.0f;
			for (StudentAreaClassificationMajor acm: student.getAreaClasfMajors()) {
				if (acm.getWeight() != null && acm.getWeight() <= 0.0001) continue; // ignore ACMs with zero or close-zero weights
				iMajors.add(new AreaClasfMajor(acm.getAcademicArea().getAcademicAreaAbbreviation(), acm.getAcademicClassification().getCode(), acm.getMajor().getCode(),
						acm.getConcentration() == null ? null : acm.getConcentration().getCode(),
						acm.getDegree() == null ? null : acm.getDegree().getReference(),
						acm.getProgram() == null ? null : acm.getProgram().getReference(),
						acm.getWeight()));
				if (projections != null) {
					rule += (acm.getWeight() == null ? 1.0 : acm.getWeight()) * projections.getProjection(acm.getAcademicArea().getAcademicAreaAbbreviation(), acm.getAcademicClassification().getCode(), acm.getMajor().getCode());
					total += (acm.getWeight() == null ? 1.0 : acm.getWeight());
				}
			}
			for (StudentAreaClassificationMinor acm: student.getAreaClasfMinors())
				iMinors.add(new AreaClasfMajor(acm.getAcademicArea().getAcademicAreaAbbreviation(), acm.getAcademicClassification().getCode(), acm.getMinor().getCode()));
			if (total > 0.0)
				iWeight = rule / total;
			for (StudentGroup g: student.getGroups())
				iGroups.add(new Group(g.getUniqueId(), g.getGroupAbbreviation(), g.getType() == null || g.getType().isKeepTogether()));
		}
		
		public WeightedStudentId(Long studentId, CurriculumClassification cc, ProjectionsProvider projections) {
			iStudentId = studentId;
			Curriculum curriculum = cc.getCurriculum();
			iWeight = 1.0f;
			if (projections != null) {
				if (curriculum.getMajors().isEmpty()) {
					iWeight = projections.getProjection(curriculum.getAcademicArea().getAcademicAreaAbbreviation(), cc.getAcademicClassification().getCode(), "");
				} else if (curriculum.getMajors().size() == 1) {
					for (PosMajor m: curriculum.getMajors())
						iWeight = projections.getProjection(curriculum.getAcademicArea().getAcademicAreaAbbreviation(), cc.getAcademicClassification().getCode(), m.getCode());
				} else {
					double rule = 1.0;
					for (PosMajor m: curriculum.getMajors())
						rule *= projections.getProjection(curriculum.getAcademicArea().getAcademicAreaAbbreviation(), cc.getAcademicClassification().getCode(), m.getCode());
					iWeight = (float)Math.pow(rule, 1.0 / curriculum.getMajors().size());
				}
			}
			if (curriculum.getMajors().isEmpty()) {
				if (!curriculum.isMultipleMajors()) {
					if (curriculum.getAcademicArea().getPosMajors().isEmpty())
						iMajors.add(new AreaClasfMajor(curriculum.getAcademicArea().getAcademicAreaAbbreviation(), cc.getAcademicClassification().getCode(), ""));
					else for (PosMajor major: curriculum.getAcademicArea().getPosMajors())
						iMajors.add(new AreaClasfMajor(curriculum.getAcademicArea().getAcademicAreaAbbreviation(), cc.getAcademicClassification().getCode(), major.getCode()));
				} else {
					iMajors.add(new AreaClasfMajor(curriculum.getAcademicArea().getAcademicAreaAbbreviation(), cc.getAcademicClassification().getCode(), ""));
				}
			} else {
				for (PosMajor major: curriculum.getMajors())
					iMajors.add(new AreaClasfMajor(curriculum.getAcademicArea().getAcademicAreaAbbreviation(), cc.getAcademicClassification().getCode(), major.getCode()));
			}
			iCurricula.add(curriculum.getAbbv() + " " + cc.getAcademicClassification().getCode());
		}

		public WeightedStudentId(Long studentId) {
			iStudentId = studentId;
			iWeight = 1.0f;
		}
		
		public WeightedStudentId(Student student, ProjectionsProvider projections) {
			this(student, projections, 1.0f);
		}
		
		public WeightedStudentId(Student student, float weight) {
			this(student, null, weight);
		}
		
		public WeightedStudentId(Student student) {
			this(student, null, 1.0f);
		}
		
		public WeightedStudentId(Long studentId, CurriculumClassification cc) {
			this(studentId, cc, null);
		}
		
		public long getStudentId() {
			return iStudentId;
		}
		
		public Student getStudent() {
			return iStudent;
		}
		
		public float getWeight() {
			return iWeight;
		}
		
		public void setWeight(float weight) {
			iWeight = weight;
		}
		
		public void setCurriculum(String curriculum) {
			iCurricula.clear(); iCurricula.add(curriculum);
		}
		
		public void setPrimaryOfferingId(Long offeringId) { iPrimaryOfferingId = offeringId; }
		public Long getPrimaryOfferingId() { return iPrimaryOfferingId; }
		
		public Set<AreaClasfMajor> getMajors() { return iMajors; }
		public Set<AreaClasfMajor> getMinors() { return iMinors; }
		public AreaClasfMajor getPrimaryMajor() {
			if (iMajors == null || iMajors.isEmpty()) return null;
			return iMajors.first();
		}
		
		public Set<Group> getGroups() { return iGroups; }
		public Group getGroup(String name) {
			for (Group g: iGroups)
				if (name.equals(g.getName())) return g;
			return null;
		}
		
		public String getArea() { return toString(iMajors, 0, ","); }
		public String getClasf() { return toString(iMajors, 1, ","); }
		public String getMajor() { return toString(iMajors, 2, ","); }
		public String getCurriculum() {
			StringBuffer ret = new StringBuffer();
			if (iCurricula.isEmpty()) {
				for (AreaClasfMajor a: iMajors) {
					if (ret.length() > 0) ret.append("|");
					ret.append(a.toString());
				}
			} else {
				for (String curriculum: iCurricula) {
					if (ret.length() > 0) ret.append("|");
					ret.append(curriculum);
				}
			}
			return ret.toString();
		}
		
		private static String toString(Set<AreaClasfMajor> set, int idx, String delim) {
			if (set == null || set.isEmpty()) return null;
			StringBuffer ret = new StringBuffer();
			for (AreaClasfMajor s: set) {
				if (ret.length() > 0) ret.append(delim);
				switch (idx) {
				case 0:
					ret.append(s.getArea()); break;
				case 1:
					ret.append(s.getClasf()); break;
				case 2:
					ret.append(s.getMajor()); break;
				}
			}
			return ret.toString();
		}
		
		public boolean match(String areaAbbv, Set<String> majors) {
			for (AreaClasfMajor a: iMajors)
				if (a.getArea().equals(areaAbbv) && majors.contains(a.getMajor())) return true;
			return false;
		}
		
		public boolean match(CurriculumClassification clasf) {
			if (clasf.getCurriculum().isMultipleMajors() && clasf.getCurriculum().getMajors().isEmpty()) return false;
			for (AreaClasfMajor a: iMajors) {
				if (a.getArea().equals(clasf.getCurriculum().getAcademicArea().getAcademicAreaAbbreviation()) && a.getClasf().equals(clasf.getAcademicClassification().getCode())) {
					if (clasf.getCurriculum().isMultipleMajors()) {
						for (PosMajor major: clasf.getCurriculum().getMajors()) {
							boolean found = false;
							for (AreaClasfMajor m: iMajors) {
								if (m.getArea().equals(a.getArea()) && m.getClasf().equals(clasf.getAcademicClassification().getCode()) && m.getMajor().equals(major.getCode())) {
									found = true; break;
								}
							}
							if (found) return true;
						}
					} else {
						if (clasf.getCurriculum().getMajors().isEmpty()) return true;
						for (PosMajor major: clasf.getCurriculum().getMajors()) {
							if (a.getMajor().equals(major.getCode())) return true;
						}
					}
				}
			}
			return false;
		}
		
		public int hashCode() {
			return Long.valueOf(getStudentId()).hashCode();
		}
		
		public boolean equals(Object o) {
			if (o == null || !(o instanceof WeightedStudentId)) return false;
			return getStudentId() == ((WeightedStudentId)o).getStudentId();
		}
		
		public String toString() {
			return String.valueOf(getStudentId());
		}
	}
	
	public static class WeightedCourseOffering {
		private transient CourseOffering iCourseOffering = null;
		private long iCourseOfferingId;
		private Long iPrimaryOfferingId;
		private float iWeight = 1.0f;
		
		public WeightedCourseOffering(CourseOffering courseOffering) {
			iCourseOffering = courseOffering;
			iCourseOfferingId = courseOffering.getUniqueId();
		}
		
		public WeightedCourseOffering(Long courseOfferingId) {
			iCourseOfferingId = courseOfferingId;
		}
		
		public WeightedCourseOffering(CourseOffering courseOffering, float weight) {
			this(courseOffering);
			iWeight = weight;
		}
		
		public WeightedCourseOffering(Long courseOfferingId, float weight) {
			this(courseOfferingId);
			iWeight = weight;
		}
		
		public Long getCourseOfferingId() { return iCourseOfferingId; }
		
		public CourseOffering getCourseOffering() { 
			if (iCourseOffering == null) iCourseOffering = CourseOfferingDAO.getInstance().get(iCourseOfferingId);
			return iCourseOffering;
		}
		
		public float getWeight() {
			return iWeight;
		}
		
		public void setPrimaryOfferingId(Long offeringId) { iPrimaryOfferingId = offeringId; }
		public Long getPrimaryOfferingId() { return iPrimaryOfferingId; }

		@Override
		public int hashCode() {
			return (int)(iCourseOfferingId ^ (iCourseOfferingId >>> 32));
		}
		
		@Override
		public boolean equals(Object o) {
			if (o == null || !(o instanceof WeightedCourseOffering)) return false;
			return getCourseOfferingId() == ((WeightedCourseOffering)o).getCourseOfferingId();
		}
		
		@Override
		public String toString() {
			return getCourseOffering().getCourseName() + (getWeight() != 1.0f ? "@" + getWeight() : "");
		}
	}
	
	public static interface ProjectionsProvider {
		public float getProjection(String areaAbbv, String clasfCode, String majorCode);
	}
}
