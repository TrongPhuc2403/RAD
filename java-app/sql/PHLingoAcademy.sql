-- ============================================================================
-- PHLingo Academy  - Database schema (improved constraints)
-- Nguon: So do ERD 13 bang do nguoi dung cung cap
-- He quan tri: MySQL 8.0.16+ hoac MariaDB 10.4+ (XAMPP)
--
-- Quy uoc chuyen doi:
--   * BIGINT(19) -> BIGINT; INTEGER(10/11) -> INT (display width khong anh huong dung luong).
--   * Khoa chinh so nguyen dung AUTO_INCREMENT de thuan tien them ban ghi.
--   * Cot co ky hieu N tren ERD -> NULL; cac cot con lai -> NOT NULL.
--   * Them CHECK theo yeu cau bo sung cua nhom; khong dung ON DELETE CASCADE.
--   * Bang attendance co unique index 'attendance' (session_id, student_id)
--     theo phan Index Specification da duoc kiem tra.
--
-- DUNG CHO LAN IMPORT DAU TIEN. Khong DROP bang hay xoa du lieu.
-- CREATE TABLE IF NOT EXISTS KHONG nang cap bang neu bang da ton tai.
-- Tuong thich MySQL CHECK: MySQL 8.0.16+; MariaDB hien dai trong XAMPP.
-- ============================================================================

CREATE DATABASE IF NOT EXISTS `phlingo_academy`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `phlingo_academy`;

-- 01. users
CREATE TABLE IF NOT EXISTS `users` (
    `user_id` BIGINT NOT NULL AUTO_INCREMENT,
    `email` VARCHAR(255) NOT NULL,
    `password_hash` VARCHAR(255) NOT NULL,
    `role` VARCHAR(30) NOT NULL,
    `status` VARCHAR(30) NOT NULL,
    `created_at` TIMESTAMP NOT NULL,
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uq_users_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 02. students
CREATE TABLE IF NOT EXISTS `students` (
    `student_id` BIGINT NOT NULL AUTO_INCREMENT,
    `full_name` VARCHAR(150) NOT NULL,
    `email` VARCHAR(255) NOT NULL,
    `phone` VARCHAR(20) NULL,
    `user_id` BIGINT NOT NULL,
    `date_of_birth` DATE NULL,
    `gender` VARCHAR(10) NULL,
    `address` VARCHAR(255) NULL,
    PRIMARY KEY (`student_id`),
    UNIQUE KEY `uq_students_user_id` (`user_id`),
    CONSTRAINT `fk_students_users`
        FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 03. teachers
CREATE TABLE IF NOT EXISTS `teachers` (
    `teacher_id` BIGINT NOT NULL AUTO_INCREMENT,
    `full_name` VARCHAR(150) NOT NULL,
    `user_id` BIGINT NOT NULL,
    `specialization` VARCHAR(150) NULL,
    `qualification` VARCHAR(150) NULL,
    PRIMARY KEY (`teacher_id`),
    UNIQUE KEY `uq_teachers_user_id` (`user_id`),
    CONSTRAINT `fk_teachers_users`
        FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 04. courses
CREATE TABLE IF NOT EXISTS `courses` (
    `course_id` BIGINT NOT NULL AUTO_INCREMENT,
    `code` VARCHAR(30) NOT NULL,
    `name` VARCHAR(150) NOT NULL,
    `tuition_listed` DECIMAL(14,2) NOT NULL,
    `status` VARCHAR(30) NOT NULL,
    `description` VARCHAR(1000) NULL,
    `level` VARCHAR(50) NULL,
    `duration_hours` INT NULL,
    PRIMARY KEY (`course_id`),
    UNIQUE KEY `uq_courses_code` (`code`),
    CONSTRAINT `chk_courses_tuition_nonnegative` CHECK (`tuition_listed` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 05. classes
CREATE TABLE IF NOT EXISTS `classes` (
    `class_id` BIGINT NOT NULL AUTO_INCREMENT,
    `course_id` BIGINT NOT NULL,
    `teacher_id` BIGINT NULL, -- lop chua phan cong giao vien
    `code` VARCHAR(30) NOT NULL,
    `name` VARCHAR(150) NOT NULL,
    `capacity` INT NOT NULL,
    `status` VARCHAR(30) NOT NULL,
    PRIMARY KEY (`class_id`),
    UNIQUE KEY `uq_classes_code` (`code`),
    CONSTRAINT `chk_classes_capacity_positive` CHECK (`capacity` > 0),
    CONSTRAINT `fk_classes_courses`
        FOREIGN KEY (`course_id`) REFERENCES `courses` (`course_id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_classes_teachers`
        FOREIGN KEY (`teacher_id`) REFERENCES `teachers` (`teacher_id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 06. class_schedules
CREATE TABLE IF NOT EXISTS `class_schedules` (
    `schedule_id` BIGINT NOT NULL AUTO_INCREMENT,
    `class_id` BIGINT NOT NULL,
    `day_of_week` INT NOT NULL,
    `start_time` TIME NOT NULL,
    `end_time` TIME NOT NULL,
    PRIMARY KEY (`schedule_id`),
    CONSTRAINT `chk_class_schedules_time_order` CHECK (`start_time` < `end_time`),
    CONSTRAINT `fk_class_schedules_classes`
        FOREIGN KEY (`class_id`) REFERENCES `classes` (`class_id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 07. enrollments
-- Khong tao UNIQUE(student_id, class_id): rang buoc chong dang ky trung
-- theo trang thai (vi du CONFIRMED) can xu ly o lop nghiep vu/transaction.
CREATE TABLE IF NOT EXISTS `enrollments` (
    `enrollment_id` BIGINT NOT NULL AUTO_INCREMENT,
    `student_id` BIGINT NOT NULL,
    `class_id` BIGINT NOT NULL,
    `status` VARCHAR(30) NOT NULL,
    `enrolled_at` TIMESTAMP NOT NULL,
    PRIMARY KEY (`enrollment_id`),
    CONSTRAINT `fk_enrollments_students`
        FOREIGN KEY (`student_id`) REFERENCES `students` (`student_id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_enrollments_classes`
        FOREIGN KEY (`class_id`) REFERENCES `classes` (`class_id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 08. tuition_fees (moi enrollment co toi da mot khoan hoc phi)
CREATE TABLE IF NOT EXISTS `tuition_fees` (
    `tuition_id` BIGINT NOT NULL AUTO_INCREMENT,
    `enrollment_id` BIGINT NOT NULL,
    `amount_due` DECIMAL(14,2) NOT NULL,
    `status` VARCHAR(30) NOT NULL,
    PRIMARY KEY (`tuition_id`),
    CONSTRAINT `chk_tuition_fees_amount_nonnegative` CHECK (`amount_due` >= 0),
    UNIQUE KEY `uq_tuition_fees_enrollment_id` (`enrollment_id`),
    CONSTRAINT `fk_tuition_fees_enrollments`
        FOREIGN KEY (`enrollment_id`) REFERENCES `enrollments` (`enrollment_id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 09. payments
CREATE TABLE IF NOT EXISTS `payments` (
    `payment_id` BIGINT NOT NULL AUTO_INCREMENT,
    `tuition_id` BIGINT NOT NULL,
    `amount` DECIMAL(14,2) NOT NULL,
    `method` VARCHAR(30) NOT NULL,
    `request_id` VARCHAR(100) NOT NULL,
    `receipt_code` VARCHAR(50) NOT NULL,
    `paid_at` TIMESTAMP NOT NULL,
    PRIMARY KEY (`payment_id`),
    CONSTRAINT `chk_payments_amount_positive` CHECK (`amount` > 0),
    UNIQUE KEY `uq_payments_request_id` (`request_id`),
    UNIQUE KEY `uq_payments_receipt_code` (`receipt_code`),
    CONSTRAINT `fk_payments_tuition_fees`
        FOREIGN KEY (`tuition_id`) REFERENCES `tuition_fees` (`tuition_id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. sessions
CREATE TABLE IF NOT EXISTS `sessions` (
    `session_id` BIGINT NOT NULL AUTO_INCREMENT,
    `class_id` BIGINT NOT NULL,
    `session_date` DATE NOT NULL,
    `start_time` TIME NOT NULL,
    `end_time` TIME NOT NULL,
    PRIMARY KEY (`session_id`),
    CONSTRAINT `chk_sessions_time_order` CHECK (`start_time` < `end_time`),
    CONSTRAINT `fk_sessions_classes`
        FOREIGN KEY (`class_id`) REFERENCES `classes` (`class_id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. attendance
CREATE TABLE IF NOT EXISTS `attendance` (
    `attendance_id` BIGINT NOT NULL AUTO_INCREMENT,
    `session_id` BIGINT NOT NULL,
    `student_id` BIGINT NOT NULL,
    `status` VARCHAR(30) NOT NULL,
    `marked_at` TIMESTAMP NULL,
    PRIMARY KEY (`attendance_id`),
    UNIQUE KEY `attendance` (`session_id`, `student_id`),
    CONSTRAINT `fk_attendance_sessions`
        FOREIGN KEY (`session_id`) REFERENCES `sessions` (`session_id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_attendance_students`
        FOREIGN KEY (`student_id`) REFERENCES `students` (`student_id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. verification_tokens
-- token: giu ten cot & VARCHAR(255) nhu ERD, nhung chi chap nhan
-- SHA-256 hex gom dung 64 ky tu chu thuong cua token bi mat;
-- token goc chi gui qua email, khong luu truc tiep vao CSDL.
CREATE TABLE IF NOT EXISTS `verification_tokens` (
    `token_id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `token` VARCHAR(255) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT 'SHA-256 hex lowercase (64 characters), never raw token',
    `expires_at` TIMESTAMP NOT NULL,
    `used_at` TIMESTAMP NULL,
    PRIMARY KEY (`token_id`),
    UNIQUE KEY `uq_verification_tokens_token` (`token`),
    CONSTRAINT `chk_verification_token_sha256_format`
        CHECK (CHAR_LENGTH(`token`) = 64 AND `token` REGEXP '^[0-9a-f]{64}$'),
    CONSTRAINT `fk_verification_tokens_users`
        FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 13. password_reset_tokens
-- token: cung quy tac SHA-256 hex 64 ky tu nhu verification_tokens.
CREATE TABLE IF NOT EXISTS `password_reset_tokens` (
    `reset_id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `token` VARCHAR(255) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT 'SHA-256 hex lowercase (64 characters), never raw token',
    `expires_at` TIMESTAMP NOT NULL,
    `used_at` TIMESTAMP NULL,
    PRIMARY KEY (`reset_id`),
    UNIQUE KEY `uq_password_reset_tokens_token` (`token`),
    CONSTRAINT `chk_password_reset_token_sha256_format`
        CHECK (CHAR_LENGTH(`token`) = 64 AND `token` REGEXP '^[0-9a-f]{64}$'),
    CONSTRAINT `fk_password_reset_tokens_users`
        FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Kiem tra sau khi import:
-- USE phlingo_academy;
-- SHOW TABLES;
-- SHOW CREATE TABLE attendance;
-- SELECT table_name, column_name, referenced_table_name, referenced_column_name
-- FROM information_schema.key_column_usage
-- WHERE table_schema = 'phlingo_academy'
--   AND referenced_table_name IS NOT NULL
-- ORDER BY table_name, column_name;

-- Luu y logic ngoai DB:
-- (1) Sinh token ngau nhien bang CSPRNG, luu SHA-256(token) 64 ky tu hex chu thuong.
-- (2) Khi xac thuc: SHA-256(token nguoi dung cung cap), kiem tra expires_at, used_at.
-- (3) Ngan dang ky trung (chi CONFIRMED) va gioi han si so trong TRANSACTION co khoa.
-- (4) Rang buoc start_time < end_time gia dinh lop khong hoc qua nua dem.
-- (5) CHECK dinh dang token khong the tu tao hash, backend phai hash token.
-- (6) Chi so cua lop can duoc kiem soat o backend trong transaction.
