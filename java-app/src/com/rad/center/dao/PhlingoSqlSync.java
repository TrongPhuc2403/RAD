package com.rad.center.dao;

import com.rad.center.model.*;
import com.rad.center.util.DatabaseConfig;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PhlingoSqlSync — Đồng bộ dữ liệu từ CenterRepository sang schema 13 bảng `phlingo_academy`
 * (PHLingoAcademy.sql) qua MySQL/MariaDB JDBC mỗi khi lưu dữ liệu.
 */
public final class PhlingoSqlSync {

    private PhlingoSqlSync() {}

    public static void syncAll(CenterRepository repo) {
        if (!DatabaseConfig.isMysqlAvailable()) return;
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Map<Integer, Long> userIdToSqlUserId = syncUsers(conn, repo.getUsers());
                syncTeachers(conn, repo.getTeachers(), userIdToSqlUserId);
                syncStudents(conn, repo.getStudents(), repo.getUsers(), userIdToSqlUserId);
                syncCourses(conn, repo.getCourses());
                syncClasses(conn, repo.getClasses());
                syncEnrollments(conn, repo.getEnrollments());
                syncTuitionFees(conn, repo.getTuitionFees());
                syncPayments(conn, repo.getPayments());
                syncSessions(conn, repo.getSessions());
                syncAttendances(conn, repo.getAttendances());
                conn.commit();
            } catch (Exception ex) {
                try { conn.rollback(); } catch (Exception ignored) {}
                System.err.println("[PhlingoSqlSync] Cảnh báo đồng bộ SQL: " + ex.getMessage());
            }
        } catch (Exception ex) {
            System.err.println("[PhlingoSqlSync] Không thể mở kết nối SQL: " + ex.getMessage());
        }
    }

    private static Map<Integer, Long> syncUsers(Connection conn, List<User> users) throws Exception {
        Map<Integer, Long> map = new HashMap<Integer, Long>();
        String upsert = "INSERT INTO users (email, password_hash, role, status, created_at) " +
                "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP) " +
                "ON DUPLICATE KEY UPDATE password_hash=VALUES(password_hash), role=VALUES(role), status=VALUES(status), user_id=LAST_INSERT_ID(user_id)";
        for (User u : users) {
            String email = (u.getEmail() != null && !u.getEmail().trim().isEmpty())
                    ? u.getEmail().trim().toLowerCase()
                    : (u.getUsername() + "@phlingo.local");
            try (PreparedStatement ps = conn.prepareStatement(upsert, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, email);
                ps.setString(2, u.getPasswordHash());
                ps.setString(3, u.getRole());
                ps.setString(4, u.getStatus());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) map.put(u.getUserId(), keys.getLong(1));
                }
            }
        }
        return map;
    }

    private static void syncTeachers(Connection conn, List<Teacher> teachers, Map<Integer, Long> userMap) throws Exception {
        String ensureUser = "INSERT INTO users (email, password_hash, role, status, created_at) VALUES (?, ?, 'TEACHER', 'ACTIVE', CURRENT_TIMESTAMP) " +
                "ON DUPLICATE KEY UPDATE user_id=LAST_INSERT_ID(user_id)";
        String upsertTeacher = "INSERT INTO teachers (teacher_id, full_name, user_id, specialization, qualification) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE full_name=VALUES(full_name), specialization=VALUES(specialization), qualification=VALUES(qualification)";
        for (Teacher t : teachers) {
            long uid = 0;
            String email = (t.getEmail() != null && !t.getEmail().trim().isEmpty())
                    ? t.getEmail().trim().toLowerCase()
                    : ("teacher" + t.getTeacherId() + "@phlingo.local");
            try (PreparedStatement lookup = conn.prepareStatement("SELECT user_id FROM users WHERE email=?")) {
                lookup.setString(1, email);
                try (ResultSet rs = lookup.executeQuery()) {
                    if (rs.next()) uid = rs.getLong(1);
                }
            }
            if (uid == 0) {
                try (PreparedStatement ps = conn.prepareStatement(ensureUser, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, email);
                    ps.setString(2, "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92");
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) uid = keys.getLong(1);
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(upsertTeacher)) {
                ps.setLong(1, t.getTeacherId());
                ps.setString(2, t.getFullName());
                ps.setLong(3, uid);
                ps.setString(4, t.getSpecialization());
                ps.setString(5, t.getTeacherCode());
                ps.executeUpdate();
            }
        }
    }

    private static void syncStudents(Connection conn, List<Student> students, List<User> users, Map<Integer, Long> userMap) throws Exception {
        String ensureUser = "INSERT INTO users (email, password_hash, role, status, created_at) VALUES (?, ?, 'STUDENT', 'ACTIVE', CURRENT_TIMESTAMP) " +
                "ON DUPLICATE KEY UPDATE user_id=LAST_INSERT_ID(user_id)";
        String upsertStu = "INSERT INTO students (student_id, full_name, email, phone, user_id, date_of_birth, gender, address) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE full_name=VALUES(full_name), email=VALUES(email), phone=VALUES(phone), date_of_birth=VALUES(date_of_birth), gender=VALUES(gender), address=VALUES(address)";
        for (Student s : students) {
            long uid = 0;
            for (User u : users) {
                if (u.getStudentId() == s.getStudentId() && userMap.containsKey(u.getUserId())) {
                    uid = userMap.get(u.getUserId());
                    break;
                }
            }
            String email = (s.getEmail() != null && !s.getEmail().trim().isEmpty())
                    ? s.getEmail().trim().toLowerCase()
                    : ("student" + s.getStudentId() + "@phlingo.local");
            if (uid == 0) {
                try (PreparedStatement lookup = conn.prepareStatement("SELECT user_id FROM users WHERE email=?")) {
                    lookup.setString(1, email);
                    try (ResultSet rs = lookup.executeQuery()) {
                        if (rs.next()) uid = rs.getLong(1);
                    }
                }
            }
            if (uid == 0) {
                try (PreparedStatement ps = conn.prepareStatement(ensureUser, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, email);
                    ps.setString(2, "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92");
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) uid = keys.getLong(1);
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(upsertStu)) {
                ps.setLong(1, s.getStudentId());
                ps.setString(2, s.getFullName());
                ps.setString(3, email);
                ps.setString(4, s.getPhone());
                ps.setLong(5, uid);
                String dob = s.getDob();
                if (dob != null && dob.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    ps.setDate(6, java.sql.Date.valueOf(dob));
                } else {
                    ps.setNull(6, java.sql.Types.DATE);
                }
                ps.setString(7, s.getGender());
                ps.setString(8, s.getAddress());
                ps.executeUpdate();
            }
        }
    }

    private static void syncCourses(Connection conn, List<Course> courses) throws Exception {
        String sql = "INSERT INTO courses (course_id, code, name, tuition_listed, status, description, level, duration_hours) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE code=VALUES(code), name=VALUES(name), tuition_listed=VALUES(tuition_listed), status=VALUES(status), description=VALUES(description), level=VALUES(level), duration_hours=VALUES(duration_hours)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Course c : courses) {
                ps.setLong(1, c.getCourseId());
                ps.setString(2, c.getCourseCode());
                ps.setString(3, c.getCourseName());
                ps.setBigDecimal(4, BigDecimal.valueOf(Math.max(0, c.getTuition())));
                ps.setString(5, c.getStatus());
                ps.setString(6, c.getDescription());
                ps.setString(7, c.getEntryLevel());
                int hours = 60;
                if (c.getDuration() != null) {
                    String digits = c.getDuration().replaceAll("[^0-9]", "");
                    if (!digits.isEmpty()) {
                        try { hours = Math.max(1, Integer.parseInt(digits)); } catch (Exception ignored) {}
                    }
                }
                ps.setInt(8, hours);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void syncClasses(Connection conn, List<ClassRoom> classes) throws Exception {
        String sql = "INSERT INTO classes (class_id, course_id, teacher_id, code, name, capacity, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE course_id=VALUES(course_id), teacher_id=VALUES(teacher_id), code=VALUES(code), name=VALUES(name), capacity=VALUES(capacity), status=VALUES(status)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (ClassRoom cl : classes) {
                ps.setLong(1, cl.getClassId());
                ps.setLong(2, cl.getCourseId());
                if (cl.getTeacherId() > 0) ps.setLong(3, cl.getTeacherId());
                else ps.setNull(3, java.sql.Types.BIGINT);
                ps.setString(4, cl.getClassCode());
                ps.setString(5, cl.getClassCode() + " (" + cl.getRoom() + ")");
                ps.setInt(6, Math.max(1, cl.getCapacity()));
                ps.setString(7, cl.getStatus());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void syncEnrollments(Connection conn, List<Enrollment> enrollments) throws Exception {
        String sql = "INSERT INTO enrollments (enrollment_id, student_id, class_id, status, enrolled_at) " +
                "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP) " +
                "ON DUPLICATE KEY UPDATE student_id=VALUES(student_id), class_id=VALUES(class_id), status=VALUES(status)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Enrollment e : enrollments) {
                ps.setLong(1, e.getEnrollmentId());
                ps.setLong(2, e.getStudentId());
                ps.setLong(3, e.getClassId());
                ps.setString(4, e.getStatus());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void syncTuitionFees(Connection conn, List<TuitionFee> fees) throws Exception {
        String sql = "INSERT INTO tuition_fees (tuition_id, enrollment_id, amount_due, status) " +
                "VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE amount_due=VALUES(amount_due), status=VALUES(status)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (TuitionFee tf : fees) {
                ps.setLong(1, tf.getTuitionId());
                ps.setLong(2, tf.getEnrollmentId());
                ps.setBigDecimal(3, BigDecimal.valueOf(Math.max(0, tf.getAmountDue())));
                ps.setString(4, tf.getStatus());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void syncPayments(Connection conn, List<Payment> payments) throws Exception {
        String sql = "INSERT INTO payments (payment_id, tuition_id, amount, method, request_id, receipt_code, paid_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP) " +
                "ON DUPLICATE KEY UPDATE amount=VALUES(amount), method=VALUES(method)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Payment p : payments) {
                if (p.getAmount() <= 0) continue;
                ps.setLong(1, p.getPaymentId());
                ps.setLong(2, p.getTuitionId());
                ps.setBigDecimal(3, BigDecimal.valueOf(p.getAmount()));
                String m = p.getMethod() == null ? "CASH" : p.getMethod().toUpperCase();
                if ("TRANSFER".equals(m)) m = "BANK_TRANSFER";
                ps.setString(4, m);
                ps.setString(5, "web-pay-" + p.getPaymentId());
                ps.setString(6, p.getPaymentCode() != null ? p.getPaymentCode() : ("PT" + p.getPaymentId()));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void syncSessions(Connection conn, List<Session> sessions) throws Exception {
        String sql = "INSERT INTO sessions (session_id, class_id, session_date, start_time, end_time) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE class_id=VALUES(class_id), session_date=VALUES(session_date), start_time=VALUES(start_time), end_time=VALUES(end_time)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Session s : sessions) {
                ps.setLong(1, s.getSessionId());
                ps.setLong(2, s.getClassId());
                String d = s.getSessionDate();
                ps.setDate(3, (d != null && d.matches("\\d{4}-\\d{2}-\\d{2}")) ? java.sql.Date.valueOf(d) : new java.sql.Date(System.currentTimeMillis()));
                ps.setTime(4, parseTime(s.getStartTime(), "18:00:00"));
                ps.setTime(5, parseTime(s.getEndTime(), "20:00:00"));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void syncAttendances(Connection conn, List<Attendance> attendances) throws Exception {
        String sql = "INSERT INTO attendance (attendance_id, session_id, student_id, status, marked_at) " +
                "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP) " +
                "ON DUPLICATE KEY UPDATE status=VALUES(status), marked_at=CURRENT_TIMESTAMP";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Attendance a : attendances) {
                ps.setLong(1, a.getAttendanceId());
                ps.setLong(2, a.getSessionId());
                ps.setLong(3, a.getStudentId());
                ps.setString(4, a.getStatus());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static java.sql.Time parseTime(String raw, String fallback) {
        String t = (raw == null || raw.trim().isEmpty()) ? fallback : raw.trim();
        if (t.length() == 5) t += ":00";
        try {
            return java.sql.Time.valueOf(t);
        } catch (Exception ex) {
            return java.sql.Time.valueOf(fallback);
        }
    }
}
