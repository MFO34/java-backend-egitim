package com.ogrenci.exception;

/** Ders bulunamadığında fırlatılır. */
public class CourseNotFoundException extends RuntimeException {
    private final String courseCode;

    public CourseNotFoundException(String courseCode) {
        super("Ders bulunamadı: " + courseCode);
        this.courseCode = courseCode;
    }

    public String getCourseCode() { return courseCode; }
}
