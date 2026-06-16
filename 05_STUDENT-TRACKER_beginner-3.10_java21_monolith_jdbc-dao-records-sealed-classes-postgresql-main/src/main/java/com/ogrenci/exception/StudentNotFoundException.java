package com.ogrenci.exception;

/** Öğrenci bulunamadığında fırlatılır. */
public class StudentNotFoundException extends RuntimeException {
    private final int studentId;

    public StudentNotFoundException(int studentId) {
        super("Öğrenci bulunamadı: ID = " + studentId);
        this.studentId = studentId;
    }

    public StudentNotFoundException(String message) {
        super(message);
        this.studentId = -1;
    }

    public int getStudentId() { return studentId; }
}
