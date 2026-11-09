package com.rad.center.model;

import java.io.Serializable;

public class Session implements Serializable {
    private static final long serialVersionUID = 1L;
    private int sessionId;
    private int classId;
    private int sessionNumber;
    private String topic;
    private String sessionDate;
    private String startTime;
    private String endTime;
    private String status; // SCHEDULED, COMPLETED, CANCELLED

    public Session() {}

    public Session(int sessionId, int classId, int sessionNumber, String topic,
                   String sessionDate, String startTime, String endTime, String status) {
        this.sessionId = sessionId;
        this.classId = classId;
        this.sessionNumber = sessionNumber;
        this.topic = topic;
        this.sessionDate = sessionDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    public int getSessionId() { return sessionId; }
    public void setSessionId(int sessionId) { this.sessionId = sessionId; }
    public int getClassId() { return classId; }
    public void setClassId(int classId) { this.classId = classId; }
    public int getSessionNumber() { return sessionNumber; }
    public void setSessionNumber(int sessionNumber) { this.sessionNumber = sessionNumber; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getSessionDate() { return sessionDate; }
    public void setSessionDate(String sessionDate) { this.sessionDate = sessionDate; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "Buổi " + sessionNumber + " (" + sessionDate + " " + startTime + ") - " + topic;
    }
}

