package com.ogrenci.model;

import java.time.LocalDateTime;

/** Öğrenci-Ders kaydı. */
public record Enrollment(
    int id,
    int studentId,
    int courseId,
    String studentName,   // JOIN ile gelen
    String courseName,    // JOIN ile gelen
    String courseCode,    // JOIN ile gelen
    LocalDateTime enrolledAt,
    String status         // active, dropped, completed
) {
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private int id = 0;
        private int studentId;
        private int courseId;
        private String studentName = "";
        private String courseName = "";
        private String courseCode = "";
        private LocalDateTime enrolledAt = LocalDateTime.now();
        private String status = "active";

        public Builder id(int v)               { this.id = v; return this; }
        public Builder studentId(int v)        { this.studentId = v; return this; }
        public Builder courseId(int v)         { this.courseId = v; return this; }
        public Builder studentName(String v)   { this.studentName = v; return this; }
        public Builder courseName(String v)    { this.courseName = v; return this; }
        public Builder courseCode(String v)    { this.courseCode = v; return this; }
        public Builder enrolledAt(LocalDateTime v) { this.enrolledAt = v; return this; }
        public Builder status(String v)        { this.status = v; return this; }

        public Enrollment build() {
            return new Enrollment(id, studentId, courseId, studentName,
                                  courseName, courseCode, enrolledAt, status);
        }
    }
}
