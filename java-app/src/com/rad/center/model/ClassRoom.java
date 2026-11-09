package com.rad.center.model;

import java.io.Serializable;

public class ClassRoom implements Serializable {
    private static final long serialVersionUID = 1L;
    private int classId;
    private String classCode;
    private int courseId;
    private int teacherId;
    private int capacity;
    private String startDate;
    private String endDate;
    private String scheduleSummary;
    private String room;
    private String status; // OPEN, ONGOING, CLOSED, CANCELLED

    public ClassRoom() {}

    public ClassRoom(int classId, String classCode, int courseId, int teacherId, int capacity,
                     String startDate, String endDate, String scheduleSummary, String room, String status) {
        this.classId = classId;
        this.classCode = classCode;
        this.courseId = courseId;
        this.teacherId = teacherId;
        this.capacity = capacity;
        this.startDate = startDate;
        this.endDate = endDate;
        this.scheduleSummary = scheduleSummary;
        this.room = room;
        this.status = status;
    }

    public int getClassId() { return classId; }
    public void setClassId(int classId) { this.classId = classId; }
    public String getClassCode() { return classCode; }
    public void setClassCode(String classCode) { this.classCode = classCode; }
    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }
    public int getTeacherId() { return teacherId; }
    public void setTeacherId(int teacherId) { this.teacherId = teacherId; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public String getScheduleSummary() { return scheduleSummary; }
    public void setScheduleSummary(String scheduleSummary) { this.scheduleSummary = scheduleSummary; }
    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return classCode + " (" + scheduleSummary + " - " + room + ")";
    }
}

