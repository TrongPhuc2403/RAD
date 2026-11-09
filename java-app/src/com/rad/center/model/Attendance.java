package com.rad.center.model;

import java.io.Serializable;

public class Attendance implements Serializable {
    private static final long serialVersionUID = 1L;
    private int attendanceId;
    private int sessionId;
    private int studentId;
    private String status; // PRESENT, ABSENT_EXCUSED, ABSENT_UNEXCUSED
    private String markedBy;
    private String markedAt;
    private String note;

    public Attendance() {}

    public Attendance(int attendanceId, int sessionId, int studentId, String status,
                      String markedBy, String markedAt, String note) {
        this.attendanceId = attendanceId;
        this.sessionId = sessionId;
        this.studentId = studentId;
        this.status = status;
        this.markedBy = markedBy;
        this.markedAt = markedAt;
        this.note = note;
    }

    public int getAttendanceId() { return attendanceId; }
    public void setAttendanceId(int attendanceId) { this.attendanceId = attendanceId; }
    public int getSessionId() { return sessionId; }
    public void setSessionId(int sessionId) { this.sessionId = sessionId; }
    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMarkedBy() { return markedBy; }
    public void setMarkedBy(String markedBy) { this.markedBy = markedBy; }
    public String getMarkedAt() { return markedAt; }
    public void setMarkedAt(String markedAt) { this.markedAt = markedAt; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}

