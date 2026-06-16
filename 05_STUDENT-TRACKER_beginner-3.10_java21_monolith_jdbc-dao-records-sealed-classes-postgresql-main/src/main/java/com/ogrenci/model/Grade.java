package com.ogrenci.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Not kaydı Record modeli. */
public record Grade(
    int id,
    int enrollmentId,
    String gradeType,      // midterm, final, quiz, homework, project
    BigDecimal score,      // 0.00 - 100.00
    LocalDateTime gradedAt,
    String comment,
    // JOIN ile gelen ek bilgiler (sadece raporlarda dolu olur)
    String studentName,
    String courseCode
) {
    // Harf notunu hesapla (AA, BA, BB, CB, CC, DC, DD, FF)
    public String letterGrade() {
        double s = score.doubleValue();
        if (s >= 90) return "AA";
        if (s >= 85) return "BA";
        if (s >= 80) return "BB";
        if (s >= 75) return "CB";
        if (s >= 70) return "CC";
        if (s >= 60) return "DC";
        if (s >= 50) return "DD";
        return "FF";
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private int id = 0;
        private int enrollmentId;
        private String gradeType;
        private BigDecimal score;
        private LocalDateTime gradedAt = LocalDateTime.now();
        private String comment = null;
        private String studentName = "";
        private String courseCode = "";

        public Builder id(int v)                  { this.id = v; return this; }
        public Builder enrollmentId(int v)        { this.enrollmentId = v; return this; }
        public Builder gradeType(String v)        { this.gradeType = v; return this; }
        public Builder score(BigDecimal v)        { this.score = v; return this; }
        public Builder gradedAt(LocalDateTime v)  { this.gradedAt = v; return this; }
        public Builder comment(String v)          { this.comment = v; return this; }
        public Builder studentName(String v)      { this.studentName = v; return this; }
        public Builder courseCode(String v)       { this.courseCode = v; return this; }

        public Grade build() {
            return new Grade(id, enrollmentId, gradeType, score,
                             gradedAt, comment, studentName, courseCode);
        }
    }
}
