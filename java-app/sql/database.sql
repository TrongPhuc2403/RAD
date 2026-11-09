-- ============================================================================
-- PHLINGO ACADEMY — HỆ THỐNG QUẢN LÝ TRUNG TÂM NGOẠI NGỮ / ĐÀO TẠO (v3.0)
-- Môn học: Phân tích và Thiết kế Yêu cầu (502050)
-- Nhóm thực hiện: Nguyễn Trọng Phúc – Lê Khải Hoàn – Trần Hữu Long
-- Script khởi tạo CSDL MySQL (13 bảng chuẩn theo Mục 8 — anh em chúng mình.docx)
-- ============================================================================

CREATE DATABASE IF NOT EXISTS language_center_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE language_center_db;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS password_reset_tokens;
DROP TABLE IF EXISTS verification_tokens;
DROP TABLE IF EXISTS attendance;
DROP TABLE IF EXISTS sessions;
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS tuition_fees;
DROP TABLE IF EXISTS enrollments;
DROP TABLE IF EXISTS class_schedules;
DROP TABLE IF EXISTS classes;
DROP TABLE IF EXISTS courses;
DROP TABLE IF EXISTS teachers;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS students;
SET FOREIGN_KEY_CHECKS = 1;

-- 1. Bảng students (Hồ sơ học viên - FR-02, FR-12, UC-03, UC-14)
CREATE TABLE students (
    student_id INT AUTO_INCREMENT PRIMARY KEY,
    student_code VARCHAR(20) NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    dob DATE NOT NULL,
    gender ENUM('Nam', 'Nữ', 'Khác') NOT NULL DEFAULT 'Nam',
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(100) NOT NULL,
    address VARCHAR(255) DEFAULT '',
    status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at DATE NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Bảng users (Quản lý tài khoản & phân quyền 5 Actor - FR-01, FR-11..14, BR-V3-10..14)
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20) DEFAULT '',
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role ENUM('ADMIN', 'TRAINING_STAFF', 'CASHIER', 'TEACHER', 'STUDENT') NOT NULL,
    status ENUM('ACTIVE', 'PENDING_VERIFICATION', 'LOCKED') NOT NULL DEFAULT 'ACTIVE',
    student_id INT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login VARCHAR(50) DEFAULT 'Chưa đăng nhập',
    CONSTRAINT fk_users_student FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Bảng teachers (Giảng viên - FR-05, BR-07)
CREATE TABLE teachers (
    teacher_id INT AUTO_INCREMENT PRIMARY KEY,
    teacher_code VARCHAR(20) NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    specialization VARCHAR(100) NOT NULL,
    status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Bảng courses (Khóa học - FR-03, BR-03, BR-04)
CREATE TABLE courses (
    course_id INT AUTO_INCREMENT PRIMARY KEY,
    course_code VARCHAR(20) NOT NULL UNIQUE,
    course_name VARCHAR(120) NOT NULL,
    description VARCHAR(500) DEFAULT '',
    duration VARCHAR(50) NOT NULL,
    tuition DECIMAL(12, 0) NOT NULL CHECK (tuition >= 0),
    entry_level VARCHAR(50) DEFAULT 'Mọi trình độ',
    status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Bảng classes (Lớp học - FR-04, BR-04, BR-05, BR-06)
CREATE TABLE classes (
    class_id INT AUTO_INCREMENT PRIMARY KEY,
    class_code VARCHAR(20) NOT NULL UNIQUE,
    course_id INT NOT NULL,
    teacher_id INT NOT NULL,
    capacity INT NOT NULL CHECK (capacity > 0),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    schedule_summary VARCHAR(120) NOT NULL,
    room VARCHAR(50) NOT NULL DEFAULT 'P.101',
    status ENUM('OPEN', 'ONGOING', 'CLOSED', 'CANCELLED') NOT NULL DEFAULT 'OPEN',
    CONSTRAINT fk_classes_course FOREIGN KEY (course_id) REFERENCES courses(course_id),
    CONSTRAINT fk_classes_teacher FOREIGN KEY (teacher_id) REFERENCES teachers(teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Bảng class_schedules (Lịch học chi tiết của lớp - FR-05, BR-07)
CREATE TABLE class_schedules (
    schedule_id INT AUTO_INCREMENT PRIMARY KEY,
    class_id INT NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    room VARCHAR(50) NOT NULL DEFAULT 'P.101',
    CONSTRAINT fk_schedules_class FOREIGN KEY (class_id) REFERENCES classes(class_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Bảng enrollments (Đăng ký lớp học - FR-06, FR-07, FR-08, BR-08..BR-13)
CREATE TABLE enrollments (
    enrollment_id INT AUTO_INCREMENT PRIMARY KEY,
    enrollment_code VARCHAR(20) NOT NULL UNIQUE,
    student_id INT NOT NULL,
    class_id INT NOT NULL,
    enrollment_date DATE NOT NULL,
    received_by VARCHAR(100) NOT NULL DEFAULT 'Nhân viên đào tạo',
    status ENUM('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED') NOT NULL DEFAULT 'CONFIRMED',
    cancel_reason VARCHAR(255) DEFAULT '',
    CONSTRAINT fk_enrollments_student FOREIGN KEY (student_id) REFERENCES students(student_id),
    CONSTRAINT fk_enrollments_class FOREIGN KEY (class_id) REFERENCES classes(class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Bảng tuition_fees (Nghĩa vụ học phí theo đăng ký - FR-09, BR-14, BR-15)
CREATE TABLE tuition_fees (
    tuition_id INT AUTO_INCREMENT PRIMARY KEY,
    enrollment_id INT NOT NULL UNIQUE,
    amount_due DECIMAL(12, 0) NOT NULL CHECK (amount_due >= 0),
    created_at DATE NOT NULL,
    status ENUM('UNPAID', 'PARTIAL', 'PAID') NOT NULL DEFAULT 'UNPAID',
    CONSTRAINT fk_tuition_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments(enrollment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. Bảng payments (Giao dịch thanh toán học phí - FR-10, FR-11, BR-16..BR-19)
CREATE TABLE payments (
    payment_id INT AUTO_INCREMENT PRIMARY KEY,
    payment_code VARCHAR(20) NOT NULL UNIQUE,
    tuition_id INT NOT NULL,
    amount DECIMAL(12, 0) NOT NULL CHECK (amount > 0),
    payment_date DATE NOT NULL,
    method ENUM('CASH', 'TRANSFER', 'CARD') NOT NULL DEFAULT 'CASH',
    received_by VARCHAR(100) NOT NULL DEFAULT 'Nhân viên thu ngân',
    note VARCHAR(255) DEFAULT '',
    CONSTRAINT fk_payments_tuition FOREIGN KEY (tuition_id) REFERENCES tuition_fees(tuition_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. Bảng sessions (Buổi học thực tế của lớp - FR-12, BR-20)
CREATE TABLE sessions (
    session_id INT AUTO_INCREMENT PRIMARY KEY,
    class_id INT NOT NULL,
    session_number INT NOT NULL,
    topic VARCHAR(200) NOT NULL,
    session_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status ENUM('SCHEDULED', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'SCHEDULED',
    UNIQUE KEY uk_class_session_date (class_id, session_date, start_time),
    CONSTRAINT fk_sessions_class FOREIGN KEY (class_id) REFERENCES classes(class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. Bảng attendance (Điểm danh học viên theo buổi học - FR-10, UC-10, UC-11, BR-V3-08, BR-V3-09)
CREATE TABLE attendance (
    attendance_id INT AUTO_INCREMENT PRIMARY KEY,
    session_id INT NOT NULL,
    student_id INT NOT NULL,
    status ENUM('PRESENT', 'ABSENT_EXCUSED', 'ABSENT_UNEXCUSED') NOT NULL DEFAULT 'PRESENT',
    marked_by VARCHAR(100) NOT NULL DEFAULT 'Giảng viên',
    marked_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    note VARCHAR(255) DEFAULT '',
    UNIQUE KEY uk_session_student (session_id, student_id),
    CONSTRAINT fk_attendance_session FOREIGN KEY (session_id) REFERENCES sessions(session_id),
    CONSTRAINT fk_attendance_student FOREIGN KEY (student_id) REFERENCES students(student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. Bảng verification_tokens (Token xác thực email học viên một lần, có hạn - FR-11, UC-12, UC-13, BR-V3-12)
CREATE TABLE verification_tokens (
    token_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME NOT NULL,
    consumed_at DATETIME NULL,
    CONSTRAINT fk_verif_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 13. Bảng password_reset_tokens (Token khôi phục mật khẩu một lần, có hạn - FR-14, UC-16, UC-17, BR-V3-12)
CREATE TABLE password_reset_tokens (
    token_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME NOT NULL,
    consumed_at DATETIME NULL,
    CONSTRAINT fk_reset_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- DỮ LIỆU MẪU PHỤC VỤ DEMO XUYÊN SUỐT (PHLINGO ACADEMY — v3.0)
-- ============================================================================

-- Học viên mẫu (Khởi tạo trước để liên kết khóa ngoại student_id cho tài khoản học viên)
INSERT INTO students (student_code, full_name, dob, gender, phone, email, address, status, created_at) VALUES
('HV001', 'Phạm Minh Anh', '2004-05-14', 'Nữ', '0912345001', 'minhanh.pham@gmail.com', 'Quận 7, TP.HCM', 'ACTIVE', '2026-10-25'),
('HV002', 'Nguyễn Hoàng Nam', '2003-08-21', 'Nam', '0912345002', 'hoangnam.ng@gmail.com', 'Quận 1, TP.HCM', 'ACTIVE', '2026-10-26'),
('HV003', 'Lê Thị Ngọc Bích', '2005-02-10', 'Nữ', '0912345003', 'ngocbich.le@gmail.com', 'Nhà Bè, TP.HCM', 'ACTIVE', '2026-10-27'),
('HV004', 'Trần Quốc Khánh', '2004-11-30', 'Nam', '0912345004', 'quockhanh.tran@gmail.com', 'Quận 4, TP.HCM', 'ACTIVE', '2026-10-28'),
('HV005', 'Võ Thanh Tùng', '2003-04-18', 'Nam', '0912345005', 'thanhtung.vo@gmail.com', 'Quận 8, TP.HCM', 'ACTIVE', '2026-10-29'),
('HV006', 'Đặng Thu Thảo', '2005-09-05', 'Nữ', '0912345006', 'thuthao.dang@gmail.com', 'Quận 7, TP.HCM', 'ACTIVE', '2026-10-30'),
('HV007', 'Bùi Đức Duy', '2004-01-22', 'Nam', '0912345007', 'ducduy.bui@gmail.com', 'Bình Chánh, TP.HCM', 'ACTIVE', '2026-11-01'),
('HV008', 'Hoàng Yến Nhi', '2005-07-12', 'Nữ', '0912345008', 'yennhi.hoang@gmail.com', 'Quận 5, TP.HCM', 'ACTIVE', '2026-11-02');

-- Tài khoản người dùng (mật khẩu mặc định: 123456 -> SHA-256: 8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92)
INSERT INTO users (username, email, phone, password_hash, full_name, role, status, student_id) VALUES
('admin', 'long.th@tdtu.edu.vn', '0901000001', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Trần Hữu Long (QT)', 'ADMIN', 'ACTIVE', NULL),
('daotao', 'phuc.nt@tdtu.edu.vn', '0901000002', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Nguyễn Trọng Phúc (DT)', 'TRAINING_STAFF', 'ACTIVE', NULL),
('thungan', 'hoan.lk@tdtu.edu.vn', '0901000003', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Lê Khải Hoàn (TN)', 'CASHIER', 'ACTIVE', NULL),
('giangvien', 'tuan.hm@tdtu.edu.vn', '0901234567', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'ThS. Hoàng Minh Tuấn (GV)', 'TEACHER', 'ACTIVE', NULL),
('hocvien', 'minhanh.pham@gmail.com', '0912345001', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Phạm Minh Anh (HV001)', 'STUDENT', 'ACTIVE', 1),
('hocvien02', 'hoangnam.ng@gmail.com', '0912345002', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Nguyễn Hoàng Nam (HV002)', 'STUDENT', 'ACTIVE', 2),
('hocvien_cho', 'ngocbich.le@gmail.com', '0912345003', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Lê Thị Ngọc Bích (HV003)', 'STUDENT', 'PENDING_VERIFICATION', 3),
('giangvien02', 'huong.ntl@tdtu-center.edu.vn', '0902345678', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'ThS. Nguyễn Thị Lan Hương (GV002)', 'TEACHER', 'ACTIVE', NULL),
('locked_user', 'locked@tdtu.edu.vn', '0901000999', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Tài khoản bị khóa', 'TRAINING_STAFF', 'LOCKED', NULL);

-- Giảng viên
INSERT INTO teachers (teacher_code, full_name, email, phone, specialization, status) VALUES
('GV001', 'ThS. Hoàng Minh Tuấn', 'tuan.hm@tdtu-center.edu.vn', '0901234567', 'TOEIC & Business English', 'ACTIVE'),
('GV002', 'ThS. Nguyễn Thị Lan Hương', 'huong.ntl@tdtu-center.edu.vn', '0902345678', 'IELTS Academic 7.5+', 'ACTIVE'),
('GV003', 'Mr. David Miller', 'david.m@tdtu-center.edu.vn', '0903456789', 'Giao tiếp Phản xạ Quốc tế', 'ACTIVE'),
('GV004', 'ThS. Trần Quốc Bảo', 'bao.tq@tdtu-center.edu.vn', '0904567890', 'TOEIC 650+ & Ngữ pháp', 'ACTIVE');

-- Khóa học (Đúng ví dụ Chương 4.2 & Chương 9.2)
INSERT INTO courses (course_code, course_name, description, duration, tuition, entry_level, status) VALUES
('KH-TOEIC450', 'TOEIC 450+ (Nền tảng & Luyện đề)', 'Khóa luyện thi TOEIC 2 kỹ năng Listening & Reading mục tiêu 450-550+', '3 tháng (36 buổi)', 3000000, 'TOEIC 250+', 'ACTIVE'),
('KH-IELTSFD', 'IELTS Foundation (4.0 - 5.0)', 'Xây dựng nền tảng 4 kỹ năng học thuật chuẩn Cambridge IELTS', '4 tháng (48 buổi)', 4000000, 'Pre-Intermediate', 'ACTIVE'),
('KH-GT01', 'Tiếng Anh Giao Tiếp Cơ Bản', 'Phát âm chuẩn IPA và phản xạ giao tiếp tình huống hàng ngày', '2.5 tháng (30 buổi)', 2500000, 'Mất gốc / Cơ bản', 'ACTIVE'),
('KH-TOEIC650', 'TOEIC 650+ Intensive', 'Luyện giải đề chuyên sâu và chiến thuật đạt 650-750+ điểm', '3 tháng (36 buổi)', 3800000, 'TOEIC 450+', 'ACTIVE');

-- Lớp học (Bao gồm TOEIC01, TOEIC02 như trong phân tích + 1 lớp đầy sĩ số để test TC-04)
INSERT INTO classes (class_code, course_id, teacher_id, capacity, start_date, end_date, schedule_summary, room, status) VALUES
('TOEIC01', 1, 1, 20, '2026-11-02', '2027-01-30', 'Thứ 2, 4, 6 (18:00 - 20:00)', 'P.201', 'OPEN'),
('TOEIC02', 1, 4, 25, '2026-11-03', '2027-01-31', 'Thứ 3, 5, 7 (18:00 - 20:00)', 'P.202', 'OPEN'),
('IELTS01', 2, 2, 15, '2026-11-02', '2027-02-28', 'Thứ 2, 4, 6 (19:30 - 21:30)', 'P.301', 'OPEN'),
('GT-VIP01', 3, 3, 3, '2026-11-03', '2027-01-15', 'Thứ 3, 5, 7 (17:30 - 19:00)', 'P.105 (VIP)', 'OPEN'),
('TOEIC-OLD', 4, 1, 20, '2026-08-01', '2026-10-30', 'Thứ 2, 4, 6 (18:00 - 20:00)', 'P.201', 'CLOSED');

-- Lịch học chi tiết
INSERT INTO class_schedules (class_id, day_of_week, start_time, end_time, room) VALUES
(1, 'Thứ 2, 4, 6', '18:00:00', '20:00:00', 'P.201'),
(2, 'Thứ 3, 5, 7', '18:00:00', '20:00:00', 'P.202'),
(3, 'Thứ 2, 4, 6', '19:30:00', '21:30:00', 'P.301'),
(4, 'Thứ 3, 5, 7', '17:30:00', '19:00:00', 'P.105 (VIP)');

-- Token xác thực email mẫu & Token khôi phục mật khẩu mẫu (băm SHA-256 theo NFR-02, BR-V3-12)
INSERT INTO verification_tokens (user_id, token_hash, expires_at, consumed_at) VALUES
(7, 'f5b0c9f08a6d4b31c120d1a8d7e92a11c3e2b0d4a5f6e7d8c9b0a1f2e3d4c5b6', '2026-12-31 23:59:59', NULL);

INSERT INTO password_reset_tokens (user_id, token_hash, expires_at, consumed_at) VALUES
(5, 'a1b2c3d4e5f60718293a4b5c6d7e8f90123456789abcdef0123456789abcdef0', '2026-12-31 23:59:59', NULL);

-- Đăng ký mẫu (TOEIC01 có 4 HV, IELTS01 có 2 HV, GT-VIP01 có 3/3 HV đã đầy sĩ số để test TC-04)
INSERT INTO enrollments (enrollment_code, student_id, class_id, enrollment_date, received_by, status, cancel_reason) VALUES
('DK001', 1, 1, '2026-10-28', 'Nguyễn Trọng Phúc', 'CONFIRMED', ''),
('DK002', 2, 1, '2026-10-29', 'Nguyễn Trọng Phúc', 'CONFIRMED', ''),
('DK003', 3, 1, '2026-10-30', 'Nguyễn Trọng Phúc', 'CONFIRMED', ''),
('DK004', 4, 1, '2026-10-31', 'Nguyễn Trọng Phúc', 'CONFIRMED', ''),
('DK005', 1, 3, '2026-10-28', 'Nguyễn Trọng Phúc', 'CONFIRMED', ''),
('DK006', 5, 3, '2026-10-30', 'Nguyễn Trọng Phúc', 'CONFIRMED', ''),
('DK007', 2, 4, '2026-10-29', 'Nguyễn Trọng Phúc', 'CONFIRMED', ''),
('DK008', 3, 4, '2026-10-30', 'Nguyễn Trọng Phúc', 'CONFIRMED', ''),
('DK009', 6, 4, '2026-10-31', 'Nguyễn Trọng Phúc', 'CONFIRMED', '');

-- Nghĩa vụ học phí tương ứng từng đăng ký (FR-09)
INSERT INTO tuition_fees (enrollment_id, amount_due, created_at, status) VALUES
(1, 3000000, '2026-10-28', 'PAID'),
(2, 3000000, '2026-10-29', 'PARTIAL'),
(3, 3000000, '2026-10-30', 'UNPAID'),
(4, 3000000, '2026-10-31', 'PARTIAL'),
(5, 4000000, '2026-10-28', 'PARTIAL'),
(6, 4000000, '2026-10-30', 'UNPAID'),
(7, 2500000, '2026-10-29', 'PAID'),
(8, 2500000, '2026-10-30', 'PARTIAL'),
(9, 2500000, '2026-10-31', 'UNPAID');

-- Giao dịch thanh toán mẫu (Minh họa thanh toán nhiều lần như Chương 4.4: 4.000.000 -> đóng 1.500.000 + 2.000.000 = còn nợ 500.000)
INSERT INTO payments (payment_code, tuition_id, amount, payment_date, method, received_by, note) VALUES
('PT001', 1, 3000000, '2026-10-28', 'TRANSFER', 'Lê Khải Hoàn', 'Đóng đủ 100% học phí TOEIC01'),
('PT002', 2, 1500000, '2026-10-29', 'CASH', 'Lê Khải Hoàn', 'Đóng đợt 1 lớp TOEIC01'),
('PT003', 4, 2000000, '2026-10-31', 'TRANSFER', 'Lê Khải Hoàn', 'Đóng đợt 1 lớp TOEIC01'),
('PT004', 5, 1500000, '2026-10-28', 'CASH', 'Lê Khải Hoàn', 'Thanh toán lần 1 - IELTS Foundation'),
('PT005', 5, 2000000, '2026-11-02', 'TRANSFER', 'Lê Khải Hoàn', 'Thanh toán lần 2 - IELTS Foundation (còn nợ 500k)'),
('PT006', 7, 2500000, '2026-10-29', 'CARD', 'Lê Khải Hoàn', 'Đóng đủ lớp GT-VIP01'),
('PT007', 8, 1000000, '2026-10-30', 'CASH', 'Lê Khải Hoàn', 'Đóng đợt 1 lớp GT-VIP01');

-- Buổi học của lớp TOEIC01 & IELTS01
INSERT INTO sessions (class_id, session_number, topic, session_date, start_time, end_time, status) VALUES
(1, 1, 'Buổi 1: Orientation & Listening Part 1 (Photographs)', '2026-11-02', '18:00:00', '20:00:00', 'COMPLETED'),
(1, 2, 'Buổi 2: Reading Part 5 (Incomplete Sentences - Từ loại)', '2026-11-04', '18:00:00', '20:00:00', 'COMPLETED'),
(1, 3, 'Buổi 3: Listening Part 2 (Question-Response)', '2026-11-06', '18:00:00', '20:00:00', 'SCHEDULED'),
(2, 1, 'Buổi 1: Nhập môn TOEIC 450+ & Chiến thuật Part 1', '2026-11-03', '18:00:00', '20:00:00', 'COMPLETED'),
(3, 1, 'Buổi 1: IELTS Listening Section 1 & Speaking Part 1', '2026-11-02', '19:30:00', '21:30:00', 'COMPLETED'),
(3, 2, 'Buổi 2: IELTS Reading Skimming & Scanning', '2026-11-04', '19:30:00', '21:30:00', 'SCHEDULED');

-- Dữ liệu điểm danh mẫu cho Buổi 1 & Buổi 2 lớp TOEIC01
INSERT INTO attendance (session_id, student_id, status, marked_by, marked_at, note) VALUES
(1, 1, 'PRESENT', 'ThS. Hoàng Minh Tuấn', '2026-11-02 18:15:00', 'Đúng giờ'),
(1, 2, 'PRESENT', 'ThS. Hoàng Minh Tuấn', '2026-11-02 18:15:00', 'Tích cực phát biểu'),
(1, 3, 'ABSENT_EXCUSED', 'ThS. Hoàng Minh Tuấn', '2026-11-02 18:15:00', 'Xin phép bận lịch thi ở trường'),
(1, 4, 'PRESENT', 'ThS. Hoàng Minh Tuấn', '2026-11-02 18:15:00', ''),
(2, 1, 'PRESENT', 'ThS. Hoàng Minh Tuấn', '2026-11-04 18:10:00', ''),
(2, 2, 'ABSENT_UNEXCUSED', 'ThS. Hoàng Minh Tuấn', '2026-11-04 18:10:00', 'Vắng không báo trước'),
(2, 3, 'PRESENT', 'ThS. Hoàng Minh Tuấn', '2026-11-04 18:10:00', 'Đã xem lại bài buổi 1'),
(2, 4, 'PRESENT', 'ThS. Hoàng Minh Tuấn', '2026-11-04 18:10:00', '');

