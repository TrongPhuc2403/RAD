USE `phlingo_academy`;

-- Seed data cho PHLingo Academy (tuong thich 100% schema 13 bang PHLingoAcademy.sql)
-- Mat khau mac dinh cho cac tai khoan demo: 123456 (SHA-256 hex)

INSERT IGNORE INTO `users` (`user_id`, `email`, `password_hash`, `role`, `status`, `created_at`) VALUES
(1, 'long.th@tdtu.edu.vn', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'ADMIN', 'ACTIVE', '2026-10-01 08:00:00'),
(2, 'phuc.nt@tdtu.edu.vn', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'TRAINING_STAFF', 'ACTIVE', '2026-10-01 08:00:00'),
(3, 'hoan.lk@tdtu.edu.vn', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'CASHIER', 'ACTIVE', '2026-10-01 08:00:00'),
(4, 'tuan.hm@tdtu.edu.vn', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'TEACHER', 'ACTIVE', '2026-10-01 08:00:00'),
(5, 'lan.nt@tdtu.edu.vn', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'TEACHER', 'ACTIVE', '2026-10-01 08:00:00'),
(6, 'bao.ngo@student.tdtu.edu.vn', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'STUDENT', 'ACTIVE', '2026-10-05 09:00:00'),
(7, 'nam.nguyen@student.tdtu.edu.vn', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'STUDENT', 'ACTIVE', '2026-10-05 09:10:00'),
(8, 'bich.le@student.tdtu.edu.vn', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'STUDENT', 'ACTIVE', '2026-10-06 10:00:00'),
(9, 'khanh.tran@student.tdtu.edu.vn', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'STUDENT', 'ACTIVE', '2026-10-07 10:30:00'),
(10, 'tung.vo@student.tdtu.edu.vn', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'STUDENT', 'ACTIVE', '2026-10-08 11:00:00'),
(11, 'huong.le@student.tdtu.edu.vn', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'STUDENT', 'PENDING_VERIFICATION', '2026-10-10 14:00:00'),
(12, 'locked@tdtu.edu.vn', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'TRAINING_STAFF', 'LOCKED', '2026-10-01 08:00:00');

INSERT IGNORE INTO `teachers` (`teacher_id`, `full_name`, `user_id`, `specialization`, `qualification`) VALUES
(1, 'ThS. Hoàng Minh Tuấn', 4, 'TOEIC & Business English', 'Thạc sĩ Ngôn ngữ Anh · TESOL'),
(2, 'ThS. Nguyễn Thị Lan', 5, 'IELTS Academic 7.5+', 'Thạc sĩ Giảng dạy Tiếng Anh · IELTS 8.5');

INSERT IGNORE INTO `students` (`student_id`, `full_name`, `email`, `phone`, `user_id`, `date_of_birth`, `gender`, `address`) VALUES
(1, 'Ngô Gia Bảo', 'bao.ngo@student.tdtu.edu.vn', '0912345001', 6, '2004-05-12', 'Nam', 'Quận 7, TP.HCM'),
(2, 'Nguyễn Hoàng Nam', 'nam.nguyen@student.tdtu.edu.vn', '0912345002', 7, '2003-11-20', 'Nam', 'Quận 4, TP.HCM'),
(3, 'Lê Thị Ngọc Bích', 'bich.le@student.tdtu.edu.vn', '0912345003', 8, '2005-02-14', 'Nữ', 'Nhà Bè, TP.HCM'),
(4, 'Trần Quốc Khánh', 'khanh.tran@student.tdtu.edu.vn', '0912345004', 9, '2004-09-02', 'Nam', 'Quận 1, TP.HCM'),
(5, 'Võ Thanh Tùng', 'tung.vo@student.tdtu.edu.vn', '0912345005', 10, '2003-07-19', 'Nam', 'Bình Thạnh, TP.HCM'),
(6, 'Lê Thị Thu Hương', 'huong.le@student.tdtu.edu.vn', '0912345006', 11, '2005-03-22', 'Nữ', 'Quận 7, TP.HCM');

INSERT IGNORE INTO `courses` (`course_id`, `code`, `name`, `tuition_listed`, `status`, `description`, `level`, `duration_hours`) VALUES
(1, 'KH-TOEIC450', 'TOEIC 450+ (Nền tảng & Luyện đề)', 3000000.00, 'ACTIVE', 'Khóa học xây dựng nền tảng Ngữ pháp, Từ vựng và chiến lược giải đề TOEIC 450+.', 'Beginner', 60),
(2, 'KH-IELTS50', 'IELTS Foundation (4.0 - 5.0)', 4000000.00, 'ACTIVE', 'Khóa học phát triển đồng bộ 4 kỹ năng IELTS Academic mục tiêu 5.0.', 'Intermediate', 72),
(3, 'KH-GTVIP', 'Tiếng Anh Giao tiếp Cơ bản', 2500000.00, 'ACTIVE', 'Lớp giao tiếp phản xạ thực tế sĩ số nhỏ.', 'Beginner', 48),
(4, 'KH-TOEIC650', 'TOEIC 650+ Intensive', 3500000.00, 'ACTIVE', 'Luyện thi TOEIC cấp tốc 650+ cho sinh viên chuẩn bị tốt nghiệp.', 'Intermediate', 60);

INSERT IGNORE INTO `classes` (`class_id`, `course_id`, `teacher_id`, `code`, `name`, `capacity`, `status`) VALUES
(1, 1, 1, 'TOEIC01', 'Lớp TOEIC 450+ Sáng/Tối K26-01', 20, 'OPEN'),
(2, 1, 1, 'TOEIC02', 'Lớp TOEIC 450+ Tối 3-5-7 K26-02', 25, 'OPEN'),
(3, 2, 2, 'IELTS01', 'Lớp IELTS Foundation 5.0 K26-01', 15, 'OPEN'),
(4, 3, 1, 'GT-VIP01', 'Lớp Giao tiếp VIP Sĩ số nhỏ', 3, 'OPEN');

INSERT IGNORE INTO `class_schedules` (`schedule_id`, `class_id`, `day_of_week`, `start_time`, `end_time`) VALUES
(1, 1, 1, '18:00:00', '20:00:00'),
(2, 1, 3, '18:00:00', '20:00:00'),
(3, 1, 5, '18:00:00', '20:00:00'),
(4, 3, 2, '18:30:00', '20:30:00'),
(5, 3, 4, '18:30:00', '20:30:00');

INSERT IGNORE INTO `enrollments` (`enrollment_id`, `student_id`, `class_id`, `status`, `enrolled_at`) VALUES
(1, 1, 1, 'CONFIRMED', '2026-10-28 09:00:00'),
(2, 2, 1, 'CONFIRMED', '2026-10-29 09:30:00'),
(3, 3, 1, 'CONFIRMED', '2026-10-30 10:00:00'),
(4, 4, 1, 'CONFIRMED', '2026-10-31 11:00:00'),
(5, 1, 3, 'CONFIRMED', '2026-10-28 14:00:00'),
(6, 5, 3, 'CONFIRMED', '2026-10-30 15:00:00'),
(7, 2, 4, 'CONFIRMED', '2026-10-29 16:00:00'),
(8, 3, 4, 'CONFIRMED', '2026-10-30 16:30:00'),
(9, 4, 4, 'CONFIRMED', '2026-10-31 17:00:00');

INSERT IGNORE INTO `tuition_fees` (`tuition_id`, `enrollment_id`, `amount_due`, `status`) VALUES
(1, 1, 3000000.00, 'PAID'),
(2, 2, 3000000.00, 'PARTIAL'),
(3, 3, 3000000.00, 'UNPAID'),
(4, 4, 3000000.00, 'PARTIAL'),
(5, 5, 4000000.00, 'PARTIAL'),
(6, 6, 4000000.00, 'UNPAID'),
(7, 7, 2500000.00, 'PAID'),
(8, 8, 2500000.00, 'PAID'),
(9, 9, 2500000.00, 'UNPAID');

INSERT IGNORE INTO `payments` (`payment_id`, `tuition_id`, `amount`, `method`, `request_id`, `receipt_code`, `paid_at`) VALUES
(1, 1, 3000000.00, 'BANK_TRANSFER', 'seed-req-001', 'PHL-SEED-001', '2026-10-28 09:15:00'),
(2, 2, 1500000.00, 'CASH', 'seed-req-002', 'PHL-SEED-002', '2026-10-29 10:00:00'),
(3, 4, 2000000.00, 'CARD', 'seed-req-003', 'PHL-SEED-003', '2026-10-31 11:20:00'),
(4, 5, 2000000.00, 'BANK_TRANSFER', 'seed-req-004', 'PHL-SEED-004', '2026-10-28 14:20:00'),
(5, 5, 1500000.00, 'CASH', 'seed-req-005', 'PHL-SEED-005', '2026-11-02 15:00:00'),
(6, 7, 2500000.00, 'CASH', 'seed-req-006', 'PHL-SEED-006', '2026-10-29 16:20:00'),
(7, 8, 2500000.00, 'BANK_TRANSFER', 'seed-req-007', 'PHL-SEED-007', '2026-10-30 16:45:00');

INSERT IGNORE INTO `sessions` (`session_id`, `class_id`, `session_date`, `start_time`, `end_time`) VALUES
(1, 1, '2026-11-02', '18:00:00', '20:00:00'),
(2, 1, '2026-11-04', '18:00:00', '20:00:00'),
(3, 3, '2026-11-03', '18:30:00', '20:30:00'),
(4, 3, '2026-11-05', '18:30:00', '20:30:00');

INSERT IGNORE INTO `attendance` (`attendance_id`, `session_id`, `student_id`, `status`, `marked_at`) VALUES
(1, 1, 1, 'PRESENT', '2026-11-02 18:15:00'),
(2, 1, 2, 'PRESENT', '2026-11-02 18:15:00'),
(3, 1, 3, 'ABSENT_EXCUSED', '2026-11-02 18:15:00'),
(4, 1, 4, 'PRESENT', '2026-11-02 18:15:00'),
(5, 2, 1, 'PRESENT', '2026-11-04 18:10:00'),
(6, 2, 2, 'ABSENT_UNEXCUSED', '2026-11-04 18:10:00');
