package com.ogrenci.model;

import java.time.LocalDateTime;

/** Ders Record modeli. */
public record Course(
    int id,
    String courseCode,    // CS101 gibi benzersiz kod
    String courseName,
    int teacherId,        // Foreign key → teachers.id
    String teacherName,   // JOIN ile gelen öğretmen adı (DB'de yok)
    int credits,
    String description,
    int capacity,
    boolean isActive,
    LocalDateTime createdAt
) {
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private int id = 0;
        private String courseCode;
        private String courseName;
        private int teacherId = 0;
        private String teacherName = "";
        private int credits = 3;
        private String description = "";
        private int capacity = 30;
        private boolean isActive = true;
        private LocalDateTime createdAt = LocalDateTime.now();

        public Builder id(int v)              { this.id = v; return this; }
        public Builder courseCode(String v)   { this.courseCode = v; return this; }
        public Builder courseName(String v)   { this.courseName = v; return this; }
        public Builder teacherId(int v)       { this.teacherId = v; return this; }
        public Builder teacherName(String v)  { this.teacherName = v; return this; }
        public Builder credits(int v)         { this.credits = v; return this; }
        public Builder description(String v)  { this.description = v; return this; }
        public Builder capacity(int v)        { this.capacity = v; return this; }
        public Builder isActive(boolean v)    { this.isActive = v; return this; }
        public Builder createdAt(LocalDateTime v) { this.createdAt = v; return this; }

        public Course build() {
            return new Course(id, courseCode, courseName, teacherId, teacherName,
                              credits, description, capacity, isActive, createdAt);
        }
    }
}
