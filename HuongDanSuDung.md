# Hướng dẫn Sử dụng Hệ thống — PHLINGO ACADEMY

Tài liệu này hướng dẫn thao tác chi tiết cho **5 Vai trò (Roles)** trên giao diện Web tại **`http://localhost:8080`**.

---

## 0. Đăng nhập & Chuyển nhanh Vai trò (`UC-01`, `UC-22`)

1. Truy cập **http://localhost:8080**.
2. Nhập **Tên đăng nhập** & **Mật khẩu** (hoặc bấm trực tiếp vào **5 nút chọn nhanh Actor** ở bên dưới form đăng nhập):

| Vai trò (Role) | Mã Actor | Tên đăng nhập | Mật khẩu | Trạng thái |
| :--- | :---: | :--- | :--- | :--- |
| **Quản trị viên (Admin)** | `QT` | `admin` | `123456` | `ACTIVE` |
| **Nhân viên Đào tạo** | `DT` | `daotao` | `123456` | `ACTIVE` |
| **Nhân viên Thu ngân** | `TN` | `thungan` | `123456` | `ACTIVE` |
| **Giảng viên** | `GV` | `giangvien` | `123456` | `ACTIVE` |
| **Học viên** | `HV` | `hocvien` | `123456` | `ACTIVE` |
| *HV chờ xác thực (Test UC-13)* | `HV` | `hocvien_cho` | `123456` | `PENDING_VERIFICATION` |
| *Tài khoản bị khóa (Test BR-V3-11)* | `DT` | `locked_user` | `123456` | `LOCKED` |

> **Lưu ý**: Trong phiên demo, thanh trên cùng (Topbar) có cụm nút **`Long·QT` | `Phúc·DT` | `Hoàn·TN` | `GV` | `HV`** giúp chuyển đổi nhanh giữa 5 vai trò để kiểm thử phân quyền RBAC mà không cần đăng xuất. Để đăng xuất chính thức (`UC-22`), bấm nút **Đăng xuất** ở góc dưới bên trái.

---

## 1. Hướng dẫn cho Quản trị viên (`ADMIN / QT`)

**Phạm vi quyền**: Toàn quyền hệ thống (quản trị tài khoản, phân quyền, khôi phục mật khẩu, giám sát toàn bộ phân hệ đào tạo, tài chính và báo cáo).

### Các thao tác chính:
1. **Quản lý Tài khoản & Phân quyền (`#/accounts` — `UC-02`)**:
   * **Tạo tài khoản mới**: Nhập Username, Họ tên, Email, SĐT, chọn Role (`ADMIN`, `TRAINING_STAFF`, `CASHIER`, `TEACHER`, `STUDENT`), chọn mật khẩu $\rightarrow$ bấm **`Tạo Tài khoản mới (UC-02)`**.
   * *Lưu ý*: Tài khoản do Admin tạo sẽ được kích hoạt **`ACTIVE` ngay lập tức**, **không cần** bước xác thực OTP Email.
   * **Đổi Role nhanh**: Tại bảng danh sách tài khoản, chọn vai trò mới ở cột *Tùy chọn Role* $\rightarrow$ bấm **`Lưu`** trên dòng đó.
   * **Reset Password 1-Click**: Bấm **`Reset Pass`** trên dòng tài khoản bất kỳ để đặt lại mật khẩu mới (tự động băm SHA-256).
   * **Khóa / Mở khóa / Kích hoạt**: Bấm **`Khóa TK`**, **`Mở khóa`** hoặc **`Kích hoạt`** để đổi trạng thái (`ACTIVE` / `LOCKED`). Tài khoản `admin` gốc được bảo vệ không thể tự khóa.
2. **Xem Hồ sơ 360° & Đăng nhập thử**:
   * Nhấp vào một dòng tài khoản để xem thẻ chi tiết 360° phía trên, hoặc bấm **`Đăng nhập thử TK này`** để kiểm tra giao diện theo đúng góc nhìn của người dùng đó.
3. **Khôi phục Dữ liệu Mẫu (`Reset Mẫu`)**:
   * Bấm **`Reset Mẫu`** ở góc dưới Sidebar để đưa toàn bộ dữ liệu và CSDL SQL về trạng thái mẫu ban đầu.

---

## 2. Hướng dẫn cho Nhân viên Đào tạo (`TRAINING_STAFF / DT`)

**Phạm vi quyền**: Quản lý Hồ sơ Học viên (`UC-03`), Khóa học (`UC-04`), Lớp học (`UC-05`), Ghi danh (`UC-06`), Buổi học & Lịch giảng dạy (`UC-07`), Xem Điểm danh & Báo cáo. *(Không có quyền quản trị tài khoản Admin).*

### Quy trình nghiệp vụ chuẩn:
1. **Quản lý Hồ sơ Học viên (`#/students` — `UC-03`)**:
   * Tìm kiếm học viên theo mã (`HV001`), họ tên, SĐT, email.
   * Thêm mới hoặc chỉnh sửa thông tin học viên (hệ thống kiểm tra định dạng SĐT 10–11 số và chống trùng email).
2. **Quản lý Khóa học (`#/courses` — `UC-04`)**:
   * Tạo/sửa khóa học (`KH-TOEIC450`, `KH-IELTS50`...), thiết lập học phí niêm yết (`tuition >= 0`), trình độ, thời lượng, trạng thái `ACTIVE`/`INACTIVE`.
   * *Quy tắc `BR-V3-05`*: Khi thay đổi học phí niêm yết của khóa học, học phí của các học viên **đã ghi danh trước đó** vẫn giữ nguyên theo giá **Snapshot** tại thời điểm đăng ký.
3. **Mở Lớp học & Kiểm soát Sĩ số (`#/classes` — `UC-05`)**:
   * Chọn Khóa học `ACTIVE`, Giảng viên phụ trách, sĩ số tối đa (`> 0`), ngày khai giảng/kết thúc, phòng học, lịch học trong tuần.
   * Hệ thống tự động kiểm tra chống trùng lịch phòng học và trùng lịch giảng viên.
4. **Ghi danh Học viên vào Lớp (`#/enrollments` — `UC-06`)**:
   * **Bước 1**: Chọn Học viên cần đăng ký.
   * **Bước 2**: Chọn Lớp học đang `OPEN` (hệ thống tự kiểm tra sĩ số còn trống và chống đăng ký trùng lớp).
   * **Bước 3**: Kiểm tra học phí Snapshot $\rightarrow$ bấm **`Xác nhận ghi danh (UC-06)`**. Hệ thống đồng thời tạo bản ghi `CONFIRMED` và khoản phải thu học phí (`UNPAID`).
   * Để chuyển nhanh sang thu tiền cho học viên vừa ghi danh, bấm nút **`Thu HP →`** trên bảng danh sách (và có nút **`← Quay lại Ghi danh`** ở trang Thu học phí).
   * Nếu ghi danh nhầm (chưa thu tiền), có thể bấm **`Hủy đăng ký`** (`CANCELLED`), sau đó bấm nút **`Xóa`** trên các dòng `CANCELLED` để làm gọn danh sách.
5. **Tạo Buổi học & Xem Lịch (`#/schedule`, `#/attendance` — `UC-07`)**:
   * Xem Thời khóa biểu dạng lưới Time-Grid hoặc Kanban theo Thứ, tạo buổi học mới cho từng lớp.

---

## 3. Hướng dẫn cho Nhân viên Thu ngân (`CASHIER / TN`)

**Phạm vi quyền**: Tra cứu Học phí & Công nợ (`UC-08`), Ghi nhận Thanh toán Nhiều đợt & Lập/In Phiếu thu (`UC-09`), Xem Báo cáo Doanh thu & Công nợ.

### Quy trình thu học phí & In phiếu thu:
1. **Tra cứu Công nợ (`#/tuition` — `UC-08`)**:
   * Xem tổng quan: Tổng Phải thu, Đã thu, Công nợ còn lại.
   * Lọc danh sách theo trạng thái (`UNPAID` — Chưa đóng, `PARTIAL` — Đóng 1 phần, `PAID` — Đã đóng đủ) hoặc tìm theo tên/mã học viên, mã lớp.
2. **Ghi nhận Thanh toán Nhiều đợt (`#/tuition` — `UC-09`)**:
   * Nhấp chọn dòng học viên cần thu tiền trên bảng công nợ.
   * Nhập **Số tiền thu đợt này** (hoặc bấm nút chọn nhanh **`Điền đúng số nợ còn lại`** / **`Đóng 50% nợ`**). Hệ thống tự động chặn nếu số tiền vượt quá dư nợ (`BR-V3-06`).
   * Chọn Phương thức thanh toán (`CASH` — Tiền mặt, `TRANSFER` — Chuyển khoản NH, `CARD` — Thẻ) và nhập ghi chú.
   * Bấm **`Ghi nhận thanh toán (UC-09)`**. Trạng thái học phí tự động cập nhật sang `PARTIAL` hoặc `PAID`.
3. **Xem & In Phiếu thu Chính thức (`Mẫu 01-TT`)**:
   * Ở bảng *Lịch sử Phiếu thu* bên dưới, bấm **`Xem / In Phiếu thu`** trên giao dịch bất kỳ.
   * Bấm **`In Phiếu thu (A5/PDF)`** để in gọn trong **1 trang A5/A4 duy nhất** (đầy đủ số tiền bằng chữ, mã QR đối soát, dấu mộc `ĐÃ THU TIỀN`).

---

## 4. Hướng dẫn cho Giảng viên (`TEACHER / GV`)

**Phạm vi quyền**: Cô lập dữ liệu chặt chẽ — Giảng viên chỉ xem được các lớp, lịch dạy, báo cáo và **chỉ được điểm danh đúng lớp học/khóa học mình trực tiếp giảng dạy**. Không xem hoặc sửa được dữ liệu lớp của giảng viên khác.

### Các thao tác chính:
1. **Xem Lịch dạy & Lớp phụ trách (`#/classes`, `#/schedule`)**:
   * Danh sách tự động lọc chỉ hiển thị các lớp được phân công cho giảng viên đang đăng nhập (VD: ThS. Hoàng Minh Tuấn chỉ thấy các lớp `TOEIC01`, `TOEIC02`, `GT-VIP01`, `TOEIC-650`; không thấy lớp `IELTS01` của ThS. Nguyễn Thị Lan).
2. **Điểm danh Học viên theo Buổi (`#/attendance` — `UC-10`)**:
   * Chọn **Lớp học của bạn** $\rightarrow$ chọn **Buổi học** (hoặc tạo buổi học mới nếu cần).
   * Danh sách chỉ hiển thị học viên có trạng thái ghi danh `CONFIRMED` (`BR-V3-08`).
   * Đánh dấu trạng thái từng học viên: **`Có mặt (PRESENT)`**, **`Vắng có phép (ABSENT_EXCUSED)`**, hoặc **`Vắng không phép (ABSENT_UNEXCUSED)`**, nhập ghi chú nếu có.
   * Bấm **`Lưu Kết quả Điểm danh Buổi học (UC-10)`** (có thể bấm **`Đặt tất cả: PRESENT`** để thao tác nhanh). Lưu nhiều lần sẽ tự cập nhật tại chỗ, không tạo bản ghi trùng (`BR-V3-09`).
3. **Tra cứu Tỷ lệ Chuyên cần (`#/attendance`, `#/reports` — `UC-11`)**:
   * Hệ thống tự động tính `% Chuyên cần` tích lũy của từng học viên và trung bình toàn lớp.

---

## 5. Hướng dẫn cho Học viên (`STUDENT / HV`)

**Phạm vi quyền**: Tự đăng ký & kích hoạt tài khoản qua Email OTP, cập nhật hồ sơ cá nhân, đổi/khôi phục mật khẩu, tìm kiếm & tự đăng ký lớp mở, xem lịch học và công nợ của riêng mình (`BR-V3-14`).

### A. Tự Đăng ký & Xác thực Email OTP (`UC-12`, `UC-13`)
1. Tại màn hình Đăng nhập, bấm **`Đăng ký tài khoản`**.
2. Ở tab **`1. Đăng ký Tài khoản Học viên & Gửi OTP Email (UC-12)`**:
   * Nhập Username, Họ tên, **Email thật của bạn** (để nhận mã OTP), SĐT, Ngày sinh, Mật khẩu.
   * Bấm **`Đăng ký & Gửi Mã OTP về Email (UC-12)`**. Tài khoản được tạo ở trạng thái `PENDING_VERIFICATION` và hệ thống gửi **Email chứa mã OTP 6 chữ số** (có logo TDTU, hiệu lực 10 phút) tới hộp thư của bạn.
3. Chuyển sang tab **`2. Xác thực Email bằng Mã OTP 6 số (UC-13)`**:
   * Nhập đúng **Email đã đăng ký** và **Mã OTP 6 chữ số** nhận trong hộp thư.
   * Bấm **`Xác thực OTP & Kích hoạt ACTIVE (UC-13)`** $\rightarrow$ Tài khoản chuyển sang `ACTIVE` và có thể đăng nhập ngay.

### B. Quên & Đặt lại Mật khẩu bằng OTP (`UC-16`, `UC-17`)
1. Tại màn hình Đăng nhập, bấm **`Quên mật khẩu?`**.
2. Nhập Email tài khoản $\rightarrow$ bấm **`Gửi Mã OTP về Email (UC-16)`**.
3. Mở hộp thư lấy mã OTP 6 chữ số $\rightarrow$ nhập OTP + Mật khẩu mới $\rightarrow$ bấm **`Đặt lại Mật khẩu bằng OTP (UC-17)`**.

### C. Cổng Học viên Tự phục vụ (`#/student-portal` — `UC-14` → `UC-21`)
Sau khi đăng nhập bằng tài khoản Học viên (`hocvien` / `123456`):
1. **Tab 1 — Tìm Khóa học & Tự đăng ký Lớp OPEN (`UC-18`, `UC-19`)**: Tìm theo tên/mã lớp hoặc trình độ, xem sĩ số & học phí Snapshot, bấm **`Tự đăng ký ngay (UC-19)`**.
2. **Tab 2 — Lịch học & Điểm danh Cá nhân (`UC-20`)**: Xem thời khóa biểu các buổi học của lớp đã `CONFIRMED` và kết quả điểm danh từng buổi.
3. **Tab 3 — Học phí & Công nợ Cá nhân (`UC-21`)**: Xem học phí Snapshot, số tiền đã đóng, dư nợ còn lại và các mã phiếu thu.
4. **Tab 4 — Hồ sơ Cá nhân & Đổi mật khẩu (`UC-14`, `UC-15`)**: Cập nhật Họ tên, SĐT, Email, Ngày sinh, Địa chỉ; hoặc đổi mật khẩu mới (yêu cầu khác mật khẩu cũ theo `BR-V3-13`).
