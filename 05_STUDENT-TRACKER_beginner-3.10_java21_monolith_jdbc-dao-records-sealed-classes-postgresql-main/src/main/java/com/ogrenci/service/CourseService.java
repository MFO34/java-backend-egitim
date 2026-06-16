package com.ogrenci.service;

import com.ogrenci.dao.CourseDAO;
import com.ogrenci.dao.EnrollmentDAO;
import com.ogrenci.dao.TeacherDAO;
import com.ogrenci.exception.CourseNotFoundException;
import com.ogrenci.model.Course;
import com.ogrenci.model.Enrollment;
import com.ogrenci.model.Teacher;

import java.util.List;
import java.util.Optional;

/** Ders ve enrollment iş mantığı. */
public class CourseService {

    private final CourseDAO courseDAO;
    private final TeacherDAO teacherDAO;
    private final EnrollmentDAO enrollmentDAO;

    public CourseService(CourseDAO courseDAO, TeacherDAO teacherDAO, EnrollmentDAO enrollmentDAO) {
        this.courseDAO = courseDAO;
        this.teacherDAO = teacherDAO;
        this.enrollmentDAO = enrollmentDAO;
    }

    public int addCourse(Course course) {
        return courseDAO.insert(course);
    }

    public Course getCourseByCode(String code) {
        return courseDAO.findByCode(code)
            .orElseThrow(() -> new CourseNotFoundException(code));
    }

    public List<Course> getAllCourses() {
        return courseDAO.findAll();
    }

    public List<Course> getCoursesByTeacher(int teacherId) {
        return courseDAO.findByTeacher(teacherId);
    }

    /** Öğrenciyi derse kaydet — kapasite kontrolüyle. */
    public int enrollStudent(int studentId, int courseId) {
        // Kapasite kontrolü
        Optional<Course> courseOpt = courseDAO.findById(courseId);
        if (courseOpt.isEmpty()) {
            throw new CourseNotFoundException("ID=" + courseId);
        }

        Course course = courseOpt.get();
        int currentCount = enrollmentDAO.findByCourse(courseId).size();

        if (currentCount >= course.capacity()) {
            throw new IllegalStateException(
                "Ders dolu! Kapasite: " + course.capacity() + ", Mevcut: " + currentCount);
        }

        return enrollmentDAO.enroll(studentId, courseId);
    }

    /** Ders kaydını iptal et. */
    public boolean dropStudent(int studentId, int courseId) {
        return enrollmentDAO.drop(studentId, courseId);
    }

    /** Derse kayıtlı öğrencileri getir. */
    public List<Enrollment> getStudentsInCourse(int courseId) {
        return enrollmentDAO.findByCourse(courseId);
    }

    /** Öğrencinin kayıtlı olduğu dersler. */
    public List<Enrollment> getStudentEnrollments(int studentId) {
        return enrollmentDAO.findByStudent(studentId);
    }

    /** Öğretmen ekle. */
    public int addTeacher(Teacher teacher) {
        return teacherDAO.insert(teacher);
    }

    public List<Teacher> getAllTeachers() {
        return teacherDAO.findAll();
    }

    /** Öğretmen ders listesini yazdır. */
    public void printTeacherCourses() {
        var teachers = teacherDAO.findAll();

        for (Teacher t : teachers) {
            System.out.println("\n" + t.fullName() + " (" + t.department() + ")");
            var courses = courseDAO.findByTeacher(t.id());
            if (courses.isEmpty()) {
                System.out.println("  Ders atanmamış.");
            } else {
                courses.forEach(c ->
                    System.out.printf("  %s — %s (%d kredi)%n",
                        c.courseCode(), c.courseName(), c.credits())
                );
            }
        }
    }
}
