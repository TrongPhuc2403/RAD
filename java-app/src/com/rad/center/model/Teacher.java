package com.rad.center.model;

import java.io.Serializable;

public class Teacher implements Serializable {
    private static final long serialVersionUID = 1L;
    private int teacherId;
    private String teacherCode;
    private String fullName;
    private String email;
    private String phone;
    private String specialization;
    private String status;

    public Teacher() {}

    public Teacher(int teacherId, String teacherCode, String fullName, String email, String phone, String specialization, String status) {
        this.teacherId = teacherId;
        this.teacherCode = teacherCode;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.specialization = specialization;
        this.status = status;
    }

    public int getTeacherId() { return teacherId; }
    public void setTeacherId(int teacherId) { this.teacherId = teacherId; }
    public String getTeacherCode() { return teacherCode; }
    public void setTeacherCode(String teacherCode) { this.teacherCode = teacherCode; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return teacherCode + " - " + fullName + " (" + specialization + ")";
    }
}

