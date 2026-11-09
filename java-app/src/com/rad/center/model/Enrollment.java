package com.rad.center.model;

import java.io.Serializable;

public class Enrollment implements Serializable {
    private static final long serialVersionUID = 1L;
    private int enrollmentId;
    private String enrollmentCode;
    private int studentId;
    private int classId;
    private String enrollmentDate;
    private String receivedBy;
    private String status; // PENDING, CONFIRMED, CANCELLED, COMPLETED
    private String cancelReason;

    public Enrollment() {}

    public Enrollment(int enrollmentId, String enrollmentCode, int studentId, int classId,
                      String enrollmentDate, String receivedBy, String status, String cancelReason) {
        this.enrollmentId = enrollmentId;
        this.enrollmentCode = enrollmentCode;
        this.studentId = studentId;
        this.classId = classId;
        this.enrollmentDate = enrollmentDate;
        this.receivedBy = receivedBy;
        this.status = status;
        this.cancelReason = cancelReason;
    }

    public int getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(int enrollmentId) { this.enrollmentId = enrollmentId; }
    public String getEnrollmentCode() { return enrollmentCode; }
    public void setEnrollmentCode(String enrollmentCode) { this.enrollmentCode = enrollmentCode; }
    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }
    public int getClassId() { return classId; }
    public void setClassId(int classId) { this.classId = classId; }
    public String getEnrollmentDate() { return enrollmentDate; }
    public void setEnrollmentDate(String enrollmentDate) { this.enrollmentDate = enrollmentDate; }
    public String getReceivedBy() { return receivedBy; }
    public void setReceivedBy(String receivedBy) { this.receivedBy = receivedBy; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
}

