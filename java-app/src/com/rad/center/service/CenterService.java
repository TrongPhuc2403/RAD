package com.rad.center.service;

import com.rad.center.dao.CenterRepository;
import com.rad.center.model.*;
import com.rad.center.util.PasswordUtil;
import com.rad.center.util.SmtpMailer;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Business Logic Layer (Service) thực thi toàn bộ 15 Yêu cầu chức năng (FR-01 -> FR-15)
 * và 25 Quy tắc nghiệp vụ (BR-01 -> BR-25) của hệ thống quản lý trung tâm ngoại ngữ.
 */
public class CenterService {
    private final CenterRepository repo;
    private User currentUser;

    // Lưu trữ mã OTP 6 chữ số gắn chặt với đúng địa chỉ Email (BR-V3-11, BR-V3-12)
    private static final long OTP_TTL_MS = 10 * 60 * 1000L; // 10 phút
    private final Map<String, String> emailOtpStore = new ConcurrentHashMap<String, String>();
    private final Map<String, Long> emailOtpExpiry = new ConcurrentHashMap<String, Long>();
    private final Map<String, String> resetOtpStore = new ConcurrentHashMap<String, String>();
    private final Map<String, Long> resetOtpExpiry = new ConcurrentHashMap<String, Long>();
    private final SecureRandom otpRandom = new SecureRandom();

    private String generateSixDigitOtp() {
        int code = 100000 + otpRandom.nextInt(900000);
        return String.format("%06d", code);
    }

    public CenterService() {
        this.repo = CenterRepository.getInstance();
        if (!repo.getUsers().isEmpty()) {
            this.currentUser = repo.getUsers().get(0);
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    private String todayStr() {
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    }

    private String nowDateTimeStr() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
    }

    private String nowVnDateTimeStr() {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());
    }

    /**
     * NFR-04: Kiểm tra phân quyền phía Backend/Service theo vai trò người dùng
     */
    public void requireRoles(String... allowedRoles) throws ValidationException {
        if (currentUser == null) {
            throw new ValidationException("NFR-04", "Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại!");
        }
        String role = currentUser.getRole();
        if ("ADMIN".equals(role)) return; // Quản trị viên (Trần Hữu Long) có toàn quyền
        for (String allowed : allowedRoles) {
            if (allowed.equalsIgnoreCase(role)) {
                return;
            }
        }
        throw new ValidationException("NFR-04", "Vai trò [" + currentUser.getRoleDisplay()
                + " - " + currentUser.getFullName() + "] không có quyền thực hiện thao tác này!");
    }

    // =========================================================================
    // FR-01: ĐĂNG NHẬP VÀ PHÂN QUYỀN (UC-01, UC-02, UC-22, BR-V3-11..13)
    // =========================================================================
    public User login(String username, String password) throws ValidationException {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new ValidationException("FR-01", "Vui lòng nhập đầy đủ tên đăng nhập/email và mật khẩu!");
        }
        String input = username.trim();
        String hash = PasswordUtil.sha256(password);
        for (User u : repo.getUsers()) {
            if (u.getUsername().equalsIgnoreCase(input) || (u.getEmail() != null && u.getEmail().equalsIgnoreCase(input))) {
                if (!u.getPasswordHash().equals(hash)) {
                    throw new ValidationException("FR-01", "Thông tin đăng nhập hoặc mật khẩu không chính xác!");
                }
                if ("LOCKED".equals(u.getStatus())) {
                    throw new ValidationException("BR-V3-11", "Tài khoản này đã bị khóa (LOCKED), vui lòng liên hệ Quản trị viên!");
                }
                if ("PENDING_VERIFICATION".equals(u.getStatus())) {
                    throw new ValidationException("BR-V3-11 / UC-13", "Tài khoản đang chờ xác thực email (PENDING_VERIFICATION). Vui lòng nhập mã xác thực (UC-13) hoặc liên hệ Quản trị viên!");
                }
                u.setLastLogin(nowDateTimeStr());
                repo.saveToDisk();
                this.currentUser = u;
                return u;
            }
        }
        throw new ValidationException("FR-01", "Tài khoản không tồn tại hoặc thông tin đăng nhập chưa chính xác!");
    }

    /**
     * UC-02 / FR-01: Quản trị viên tạo tài khoản mới và gán vai trò (QT, DT, TN, GV, HV)
     */
    public User addUser(String username, String password, String fullName, String role, String status) throws ValidationException {
        return addUser(username, password, fullName, role, status, null, null, 0);
    }

    public User addUser(String username, String password, String fullName, String role, String status,
                        String email, String phone, int studentId) throws ValidationException {
        requireRoles("ADMIN");
        if (username == null || username.trim().isEmpty() || fullName == null || fullName.trim().isEmpty()) {
            throw new ValidationException("UC-02 / FR-01", "Tên đăng nhập và họ tên người dùng không được để trống!");
        }
        String cleanUser = username.trim().toLowerCase();
        String cleanEmail = (email == null || email.trim().isEmpty()) ? (cleanUser + "@tdtu.edu.vn") : email.trim().toLowerCase();
        for (User u : repo.getUsers()) {
            if (u.getUsername().equalsIgnoreCase(cleanUser)) {
                throw new ValidationException("UC-02 / FR-01", "Tên đăng nhập '" + cleanUser + "' đã tồn tại trên hệ thống!");
            }
            if (u.getEmail() != null && !u.getEmail().isEmpty() && u.getEmail().equalsIgnoreCase(cleanEmail)) {
                throw new ValidationException("UC-02 / FR-01", "Email '" + cleanEmail + "' đã được gắn với tài khoản '" + u.getUsername() + "'!");
            }
        }
        int nextId = repo.getUsers().size() + 1;
        for (User u : repo.getUsers()) if (u.getUserId() >= nextId) nextId = u.getUserId() + 1;
        String cleanRole = (role == null || role.trim().isEmpty()) ? "TRAINING_STAFF" : role.trim().toUpperCase();
        if (!"ADMIN".equals(cleanRole) && !"TRAINING_STAFF".equals(cleanRole)
                && !"CASHIER".equals(cleanRole) && !"TEACHER".equals(cleanRole) && !"STUDENT".equals(cleanRole)) {
            throw new ValidationException("UC-02 / FR-01", "Vai trò không hợp lệ! Phạm vi v3.0 chỉ gồm 5 vai trò: ADMIN (QT), TRAINING_STAFF (DT), CASHIER (TN), TEACHER (GV), STUDENT (HV).");
        }
        int actualStudentId = 0;
        if ("STUDENT".equals(cleanRole)) {
            if (studentId > 0) {
                boolean found = false;
                for (Student s : repo.getStudents()) {
                    if (s.getStudentId() == studentId) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new ValidationException("UC-02", "Mã hồ sơ học viên liên kết (ID: " + studentId + ") không tồn tại!");
                }
                actualStudentId = studentId;
            } else {
                // Tự động tạo hồ sơ học viên mới đồng bộ trong danh sách
                int nextStuId = repo.getStudents().size() + 1;
                for (Student s : repo.getStudents()) if (s.getStudentId() >= nextStuId) nextStuId = s.getStudentId() + 1;
                String stuCode = generateNextStudentCode();
                String sPhone = (phone == null || phone.trim().isEmpty()) ? ("0912345" + String.format("%03d", nextStuId)) : phone.trim();
                Student newStu = new Student(nextStuId, stuCode, fullName.trim(), "2005-01-01", "Chưa xác định",
                        sPhone, cleanEmail, "TP.HCM", "ACTIVE", todayStr());
                repo.getStudents().add(newStu);
                actualStudentId = nextStuId;
            }
        }
        String hash = PasswordUtil.sha256((password == null || password.isEmpty()) ? "123456" : password);
        String cleanPhone = (phone == null || phone.trim().isEmpty()) ? ("09010000" + String.format("%02d", nextId)) : phone.trim();
        // Nếu Admin trực tiếp tạo tài khoản (UC-02), kích hoạt ACTIVE ngay lập tức (không cần bước xác thực OTP Email)
        String finalStatus = (status != null && "LOCKED".equalsIgnoreCase(status.trim())) ? "LOCKED" : "ACTIVE";
        User created = new User(nextId, cleanUser, hash, fullName.trim(),
                cleanRole,
                finalStatus,
                cleanEmail, cleanPhone, actualStudentId, todayStr(), "Tạo bởi Admin (Kích hoạt ngay - UC-02)");
        repo.getUsers().add(created);
        repo.saveToDisk();
        return created;
    }

    /**
     * UC-02 / FR-01: Quản trị viên cập nhật vai trò, thông tin liên hệ hoặc khóa/mở khóa/kích hoạt tài khoản
     */
    public User updateUser(int userId, String fullName, String role, String status) throws ValidationException {
        return updateUser(userId, fullName, role, status, null, null, -1);
    }

    public User updateUser(int userId, String fullName, String role, String status,
                           String email, String phone, int studentId) throws ValidationException {
        requireRoles("ADMIN");
        for (User u : repo.getUsers()) {
            if (u.getUserId() == userId) {
                if ("admin".equalsIgnoreCase(u.getUsername())) {
                    if ("LOCKED".equalsIgnoreCase(status) || "PENDING_VERIFICATION".equalsIgnoreCase(status)) {
                        throw new ValidationException("UC-02 / BR-V3-11", "Không thể khóa hoặc hủy kích hoạt tài khoản Quản trị viên gốc (admin)!");
                    }
                    if (role != null && !"ADMIN".equalsIgnoreCase(role.trim())) {
                        throw new ValidationException("UC-02", "Không được tự tước quyền Quản trị viên (ADMIN) của tài khoản quản trị gốc!");
                    }
                }
                if (email != null && !email.trim().isEmpty()) {
                    for (User other : repo.getUsers()) {
                        if (other.getUserId() != userId && email.trim().equalsIgnoreCase(other.getEmail())) {
                            throw new ValidationException("UC-02", "Email '" + email.trim() + "' đã trùng với tài khoản '" + other.getUsername() + "'!");
                        }
                    }
                    u.setEmail(email.trim());
                }
                if (phone != null && !phone.trim().isEmpty()) u.setPhone(phone.trim());
                if (role != null && !role.trim().isEmpty()) {
                    String cleanRole = role.trim().toUpperCase();
                    if (!"ADMIN".equals(cleanRole) && !"TRAINING_STAFF".equals(cleanRole)
                            && !"CASHIER".equals(cleanRole) && !"TEACHER".equals(cleanRole) && !"STUDENT".equals(cleanRole)) {
                        throw new ValidationException("UC-02 / FR-01", "Vai trò không hợp lệ! Phạm vi v3.0 chỉ gồm 5 vai trò: ADMIN (QT), TRAINING_STAFF (DT), CASHIER (TN), TEACHER (GV), STUDENT (HV).");
                    }
                    u.setRole(cleanRole);
                }
                if ("STUDENT".equalsIgnoreCase(u.getRole())) {
                    if (studentId == -99) {
                        if (u.getStudentId() <= 0) {
                            int nextStuId = repo.getStudents().size() + 1;
                            for (Student s : repo.getStudents()) if (s.getStudentId() >= nextStuId) nextStuId = s.getStudentId() + 1;
                            String stuCode = generateNextStudentCode();
                            String sPhone = (phone != null && !phone.trim().isEmpty()) ? phone.trim() : u.getPhone();
                            String sEmail = (email != null && !email.trim().isEmpty()) ? email.trim() : u.getEmail();
                            Student newStu = new Student(nextStuId, stuCode, u.getFullName(), "2005-01-01", "Chưa xác định",
                                    sPhone, sEmail, "TP.HCM", "ACTIVE", todayStr());
                            repo.getStudents().add(newStu);
                            u.setStudentId(nextStuId);
                        }
                    } else if (studentId >= 0) {
                        u.setStudentId(studentId);
                    }
                } else {
                    u.setStudentId(0);
                }
                if (status != null && !status.trim().isEmpty()) u.setStatus(status.trim());
                repo.saveToDisk();
                return u;
            }
        }
        throw new ValidationException("UC-02", "Không tìm thấy tài khoản cần cập nhật!");
    }

    /**
     * UC-02 / FR-01 / BR-V3-13: Quản trị viên Reset mật khẩu cho bất kỳ tài khoản nào
     */
    public User resetUserPassword(int userId, String newPassword) throws ValidationException {
        requireRoles("ADMIN");
        for (User u : repo.getUsers()) {
            if (u.getUserId() == userId) {
                String rawPass = (newPassword == null || newPassword.trim().isEmpty()) ? "123456" : newPassword.trim();
                if (rawPass.length() < 6) {
                    throw new ValidationException("NFR-02", "Mật khẩu mới phải có tối thiểu 6 ký tự!");
                }
                u.setPasswordHash(PasswordUtil.sha256(rawPass));
                repo.saveToDisk();
                return u;
            }
        }
        throw new ValidationException("UC-02", "Không tìm thấy tài khoản cần đặt lại mật khẩu!");
    }

    // =========================================================================
    // UC-12 -> UC-17: CỔNG TỰ PHỤC VỤ HỌC VIÊN (ĐĂNG KÝ, XÁC THỰC EMAIL, ĐỔI/QUÊN MK)
    // =========================================================================
    public User registerStudentAccount(String fullName, String email, String phone, String password) throws ValidationException {
        return registerStudentAccount(null, fullName, email, phone, "2005-01-01", password);
    }

    public User registerStudentAccount(String username, String fullName, String email, String phone, String dob, String password) throws ValidationException {
        if (fullName == null || fullName.trim().isEmpty() || email == null || !email.contains("@") || !email.contains(".")) {
            throw new ValidationException("UC-12 / FR-11", "Vui lòng nhập đầy đủ họ tên và địa chỉ Email hợp lệ để nhận mã OTP!");
        }
        if (phone == null || !phone.trim().matches("^0[0-9]{9,10}$")) {
            throw new ValidationException("UC-12 / FR-11", "Số điện thoại phải gồm 10-11 chữ số bắt đầu bằng 0!");
        }
        if (password == null || password.length() < 6) {
            throw new ValidationException("UC-12 / NFR-02", "Mật khẩu phải có từ 6 ký tự trở lên!");
        }
        String cleanEmail = email.trim().toLowerCase();
        String requestedUname = (username != null && !username.trim().isEmpty())
                ? username.trim().replaceAll("[^a-zA-Z0-9_.]", "").toLowerCase()
                : cleanEmail.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "").toLowerCase();
        if (requestedUname.isEmpty()) {
            requestedUname = "hocvien" + (repo.getUsers().size() + 1);
        }

        for (User u : repo.getUsers()) {
            if (cleanEmail.equalsIgnoreCase(u.getEmail())) {
                throw new ValidationException("UC-12 / FR-11", "Email '" + cleanEmail + "' đã được đăng ký trên hệ thống! Vui lòng dùng email khác hoặc chuyển sang tab Xác thực Email (UC-13).");
            }
            if (u.getUsername().equalsIgnoreCase(requestedUname)) {
                if (username != null && !username.trim().isEmpty()) {
                    throw new ValidationException("UC-12 / FR-11", "Tên đăng nhập '" + requestedUname + "' đã tồn tại! Vui lòng chọn tên đăng nhập khác.");
                } else {
                    requestedUname = requestedUname + (repo.getUsers().size() + 1);
                }
            }
        }

        int nextStuId = repo.getStudents().size() + 1;
        for (Student s : repo.getStudents()) if (s.getStudentId() >= nextStuId) nextStuId = s.getStudentId() + 1;
        String stuCode = generateNextStudentCode();
        String cleanDob = (dob != null && !dob.trim().isEmpty()) ? dob.trim() : "2005-01-01";

        // Phát hành mã OTP 6 số ngẫu nhiên gắn chặt với đúng email vừa đăng ký và gửi qua Gmail SMTP thực tế
        String otpCode = generateSixDigitOtp();
        try {
            SmtpMailer.sendVerificationOtpEmail(cleanEmail, fullName.trim(), requestedUname, stuCode, otpCode);
        } catch (Exception ex) {
            throw new ValidationException("UC-12 / BR-V3-11", "Không thể gửi email xác thực OTP tới '" + cleanEmail + "'. Chi tiết SMTP: " + ex.getMessage());
        }

        emailOtpStore.put(cleanEmail, otpCode);
        emailOtpExpiry.put(cleanEmail, System.currentTimeMillis() + OTP_TTL_MS);

        // Tạo hồ sơ học viên liên kết + tài khoản PENDING_VERIFICATION trong cùng giao dịch (UC-12)
        Student newStu = new Student(nextStuId, stuCode, fullName.trim(), cleanDob, "Chưa xác định",
                phone.trim(), cleanEmail, "TP.HCM", "ACTIVE", todayStr());
        repo.getStudents().add(newStu);

        int nextUid = repo.getUsers().size() + 1;
        for (User u : repo.getUsers()) if (u.getUserId() >= nextUid) nextUid = u.getUserId() + 1;
        User newUser = new User(nextUid, requestedUname, PasswordUtil.sha256(password),
                fullName.trim() + " (" + stuCode + ")", "STUDENT", "PENDING_VERIFICATION",
                cleanEmail, phone.trim(), nextStuId, todayStr(), "Đã gửi OTP tới " + cleanEmail + " (UC-12)");
        repo.getUsers().add(newUser);
        repo.saveToDisk();
        return newUser;
    }

    public User resendVerificationOtp(String emailInput) throws ValidationException {
        if (emailInput == null || emailInput.trim().isEmpty() || !emailInput.contains("@")) {
            throw new ValidationException("UC-13 / BR-V3-11", "Vui lòng nhập đúng địa chỉ Email đã đăng ký tài khoản để gửi lại mã OTP!");
        }
        String cleanEmail = emailInput.trim().toLowerCase();
        for (User u : repo.getUsers()) {
            if (u.getEmail() != null && u.getEmail().equalsIgnoreCase(cleanEmail)) {
                String stuCode = "HV" + String.format("%03d", Math.max(1, u.getStudentId()));
                for (Student s : repo.getStudents()) {
                    if (s.getStudentId() == u.getStudentId()) {
                        stuCode = s.getStudentCode();
                        break;
                    }
                }
                String otpCode = generateSixDigitOtp();
                try {
                    SmtpMailer.sendVerificationOtpEmail(cleanEmail, u.getFullName(), u.getUsername(), stuCode, otpCode);
                } catch (Exception ex) {
                    throw new ValidationException("UC-13 / BR-V3-11", "Lỗi gửi email OTP tới '" + cleanEmail + "': " + ex.getMessage());
                }
                emailOtpStore.put(cleanEmail, otpCode);
                emailOtpExpiry.put(cleanEmail, System.currentTimeMillis() + OTP_TTL_MS);
                return u;
            }
        }
        throw new ValidationException("UC-13", "Không tìm thấy tài khoản nào đăng ký với địa chỉ email '" + cleanEmail + "'!");
    }

    public User verifyStudentEmail(String emailInput, String otpInput) throws ValidationException {
        if (emailInput == null || emailInput.trim().isEmpty() || !emailInput.contains("@")) {
            throw new ValidationException("UC-13 / BR-V3-11", "Vui lòng nhập chính xác địa chỉ Email đã dùng khi đăng ký tài khoản để xác minh!");
        }
        if (otpInput == null || otpInput.trim().isEmpty()) {
            throw new ValidationException("UC-13 / BR-V3-12", "Vui lòng nhập mã OTP 6 chữ số đã được gửi về hộp thư Email của bạn!");
        }
        String cleanEmail = emailInput.trim().toLowerCase();
        String cleanOtp = otpInput.trim();

        User targetUser = null;
        for (User u : repo.getUsers()) {
            if (u.getEmail() != null && u.getEmail().equalsIgnoreCase(cleanEmail)) {
                targetUser = u;
                break;
            }
        }
        if (targetUser == null) {
            throw new ValidationException("UC-13 / BR-V3-11", "Email '" + cleanEmail + "' không khớp với bất kỳ tài khoản chờ xác thực nào trên hệ thống!");
        }

        String expectedOtp = emailOtpStore.get(cleanEmail);
        Long expiry = emailOtpExpiry.get(cleanEmail);
        if (expectedOtp == null || expiry == null) {
            throw new ValidationException("UC-13 / BR-V3-12", "Chưa có mã OTP nào được cấp cho email '" + cleanEmail + "' hoặc mã đã được sử dụng! Vui lòng nhấn 'Gửi lại mã OTP về Email'.");
        }
        if (System.currentTimeMillis() > expiry.longValue()) {
            emailOtpStore.remove(cleanEmail);
            emailOtpExpiry.remove(cleanEmail);
            throw new ValidationException("UC-13 / BR-V3-12", "Mã OTP của email '" + cleanEmail + "' đã hết hạn (quá 10 phút)! Vui lòng nhấn 'Gửi lại mã OTP về Email'.");
        }
        if (!expectedOtp.equalsIgnoreCase(cleanOtp)) {
            throw new ValidationException("UC-13 / BR-V3-12", "Mã OTP '" + cleanOtp + "' không chính xác! Vui lòng kiểm tra hộp thư '" + cleanEmail + "' và nhập đúng mã 6 chữ số.");
        }

        // Tiêu hủy OTP ngay sau khi xác thực thành công (Single-use — BR-V3-12)
        emailOtpStore.remove(cleanEmail);
        emailOtpExpiry.remove(cleanEmail);

        targetUser.setStatus("ACTIVE");
        targetUser.setLastLogin("Đã xác thực OTP Email (" + nowDateTimeStr() + ")");
        repo.saveToDisk();
        return targetUser;
    }

    public User changeOwnPassword(String currentPassword, String newPassword) throws ValidationException {
        if (currentUser == null) throw new ValidationException("UC-15", "Vui lòng đăng nhập để đổi mật khẩu!");
        if (currentPassword == null || newPassword == null || newPassword.length() < 6) {
            throw new ValidationException("UC-15 / FR-13", "Mật khẩu mới phải từ 6 ký tự trở lên!");
        }
        String curHash = PasswordUtil.sha256(currentPassword);
        if (!currentUser.getPasswordHash().equals(curHash)) {
            throw new ValidationException("UC-15", "Mật khẩu hiện tại không chính xác!");
        }
        if (currentPassword.equals(newPassword)) {
            throw new ValidationException("UC-15", "Mật khẩu mới không được trùng với mật khẩu hiện tại!");
        }
        currentUser.setPasswordHash(PasswordUtil.sha256(newPassword));
        repo.saveToDisk();
        return currentUser;
    }

    public User requestPasswordResetOtp(String emailOrUser) throws ValidationException {
        if (emailOrUser == null || emailOrUser.trim().isEmpty()) {
            throw new ValidationException("UC-16 / BR-V3-12", "Vui lòng nhập địa chỉ Email của tài khoản cần khôi phục mật khẩu!");
        }
        String key = emailOrUser.trim().toLowerCase();
        for (User u : repo.getUsers()) {
            if ((u.getEmail() != null && u.getEmail().equalsIgnoreCase(key)) || u.getUsername().equalsIgnoreCase(key)) {
                String targetEmail = u.getEmail();
                if (targetEmail == null || !targetEmail.contains("@")) {
                    throw new ValidationException("UC-16", "Tài khoản '@" + u.getUsername() + "' chưa có địa chỉ Email hợp lệ!");
                }
                String cleanEmail = targetEmail.trim().toLowerCase();
                String otpCode = generateSixDigitOtp();
                try {
                    SmtpMailer.sendPasswordResetOtpEmail(cleanEmail, u.getFullName(), u.getUsername(), otpCode);
                } catch (Exception ex) {
                    throw new ValidationException("UC-16 / BR-V3-12", "Không thể gửi email OTP khôi phục mật khẩu tới '" + cleanEmail + "': " + ex.getMessage());
                }
                resetOtpStore.put(cleanEmail, otpCode);
                resetOtpExpiry.put(cleanEmail, System.currentTimeMillis() + OTP_TTL_MS);
                return u;
            }
        }
        throw new ValidationException("UC-16", "Không tìm thấy tài khoản nào khớp với '" + emailOrUser.trim() + "'!");
    }

    public User resetPasswordByToken(String emailOrUser, String token, String newPassword) throws ValidationException {
        if (emailOrUser == null || emailOrUser.trim().isEmpty() || token == null || token.trim().isEmpty()) {
            throw new ValidationException("UC-17 / BR-V3-12", "Vui lòng nhập đầy đủ Email và mã OTP 6 chữ số nhận qua Email!");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new ValidationException("UC-17", "Mật khẩu mới phải có ít nhất 6 ký tự!");
        }
        String key = emailOrUser.trim().toLowerCase();
        String cleanOtp = token.trim();
        for (User u : repo.getUsers()) {
            if ((u.getEmail() != null && u.getEmail().equalsIgnoreCase(key)) || u.getUsername().equalsIgnoreCase(key)) {
                String cleanEmail = u.getEmail() != null ? u.getEmail().trim().toLowerCase() : "";
                String expectedOtp = resetOtpStore.get(cleanEmail);
                Long expiry = resetOtpExpiry.get(cleanEmail);
                if (expectedOtp == null || expiry == null) {
                    throw new ValidationException("UC-17 / BR-V3-12", "Chưa có mã OTP đặt lại mật khẩu nào được gửi tới '" + cleanEmail + "'! Vui lòng nhấn 'Gửi mã OTP về Email (UC-16)' trước.");
                }
                if (System.currentTimeMillis() > expiry.longValue()) {
                    resetOtpStore.remove(cleanEmail);
                    resetOtpExpiry.remove(cleanEmail);
                    throw new ValidationException("UC-17 / BR-V3-12", "Mã OTP đặt lại mật khẩu đã hết hạn (quá 10 phút)! Vui lòng gửi lại mã mới.");
                }
                if (!expectedOtp.equalsIgnoreCase(cleanOtp)) {
                    throw new ValidationException("UC-17 / BR-V3-12", "Mã OTP '" + cleanOtp + "' không chính xác! Vui lòng kiểm tra email '" + cleanEmail + "'.");
                }
                resetOtpStore.remove(cleanEmail);
                resetOtpExpiry.remove(cleanEmail);
                u.setPasswordHash(PasswordUtil.sha256(newPassword));
                repo.saveToDisk();
                return u;
            }
        }
        throw new ValidationException("UC-17", "Không tìm thấy tài khoản khớp với yêu cầu đặt lại mật khẩu!");
    }

    // =========================================================================
    // FR-02: QUẢN LÝ HỌC VIÊN (UC-02, BR-01, BR-08, TC-01, TC-02)
    // =========================================================================
    public List<Student> searchStudents(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<Student>(repo.getStudents());
        }
        String kw = keyword.trim().toLowerCase();
        List<Student> result = new ArrayList<Student>();
        for (Student s : repo.getStudents()) {
            if (s.getStudentCode().toLowerCase().contains(kw)
                    || s.getFullName().toLowerCase().contains(kw)
                    || s.getPhone().toLowerCase().contains(kw)
                    || s.getEmail().toLowerCase().contains(kw)) {
                result.add(s);
            }
        }
        return result;
    }

    public String generateNextStudentCode() {
        int max = 0;
        for (Student s : repo.getStudents()) {
            if (s.getStudentCode().startsWith("HV")) {
                try {
                    int n = Integer.parseInt(s.getStudentCode().substring(2));
                    if (n > max) max = n;
                } catch (NumberFormatException ignored) {}
            }
        }
        return String.format("HV%03d", max + 1);
    }

    public Student addStudent(String studentCode, String fullName, String dob, String gender,
                              String phone, String email, String address) throws ValidationException {
        requireRoles("ADMIN", "TRAINING_STAFF");
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new ValidationException("UC-03 / TC-V3-02", "Họ và tên học viên không được để trống!");
        }
        if (phone == null || !phone.trim().matches("^0[0-9]{9,10}$")) {
            throw new ValidationException("UC-03 / FR-02", "Số điện thoại không hợp lệ (phải gồm 10-11 chữ số bắt đầu bằng 0)!");
        }
        if (email == null || !email.trim().contains("@")) {
            throw new ValidationException("UC-03 / FR-02", "Địa chỉ Email không hợp lệ!");
        }
        if (dob == null || !dob.trim().matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            throw new ValidationException("UC-03 / FR-02", "Ngày sinh phải đúng định dạng YYYY-MM-DD!");
        }
        String code = (studentCode == null || studentCode.trim().isEmpty())
                ? generateNextStudentCode() : studentCode.trim().toUpperCase();

        for (Student existing : repo.getStudents()) {
            if (existing.getStudentCode().equalsIgnoreCase(code)) {
                throw new ValidationException("UC-03 / NFR-07", "Mã học viên '" + code + "' đã tồn tại trên hệ thống!");
            }
            if (existing.getPhone().equals(phone.trim())) {
                throw new ValidationException("UC-03 / NFR-07", "Số điện thoại '" + phone.trim() + "' đã được đăng ký cho học viên " + existing.getFullName() + "!");
            }
        }

        int nextId = repo.getStudents().size() + 1;
        for (Student s : repo.getStudents()) {
            if (s.getStudentId() >= nextId) nextId = s.getStudentId() + 1;
        }

        Student created = new Student(nextId, code, fullName.trim(), dob.trim(),
                gender == null ? "Nam" : gender, phone.trim(), email.trim(),
                address == null ? "" : address.trim(), "ACTIVE", todayStr());
        repo.getStudents().add(created);
        repo.executeMySqlInsertStudent(created);
        repo.saveToDisk();
        return created;
    }

    public Student updateStudent(int studentId, String fullName, String dob, String gender,
                                 String phone, String email, String address, String status) throws ValidationException {
        if (currentUser != null && "STUDENT".equals(currentUser.getRole())) {
            int ownSid = currentUser.getStudentId() > 0 ? currentUser.getStudentId() : 1;
            if (studentId != ownSid) {
                throw new ValidationException("BR-V3-10 / NFR-03", "Học viên chỉ được phép chỉnh sửa hồ sơ cá nhân gắn với tài khoản đăng nhập của chính mình!");
            }
        } else {
            requireRoles("ADMIN", "TRAINING_STAFF");
        }
        Student target = repo.findStudentById(studentId);
        if (target == null) {
            throw new ValidationException("UC-03 / FR-02", "Không tìm thấy hồ sơ học viên cần cập nhật!");
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new ValidationException("UC-03 / TC-V3-02", "Họ và tên học viên không được để trống!");
        }
        if (phone == null || !phone.trim().matches("^0[0-9]{9,10}$")) {
            throw new ValidationException("UC-03 / FR-02", "Số điện thoại không hợp lệ!");
        }
        if (email == null || !email.trim().contains("@")) {
            throw new ValidationException("UC-03 / FR-02", "Email không hợp lệ!");
        }
        for (Student other : repo.getStudents()) {
            if (other.getStudentId() != studentId && other.getPhone().equals(phone.trim())) {
                throw new ValidationException("UC-03 / NFR-07", "Số điện thoại này đã trùng với học viên " + other.getFullName() + "!");
            }
        }
        target.setFullName(fullName.trim());
        target.setDob(dob.trim());
        target.setGender(gender);
        target.setPhone(phone.trim());
        target.setEmail(email.trim());
        target.setAddress(address == null ? "" : address.trim());
        if (status != null && !status.isEmpty()) {
            target.setStatus(status);
        }
        repo.saveToDisk();
        return target;
    }

    /**
     * UC-03 / Phân quyền ADMIN: Xóa hồ sơ học viên và đồng bộ cascade toàn bộ dữ liệu liên quan
     * (gỡ liên kết User, xóa Enrollments, TuitionFees, Payments, Attendances).
     */
    public void deleteStudent(int studentId) throws ValidationException {
        requireRoles("ADMIN");
        Student target = repo.findStudentById(studentId);
        if (target == null) {
            throw new ValidationException("UC-03", "Không tìm thấy hồ sơ học viên cần xóa (ID: " + studentId + ")!");
        }

        // 1. Unlink User accounts pointing to this student
        for (User u : repo.getUsers()) {
            if (u.getStudentId() == studentId) {
                u.setStudentId(0);
            }
        }

        // 2. Cascade delete Attendances of this student
        repo.getAttendances().removeIf(a -> a.getStudentId() == studentId);

        // 3. Find all enrollments of this student
        List<Integer> enrollmentIds = new ArrayList<Integer>();
        for (Enrollment e : repo.getEnrollments()) {
            if (e.getStudentId() == studentId) {
                enrollmentIds.add(e.getEnrollmentId());
            }
        }

        // 4. Find all tuition fee IDs linked to these enrollments
        List<Integer> tuitionIds = new ArrayList<Integer>();
        for (TuitionFee tf : repo.getTuitionFees()) {
            if (enrollmentIds.contains(tf.getEnrollmentId())) {
                tuitionIds.add(tf.getTuitionId());
            }
        }

        // 5. Cascade delete Payments for these tuitions
        repo.getPayments().removeIf(p -> tuitionIds.contains(p.getTuitionId()));

        // 6. Cascade delete TuitionFees
        repo.getTuitionFees().removeIf(tf -> enrollmentIds.contains(tf.getEnrollmentId()));

        // 7. Cascade delete Enrollments
        repo.getEnrollments().removeIf(e -> e.getStudentId() == studentId);

        // 8. Remove the Student
        repo.getStudents().removeIf(s -> s.getStudentId() == studentId);

        // 9. Persist to disk
        repo.saveToDisk();
    }

    // =========================================================================
    // QUẢN LÝ GIẢNG VIÊN (UC-05 · FR-04)
    // =========================================================================
    public Teacher addTeacher(String teacherCode, String fullName, String email, String phone, String specialty) throws ValidationException {
        requireRoles("ADMIN", "TRAINING_STAFF");
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new ValidationException("UC-05 / FR-04", "Họ và tên giảng viên không được để trống!");
        }
        if (phone == null || !phone.trim().matches("^0[0-9]{9,10}$")) {
            throw new ValidationException("UC-05 / FR-04", "Số điện thoại giảng viên không hợp lệ!");
        }
        if (email == null || !email.trim().contains("@")) {
            throw new ValidationException("UC-05 / FR-04", "Email giảng viên không hợp lệ!");
        }
        int nextId = repo.getTeachers().size() + 1;
        for (Teacher t : repo.getTeachers()) if (t.getTeacherId() >= nextId) nextId = t.getTeacherId() + 1;
        String code = (teacherCode == null || teacherCode.trim().isEmpty())
                ? String.format("GV%03d", nextId) : teacherCode.trim().toUpperCase();
        for (Teacher t : repo.getTeachers()) {
            if (t.getTeacherCode().equalsIgnoreCase(code)) {
                throw new ValidationException("UC-05 / NFR-07", "Mã giảng viên '" + code + "' đã tồn tại!");
            }
        }
        Teacher created = new Teacher(nextId, code, fullName.trim(), email.trim(), phone.trim(),
                specialty == null || specialty.trim().isEmpty() ? "Tiếng Anh Học thuật" : specialty.trim(), "ACTIVE");
        repo.getTeachers().add(created);
        repo.saveToDisk();
        return created;
    }

    public Teacher updateTeacher(int teacherId, String fullName, String email, String phone, String specialty, String status) throws ValidationException {
        requireRoles("ADMIN", "TRAINING_STAFF");
        Teacher target = repo.findTeacherById(teacherId);
        if (target == null) throw new ValidationException("UC-05 / FR-04", "Không tìm thấy hồ sơ giảng viên!");
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new ValidationException("UC-05 / FR-04", "Họ tên giảng viên không được để trống!");
        }
        target.setFullName(fullName.trim());
        target.setEmail(email == null ? "" : email.trim());
        target.setPhone(phone == null ? "" : phone.trim());
        target.setSpecialization(specialty == null ? "" : specialty.trim());
        if (status != null && !status.isEmpty()) target.setStatus(status);
        repo.saveToDisk();
        return target;
    }

    // =========================================================================
    // FR-03, FR-04: QUẢN LÝ KHÓA HỌC & LỚP HỌC (UC-04, UC-05 · BR-V3-01, BR-V3-02, BR-V3-05)
    // =========================================================================
    public List<Course> getAllCourses() {
        return new ArrayList<Course>(repo.getCourses());
    }

    public List<Teacher> getAllTeachers() {
        return new ArrayList<Teacher>(repo.getTeachers());
    }

    public List<ClassRoom> getAllClasses() {
        return new ArrayList<ClassRoom>(repo.getClasses());
    }

    public Course addCourse(String courseCode, String courseName, String description,
                            String duration, long tuition, String entryLevel, String status) throws ValidationException {
        requireRoles("ADMIN", "TRAINING_STAFF");
        if (courseCode == null || courseCode.trim().isEmpty() || courseName == null || courseName.trim().isEmpty()) {
            throw new ValidationException("UC-04 / FR-03", "Mã khóa học và tên khóa học không được để trống!");
        }
        if (tuition < 0) {
            throw new ValidationException("UC-04 / FR-03", "Học phí mặc định không được nhỏ hơn 0!");
        }
        for (Course c : repo.getCourses()) {
            if (c.getCourseCode().equalsIgnoreCase(courseCode.trim())) {
                throw new ValidationException("UC-04 / FR-03", "Mã khóa học '" + courseCode + "' đã tồn tại!");
            }
        }
        int nextId = repo.getCourses().size() + 1;
        Course created = new Course(nextId, courseCode.trim().toUpperCase(), courseName.trim(),
                description == null ? "" : description.trim(),
                duration == null || duration.trim().isEmpty() ? "3 tháng" : duration.trim(),
                tuition, entryLevel == null ? "Mọi trình độ" : entryLevel.trim(),
                status == null ? "ACTIVE" : status);
        repo.getCourses().add(created);
        repo.saveToDisk();
        return created;
    }

    public Course updateCourse(int courseId, String courseName, String description,
                               String duration, long tuition, String entryLevel, String status) throws ValidationException {
        requireRoles("ADMIN", "TRAINING_STAFF");
        Course c = repo.findCourseById(courseId);
        if (c == null) throw new ValidationException("UC-04 / FR-03", "Không tìm thấy khóa học!");
        if (courseName == null || courseName.trim().isEmpty()) {
            throw new ValidationException("UC-04 / FR-03", "Tên khóa học không được để trống!");
        }
        if (tuition < 0) {
            throw new ValidationException("UC-04 / FR-03", "Học phí không được âm!");
        }
        c.setCourseName(courseName.trim());
        c.setDescription(description == null ? "" : description.trim());
        c.setDuration(duration.trim());
        c.setTuition(tuition);
        c.setEntryLevel(entryLevel.trim());
        c.setStatus(status);
        repo.saveToDisk();
        return c;
    }

    /**
     * Đếm sĩ số hiện tại của lớp (chỉ tính các đăng ký có trạng thái CONFIRMED theo BR-V3-02).
     */
    public int getConfirmedEnrollmentCount(int classId) {
        int count = 0;
        for (Enrollment e : repo.getEnrollments()) {
            if (e.getClassId() == classId && "CONFIRMED".equals(e.getStatus())) {
                count++;
            }
        }
        return count;
    }

    public ClassRoom addClassRoom(String classCode, int courseId, int teacherId, int capacity,
                                  String startDate, String endDate, String scheduleSummary,
                                  String room, String status) throws ValidationException {
        requireRoles("ADMIN", "TRAINING_STAFF");
        if (classCode == null || classCode.trim().isEmpty()) {
            throw new ValidationException("UC-05 / FR-04", "Mã lớp học không được để trống!");
        }
        if (capacity <= 0) {
            throw new ValidationException("UC-05 / BR-V3-02", "Sĩ số tối đa của lớp phải lớn hơn 0!");
        }
        Course course = repo.findCourseById(courseId);
        if (course == null) {
            throw new ValidationException("UC-05 / FR-04", "Khóa học trực thuộc không tồn tại!");
        }
        Teacher teacher = repo.findTeacherById(teacherId);
        if (teacher == null) {
            throw new ValidationException("UC-05 / FR-04", "Giảng viên phụ trách không tồn tại!");
        }
        if (startDate == null || endDate == null || startDate.compareTo(endDate) > 0) {
            throw new ValidationException("UC-05 / FR-04", "Ngày kết thúc lớp học phải sau ngày bắt đầu!");
        }
        String cleanCode = classCode.trim().toUpperCase();
        String cleanRoom = room == null || room.trim().isEmpty() ? "P.201" : room.trim();
        for (ClassRoom existing : repo.getClasses()) {
            if (existing.getClassCode().equalsIgnoreCase(cleanCode)) {
                throw new ValidationException("UC-05 / FR-04", "Mã lớp '" + cleanCode + "' đã tồn tại!");
            }
            // UC-05: Kiểm tra trùng lịch giảng viên hoặc trùng phòng học đối với các lớp đang mở
            if (("OPEN".equals(existing.getStatus()) || "ONGOING".equals(existing.getStatus()))
                    && existing.getScheduleSummary().equalsIgnoreCase(scheduleSummary.trim())) {
                if (existing.getTeacherId() == teacherId) {
                    throw new ValidationException("UC-05 / FR-04", "Giảng viên " + teacher.getFullName()
                            + " đã được phân công dạy lớp " + existing.getClassCode()
                            + " trùng lịch (" + scheduleSummary + ")!");
                }
                if (existing.getRoom().equalsIgnoreCase(cleanRoom)) {
                    throw new ValidationException("UC-05 / FR-04", "Phòng học " + cleanRoom
                            + " đã được xếp cho lớp " + existing.getClassCode()
                            + " vào cùng khung giờ (" + scheduleSummary + ")!");
                }
            }
        }

        int nextId = repo.getClasses().size() + 1;
        for (ClassRoom c : repo.getClasses()) if (c.getClassId() >= nextId) nextId = c.getClassId() + 1;

        ClassRoom created = new ClassRoom(nextId, cleanCode, courseId, teacherId, capacity,
                startDate.trim(), endDate.trim(), scheduleSummary.trim(),
                cleanRoom,
                status == null ? "OPEN" : status);
        repo.getClasses().add(created);
        repo.saveToDisk();
        return created;
    }

    public ClassRoom updateClassRoom(int classId, int courseId, int teacherId, int capacity,
                                     String startDate, String endDate, String scheduleSummary,
                                     String room, String status) throws ValidationException {
        requireRoles("ADMIN", "TRAINING_STAFF");
        ClassRoom target = repo.findClassById(classId);
        if (target == null) {
            throw new ValidationException("UC-05 / FR-04", "Không tìm thấy lớp học cần cập nhật!");
        }
        if (capacity <= 0) {
            throw new ValidationException("UC-05 / BR-V3-02", "Sĩ số tối đa phải lớn hơn 0!");
        }
        int currentEnrolled = getConfirmedEnrollmentCount(classId);
        if (capacity < currentEnrolled) {
            throw new ValidationException("UC-05 / BR-V3-02", "Không thể giảm sĩ số tối đa (" + capacity
                    + ") xuống thấp hơn số học viên đang đăng ký hiệu lực (" + currentEnrolled + ")!");
        }
        Teacher teacher = repo.findTeacherById(teacherId);
        for (ClassRoom other : repo.getClasses()) {
            if (other.getClassId() != classId
                    && ("OPEN".equals(other.getStatus()) || "ONGOING".equals(other.getStatus()))
                    && ("OPEN".equals(status) || "ONGOING".equals(status))
                    && other.getScheduleSummary().equalsIgnoreCase(scheduleSummary.trim())) {
                if (other.getTeacherId() == teacherId) {
                    throw new ValidationException("UC-05 / FR-04", "Giảng viên " + (teacher != null ? teacher.getFullName() : "")
                            + " bị trùng lịch với lớp " + other.getClassCode() + "!");
                }
                if (other.getRoom().equalsIgnoreCase(room.trim())) {
                    throw new ValidationException("UC-05 / FR-04", "Phòng học " + room.trim()
                            + " bị trùng lịch với lớp " + other.getClassCode() + "!");
                }
            }
        }
        target.setCourseId(courseId);
        target.setTeacherId(teacherId);
        target.setCapacity(capacity);
        target.setStartDate(startDate.trim());
        target.setEndDate(endDate.trim());
        target.setScheduleSummary(scheduleSummary.trim());
        target.setRoom(room.trim());
        target.setStatus(status);
        repo.saveToDisk();
        return target;
    }

    // =========================================================================
    // FR-05, FR-06: ĐĂNG KÝ LỚP HỌC & TỰ ĐỘNG TẠO HỌC PHÍ
    // (UC-06, UC-19 · BR-V3-01 -> BR-V3-05 · TC-V3-05, TC-V3-06)
    // =========================================================================
    public List<Enrollment> getAllEnrollments() {
        return new ArrayList<Enrollment>(repo.getEnrollments());
    }

    public Enrollment enrollStudent(int studentId, int classId) throws ValidationException {
        if (currentUser != null && "STUDENT".equals(currentUser.getRole())) {
            int ownSid = currentUser.getStudentId() > 0 ? currentUser.getStudentId() : 1;
            if (studentId != ownSid) {
                throw new ValidationException("UC-19 / BR-V3-14", "Học viên chỉ được tự đăng ký lớp học cho chính hồ sơ học viên của phiên đăng nhập!");
            }
        } else {
            requireRoles("ADMIN", "TRAINING_STAFF");
        }
        // UC-06: Học viên phải có hồ sơ hợp lệ trước khi đăng ký
        Student student = repo.findStudentById(studentId);
        if (student == null || !"ACTIVE".equals(student.getStatus())) {
            throw new ValidationException("UC-06 / FR-05", "Học viên không tồn tại hoặc hồ sơ đã ngừng hoạt động!");
        }

        // BR-V3-01: Lớp OPEN được đăng ký; IN_PROGRESS, COMPLETED, CANCELLED/CLOSED không nhận đăng ký mới
        ClassRoom clazz = repo.findClassById(classId);
        if (clazz == null) {
            throw new ValidationException("UC-06 / FR-05", "Lớp học không tồn tại!");
        }
        if (!"OPEN".equals(clazz.getStatus())) {
            throw new ValidationException("BR-V3-01 / UC-06", "Lớp " + clazz.getClassCode()
                    + " hiện ở trạng thái '" + clazz.getStatus() + "', không tiếp nhận đăng ký mới (chỉ lớp OPEN mới nhận đăng ký)!");
        }

        // BR-V3-03 & TC-V3-06: Không tồn tại hai đăng ký CONFIRMED của cùng học viên/lớp
        for (Enrollment e : repo.getEnrollments()) {
            if (e.getStudentId() == studentId && e.getClassId() == classId && "CONFIRMED".equals(e.getStatus())) {
                throw new ValidationException("BR-V3-03 / TC-V3-06", "Học viên " + student.getFullName()
                        + " đã có đăng ký hợp lệ (" + e.getEnrollmentCode() + ") trong lớp " + clazz.getClassCode() + "!");
            }
        }

        // BR-V3-02 & TC-V3-06: Sĩ số = số enrollment CONFIRMED (chưa hủy), phải <= capacity
        int currentCount = getConfirmedEnrollmentCount(classId);
        if (currentCount >= clazz.getCapacity()) {
            throw new ValidationException("BR-V3-02 / TC-V3-06", "Lớp " + clazz.getClassCode()
                    + " đã đủ sĩ số tối đa (" + currentCount + "/" + clazz.getCapacity() + "). Hệ thống từ chối đăng ký!");
        }

        // Tạo bản ghi đăng ký + nghĩa vụ học phí trong cùng giao dịch (UC-06 / FR-05, FR-06 / BR-V3-04, BR-V3-05)
        int nextEnrollmentId = repo.getEnrollments().size() + 1;
        for (Enrollment e : repo.getEnrollments()) {
            if (e.getEnrollmentId() >= nextEnrollmentId) nextEnrollmentId = e.getEnrollmentId() + 1;
        }
        String enrollCode = String.format("DK%03d", nextEnrollmentId);
        String staffName = currentUser != null ? currentUser.getFullName() : "Nguyễn Trọng Phúc (Đào tạo)";

        Enrollment enrollment = new Enrollment(
                nextEnrollmentId, enrollCode, studentId, classId, todayStr(), staffName, "CONFIRMED", ""
        );

        Course course = repo.findCourseById(clazz.getCourseId());
        long tuitionAmount = (course != null) ? course.getTuition() : 3000000L;

        int nextTuitionId = repo.getTuitionFees().size() + 1;
        for (TuitionFee tf : repo.getTuitionFees()) {
            if (tf.getTuitionId() >= nextTuitionId) nextTuitionId = tf.getTuitionId() + 1;
        }
        TuitionFee tuitionFee = new TuitionFee(nextTuitionId, nextEnrollmentId, tuitionAmount, todayStr(), "UNPAID");

        repo.getEnrollments().add(enrollment);
        repo.getTuitionFees().add(tuitionFee);
        repo.saveToDisk();
        return enrollment;
    }

    public void cancelEnrollment(int enrollmentId, String reason) throws ValidationException {
        requireRoles("ADMIN", "TRAINING_STAFF");
        Enrollment enrollment = repo.findEnrollmentById(enrollmentId);
        if (enrollment == null) {
            throw new ValidationException("UC-06 / FR-05", "Không tìm thấy bản ghi đăng ký!");
        }
        if ("CANCELLED".equals(enrollment.getStatus())) {
            throw new ValidationException("UC-06 / BR-V3-02", "Đăng ký này đã ở trạng thái hủy từ trước!");
        }
        // UC-06 & BR-V3-07: Chỉ cho phép hủy trực tiếp khi chưa thu tiền (paid == 0).
        for (TuitionFee tf : repo.getTuitionFees()) {
            if (tf.getEnrollmentId() == enrollmentId) {
                long paid = getTotalPaidForTuition(tf.getTuitionId());
                if (paid > 0) {
                    throw new ValidationException("UC-06 / BR-V3-07", "Đăng ký " + enrollment.getEnrollmentCode()
                            + " đã phát sinh thanh toán (" + paid + " VNĐ). Hệ thống chặn hủy trực tiếp để bảo toàn lịch sử thu tiền và công nợ (BR-V3-07)!");
                }
                break;
            }
        }
        enrollment.setStatus("CANCELLED");
        enrollment.setCancelReason(reason == null || reason.trim().isEmpty() ? "Học viên xin hủy lớp (chưa phát sinh thu học phí)" : reason.trim());
        repo.saveToDisk();
    }

    public void deleteCancelledEnrollment(int enrollmentId) throws ValidationException {
        requireRoles("ADMIN", "TRAINING_STAFF");
        Enrollment target = null;
        for (Enrollment e : repo.getEnrollments()) {
            if (e.getEnrollmentId() == enrollmentId) {
                target = e;
                break;
            }
        }
        if (target == null) {
            throw new ValidationException("UC-06 / FR-05", "Không tìm thấy bản ghi đăng ký cần xóa!");
        }
        if (!"CANCELLED".equalsIgnoreCase(target.getStatus())) {
            throw new ValidationException("UC-06 / BR-V3-02", "Chỉ được phép xóa các bản ghi đăng ký đã ở trạng thái CANCELLED (Đã hủy)!");
        }
        // Xóa nghĩa vụ học phí đi kèm của bản ghi đăng ký đã hủy
        java.util.Iterator<TuitionFee> tfIt = repo.getTuitionFees().iterator();
        while (tfIt.hasNext()) {
            TuitionFee tf = tfIt.next();
            if (tf.getEnrollmentId() == enrollmentId) {
                tfIt.remove();
            }
        }
        repo.getEnrollments().remove(target);
        repo.saveToDisk();
    }

    // =========================================================================
    // FR-10, FR-11: QUẢN LÝ HỌC PHÍ & THANH TOÁN (UC-05, BR-16 -> BR-19, TC-06, TC-07)
    // =========================================================================
    public List<TuitionFee> getAllTuitionFees() {
        return new ArrayList<TuitionFee>(repo.getTuitionFees());
    }

    public List<Payment> getPaymentsByTuitionId(int tuitionId) {
        List<Payment> list = new ArrayList<Payment>();
        for (Payment p : repo.getPayments()) {
            if (p.getTuitionId() == tuitionId) {
                list.add(p);
            }
        }
        return list;
    }

    public List<Payment> getAllPayments() {
        return new ArrayList<Payment>(repo.getPayments());
    }

    public long getTotalPaidForTuition(int tuitionId) {
        long sum = 0L;
        for (Payment p : repo.getPayments()) {
            if (p.getTuitionId() == tuitionId) {
                sum += p.getAmount();
            }
        }
        return sum;
    }

    /**
     * BR-19: Công nợ = Học phí phải thu - Tổng thanh toán hợp lệ
     */
    public long getRemainingDebt(int tuitionId) {
        TuitionFee tf = repo.findTuitionById(tuitionId);
        if (tf == null) return 0L;
        long rem = tf.getAmountDue() - getTotalPaidForTuition(tuitionId);
        return Math.max(0L, rem);
    }

    public Payment recordPayment(int tuitionId, long amount, String method, String note) throws ValidationException {
        requireRoles("ADMIN", "CASHIER");
        TuitionFee tf = repo.findTuitionById(tuitionId);
        if (tf == null) {
            throw new ValidationException("FR-10", "Không tìm thấy khoản nghĩa vụ học phí cần thanh toán!");
        }
        // BR-V3-06: Thanh toán > 0, không vượt công nợ; chống ghi nhận lặp
        if (amount <= 0) {
            throw new ValidationException("BR-V3-06 / UC-09", "Số tiền thanh toán phải lớn hơn 0 VNĐ!");
        }
        long remainingDebt = getRemainingDebt(tuitionId);
        if (remainingDebt == 0) {
            throw new ValidationException("BR-V3-06 / BR-V3-07", "Khoản học phí này đã được thanh toán đủ 100% (PAID), không còn công nợ!");
        }
        if (amount > remainingDebt) {
            throw new ValidationException("BR-V3-06 / TC-V3-09", "Số tiền thanh toán (" + amount
                    + " VNĐ) vượt quá số tiền còn nợ (" + remainingDebt + " VNĐ)!");
        }

        int nextPayId = repo.getPayments().size() + 1;
        for (Payment p : repo.getPayments()) {
            if (p.getPaymentId() >= nextPayId) nextPayId = p.getPaymentId() + 1;
        }
        String payCode = String.format("PT%03d", nextPayId);
        String cashier = currentUser != null ? currentUser.getFullName() : "Lê Khải Hoàn (Thu ngân)";

        Payment payment = new Payment(nextPayId, payCode, tuitionId, amount, todayStr(),
                method == null ? "CASH" : method, cashier, note == null ? "" : note.trim());
        repo.getPayments().add(payment);

        // Cập nhật trạng thái nghĩa vụ học phí
        long newDebt = getRemainingDebt(tuitionId);
        if (newDebt == 0) {
            tf.setStatus("PAID");
        } else {
            tf.setStatus("PARTIAL");
        }

        repo.saveToDisk();
        return payment;
    }

    // =========================================================================
    // FR-07, FR-10: QUẢN LÝ BUỔI HỌC & ĐIỂM DANH HỌC VIÊN
    // (UC-07, UC-10, UC-11 · BR-V3-08, BR-V3-09 · TC-V3-07, TC-V3-10)
    // =========================================================================
    public List<Session> getSessionsByClass(int classId) {
        List<Session> list = new ArrayList<Session>();
        for (Session s : repo.getSessions()) {
            if (s.getClassId() == classId) {
                list.add(s);
            }
        }
        return list;
    }

    private Teacher findTeacherForCurrentUser() {
        if (currentUser == null || !"TEACHER".equalsIgnoreCase(currentUser.getRole())) {
            return null;
        }
        String uname = currentUser.getUsername() == null ? "" : currentUser.getUsername().trim().toLowerCase();
        if ("giangvien".equals(uname)) {
            Teacher t1 = repo.findTeacherById(1);
            if (t1 != null) return t1;
        }
        if ("giangvien02".equals(uname)) {
            Teacher t2 = repo.findTeacherById(2);
            if (t2 != null) return t2;
        }
        String uPhone = currentUser.getPhone() == null ? "" : currentUser.getPhone().trim();
        String uEmail = currentUser.getEmail() == null ? "" : currentUser.getEmail().trim().toLowerCase();
        String uFull = currentUser.getFullName() == null ? "" : currentUser.getFullName().toLowerCase();
        for (Teacher t : repo.getTeachers()) {
            if (!uPhone.isEmpty() && uPhone.equals(t.getPhone())) return t;
            if (!uEmail.isEmpty() && t.getEmail() != null && uEmail.equalsIgnoreCase(t.getEmail())) return t;
            if (t.getFullName() != null && uFull.contains(t.getFullName().toLowerCase())) return t;
            if (t.getTeacherCode() != null && uFull.contains(t.getTeacherCode().toLowerCase())) return t;
        }
        return repo.getTeachers().isEmpty() ? null : repo.getTeachers().get(0);
    }

    private void requireTeacherAssignedToClass(ClassRoom clazz) throws ValidationException {
        if (currentUser != null && "TEACHER".equalsIgnoreCase(currentUser.getRole())) {
            Teacher myTeacher = findTeacherForCurrentUser();
            if (myTeacher != null && clazz != null && clazz.getTeacherId() != myTeacher.getTeacherId()) {
                throw new ValidationException("UC-10 / BR-V3-08 / NFR-03",
                        "Giảng viên " + myTeacher.getFullName() + " (" + myTeacher.getTeacherCode()
                        + ") không phụ trách lớp " + clazz.getClassCode()
                        + ", không được phép truy cập hoặc điểm danh lớp của giảng viên khác!");
            }
        }
    }

    public Session addSession(int classId, String topic, String sessionDate, String startTime, String endTime) throws ValidationException {
        requireRoles("ADMIN", "TRAINING_STAFF", "TEACHER");
        ClassRoom clazz = repo.findClassById(classId);
        if (clazz == null) {
            throw new ValidationException("UC-07 / BR-V3-08", "Lớp học không tồn tại!");
        }
        requireTeacherAssignedToClass(clazz);
        if (topic == null || topic.trim().isEmpty()) {
            throw new ValidationException("UC-07 / FR-07", "Chủ đề buổi học không được để trống!");
        }
        if (sessionDate == null || !sessionDate.trim().matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            throw new ValidationException("UC-07 / FR-07", "Ngày học phải đúng định dạng YYYY-MM-DD!");
        }
        List<Session> classSessions = getSessionsByClass(classId);
        for (Session existing : classSessions) {
            if (existing.getSessionDate().equals(sessionDate.trim())
                    && existing.getStartTime().equals(startTime.trim())) {
                throw new ValidationException("UC-07 / FR-07", "Lớp " + clazz.getClassCode()
                        + " đã có buổi học vào ngày " + sessionDate + " lúc " + startTime + "!");
            }
        }
        int nextId = repo.getSessions().size() + 1;
        for (Session s : repo.getSessions()) if (s.getSessionId() >= nextId) nextId = s.getSessionId() + 1;
        int nextNum = classSessions.size() + 1;

        Session created = new Session(nextId, classId, nextNum, topic.trim(),
                sessionDate.trim(), startTime.trim(), endTime.trim(), "SCHEDULED");
        repo.getSessions().add(created);
        repo.saveToDisk();
        return created;
    }

    /**
     * BR-V3-08: Chỉ học viên có đăng ký hợp lệ (CONFIRMED) tại lớp mới nằm trong danh sách điểm danh.
     */
    public List<Student> getEligibleStudentsForClass(int classId) {
        List<Student> list = new ArrayList<Student>();
        for (Enrollment e : repo.getEnrollments()) {
            if (e.getClassId() == classId && "CONFIRMED".equals(e.getStatus())) {
                Student s = repo.findStudentById(e.getStudentId());
                if (s != null) {
                    list.add(s);
                }
            }
        }
        return list;
    }

    public Attendance getAttendanceRecord(int sessionId, int studentId) {
        for (Attendance a : repo.getAttendances()) {
            if (a.getSessionId() == sessionId && a.getStudentId() == studentId) {
                return a;
            }
        }
        return null;
    }

    /**
     * UC-10, FR-10, BR-V3-08, BR-V3-09, TC-V3-10: Lưu hoặc cập nhật điểm danh.
     * Mỗi học viên chỉ có tối đa 1 bản ghi điểm danh cho mỗi buổi học (UNIQUE session_id + student_id).
     */
    public Attendance markAttendance(int sessionId, int studentId, String status, String note) throws ValidationException {
        requireRoles("ADMIN", "TRAINING_STAFF", "TEACHER");
        Session session = repo.findSessionById(sessionId);
        if (session == null) {
            throw new ValidationException("UC-10 / BR-V3-08", "Buổi học không tồn tại!");
        }
        ClassRoom sessionClass = repo.findClassById(session.getClassId());
        if (sessionClass != null) {
            requireTeacherAssignedToClass(sessionClass);
        }
        if (!"PRESENT".equals(status) && !"ABSENT_EXCUSED".equals(status) && !"ABSENT_UNEXCUSED".equals(status)) {
            throw new ValidationException("UC-10 / FR-10", "Trạng thái điểm danh không hợp lệ!");
        }
        // Kiểm tra học viên có đăng ký CONFIRMED trong lớp của buổi học không (BR-V3-08)
        boolean eligible = false;
        for (Enrollment e : repo.getEnrollments()) {
            if (e.getClassId() == session.getClassId()
                    && e.getStudentId() == studentId
                    && "CONFIRMED".equals(e.getStatus())) {
                eligible = true;
                break;
            }
        }
        if (!eligible) {
            throw new ValidationException("BR-V3-08", "Học viên không có đăng ký hợp lệ trong lớp của buổi học này!");
        }

        String marker = currentUser != null ? currentUser.getFullName() : "Giảng viên";
        Attendance existing = getAttendanceRecord(sessionId, studentId);
        if (existing != null) {
            // TC-09 / BR-V3-09: Điểm danh trùng buổi -> Cập nhật bản ghi hiện có, cập nhật thời gian kèm "(đã chỉnh sửa)"
            existing.setStatus(status);
            existing.setMarkedBy(marker);
            existing.setMarkedAt(nowVnDateTimeStr() + " (đã chỉnh sửa)");
            existing.setNote(note == null ? "" : note.trim());
            session.setStatus("COMPLETED");
            repo.saveToDisk();
            return existing;
        } else {
            int nextId = repo.getAttendances().size() + 1;
            for (Attendance a : repo.getAttendances()) {
                if (a.getAttendanceId() >= nextId) nextId = a.getAttendanceId() + 1;
            }
            Attendance created = new Attendance(nextId, sessionId, studentId, status,
                    marker, nowVnDateTimeStr(), note == null ? "" : note.trim());
            repo.getAttendances().add(created);
            session.setStatus("COMPLETED");
            repo.saveToDisk();
            return created;
        }
    }

    /**
     * FR-14 & BR-25: Tính tỷ lệ chuyên cần của học viên trong lớp (% số buổi PRESENT / tổng số buổi đã điểm danh).
     */
    public double calculateStudentAttendanceRate(int classId, int studentId) {
        List<Session> sessions = getSessionsByClass(classId);
        int totalMarked = 0;
        int presentCount = 0;
        for (Session s : sessions) {
            Attendance a = getAttendanceRecord(s.getSessionId(), studentId);
            if (a != null) {
                totalMarked++;
                if ("PRESENT".equals(a.getStatus())) {
                    presentCount++;
                }
            }
        }
        if (totalMarked == 0) return 0.0;
        return (presentCount * 100.0) / totalMarked;
    }

    // =========================================================================
    // FR-15: THỐNG KÊ TỔNG HỢP (UC-07)
    // =========================================================================
    public long getTotalTuitionDueAll() {
        long total = 0L;
        for (TuitionFee tf : repo.getTuitionFees()) {
            Enrollment e = repo.findEnrollmentById(tf.getEnrollmentId());
            if (e != null && !"CANCELLED".equals(e.getStatus())) {
                total += tf.getAmountDue();
            }
        }
        return total;
    }

    public long getTotalCollectedAll() {
        long total = 0L;
        for (Payment p : repo.getPayments()) {
            total += p.getAmount();
        }
        return total;
    }

    public long getTotalDebtAll() {
        long debt = 0L;
        for (TuitionFee tf : repo.getTuitionFees()) {
            Enrollment e = repo.findEnrollmentById(tf.getEnrollmentId());
            if (e != null && !"CANCELLED".equals(e.getStatus())) {
                debt += getRemainingDebt(tf.getTuitionId());
            }
        }
        return debt;
    }
}
