package com.rad.center.model;

import java.io.Serializable;

public class Course implements Serializable {
    private static final long serialVersionUID = 1L;
    private int courseId;
    private String courseCode;
    private String courseName;
    private String description;
    private String duration;
    private long tuition;
    private String entryLevel;
    private String status; // ACTIVE, INACTIVE

    public Course() {}

    public Course(int courseId, String courseCode, String courseName, String description,
                  String duration, long tuition, String entryLevel, String status) {
        this.courseId = courseId;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.description = description;
        this.duration = duration;
        this.tuition = tuition;
        this.entryLevel = entryLevel;
        this.status = status;
    }

    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    public long getTuition() { return tuition; }
    public void setTuition(long tuition) { this.tuition = tuition; }
    public String getEntryLevel() { return entryLevel; }
    public void setEntryLevel(String entryLevel) { this.entryLevel = entryLevel; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return courseCode + " - " + courseName;
    }
}

