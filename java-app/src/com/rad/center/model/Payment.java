package com.rad.center.model;

import java.io.Serializable;

public class Payment implements Serializable {
    private static final long serialVersionUID = 1L;
    private int paymentId;
    private String paymentCode;
    private int tuitionId;
    private long amount;
    private String paymentDate;
    private String method; // CASH, TRANSFER, CARD
    private String receivedBy;
    private String note;

    public Payment() {}

    public Payment(int paymentId, String paymentCode, int tuitionId, long amount,
                   String paymentDate, String method, String receivedBy, String note) {
        this.paymentId = paymentId;
        this.paymentCode = paymentCode;
        this.tuitionId = tuitionId;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.method = method;
        this.receivedBy = receivedBy;
        this.note = note;
    }

    public int getPaymentId() { return paymentId; }
    public void setPaymentId(int paymentId) { this.paymentId = paymentId; }
    public String getPaymentCode() { return paymentCode; }
    public void setPaymentCode(String paymentCode) { this.paymentCode = paymentCode; }
    public int getTuitionId() { return tuitionId; }
    public void setTuitionId(int tuitionId) { this.tuitionId = tuitionId; }
    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }
    public String getPaymentDate() { return paymentDate; }
    public void setPaymentDate(String paymentDate) { this.paymentDate = paymentDate; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getReceivedBy() { return receivedBy; }
    public void setReceivedBy(String receivedBy) { this.receivedBy = receivedBy; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}

