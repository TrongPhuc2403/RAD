package com.rad.center.model;

import java.io.Serializable;

public class TuitionFee implements Serializable {
    private static final long serialVersionUID = 1L;
    private int tuitionId;
    private int enrollmentId;
    private long amountDue;
    private String createdAt;
    private String status; // UNPAID, PARTIAL, PAID

    public TuitionFee() {}

    public TuitionFee(int tuitionId, int enrollmentId, long amountDue, String createdAt, String status) {
        this.tuitionId = tuitionId;
        this.enrollmentId = enrollmentId;
        this.amountDue = amountDue;
        this.createdAt = createdAt;
        this.status = status;
    }

    public int getTuitionId() { return tuitionId; }
    public void setTuitionId(int tuitionId) { this.tuitionId = tuitionId; }
    public int getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(int enrollmentId) { this.enrollmentId = enrollmentId; }
    public long getAmountDue() { return amountDue; }
    public void setAmountDue(long amountDue) { this.amountDue = amountDue; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

