package com.rad.center.model;

import java.io.Serializable;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private int userId;
    private String username;
    private String passwordHash;
    private String fullName;
    private String role; // 5 Roles chuẩn v3.0: ADMIN (QT), TRAINING_STAFF (DT), CASHIER (TN), TEACHER (GV), STUDENT (HV)
    private String status; // ACTIVE, PENDING_VERIFICATION, LOCKED
    private String email;
    private String phone;
    private int studentId; // FK nullable (0 if staff/admin, >0 if linked to Student)
    private String createdAt;
    private String lastLogin;

    public User() {}

    public User(int userId, String username, String passwordHash, String fullName, String role, String status) {
        this(userId, username, passwordHash, fullName, role, status,
                username + "@tdtu.edu.vn", "090100000" + userId, 0, "2026-10-15", "2026-11-06 08:30");
    }

    public User(int userId, String username, String passwordHash, String fullName, String role, String status,
                String email, String phone, int studentId, String createdAt, String lastLogin) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
        this.status = status;
        this.email = email != null ? email : "";
        this.phone = phone != null ? phone : "";
        this.studentId = studentId;
        this.createdAt = createdAt != null ? createdAt : "2026-10-15";
        this.lastLogin = lastLogin != null ? lastLogin : "Chưa đăng nhập";
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getEmail() { return email != null ? email : ""; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone != null ? phone : ""; }
    public void setPhone(String phone) { this.phone = phone; }
    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }
    public String getCreatedAt() { return createdAt != null ? createdAt : "2026-10-15"; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getLastLogin() { return lastLogin != null ? lastLogin : "Chưa đăng nhập"; }
    public void setLastLogin(String lastLogin) { this.lastLogin = lastLogin; }

    public String getRoleDisplay() {
        if ("ADMIN".equals(role)) return "Quản trị viên (QT)";
        if ("TRAINING_STAFF".equals(role)) return "Nhân viên đào tạo (DT)";
        if ("CASHIER".equals(role)) return "Nhân viên thu ngân (TN)";
        if ("TEACHER".equals(role)) return "Giảng viên (GV)";
        if ("STUDENT".equals(role)) return "Học viên (HV)";
        return role;
    }
}

