# PHLINGO ACADEMY — Hệ thống Quản lý Trung tâm Ngoại ngữ

> **Đồ án Cuối kỳ Học phần Phân tích và Thiết kế Yêu cầu (502050)**  
> **Trường Đại học Tôn Đức Thắng (TDTU) — Khoa Công nghệ Thông tin**

Hệ thống quản lý toàn diện nghiệp vụ Trung tâm Ngoại ngữ **PHLINGO ACADEMY**, bám sát 100% tài liệu đặc tả v3.0 với **5 Tác nhân (Roles)**, **22 Use Cases (`UC-01` → `UC-22`)**, **18 Yêu cầu Chức năng (`FR-01` → `FR-18`)**, **10 Yêu cầu Phi chức năng (`NFR-01` → `NFR-10`)** và **14 Quy tắc Nghiệp vụ (`BR-V3-01` → `BR-V3-14`)**.

---

## 1. Nhóm Thực hiện Đề tài

| Thành viên | Vai trò trong Đề tài | Tài khoản Demo (Web) | Phụ trách chính |
| :--- | :--- | :--- | :--- |
| **Trần Hữu Long** | Quản trị viên (`ADMIN / QT`) | `admin` / `123456` | Kiến trúc 3 tầng Java, Web UI, Phân quyền RBAC (`UC-01, UC-02`), Xác thực OTP Gmail SMTP (`UC-12..17`), Đồng bộ JDBC SQL |
| **Nguyễn Trọng Phúc** | Nhân viên Đào tạo (`TRAINING_STAFF / DT`) | `daotao` / `123456` | Quản lý Học viên (`UC-03`), Khóa học (`UC-04`), Lớp học & Sĩ số (`UC-05`), Ghi danh (`UC-06`), Buổi học (`UC-07`) |
| **Lê Khải Hoàn** | Nhân viên Thu ngân (`CASHIER / TN`) | `thungan` / `123456` | Schema SQL 13 bảng (`PHLingoAcademy.sql`), REST API (`phlingo-api`), Thu học phí nhiều đợt & Công nợ (`UC-08, UC-09, UC-21`) |

---

## 2. Kiến trúc & Cấu trúc Thư mục Dự án

```text
RAD/
├── prototype/                     # Giao diện Web SPA (HTML5, CSS3, Vanilla JS) phục vụ tại http://localhost:8080
│   ├── index.html
│   ├── styles.css
│   ├── app.js
│   └── assets/                    # Logo TDTU chính thức (tdtu-logo.png, tdtu-favicon.png)
├── java-app/                      # Ứng dụng Java Web Server + Swing Desktop + Tầng nghiệp vụ & JDBC
│   ├── src/com/rad/center/
│   │   ├── model/                 # 10 Entity Models (User, Student, Teacher, Course, ClassRoom, ...)
│   │   ├── dao/                   # CenterRepository.java & PhlingoSqlSync.java (đồng bộ MySQL 13 bảng)
│   │   ├── service/               # CenterService.java (toàn bộ rule BR-V3-01..14)
│   │   ├── server/                # WebAppServer.java (HTTP Server + REST API cho Web UI)
│   │   ├── util/                  # DatabaseConfig.java, PasswordUtil.java, SmtpMailer.java (Gmail SSL OTP)
│   │   └── ui/                    # LanguageCenterApp.java & các màn hình Java Swing
│   ├── sql/
│   │   ├── PHLingoAcademy.sql     # Schema chuẩn 13 bảng (MySQL 8.0.16+ / MariaDB 10.4+)
│   │   └── seed_phlingo_academy.sql # Dữ liệu mẫu chuẩn cho 13 bảng
│   ├── lib/mysql-connector-j-8.4.0.jar
│   ├── build.bat / run.bat / test.bat
├── phlingo-api/                   # Máy chủ REST API độc lập (Bearer Token + PBKDF2-SHA256 + MySQL JDBC)
│   ├── ApiServer.java
│   ├── Database.java
│   ├── Security.java
│   ├── Mailer.java
│   ├── Json.java
│   └── ApiException.java
├── docs/                          # Toàn bộ sơ đồ UML (Use Case, Activity, Sequence, ERD) & Báo cáo Word
│   ├── usecase/                   # 23 sơ đồ Use Case (Tổng quan + UC-01..UC-22)
│   ├── activity/                  # Sơ đồ Activity các luồng chính
│   ├── sequence/                  # Sơ đồ Sequence các nghiệp vụ trọng tâm
│   ├── erd/                       # Sơ đồ ERD 13 bảng (ERD_PHLingoAcademy.png)
│   └── BAO_CAO_CUOI_KY_HOAN_CHINH.docx
├── data/                          # Kho lưu trữ cục bộ dự phòng khi chạy offline
├── README.md                      # Tài liệu tổng quan & hướng dẫn cài đặt
└── HuongDanSuDung.md              # Hướng dẫn sử dụng chi tiết cho từng Role (QT, DT, TN, GV, HV)
```

---

## 3. Cơ sở Dữ liệu SQL (13 Bảng chuẩn ERD)

Schema tại `java-app/sql/PHLingoAcademy.sql` gồm đầy đủ **13 bảng** với ràng buộc khóa ngoại (`RESTRICT`) và `CHECK`:
1. `users` — Tài khoản đăng nhập & trạng thái (`ACTIVE`, `PENDING_VERIFICATION`, `LOCKED`)
2. `students` — Hồ sơ học viên liên kết `user_id`
3. `teachers` — Hồ sơ giảng viên liên kết `user_id`
4. `courses` — Chương trình khóa học & học phí niêm yết (`tuition_listed >= 0`)
5. `classes` — Lớp học mở, sĩ số tối đa (`capacity > 0`), giảng viên phụ trách
6. `class_schedules` — Thời khóa biểu cố định trong tuần (`start_time < end_time`)
7. `enrollments` — Ghi danh học viên vào lớp (`CONFIRMED`, `CANCELLED`)
8. `tuition_fees` — Học phí Snapshot gắn 1-1 với `enrollment_id` (`UNPAID`, `PARTIAL`, `PAID`)
9. `payments` — Giao dịch thu tiền nhiều đợt (`amount > 0`, chống trùng `request_id` / `receipt_code`)
10. `sessions` — Các buổi học thực tế của lớp
11. `attendance` — Điểm danh (`UNIQUE(session_id, student_id)`)
12. `verification_tokens` — Token/OTP xác thực email băm SHA-256 (64 hex)
13. `password_reset_tokens` — Token khôi phục mật khẩu dùng 1 lần băm SHA-256

---

## 4. Hướng dẫn Cài đặt & Chạy Hệ thống

### Yêu cầu môi trường
* **Java JDK 8+** (đã test tốt trên JDK 8 / 11 / 17+)
* **MySQL 8.0.16+** hoặc **MariaDB 10.4+ (XAMPP)**

### Bước 1: Khởi tạo Cơ sở dữ liệu SQL
Mở **MySQL Workbench**, **phpMyAdmin (XAMPP)** hoặc MySQL CLI, chạy lần lượt 2 file:
1. `java-app/sql/PHLingoAcademy.sql` (tạo database `phlingo_academy` và 13 bảng).
2. `java-app/sql/seed_phlingo_academy.sql` (nạp dữ liệu mẫu 5 vai trò, học viên, khóa học, lớp học, công nợ, điểm danh).

### Bước 2: Chạy Ứng dụng Web chính (Giao diện + API + Đồng bộ SQL)
Tại thư mục gốc `RAD/`:

```powershell
# Biên dịch mã nguồn Java (kèm MySQL Connector/J)
javac -encoding UTF-8 -cp "java-app/lib/mysql-connector-j-8.4.0.jar" -d java-app/out (Get-ChildItem -Recurse java-app/src -Filter *.java | ForEach-Object { $_.FullName })

# Khởi chạy Web Server tại http://localhost:8080
java "-Dfile.encoding=UTF-8" -cp "java-app/out;java-app/bin;java-app/lib/mysql-connector-j-8.4.0.jar" com.rad.center.ui.LanguageCenterApp --web-only
```

> **Mẹo cấu hình cổng SQL**: Hệ thống mặc định kết nối `127.0.0.1:3306/phlingo_academy` (`root`, không mật khẩu) và tự động thử cổng `3307` nếu chạy XAMPP song song. Bạn có thể tùy chỉnh qua biến môi trường `API_DB_HOST`, `API_DB_PORT`, `API_DB_NAME`, `API_DB_USER`, `API_DB_PASSWORD`.

Mở trình duyệt truy cập: **http://localhost:8080**  
Kiểm tra trạng thái SQL trực tiếp: **http://localhost:8080/api/health**

### Bước 3 (Tùy chọn): Chạy Máy chủ REST API độc lập (`phlingo-api`)
```powershell
javac -encoding UTF-8 -cp "java-app/lib/mysql-connector-j-8.4.0.jar" -d phlingo-api/out phlingo-api/*.java
$env:API_PORT='8081'; $env:API_DB_PORT='3307'
java "-Dfile.encoding=UTF-8" -cp "phlingo-api/out;java-app/lib/mysql-connector-j-8.4.0.jar" phlingo.api.ApiServer
```
* Endpoint kiểm tra: `GET http://127.0.0.1:8081/api/health`

---

## 5. Tính năng Xác thực Email OTP thực tế (`UC-12`, `UC-13`, `UC-16`, `UC-17`)
* `SmtpMailer.java` sử dụng Gmail SMTP SSL (`smtp.gmail.com:465`) gửi email HTML mang logo chính thức Trường Đại học Tôn Đức Thắng (`PHLINGO ACADEMY`) kèm mã **OTP 6 chữ số** (hiệu lực 10 phút, dùng 1 lần).
* Chỉ áp dụng khi Học viên **tự đăng ký** hoặc **quên mật khẩu**. Tài khoản do Quản trị viên (`ADMIN`) tạo trực tiếp trong mục **Tài khoản & Phân quyền (`UC-02`)** được kích hoạt `ACTIVE` ngay lập tức.

---

## 6. Tài liệu Tham khảo
* Hướng dẫn sử dụng chi tiết cho từng Role: xem file [`HuongDanSuDung.md`](./HuongDanSuDung.md)
* Sơ đồ phân tích thiết kế (UCD, Activity, Sequence, ERD): xem thư mục [`docs/`](./docs/)
