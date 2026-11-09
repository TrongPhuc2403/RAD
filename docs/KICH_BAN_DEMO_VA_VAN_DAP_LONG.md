# HƯỚNG DẪN CHẠY CHƯƠNG TRÌNH, KỊCH BẢN DEMO (5–6 PHÚT) & CÂU HỎI VẤN ĐÁP CỦA LONG

## 1. Cách khởi chạy nhanh khi lên bục Demo
- **Chạy ứng dụng Java Swing (Mục 3.2 Đề thi):**
  - Mở thư mục `d:\working\Learning\RAD\java-app\` và nhấp đúp vào **`run.bat`** (hoặc chạy `run.bat --direct` để vào thẳng màn hình chính).
  - Tài khoản đăng nhập mặc định (mật khẩu tất cả là `123456` – hoặc bấm nút **Chọn nhanh vai trò Demo** ngay trên màn hình Đăng nhập):
    - Quản trị viên (Full quyền): `admin` / `123456`
    - Nhân viên đào tạo: `daotao` / `123456`
    - Nhân viên thu ngân: `thungan` / `123456`
    - Giảng viên: `giangvien` / `123456`
  - **Mẹo cực hay khi Demo:** Ở góc dưới bên trái Sidebar có nút **"Khôi phục Dữ liệu Mẫu Demo"**. Trước khi lên thuyết trình (hoặc sau khi chạy thử ở nhà), chỉ cần bấm nút này 1 lần là toàn bộ dữ liệu quay về trạng thái mẫu chuẩn sạch đẹp!
- **Mở Prototype Giao diện 10 màn hình (Mục 3.1 Đề thi):**
  - Mở trực tiếp file `d:\working\Learning\RAD\prototype\index.html` trên trình duyệt bất kỳ (Chrome / Edge).

---

## 2. Kịch bản Thuyết trình & Demo 5–6 phút của Long (Nối tiếp sau phần của Phúc và Hoàn)

### Phút 1: Giới thiệu Giao diện Prototype & Kiến trúc Phân tầng Java (NFR-08)
- *"Thưa thầy/cô và các bạn, tiếp nối phần thiết kế UML và CSDL của bạn Hoàn, em là **Trần Hữu Long**, phụ trách thiết kế giao diện Prototype, lập trình ứng dụng Java và kiểm thử tích hợp."*
- Trình chiếu nhanh `prototype/index.html` (10 màn hình thống nhất bố cục) và mở ứng dụng Java Swing (`run.bat`).
- Giải thích ngắn gọn kiến trúc 4 tầng:
  1. `com.rad.center.ui` (Presentation Layer – Java Swing Forms/Tables)
  2. `com.rad.center.service` (Business Logic Layer – `CenterService` kiểm soát toàn bộ 15 FR và 25 quy tắc nghiệp vụ BR-01..BR-25)
  3. `com.rad.center.dao` & `model` (Data Access Layer – JDBC MySQL & Lưu trữ bền vững NFR-09)
  4. Cơ sở dữ liệu `language_center_db` (11 bảng liên kết chặt chẽ).

### Phút 2: Demo Chức năng 1 (Quản lý Học viên) & Chức năng 2 (Quản lý Lớp học)
1. Tại màn hình **Đăng nhập (UC-01)**, nhấn chọn vai trò **Quản trị (Long)** và bấm **Đăng nhập**.
2. Vào mục **2. Quản lý Học viên (CN1)**:
   - Xóa trống ô *Họ và tên*, bấm **+ Thêm học viên mới** ➔ Hệ thống lập tức chặn và báo lỗi `[TC-02 / BR-08]` (Chứng minh kiểm tra dữ liệu đầu vào).
   - Nhập Họ tên: `Ngô Gia Bảo`, SĐT: `0912345009`, Email: `giabao@gmail.com` ➔ Bấm **+ Thêm học viên mới** ➔ Tạo thành công mã `HV009` (**TC-01**).
   - Nhấp vào học viên `HV001 - Phạm Minh Anh` ở bảng trên để thầy/cô thấy bảng dưới hiển thị ngay lịch sử các lớp đã đăng ký, học phí và tỷ lệ chuyên cần của học viên đó.
3. Vào mục **4. Quản lý Lớp học (CN2)**:
   - Chỉ cho giảng viên thấy lớp `TOEIC01` hiện có sĩ số **4/20** (còn 16 chỗ trống) và lớp `GT-VIP01` có sĩ số **3/3 (0 chỗ trống - ĐẦY SĨ SỐ)**.

### Phút 3 – 4: Demo Chức năng 3 (Đăng ký Lớp học & Tự động sinh Học phí - UC-04)
1. Vào mục **5. Đăng ký Lớp học (CN3)**:
   - **Thử nghiệm Ngoại lệ 1 – Đăng ký trùng lớp (TC-05 / BR-10):** Chọn `HV001 - Phạm Minh Anh` và lớp `TOEIC01` ➔ Bấm **Xác nhận Đăng ký** ➔ Hệ thống từ chối ngay vì `HV001` đã có đăng ký hợp lệ `DK001` trong lớp này.
   - **Thử nghiệm Ngoại lệ 2 – Lớp đã đầy sĩ số (TC-04 / BR-06):** Chọn `HV009 - Ngô Gia Bảo` và lớp `GT-VIP01 (3/3)` ➔ Bấm **Xác nhận Đăng ký** ➔ Hệ thống từ chối ngay vì lớp đã đủ sĩ số tối đa.
   - **Luồng chính – Đăng ký hợp lệ (TC-03 / FR-06, FR-09):** Chọn `HV009 - Ngô Gia Bảo` và lớp `TOEIC01` ➔ Bấm **Xác nhận Đăng ký** ➔ Hệ thống thông báo tạo thành công mã đăng ký `DK010`, sĩ số lớp `TOEIC01` tăng lên `5/20` và **tự động sinh khoản nghĩa vụ học phí 3.000.000 VNĐ**!

### Phút 4 – 5: Demo Chức năng 4 (Thu Học phí nhiều lần & Quản lý Công nợ - UC-05)
1. Vào mục **6. Thu Học phí & Công nợ (CN4)**:
   - Chọn dòng của `DK005 - Phạm Minh Anh (IELTS01)` (Tổng phải thu `4.000.000 VNĐ`, đã đóng 2 đợt `1.500.000 + 2.000.000 = 3.500.000 VNĐ`, còn nợ `500.000 VNĐ` – đúng ví dụ Chương 4.4 trong báo cáo) hoặc chọn dòng `DK010` của `Ngô Gia Bảo` vừa tạo.
   - **Thử nghiệm Ngoại lệ – Thanh toán vượt số dư (TC-07 / BR-18):** Nhập số tiền thu lớn hơn số tiền còn nợ (ví dụ nhập `9.000.000`) ➔ Bấm **Xác nhận Thu Học phí** ➔ Hệ thống chặn và báo lỗi vượt quá công nợ!
   - **Luồng chính – Thanh toán một phần (TC-06 / FR-10):** Nhập số tiền hợp lệ (ví dụ `500.000` hoặc `1.000.000`), chọn `TRANSFER` ➔ Bấm **Xác nhận Thu Học phí** ➔ Hệ thống sinh mã phiếu thu mới, trừ trực tiếp vào số tiền còn nợ và thêm ngay dòng giao dịch vào bảng **Lịch sử Giao dịch Thanh toán** bên dưới.

### Phút 5 – 6: Demo Chức năng 5 (Điểm danh Học viên & Thống kê Chuyên cần - UC-06)
1. Vào mục **7. Điểm danh Học viên (CN5)**:
   - Chọn lớp `TOEIC01` và `Buổi 3` ➔ Thầy/cô sẽ thấy học viên `HV009 - Ngô Gia Bảo` vừa đăng ký ở bước trước đã tự động xuất hiện trong danh sách điểm danh (nhờ liên kết dữ liệu `CONFIRMED` theo `BR-21`).
   - Đổi trạng thái điểm danh của một học viên sang `ABSENT_EXCUSED` (Vắng có phép), nhập ghi chú và bấm **Lưu Điểm danh (FR-13)** (**TC-08**).
   - Bấm **Lưu Điểm danh** thêm một lần nữa để chứng minh hệ thống chỉ cập nhật (`UPDATE`) bản ghi hiện có trên cặp khóa `(session_id, student_id)` chứ tuyệt đối không tạo bản ghi trùng lặp (**TC-09 / BR-22**) và cột **Tỷ lệ Chuyên cần (FR-14)** tự động cập nhật phần trăm tức thời!
2. Chuyển sang mục **8. Báo cáo & Thống kê (FR-15)** để tổng kết toàn bộ số liệu doanh thu, công nợ, tỷ lệ lấp đầy lớp và chuyên cần (**TC-10**).

---

## 3. Bộ Câu hỏi & Trả lời Vấn đáp Trọng tâm cho Long

1. **Hỏi: Tại sao nhóm tách riêng hai thực thể `Course` (Khóa học) và `Class` (Lớp học)?**
   - **Đáp:** Theo quy tắc `BR-03` và `BR-04`, `Course` là chương trình đào tạo khung (ví dụ *TOEIC 450+*, học phí chuẩn 3.000.000đ, thời lượng 3 tháng), còn `Class` là một đợt mở lớp cụ thể của khóa học đó (ví dụ *TOEIC01* học tối 2-4-6 tại P.201 do ThS. Tuấn dạy, *TOEIC02* học tối 3-5-7 tại P.202). Nhờ tách riêng, trung tâm có thể mở hàng chục lớp từ một khóa học mà không bị dư thừa dữ liệu mô tả khóa học.
2. **Hỏi: Tại sao khi thay đổi học phí mặc định của Khóa học (`courses.tuition`), học phí của các học viên đã đăng ký trước đó không bị nhảy theo?**
   - **Đáp:** Theo quy tắc `BR-15` và `FR-09`, ngay tại thời điểm học viên đăng ký lớp thành công, hệ thống sao chép mức học phí áp dụng vào cột `tuition_fees.amount_due` của riêng đăng ký đó. Do đó nếu tháng sau trung tâm tăng học phí khóa học cho các lớp mới, công nợ của các học viên cũ vẫn được giữ nguyên minh bạch.
3. **Hỏi: Làm sao chương trình Java đảm bảo không bị lỗi chia cho 0 khi tính tỷ lệ chuyên cần cho lớp mới mở chưa điểm danh buổi nào?**
   - **Đáp:** Trong hàm `CenterService.calculateStudentAttendanceRate(classId, studentId)`, hệ thống đếm tổng số buổi học đã có bản ghi điểm danh (`totalMarked`). Nếu `totalMarked == 0`, hàm trả về `0.0%` ngay lập tức trước khi thực hiện phép chia `(presentCount * 100.0) / totalMarked`, đáp ứng đúng điều kiện nghiệm thu của `FR-14`.

