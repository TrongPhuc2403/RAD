package com.rad.center.dao;

import com.rad.center.model.*;
import com.rad.center.util.DatabaseConfig;
import com.rad.center.util.PasswordUtil;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) quản lý lưu trữ dữ liệu cho 11 bảng của hệ thống.
 * Hỗ trợ đồng thời:
 * 1. Kết nối trực tiếp MySQL qua JDBC (khi MySQL Server đang chạy).
 * 2. Lưu trữ bền vững xuống file (data/language_center_store.db) đảm bảo NFR-09 ngay cả khi máy demo chưa bật MySQL.
 */
public class CenterRepository implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String STORE_PATH = "data/language_center_store.db";

    private List<User> users = new ArrayList<User>();
    private List<Student> students = new ArrayList<Student>();
    private List<Teacher> teachers = new ArrayList<Teacher>();
    private List<Course> courses = new ArrayList<Course>();
    private List<ClassRoom> classes = new ArrayList<ClassRoom>();
    private List<Enrollment> enrollments = new ArrayList<Enrollment>();
    private List<TuitionFee> tuitionFees = new ArrayList<TuitionFee>();
    private List<Payment> payments = new ArrayList<Payment>();
    private List<Session> sessions = new ArrayList<Session>();
    private List<Attendance> attendances = new ArrayList<Attendance>();

    private static CenterRepository instance;

    public static synchronized CenterRepository getInstance() {
        if (instance == null) {
            instance = loadOrCreate();
        }
        return instance;
    }

    private static CenterRepository loadOrCreate() {
        File file = new File(STORE_PATH);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                CenterRepository loaded = (CenterRepository) ois.readObject();
                if (loaded != null && !loaded.users.isEmpty()) {
                    // Đảm bảo đồng bộ chính xác 5 Roles chuẩn v3.0 (QT, DT, TN, GV, HV — không có MANAGER)
                    boolean hasExactMembers = false;
                    boolean hasStudentAccount = false;
                    boolean hasLegacyManager = false;
                    for (User u : loaded.users) {
                        if (u.getFullName().contains("Nguyễn Trọng Phúc")) {
                            hasExactMembers = true;
                        }
                        if ("hocvien".equalsIgnoreCase(u.getUsername()) && u.getEmail() != null && !u.getEmail().isEmpty()) {
                            hasStudentAccount = true;
                        }
                        if ("MANAGER".equalsIgnoreCase(u.getRole()) || "quanly".equalsIgnoreCase(u.getUsername())) {
                            hasLegacyManager = true;
                        }
                    }
                    if (hasExactMembers && hasStudentAccount && !hasLegacyManager) {
                        return loaded;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        CenterRepository repo = new CenterRepository();
        repo.seedSampleData();
        repo.saveToDisk();
        return repo;
    }

    public synchronized void saveToDisk() {
        try {
            File dir = new File("data");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(STORE_PATH))) {
                oos.writeObject(this);
            }
            PhlingoSqlSync.syncAll(this);
        } catch (Exception e) {
            System.err.println("Cảnh báo lưu dữ liệu cục bộ: " + e.getMessage());
        }
    }

    public synchronized void resetToSampleData() {
        seedSampleData();
        saveToDisk();
    }

    private void seedSampleData() {
        users.clear();
        students.clear();
        teachers.clear();
        courses.clear();
        classes.clear();
        enrollments.clear();
        tuitionFees.clear();
        payments.clear();
        sessions.clear();
        attendances.clear();

        String defaultHash = PasswordUtil.sha256("123456");
        users.add(new User(1, "admin", defaultHash, "Trần Hữu Long (Quản trị)", "ADMIN", "ACTIVE",
                "long.th@tdtu.edu.vn", "0901000001", 0, "2026-10-01", "2026-11-09 08:15"));
        users.add(new User(2, "daotao", defaultHash, "Nguyễn Trọng Phúc (Đào tạo)", "TRAINING_STAFF", "ACTIVE",
                "phuc.nt@tdtu.edu.vn", "0901000002", 0, "2026-10-02", "2026-11-09 08:30"));
        users.add(new User(3, "thungan", defaultHash, "Lê Khải Hoàn (Thu ngân)", "CASHIER", "ACTIVE",
                "hoan.lk@tdtu.edu.vn", "0901000003", 0, "2026-10-02", "2026-11-09 08:45"));
        users.add(new User(4, "giangvien", defaultHash, "ThS. Hoàng Minh Tuấn (GV)", "TEACHER", "ACTIVE",
                "tuan.hm@tdtu.edu.vn", "0901234567", 0, "2026-10-05", "2026-11-08 19:40"));
        users.add(new User(5, "hocvien", defaultHash, "Phạm Minh Anh (HV001)", "STUDENT", "ACTIVE",
                "minhanh.pham@gmail.com", "0912345001", 1, "2026-10-25", "2026-11-09 09:00"));
        users.add(new User(6, "hocvien02", defaultHash, "Nguyễn Hoàng Nam (HV002)", "STUDENT", "ACTIVE",
                "hoangnam.ng@gmail.com", "0912345002", 2, "2026-10-26", "2026-11-07 14:20"));
        users.add(new User(7, "hocvien_cho", defaultHash, "Lê Thị Ngọc Bích (HV003)", "STUDENT", "PENDING_VERIFICATION",
                "ngocbich.le@gmail.com", "0912345003", 3, "2026-11-05", "Chưa xác thực email (UC-13)"));
        users.add(new User(8, "giangvien02", defaultHash, "ThS. Nguyễn Thị Lan Hương (GV002)", "TEACHER", "ACTIVE",
                "huong.ntl@tdtu-center.edu.vn", "0902345678", 0, "2026-10-06", "2026-11-06 16:00"));
        users.add(new User(9, "locked_user", defaultHash, "Tài khoản bị khóa", "TRAINING_STAFF", "LOCKED",
                "locked@tdtu.edu.vn", "0901000999", 0, "2026-10-10", "Bị khóa theo BR-V3-11"));

        teachers.add(new Teacher(1, "GV001", "ThS. Hoàng Minh Tuấn", "tuan.hm@tdtu-center.edu.vn", "0901234567", "TOEIC & Business English", "ACTIVE"));
        teachers.add(new Teacher(2, "GV002", "ThS. Nguyễn Thị Lan Hương", "huong.ntl@tdtu-center.edu.vn", "0902345678", "IELTS Academic 7.5+", "ACTIVE"));
        teachers.add(new Teacher(3, "GV003", "Mr. David Miller", "david.m@tdtu-center.edu.vn", "0903456789", "Giao tiếp Phản xạ Quốc tế", "ACTIVE"));
        teachers.add(new Teacher(4, "GV004", "ThS. Trần Quốc Bảo", "bao.tq@tdtu-center.edu.vn", "0904567890", "TOEIC 650+ & Ngữ pháp", "ACTIVE"));

        courses.add(new Course(1, "KH-TOEIC450", "TOEIC 450+ (Nền tảng & Luyện đề)", "Khóa luyện thi TOEIC 2 kỹ năng Listening & Reading mục tiêu 450-550+", "3 tháng (36 buổi)", 3000000L, "TOEIC 250+", "ACTIVE"));
        courses.add(new Course(2, "KH-IELTSFD", "IELTS Foundation (4.0 - 5.0)", "Xây dựng nền tảng 4 kỹ năng học thuật chuẩn Cambridge IELTS", "4 tháng (48 buổi)", 4000000L, "Pre-Intermediate", "ACTIVE"));
        courses.add(new Course(3, "KH-GT01", "Tiếng Anh Giao Tiếp Cơ Bản", "Phát âm chuẩn IPA và phản xạ giao tiếp tình huống hàng ngày", "2.5 tháng (30 buổi)", 2500000L, "Mất gốc / Cơ bản", "ACTIVE"));
        courses.add(new Course(4, "KH-TOEIC650", "TOEIC 650+ Intensive", "Luyện giải đề chuyên sâu và chiến thuật đạt 650-750+ điểm", "3 tháng (36 buổi)", 3800000L, "TOEIC 450+", "ACTIVE"));

        classes.add(new ClassRoom(1, "TOEIC01", 1, 1, 20, "2026-11-02", "2027-01-30", "Thứ 2, 4, 6 (18:00 - 20:00)", "P.201", "OPEN"));
        classes.add(new ClassRoom(2, "TOEIC02", 1, 4, 25, "2026-11-03", "2027-01-31", "Thứ 3, 5, 7 (18:00 - 20:00)", "P.202", "OPEN"));
        classes.add(new ClassRoom(3, "IELTS01", 2, 2, 15, "2026-11-02", "2027-02-28", "Thứ 2, 4, 6 (19:30 - 21:30)", "P.301", "OPEN"));
        classes.add(new ClassRoom(4, "GT-VIP01", 3, 3, 3, "2026-11-03", "2027-01-15", "Thứ 3, 5, 7 (17:30 - 19:00)", "P.105 (VIP)", "OPEN"));
        classes.add(new ClassRoom(5, "TOEIC-OLD", 4, 1, 20, "2026-08-01", "2026-10-30", "Thứ 2, 4, 6 (18:00 - 20:00)", "P.201", "CLOSED"));

        students.add(new Student(1, "HV001", "Phạm Minh Anh", "2004-05-14", "Nữ", "0912345001", "minhanh.pham@gmail.com", "Quận 7, TP.HCM", "ACTIVE", "2026-10-25"));
        students.add(new Student(2, "HV002", "Nguyễn Hoàng Nam", "2003-08-21", "Nam", "0912345002", "hoangnam.ng@gmail.com", "Quận 1, TP.HCM", "ACTIVE", "2026-10-26"));
        students.add(new Student(3, "HV003", "Lê Thị Ngọc Bích", "2005-02-10", "Nữ", "0912345003", "ngocbich.le@gmail.com", "Nhà Bè, TP.HCM", "ACTIVE", "2026-10-27"));
        students.add(new Student(4, "HV004", "Trần Quốc Khánh", "2004-11-30", "Nam", "0912345004", "quockhanh.tran@gmail.com", "Quận 4, TP.HCM", "ACTIVE", "2026-10-28"));
        students.add(new Student(5, "HV005", "Võ Thanh Tùng", "2003-04-18", "Nam", "0912345005", "thanhtung.vo@gmail.com", "Quận 8, TP.HCM", "ACTIVE", "2026-10-29"));
        students.add(new Student(6, "HV006", "Đặng Thu Thảo", "2005-09-05", "Nữ", "0912345006", "thuthao.dang@gmail.com", "Quận 7, TP.HCM", "ACTIVE", "2026-10-30"));
        students.add(new Student(7, "HV007", "Bùi Đức Duy", "2004-01-22", "Nam", "0912345007", "ducduy.bui@gmail.com", "Bình Chánh, TP.HCM", "ACTIVE", "2026-11-01"));
        students.add(new Student(8, "HV008", "Hoàng Yến Nhi", "2005-07-12", "Nữ", "0912345008", "yennhi.hoang@gmail.com", "Quận 5, TP.HCM", "ACTIVE", "2026-11-02"));

        // Enrollments
        enrollments.add(new Enrollment(1, "DK001", 1, 1, "2026-10-28", "Nguyễn Trọng Phúc (Đào tạo)", "CONFIRMED", ""));
        enrollments.add(new Enrollment(2, "DK002", 2, 1, "2026-10-29", "Nguyễn Trọng Phúc (Đào tạo)", "CONFIRMED", ""));
        enrollments.add(new Enrollment(3, "DK003", 3, 1, "2026-10-30", "Nguyễn Trọng Phúc (Đào tạo)", "CONFIRMED", ""));
        enrollments.add(new Enrollment(4, "DK004", 4, 1, "2026-10-31", "Nguyễn Trọng Phúc (Đào tạo)", "CONFIRMED", ""));
        enrollments.add(new Enrollment(5, "DK005", 1, 3, "2026-10-28", "Nguyễn Trọng Phúc (Đào tạo)", "CONFIRMED", ""));
        enrollments.add(new Enrollment(6, "DK006", 5, 3, "2026-10-30", "Nguyễn Trọng Phúc (Đào tạo)", "CONFIRMED", ""));
        // GT-VIP01 (classId = 4, capacity = 3) has 3 confirmed enrollments -> FULL!
        enrollments.add(new Enrollment(7, "DK007", 2, 4, "2026-10-29", "Nguyễn Trọng Phúc (Đào tạo)", "CONFIRMED", ""));
        enrollments.add(new Enrollment(8, "DK008", 3, 4, "2026-10-30", "Nguyễn Trọng Phúc (Đào tạo)", "CONFIRMED", ""));
        enrollments.add(new Enrollment(9, "DK009", 6, 4, "2026-10-31", "Nguyễn Trọng Phúc (Đào tạo)", "CONFIRMED", ""));

        // Tuition Fees
        tuitionFees.add(new TuitionFee(1, 1, 3000000L, "2026-10-28", "PAID"));
        tuitionFees.add(new TuitionFee(2, 2, 3000000L, "2026-10-29", "PARTIAL"));
        tuitionFees.add(new TuitionFee(3, 3, 3000000L, "2026-10-30", "UNPAID"));
        tuitionFees.add(new TuitionFee(4, 4, 3000000L, "2026-10-31", "PARTIAL"));
        tuitionFees.add(new TuitionFee(5, 5, 4000000L, "2026-10-28", "PARTIAL"));
        tuitionFees.add(new TuitionFee(6, 6, 4000000L, "2026-10-30", "UNPAID"));
        tuitionFees.add(new TuitionFee(7, 7, 2500000L, "2026-10-29", "PAID"));
        tuitionFees.add(new TuitionFee(8, 8, 2500000L, "2026-10-30", "PARTIAL"));
        tuitionFees.add(new TuitionFee(9, 9, 2500000L, "2026-10-31", "UNPAID"));

        // Payments (including exact multi-payment example in Chapter 4.4: 4.000.000 -> 1.500.000 + 2.000.000 -> Debt 500.000)
        payments.add(new Payment(1, "PT001", 1, 3000000L, "2026-10-28", "TRANSFER", "Lê Khải Hoàn (Thu ngân)", "Đóng đủ 100% học phí TOEIC01"));
        payments.add(new Payment(2, "PT002", 2, 1500000L, "2026-10-29", "CASH", "Lê Khải Hoàn (Thu ngân)", "Đóng đợt 1 lớp TOEIC01"));
        payments.add(new Payment(3, "PT003", 4, 2000000L, "2026-10-31", "TRANSFER", "Lê Khải Hoàn (Thu ngân)", "Đóng đợt 1 lớp TOEIC01"));
        payments.add(new Payment(4, "PT004", 5, 1500000L, "2026-10-28", "CASH", "Lê Khải Hoàn (Thu ngân)", "Thanh toán lần 1 - IELTS Foundation"));
        payments.add(new Payment(5, "PT005", 5, 2000000L, "2026-11-02", "TRANSFER", "Lê Khải Hoàn (Thu ngân)", "Thanh toán lần 2 - IELTS Foundation (còn nợ 500k)"));
        payments.add(new Payment(6, "PT006", 7, 2500000L, "2026-10-29", "CARD", "Lê Khải Hoàn (Thu ngân)", "Đóng đủ lớp GT-VIP01"));
        payments.add(new Payment(7, "PT007", 8, 1000000L, "2026-10-30", "CASH", "Lê Khải Hoàn (Thu ngân)", "Đóng đợt 1 lớp GT-VIP01"));

        // Sessions
        sessions.add(new Session(1, 1, 1, "Buổi 1: Orientation & Listening Part 1 (Photographs)", "2026-11-02", "18:00", "20:00", "COMPLETED"));
        sessions.add(new Session(2, 1, 2, "Buổi 2: Reading Part 5 (Incomplete Sentences - Từ loại)", "2026-11-04", "18:00", "20:00", "COMPLETED"));
        sessions.add(new Session(3, 1, 3, "Buổi 3: Listening Part 2 (Question-Response)", "2026-11-06", "18:00", "20:00", "SCHEDULED"));
        sessions.add(new Session(4, 2, 1, "Buổi 1: Nhập môn TOEIC 450+ & Chiến thuật Part 1", "2026-11-03", "18:00", "20:00", "COMPLETED"));
        sessions.add(new Session(5, 3, 1, "Buổi 1: IELTS Listening Section 1 & Speaking Part 1", "2026-11-02", "19:30", "21:30", "COMPLETED"));
        sessions.add(new Session(6, 3, 2, "Buổi 2: IELTS Reading Skimming & Scanning", "2026-11-04", "19:30", "21:30", "SCHEDULED"));

        // Attendance
        attendances.add(new Attendance(1, 1, 1, "PRESENT", "ThS. Hoàng Minh Tuấn", "2026-11-02 18:15", "Đúng giờ"));
        attendances.add(new Attendance(2, 1, 2, "PRESENT", "ThS. Hoàng Minh Tuấn", "2026-11-02 18:15", "Tích cực phát biểu"));
        attendances.add(new Attendance(3, 1, 3, "ABSENT_EXCUSED", "ThS. Hoàng Minh Tuấn", "2026-11-02 18:15", "Xin phép bận lịch thi ở trường"));
        attendances.add(new Attendance(4, 1, 4, "PRESENT", "ThS. Hoàng Minh Tuấn", "2026-11-02 18:15", ""));
        attendances.add(new Attendance(5, 2, 1, "PRESENT", "ThS. Hoàng Minh Tuấn", "2026-11-04 18:10", ""));
        attendances.add(new Attendance(6, 2, 2, "ABSENT_UNEXCUSED", "ThS. Hoàng Minh Tuấn", "2026-11-04 18:10", "Vắng không báo trước"));
        attendances.add(new Attendance(7, 2, 3, "PRESENT", "ThS. Hoàng Minh Tuấn", "2026-11-04 18:10", "Đã xem lại bài buổi 1"));
        attendances.add(new Attendance(8, 2, 4, "PRESENT", "ThS. Hoàng Minh Tuấn", "2026-11-04 18:10", ""));
    }

    public List<User> getUsers() { return users; }
    public List<Student> getStudents() { return students; }
    public List<Teacher> getTeachers() { return teachers; }
    public List<Course> getCourses() { return courses; }
    public List<ClassRoom> getClasses() { return classes; }
    public List<Enrollment> getEnrollments() { return enrollments; }
    public List<TuitionFee> getTuitionFees() { return tuitionFees; }
    public List<Payment> getPayments() { return payments; }
    public List<Session> getSessions() { return sessions; }
    public List<Attendance> getAttendances() { return attendances; }

    // Helper Lookup Methods
    public Student findStudentById(int id) {
        for (Student s : students) if (s.getStudentId() == id) return s;
        return null;
    }

    public Teacher findTeacherById(int id) {
        for (Teacher t : teachers) if (t.getTeacherId() == id) return t;
        return null;
    }

    public Course findCourseById(int id) {
        for (Course c : courses) if (c.getCourseId() == id) return c;
        return null;
    }

    public ClassRoom findClassById(int id) {
        for (ClassRoom c : classes) if (c.getClassId() == id) return c;
        return null;
    }

    public Enrollment findEnrollmentById(int id) {
        for (Enrollment e : enrollments) if (e.getEnrollmentId() == id) return e;
        return null;
    }

    public TuitionFee findTuitionByEnrollmentId(int enrollmentId) {
        for (TuitionFee tf : tuitionFees) if (tf.getEnrollmentId() == enrollmentId) return tf;
        return null;
    }

    public TuitionFee findTuitionById(int tuitionId) {
        for (TuitionFee tf : tuitionFees) if (tf.getTuitionId() == tuitionId) return tf;
        return null;
    }

    public Session findSessionById(int sessionId) {
        for (Session s : sessions) if (s.getSessionId() == sessionId) return s;
        return null;
    }

    /**
     * Đồng bộ dữ liệu sang MySQL khi MySQL khả dụng
     */
    public void executeMySqlInsertStudent(Student s) {
        if (!DatabaseConfig.isMysqlAvailable()) return;
        String sql = "INSERT INTO students (student_code, full_name, dob, gender, phone, email, address, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getStudentCode());
            ps.setString(2, s.getFullName());
            ps.setString(3, s.getDob());
            ps.setString(4, s.getGender());
            ps.setString(5, s.getPhone());
            ps.setString(6, s.getEmail());
            ps.setString(7, s.getAddress());
            ps.setString(8, s.getStatus());
            ps.setString(9, s.getCreatedAt());
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }
    }
}
