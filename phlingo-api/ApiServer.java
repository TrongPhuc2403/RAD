package phlingo.api;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

/** Framework-free HTTP/JSON API for the PHLingoAcademy MySQL schema. */
public final class ApiServer {
    private static final int MAX_BODY = 1024 * 1024;
    private final Database db = new Database();
    private final Security.Sessions sessions = new Security.Sessions();
    private final Mailer mailer = new Mailer();

    public static void main(String[] args) throws Exception {
        final ApiServer app = new ApiServer();
        app.db.ping();
        app.bootstrapAdmin();
        int port = integerSetting("API_PORT", 8080);
        String bind = setting("API_BIND", "127.0.0.1");
        HttpServer server = HttpServer.create(new InetSocketAddress(bind, port), 0);
        server.createContext("/api", app.new Handler());
        server.setExecutor(Executors.newFixedThreadPool(Math.max(4, Runtime.getRuntime().availableProcessors())));
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() { public void run() { server.stop(1); } }));
        server.start();
        System.out.println("PHLingo API listening on http://" + bind + ":" + port + "/api");
        if (!app.mailer.configured()) System.out.println("Email workflows require API_SMTP_HOST and API_SMTP_FROM.");
    }

    private void bootstrapAdmin() throws SQLException {
        Number count = scalar("SELECT COUNT(*) FROM users WHERE UPPER(REPLACE(role,'-','_')) IN ('ADMIN','QT','ADMINISTRATOR') AND UPPER(status)='ACTIVE'");
        if (count != null && count.longValue() > 0) return;
        String email = setting("API_BOOTSTRAP_ADMIN_EMAIL", "").toLowerCase(Locale.ROOT);
        String password = setting("API_BOOTSTRAP_ADMIN_PASSWORD", "");
        if (email.isEmpty() || password.isEmpty()) {
            System.err.println("No ACTIVE administrator found. Set API_BOOTSTRAP_ADMIN_EMAIL and API_BOOTSTRAP_ADMIN_PASSWORD once to create one.");
            return;
        }
        validateEmail(email); validatePassword(password);
        if (one("SELECT user_id FROM users WHERE email=?", email) != null) throw new IllegalStateException("Bootstrap email already exists but is not an ACTIVE administrator.");
        execute("INSERT INTO users(email,password_hash,role,status,created_at) VALUES(?,?,?,?,CURRENT_TIMESTAMP)", email, Security.hashPassword(password), Security.Roles.ADMIN, "ACTIVE");
        System.out.println("Created initial administrator account: " + email);
    }

    private final class Handler implements HttpHandler {
        @Override public void handle(HttpExchange exchange) throws IOException {
            addCors(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { exchange.sendResponseHeaders(204, -1); exchange.close(); return; }
            try {
                Request req = new Request(exchange);
                Map<String, Object> response = route(req);
                int status = req.responseStatus == 0 ? 200 : req.responseStatus;
                if (req.html != null) sendHtml(exchange, status, req.html); else send(exchange, status, response);
            } catch (ApiException ex) {
                sendError(exchange, ex.status, ex.code, ex.getMessage());
            } catch (IllegalArgumentException ex) {
                sendError(exchange, 400, "VALIDATION_ERROR", ex.getMessage() == null ? "Dữ liệu yêu cầu không hợp lệ." : ex.getMessage());
            } catch (SQLException ex) {
                if (ex.getSQLState() != null && ex.getSQLState().startsWith("23")) sendError(exchange, 409, "CONSTRAINT_VIOLATION", "Dữ liệu bị trùng hoặc không thỏa ràng buộc.");
                else { System.err.println("Database request failed: " + ex.getClass().getSimpleName()); sendError(exchange, 500, "DATABASE_ERROR", "Không thể hoàn tất thao tác với cơ sở dữ liệu."); }
            } catch (Exception ex) {
                System.err.println("API request failed: " + ex.getClass().getSimpleName()); sendError(exchange, 500, "INTERNAL_ERROR", "Máy chủ không thể hoàn tất yêu cầu.");
            } finally { exchange.close(); }
        }
    }

    private Map<String, Object> route(Request r) throws Exception {
        if (r.is("health")) return result("ok", true, "database", "mysql");
        if (r.is("auth", "login") && r.method("POST")) return login(r);
        if (r.is("auth", "register") && r.method("POST")) return registerStudent(r);
        if (r.is("auth", "verify-email") && (r.method("GET") || r.method("POST"))) return verifyEmail(r);
        if (r.is("auth", "verification", "resend") && r.method("POST")) return resendVerification(r);
        if (r.is("auth", "forgot-password") && r.method("POST")) return forgotPassword(r);
        if (r.is("auth", "reset-password") && r.method("GET")) { r.html = resetPasswordPage(r.q("token")); return result(); }
        if (r.is("auth", "reset-password") && r.method("POST")) return resetPassword(r);
        if (r.is("public", "courses") && r.method("GET")) return publicCourses(r);
        if (r.is("public", "classes") && r.method("GET")) return publicClasses(r);

        User user = authenticate(r);
        if (r.is("auth", "logout") && r.method("POST")) { sessions.revoke(r.bearer); return result("ok", true, "message", "Đã đăng xuất."); }
        if (r.is("auth", "me") && r.method("GET")) return result("user", user.publicView());
        if (r.is("auth", "change-password") && r.method("POST")) return changePassword(r, user);
        if (r.is("me", "email-change") && r.method("POST")) return requestEmailChange(r, user);
        if (r.is("me", "profile")) return meProfile(r, user);
        if (r.is("me", "schedule") && r.method("GET")) return mySchedule(user);
        if (r.is("me", "tuition-fees") && r.method("GET")) return myTuition(user);
        if (r.is("me", "attendance") && r.method("GET")) return myAttendance(user);
        if (r.is("me", "enrollments") && r.method("POST")) return enroll(r, user, true);

        if (r.first("admin")) return adminRoutes(r, user);
        if (r.first("students")) return studentRoutes(r, user);
        if (r.first("teachers")) return teacherRoutes(r, user);
        if (r.first("courses")) return courseRoutes(r, user);
        if (r.first("classes")) return classRoutes(r, user);
        if (r.first("enrollments")) return enrollmentRoutes(r, user);
        if (r.first("tuition-fees")) return tuitionRoutes(r, user);
        if (r.first("payments")) return paymentRoutes(r, user);
        if (r.first("sessions")) return sessionRoutes(r, user);
        if (r.first("attendance")) return attendanceRoutes(r, user);
        throw new ApiException(404, "ENDPOINT_NOT_FOUND", "API endpoint không tồn tại.");
    }

    private Map<String, Object> login(Request r) throws SQLException {
        String email = requiredEmail(r.body(), "email"); String password = requiredText(r.body(), "password");
        Map<String, Object> row = one("SELECT user_id,email,password_hash,role,status FROM users WHERE email=?", email);
        if (row == null || !Security.matchesPassword(password, str(row.get("password_hash")))) throw new ApiException(401, "LOGIN_FAILED", "Email hoặc mật khẩu không đúng.");
        String status = upper(str(row.get("status")));
        if (!"ACTIVE".equals(status)) throw new ApiException(403, "ACCOUNT_NOT_ACTIVE", "Tài khoản chưa được kích hoạt hoặc đang bị khóa.");
        String role = Security.Roles.normalize(str(row.get("role")));
        if (role.isEmpty()) throw ApiException.forbidden();
        long id = number(row.get("user_id")).longValue();
        String stored = str(row.get("password_hash"));
        if (!stored.startsWith("pbkdf2_sha256$")) execute("UPDATE users SET password_hash=? WHERE user_id=?", Security.hashPassword(password), id);
        String token = sessions.create(id);
        User u = loadUser(id); return result("ok", true, "access_token", token, "token_type", "Bearer", "expires_in", 43200, "user", u.publicView());
    }

    private Map<String, Object> registerStudent(Request r) throws Exception {
        Map<String, Object> b = r.body(); String fullName = requiredText(b, "full_name"); String email = requiredEmail(b, "email");
        String password = requiredText(b, "password"); String confirm = requiredText(b, "confirm_password");
        if (!password.equals(confirm)) throw ApiException.bad("Mật khẩu xác nhận không khớp.");
        validatePassword(password);
        String rawToken = Security.randomToken(); long userId;
        try (Connection c = db.open()) {
            c.setAutoCommit(false);
            try {
                Map<String,Object> existing = one(c, "SELECT user_id,role,status FROM users WHERE email=? FOR UPDATE", email);
                if (existing != null) {
                    if (!Security.Roles.STUDENT.equals(Security.Roles.normalize(str(existing.get("role")))) || !"PENDING_VERIFICATION".equals(upper(str(existing.get("status")))))
                        throw ApiException.conflict("Email đã được sử dụng.");
                    Map<String,Object> student = one(c, "SELECT student_id,email FROM students WHERE user_id=?", number(existing.get("user_id")).longValue());
                    if (student == null || !email.equalsIgnoreCase(str(student.get("email")))) throw ApiException.conflict("Email đã được sử dụng.");
                    userId = number(existing.get("user_id")).longValue();
                    execute(c, "UPDATE students SET full_name=?,phone=COALESCE(?,phone) WHERE user_id=?", fullName, optionalText(b.get("phone")), userId);
                    execute(c, "UPDATE users SET password_hash=? WHERE user_id=?", Security.hashPassword(password), userId);
                } else {
                    userId = insert(c, "INSERT INTO users(email,password_hash,role,status,created_at) VALUES(?,?,?,?,CURRENT_TIMESTAMP)", email, Security.hashPassword(password), Security.Roles.STUDENT, "PENDING_VERIFICATION");
                    insert(c, "INSERT INTO students(full_name,email,phone,user_id,date_of_birth,gender,address) VALUES(?,?,?,?,?,?,?)",
                        fullName, email, optionalText(b.get("phone")), userId, null, null, null);
                }
                saveOneTimeToken(c, "verification_tokens", "token_id", userId, rawToken, 24);
                c.commit();
            } catch (Exception ex) { rollback(c); throw ex; }
        }
        r.responseStatus = 201;
        try { sendVerification(email, rawToken); return result("ok", true, "email_sent", true, "message", "Tài khoản đã được tạo. Hãy kiểm tra email để xác minh."); }
        catch (ApiException ex) { return result("ok", true, "email_sent", false, "message", "Tài khoản đang chờ xác minh. Email chưa gửi được; hãy cấu hình SMTP rồi gọi /api/auth/verification/resend."); }
    }

    private Map<String, Object> verifyEmail(Request r) throws SQLException {
        String token = r.method("GET") ? r.q("token") : requiredText(r.body(), "token");
        if (token.isEmpty()) throw ApiException.bad("Thiếu token xác minh.");
        try (Connection c = db.open()) {
            c.setAutoCommit(false);
            try {
                Map<String,Object> t = one(c, "SELECT token_id,user_id,expires_at,used_at FROM verification_tokens WHERE token=? FOR UPDATE", Security.sha256Hex(token));
                if (t == null || t.get("used_at") != null || timestamp(t.get("expires_at")).before(new Timestamp(System.currentTimeMillis()))) throw ApiException.bad("Token xác minh không hợp lệ hoặc đã hết hạn.");
                long userId = number(t.get("user_id")).longValue();
                if (token.startsWith("E.")) {
                    String[] parts = token.split("\\.", 3); if (parts.length != 3) throw ApiException.bad("Token đổi email không hợp lệ.");
                    String newEmail;
                    try { newEmail = validateEmail(new String(java.util.Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8)); }
                    catch (IllegalArgumentException ex) { throw ApiException.bad("Token đổi email không hợp lệ."); }
                    if (one(c, "SELECT user_id FROM users WHERE email=? AND user_id<>?", newEmail, userId) != null) throw ApiException.conflict("Email mới đã được sử dụng.");
                    execute(c, "UPDATE users SET email=? WHERE user_id=?", newEmail, userId);
                    execute(c, "UPDATE students SET email=? WHERE user_id=?", newEmail, userId);
                } else {
                    int changed = execute(c, "UPDATE users SET status='ACTIVE' WHERE user_id=? AND status='PENDING_VERIFICATION'", userId);
                    if (changed == 0) throw ApiException.conflict("Tài khoản đã kích hoạt hoặc không còn chờ xác minh.");
                }
                execute(c, "UPDATE verification_tokens SET used_at=CURRENT_TIMESTAMP WHERE token_id=?", number(t.get("token_id")).longValue());
                c.commit();
            } catch (Exception ex) { rollback(c); throw ex; }
        }
        return result("ok", true, "message", "Email đã xác minh; tài khoản hiện đã hoạt động.");
    }

    private Map<String, Object> resendVerification(Request r) {
        String email = optionalEmail(r.body().get("email"));
        if (!email.isEmpty() && mailer.configured()) {
            try (Connection c = db.open()) {
                Map<String,Object> row = one(c, "SELECT user_id,status FROM users WHERE email=? AND role='STUDENT'", email);
                if (row != null && "PENDING_VERIFICATION".equals(upper(str(row.get("status"))))) {
                    String token = Security.randomToken(); c.setAutoCommit(false);
                    try { saveOneTimeToken(c, "verification_tokens", "token_id", number(row.get("user_id")).longValue(), token, 24); sendVerification(email, token); c.commit(); }
                    catch (Exception ex) { rollback(c); System.err.println("Verification email was not delivered."); }
                }
            } catch (Exception ex) { System.err.println("Verification email request failed."); }
        }
        return result("ok", true, "message", "Nếu tài khoản cần xác minh tồn tại, email hướng dẫn sẽ được gửi.");
    }

    private Map<String, Object> forgotPassword(Request r) {
        String email = optionalEmail(r.body().get("email"));
        if (!email.isEmpty() && mailer.configured()) {
            try (Connection c = db.open()) {
                Map<String,Object> row = one(c, "SELECT user_id,status FROM users WHERE email=?", email);
                if (row != null && "ACTIVE".equals(upper(str(row.get("status"))))) {
                    String token = Security.randomToken(); c.setAutoCommit(false);
                    try {
                        saveOneTimeToken(c, "password_reset_tokens", "reset_id", number(row.get("user_id")).longValue(), token, 1);
                        mailer.send(email, "Đặt lại mật khẩu PHLingo", "Mở liên kết sau trong vòng 1 giờ để đặt mật khẩu mới:\n" + publicBase() + "/api/auth/reset-password?token=" + token + "\nNếu bạn không yêu cầu, hãy bỏ qua email này.");
                        c.commit();
                    } catch (Exception ex) { rollback(c); System.err.println("Password reset email was not delivered."); }
                }
            } catch (Exception ex) { System.err.println("Password reset request failed."); }
        }
        return result("ok", true, "message", "Nếu email thuộc tài khoản hợp lệ, hướng dẫn đặt lại mật khẩu sẽ được gửi.");
    }

    private Map<String, Object> resetPassword(Request r) throws SQLException {
        Map<String,Object> b = r.body(); String token = requiredText(b, "token"); String password = requiredText(b, "new_password");
        String confirm = requiredText(b, "confirm_password"); if (!password.equals(confirm)) throw ApiException.bad("Mật khẩu xác nhận không khớp."); validatePassword(password);
        long userId;
        try (Connection c = db.open()) {
            c.setAutoCommit(false);
            try {
                Map<String,Object> t = one(c, "SELECT reset_id,user_id,expires_at,used_at FROM password_reset_tokens WHERE token=? FOR UPDATE", Security.sha256Hex(token));
                if (t == null || t.get("used_at") != null || timestamp(t.get("expires_at")).before(new Timestamp(System.currentTimeMillis()))) throw ApiException.bad("Token đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.");
                userId = number(t.get("user_id")).longValue();
                execute(c, "UPDATE users SET password_hash=? WHERE user_id=?", Security.hashPassword(password), userId);
                execute(c, "UPDATE password_reset_tokens SET used_at=CURRENT_TIMESTAMP WHERE reset_id=?", number(t.get("reset_id")).longValue());
                c.commit();
            } catch (Exception ex) { rollback(c); throw ex; }
        }
        sessions.revokeUser(userId);
        return result("ok", true, "message", "Mật khẩu đã được đặt lại. Vui lòng đăng nhập lại.");
    }

    private Map<String, Object> changePassword(Request r, User user) throws SQLException {
        Map<String,Object> b = r.body(); String oldPassword = requiredText(b, "current_password"); String newPassword = requiredText(b, "new_password");
        String confirm = requiredText(b, "confirm_password"); if (!newPassword.equals(confirm)) throw ApiException.bad("Mật khẩu xác nhận không khớp."); validatePassword(newPassword);
        Map<String,Object> current = one("SELECT password_hash FROM users WHERE user_id=?", user.id);
        if (current == null || !Security.matchesPassword(oldPassword, str(current.get("password_hash")))) throw new ApiException(400, "CURRENT_PASSWORD_INVALID", "Mật khẩu hiện tại không đúng.");
        if (Security.matchesPassword(newPassword, str(current.get("password_hash")))) throw ApiException.bad("Mật khẩu mới phải khác mật khẩu hiện tại.");
        execute("UPDATE users SET password_hash=? WHERE user_id=?", Security.hashPassword(newPassword), user.id);
        sessions.revokeUser(user.id); String token = sessions.create(user.id);
        return result("ok", true, "access_token", token, "token_type", "Bearer", "expires_in", 43200, "message", "Đã đổi mật khẩu; phiên cũ đã bị thu hồi.");
    }

    private Map<String,Object> requestEmailChange(Request r, User user) throws SQLException {
        requireRole(user, Security.Roles.STUDENT); if (user.studentId <= 0) throw ApiException.forbidden();
        String newEmail = requiredEmail(r.body(), "new_email"), password = requiredText(r.body(), "current_password");
        if (newEmail.equalsIgnoreCase(user.email)) throw ApiException.bad("Email mới giống email hiện tại.");
        Map<String,Object> credentials = one("SELECT password_hash FROM users WHERE user_id=?", user.id);
        if (credentials == null || !Security.matchesPassword(password, str(credentials.get("password_hash")))) throw new ApiException(400, "CURRENT_PASSWORD_INVALID", "Mật khẩu hiện tại không đúng.");
        if (one("SELECT user_id FROM users WHERE email=? AND user_id<>?", newEmail, user.id) != null) throw ApiException.conflict("Email mới đã được sử dụng.");
        if (!mailer.configured()) throw new ApiException(503, "EMAIL_NOT_CONFIGURED", "Email chưa được cấu hình; không thể xác minh địa chỉ mới.");
        String token = "E." + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(newEmail.getBytes(StandardCharsets.UTF_8)) + "." + Security.randomToken();
        try (Connection c = db.open()) {
            c.setAutoCommit(false);
            try {
                saveOneTimeToken(c, "verification_tokens", "token_id", user.id, token, 24);
                mailer.send(newEmail, "Xác minh email mới PHLingo", "Mở liên kết sau trong vòng 24 giờ để đổi email đăng nhập:\n" + publicBase() + "/api/auth/verify-email?token=" + token + "\nEmail hiện tại sẽ không đổi nếu liên kết không được xác minh.");
                c.commit();
            } catch (IOException ex) { rollback(c); throw new ApiException(503, "EMAIL_UNAVAILABLE", "Không gửi được email xác minh; hãy thử lại sau."); }
            catch (Exception ex) { rollback(c); throw ex; }
        }
        return result("ok", true, "message", "Đã gửi liên kết xác minh đến email mới. Email hiện tại được giữ đến khi xác minh.");
    }

    private Map<String,Object> adminRoutes(Request r, User user) throws SQLException {
        requireRole(user, Security.Roles.ADMIN);
        if (r.is("admin", "users") && r.method("GET")) {
            String q = r.q("q"), role = Security.Roles.normalize(r.q("role")), status = upper(r.q("status"));
            StringBuilder sql = new StringBuilder("SELECT user_id,email,role,status,created_at FROM users WHERE 1=1"); List<Object> args = new ArrayList<Object>();
            if (!q.isEmpty()) { sql.append(" AND email LIKE ?"); args.add("%" + q + "%"); }
            if (!role.isEmpty()) { sql.append(" AND UPPER(role)=?"); args.add(role); }
            if (!status.isEmpty()) { sql.append(" AND UPPER(status)=?"); args.add(status); }
            sql.append(" ORDER BY user_id DESC LIMIT ?"); args.add(limit(r, 200));
            return result("items", many(sql.toString(), args.toArray()));
        }
        if (r.is("admin", "users") && r.method("POST")) {
            Map<String,Object> b = r.body(); String role = Security.Roles.normalize(requiredText(b, "role"));
            if (!Arrays.asList(Security.Roles.ADMIN, Security.Roles.TRAINING, Security.Roles.CASHIER).contains(role)) throw ApiException.bad("Tạo tài khoản giảng viên qua /api/admin/teachers; học viên đăng ký qua /api/auth/register hoặc /api/students.");
            String email = requiredEmail(b, "email"), password = requiredText(b, "password"); validatePassword(password);
            long id = insert("INSERT INTO users(email,password_hash,role,status,created_at) VALUES(?,?,?,?,CURRENT_TIMESTAMP)", email, Security.hashPassword(password), role, "ACTIVE");
            r.responseStatus = 201; return result("user", loadUser(id).publicView());
        }
        if (r.segs.size() == 3 && r.first("admin") && "users".equals(r.seg(1)) && r.method("PATCH")) {
            long id = id(r.seg(2)); Map<String,Object> b = r.body();
            Map<String,Object> target = one("SELECT user_id,role,status FROM users WHERE user_id=?", id); if (target == null) throw ApiException.missing("Tài khoản");
            String newRole = b.containsKey("role") ? Security.Roles.normalize(requiredText(b, "role")) : Security.Roles.normalize(str(target.get("role")));
            String newStatus = b.containsKey("status") ? upper(requiredText(b, "status")) : upper(str(target.get("status")));
            if (!Security.Roles.valid(newRole)) throw ApiException.bad("Vai trò không hợp lệ.");
            if (!Arrays.asList("ACTIVE", "LOCKED", "PENDING_VERIFICATION").contains(newStatus)) throw ApiException.bad("Trạng thái tài khoản không hợp lệ.");
            if (Security.Roles.STUDENT.equals(newRole) || Security.Roles.TEACHER.equals(newRole)) throw ApiException.bad("Không thể đổi tài khoản hiện có sang học viên/giảng viên vì cần bản ghi hồ sơ liên kết.");
            if (id == user.id && !Security.Roles.ADMIN.equals(newRole)) throw ApiException.conflict("Không thể tự gỡ vai trò quản trị của phiên hiện tại.");
            if (Security.Roles.ADMIN.equals(str(target.get("role"))) && "ACTIVE".equals(upper(str(target.get("status")))) && (!Security.Roles.ADMIN.equals(newRole) || !"ACTIVE".equals(newStatus))) {
                Number admins = scalar("SELECT COUNT(*) FROM users WHERE UPPER(REPLACE(role,'-','_')) IN ('ADMIN','QT','ADMINISTRATOR') AND UPPER(status)='ACTIVE'");
                if (admins != null && admins.intValue() <= 1) throw ApiException.conflict("Không thể khóa hoặc đổi vai trò quản trị viên ACTIVE cuối cùng.");
            }
            execute("UPDATE users SET role=?,status=? WHERE user_id=?", newRole, newStatus, id);
            if (!"ACTIVE".equals(newStatus)) sessions.revokeUser(id);
            return result("user", loadUser(id).publicView());
        }
        if (r.is("admin", "teachers") && r.method("POST")) {
            Map<String,Object> b = r.body(); String name = requiredText(b, "full_name"), email = requiredEmail(b, "email"), password = requiredText(b, "password"); validatePassword(password);
            try (Connection c = db.open()) {
                c.setAutoCommit(false);
                try {
                    long userId = insert(c, "INSERT INTO users(email,password_hash,role,status,created_at) VALUES(?,?,?,?,CURRENT_TIMESTAMP)", email, Security.hashPassword(password), Security.Roles.TEACHER, "ACTIVE");
                    long teacherId = insert(c, "INSERT INTO teachers(full_name,user_id,specialization,qualification) VALUES(?,?,?,?)", name, userId, optionalText(b.get("specialization")), optionalText(b.get("qualification")));
                    c.commit(); r.responseStatus = 201; return result("teacher", one("SELECT t.teacher_id,t.full_name,t.specialization,t.qualification,u.email,u.user_id FROM teachers t JOIN users u ON u.user_id=t.user_id WHERE t.teacher_id=?", teacherId));
                } catch (Exception ex) { rollback(c); throw ex; }
            }
        }
        throw new ApiException(404, "ENDPOINT_NOT_FOUND", "API endpoint không tồn tại.");
    }

    private Map<String,Object> studentRoutes(Request r, User user) throws SQLException {
        if (r.segs.size() == 3 && "tuition-fees".equals(r.seg(2)) && r.method("GET")) {
            requireRole(user, Security.Roles.ADMIN, Security.Roles.CASHIER);
            long studentId = id(r.seg(1)); ensureExists("SELECT student_id FROM students WHERE student_id=?", studentId, "Học viên");
            return result("items", many(tuitionSelect() + " WHERE s.student_id=? ORDER BY tf.tuition_id DESC", studentId));
        }
        requireRole(user, Security.Roles.ADMIN, Security.Roles.TRAINING);
        if (r.is("students") && r.method("GET")) {
            String q = r.q("q"); String sql = studentSelect() + " WHERE (?='' OR s.full_name LIKE ? OR s.email LIKE ? OR COALESCE(s.phone,'') LIKE ?) ORDER BY s.student_id DESC LIMIT ?";
            String like = "%" + q + "%"; return result("items", many(sql, q, like, like, like, limit(r, 200)));
        }
        if (r.is("students") && r.method("POST")) {
            Map<String,Object> b = r.body(); String name = requiredText(b, "full_name"), email = requiredEmail(b, "email");
            if (one("SELECT user_id FROM users WHERE email=?", email) != null) throw ApiException.conflict("Email đã được sử dụng.");
            String randomPassword = Security.randomToken();
            try (Connection c = db.open()) {
                c.setAutoCommit(false);
                try {
                    long userId = insert(c, "INSERT INTO users(email,password_hash,role,status,created_at) VALUES(?,?,?,?,CURRENT_TIMESTAMP)", email, Security.hashPassword(randomPassword), Security.Roles.STUDENT, "PENDING_VERIFICATION");
                    long studentId = insert(c, "INSERT INTO students(full_name,email,phone,user_id,date_of_birth,gender,address) VALUES(?,?,?,?,?,?,?)", name, email,
                        optionalText(b.get("phone")), userId, dateOrNull(b.get("date_of_birth")), optionalText(b.get("gender")), optionalText(b.get("address")));
                    c.commit(); r.responseStatus = 201; return result("student", one(studentSelect() + " WHERE s.student_id=?", studentId));
                } catch (Exception ex) { rollback(c); throw ex; }
            }
        }
        if (r.segs.size() == 2 && r.method("GET")) {
            long studentId = id(r.seg(1)); Map<String,Object> found = one(studentSelect() + " WHERE s.student_id=?", studentId); if (found == null) throw ApiException.missing("Học viên"); return result("student", found);
        }
        if (r.segs.size() == 2 && r.method("PATCH")) {
            long studentId = id(r.seg(1)); Map<String,Object> found = one("SELECT student_id,user_id FROM students WHERE student_id=?", studentId); if (found == null) throw ApiException.missing("Học viên");
            Map<String,Object> b = r.body();
            String name = b.containsKey("full_name") ? requiredText(b, "full_name") : null;
            String phone = b.containsKey("phone") ? optionalText(b.get("phone")) : null;
            Date dob = b.containsKey("date_of_birth") ? dateOrNull(b.get("date_of_birth")) : null;
            String gender = b.containsKey("gender") ? optionalText(b.get("gender")) : null;
            String address = b.containsKey("address") ? optionalText(b.get("address")) : null;
            if (b.containsKey("email")) throw ApiException.bad("Không được đổi email từ hồ sơ quản trị; học viên cần quy trình xác minh email.");
            execute("UPDATE students SET full_name=COALESCE(?,full_name),phone=IF(?=1,?,phone),date_of_birth=IF(?=1,?,date_of_birth),gender=IF(?=1,?,gender),address=IF(?=1,?,address) WHERE student_id=?",
                name, b.containsKey("phone") ? 1 : 0, phone, b.containsKey("date_of_birth") ? 1 : 0, dob, b.containsKey("gender") ? 1 : 0, gender, b.containsKey("address") ? 1 : 0, address, studentId);
            return result("student", one(studentSelect() + " WHERE s.student_id=?", studentId));
        }
        if (r.segs.size() == 2 && "DELETE".equals(r.method)) {
            long studentId = id(r.seg(1)); Map<String,Object> found = one("SELECT user_id FROM students WHERE student_id=?", studentId); if (found == null) throw ApiException.missing("Học viên");
            execute("UPDATE users SET status='LOCKED' WHERE user_id=?", number(found.get("user_id")).longValue()); sessions.revokeUser(number(found.get("user_id")).longValue());
            return result("ok", true, "message", "Đã ngừng tài khoản học viên; hồ sơ và lịch sử được giữ lại.");
        }
        throw new ApiException(404, "ENDPOINT_NOT_FOUND", "API endpoint không tồn tại.");
    }

    private Map<String,Object> teacherRoutes(Request r, User user) throws SQLException {
        requireRole(user, Security.Roles.ADMIN, Security.Roles.TRAINING);
        if (r.is("teachers") && r.method("GET")) return result("items", many("SELECT t.teacher_id,t.full_name,t.specialization,t.qualification,u.user_id,u.email,u.status FROM teachers t JOIN users u ON u.user_id=t.user_id ORDER BY t.teacher_id LIMIT ?", limit(r, 200)));
        if (r.segs.size() == 2 && r.method("GET")) {
            Map<String,Object> row = one("SELECT t.teacher_id,t.full_name,t.specialization,t.qualification,u.user_id,u.email,u.status FROM teachers t JOIN users u ON u.user_id=t.user_id WHERE t.teacher_id=?", id(r.seg(1)));
            if (row == null) throw ApiException.missing("Giảng viên"); return result("teacher", row);
        }
        if (r.segs.size() == 2 && r.method("PATCH")) {
            long teacherId = id(r.seg(1)); Map<String,Object> b = r.body(); Map<String,Object> old = one("SELECT teacher_id FROM teachers WHERE teacher_id=?", teacherId); if (old == null) throw ApiException.missing("Giảng viên");
            execute("UPDATE teachers SET full_name=COALESCE(?,full_name),specialization=IF(?=1,?,specialization),qualification=IF(?=1,?,qualification) WHERE teacher_id=?",
                b.containsKey("full_name") ? requiredText(b, "full_name") : null, b.containsKey("specialization") ? 1 : 0, optionalText(b.get("specialization")), b.containsKey("qualification") ? 1 : 0, optionalText(b.get("qualification")), teacherId);
            return result("teacher", one("SELECT t.teacher_id,t.full_name,t.specialization,t.qualification,u.user_id,u.email,u.status FROM teachers t JOIN users u ON u.user_id=t.user_id WHERE t.teacher_id=?", teacherId));
        }
        throw new ApiException(404, "ENDPOINT_NOT_FOUND", "API endpoint không tồn tại.");
    }

    private Map<String,Object> courseRoutes(Request r, User user) throws SQLException {
        requireRole(user, Security.Roles.ADMIN, Security.Roles.TRAINING);
        if (r.is("courses") && r.method("GET")) {
            String q = r.q("q"), status = upper(r.q("status"));
            String sql = "SELECT " + courseColumns() + " FROM courses c WHERE (?='' OR c.name LIKE ? OR c.code LIKE ?) AND (?='' OR UPPER(c.status)=?) ORDER BY c.course_id DESC LIMIT ?";
            String like = "%" + q + "%"; return result("items", many(sql, q, like, like, status, status, limit(r, 200)));
        }
        if (r.is("courses") && r.method("POST")) {
            Map<String,Object> b = r.body(); String code = requiredText(b, "code").toUpperCase(Locale.ROOT), name = requiredText(b, "name");
            BigDecimal tuition = money(b.get("tuition_listed"), "tuition_listed", true); if (tuition.signum() < 0) throw ApiException.bad("Học phí niêm yết không được âm.");
            String status = optionalText(b.get("status")); if (status == null) status = "ACTIVE"; validateCourseStatus(status);
            Integer hours = optionalInt(b.get("duration_hours")); if (hours != null && hours.intValue() <= 0) throw ApiException.bad("Thời lượng phải lớn hơn 0.");
            long id = insert("INSERT INTO courses(code,name,tuition_listed,status,description,level,duration_hours) VALUES(?,?,?,?,?,?,?)",
                code, name, tuition, upper(status), optionalText(b.get("description")), optionalText(b.get("level")), hours);
            r.responseStatus = 201; return result("course", one("SELECT " + courseColumns() + " FROM courses c WHERE c.course_id=?", id));
        }
        if (r.segs.size() == 2 && r.method("PATCH")) {
            long id = id(r.seg(1)); Map<String,Object> old = one("SELECT * FROM courses WHERE course_id=?", id); if (old == null) throw ApiException.missing("Khóa học");
            Map<String,Object> b = r.body();
            String code = b.containsKey("code") ? requiredText(b, "code").toUpperCase(Locale.ROOT) : str(old.get("code"));
            String name = b.containsKey("name") ? requiredText(b, "name") : str(old.get("name"));
            BigDecimal tuition = b.containsKey("tuition_listed") ? money(b.get("tuition_listed"), "tuition_listed", true) : (BigDecimal) old.get("tuition_listed");
            if (tuition.signum() < 0) throw ApiException.bad("Học phí niêm yết không được âm.");
            String status = b.containsKey("status") ? upper(requiredText(b, "status")) : upper(str(old.get("status"))); validateCourseStatus(status);
            String description = b.containsKey("description") ? optionalText(b.get("description")) : str(old.get("description"));
            String level = b.containsKey("level") ? optionalText(b.get("level")) : str(old.get("level"));
            Integer duration = b.containsKey("duration_hours") ? optionalInt(b.get("duration_hours")) : (old.get("duration_hours") == null ? null : number(old.get("duration_hours")).intValue());
            if (duration != null && duration.intValue() <= 0) throw ApiException.bad("Thời lượng phải lớn hơn 0.");
            execute("UPDATE courses SET code=?,name=?,tuition_listed=?,status=?,description=?,level=?,duration_hours=? WHERE course_id=?", code, name, tuition, status, description, level, duration, id);
            return result("course", one("SELECT " + courseColumns() + " FROM courses c WHERE c.course_id=?", id));
        }
        throw new ApiException(404, "ENDPOINT_NOT_FOUND", "API endpoint không tồn tại.");
    }

    private Map<String,Object> publicCourses(Request r) throws SQLException {
        String q = r.q("q"), level = r.q("level"); String like = "%" + q + "%";
        List<Map<String,Object>> items = many("SELECT " + courseColumns() + " FROM courses c WHERE UPPER(c.status) IN ('ACTIVE','OPEN','PUBLISHED') AND (?='' OR c.name LIKE ? OR c.code LIKE ?) AND (?='' OR c.level LIKE ?) AND EXISTS (SELECT 1 FROM classes cl WHERE cl.course_id=c.course_id AND cl.status='OPEN') ORDER BY c.name LIMIT ?",
            q, like, like, level, "%" + level + "%", limit(r, 200));
        return result("items", items);
    }

    private Map<String,Object> publicClasses(Request r) throws SQLException {
        StringBuilder sql = new StringBuilder(classSelect() + " WHERE cl.status='OPEN' AND UPPER(c.status) IN ('ACTIVE','OPEN','PUBLISHED')"); List<Object> args = new ArrayList<Object>();
        String q = r.q("q"); if (!q.isEmpty()) { sql.append(" AND (cl.name LIKE ? OR cl.code LIKE ? OR c.name LIKE ? OR c.code LIKE ?)"); String like = "%" + q + "%"; args.add(like); args.add(like); args.add(like); args.add(like); }
        String level = r.q("level"); if (!level.isEmpty()) { sql.append(" AND c.level LIKE ?"); args.add("%" + level + "%"); }
        if (!r.q("course_id").isEmpty()) { sql.append(" AND c.course_id=?"); args.add(id(r.q("course_id"))); }
        if (!r.q("day_of_week").isEmpty()) { int day = integer(r.q("day_of_week"), -1); if (day < 1 || day > 7) throw ApiException.bad("day_of_week dùng chuẩn ISO: 1=Thứ Hai đến 7=Chủ Nhật."); sql.append(" AND EXISTS (SELECT 1 FROM class_schedules cs WHERE cs.class_id=cl.class_id AND cs.day_of_week=?)"); args.add(day); }
        sql.append(" ORDER BY c.name,cl.code LIMIT ?"); args.add(limit(r, 200));
        return result("items", many(sql.toString(), args.toArray()));
    }

    private Map<String,Object> classRoutes(Request r, User user) throws SQLException {
        if (r.segs.size() >= 3 && "schedules".equals(r.seg(2))) return classScheduleRoutes(r, user);
        if (r.segs.size() == 3 && "attendance-summary".equals(r.seg(2)) && r.method("GET")) return attendanceSummary(r, user);
        requireRole(user, Security.Roles.ADMIN, Security.Roles.TRAINING);
        if (r.is("classes") && r.method("GET")) {
            StringBuilder sql = new StringBuilder(classSelect() + " WHERE (?='' OR cl.name LIKE ? OR cl.code LIKE ?) AND (?='' OR cl.status=?) AND (?=0 OR c.course_id=?)");
            String q = r.q("q"), status = upper(r.q("status")); String like = "%" + q + "%"; long courseId = r.q("course_id").isEmpty() ? 0 : id(r.q("course_id"));
            sql.append(" ORDER BY cl.class_id DESC LIMIT ?"); return result("items", many(sql.toString(), q, like, like, status, status, courseId, courseId, limit(r, 200)));
        }
        if (r.is("classes") && r.method("POST")) {
            Map<String,Object> b = r.body(); long courseId = idValue(b.get("course_id"), "course_id");
            ensureExists("SELECT course_id FROM courses WHERE course_id=?", courseId, "Khóa học");
            String code = requiredText(b, "code").toUpperCase(Locale.ROOT), name = requiredText(b, "name"); int capacity = positiveInt(b.get("capacity"), "capacity");
            Long teacherId = nullableLong(b.get("teacher_id")); if (teacherId != null) ensureExists("SELECT teacher_id FROM teachers WHERE teacher_id=?", teacherId, "Giảng viên");
            String status = optionalText(b.get("status")); if (status == null) status = "OPEN"; status = upper(status); validateClassStatus(status);
            if (!"OPEN".equals(status)) throw ApiException.bad("Lớp mới phải bắt đầu ở trạng thái OPEN.");
            long id = insert("INSERT INTO classes(course_id,teacher_id,code,name,capacity,status) VALUES(?,?,?,?,?,?)", courseId, teacherId, code, name, capacity, status);
            r.responseStatus = 201; return result("class", one(classSelect() + " WHERE cl.class_id=?", id));
        }
        if (r.segs.size() == 2 && r.method("GET")) {
            Map<String,Object> row = one(classSelect() + " WHERE cl.class_id=?", id(r.seg(1))); if (row == null) throw ApiException.missing("Lớp học"); return result("class", row);
        }
        if (r.segs.size() == 2 && r.method("PATCH")) {
            long id = id(r.seg(1)); Map<String,Object> b = r.body();
            try (Connection c = db.open()) {
                c.setAutoCommit(false);
                try {
                    Map<String,Object> old = one(c, "SELECT * FROM classes WHERE class_id=? FOR UPDATE", id); if (old == null) throw ApiException.missing("Lớp học");
                    int capacity = b.containsKey("capacity") ? positiveInt(b.get("capacity"), "capacity") : number(old.get("capacity")).intValue();
                    Number enrolled = scalar(c, "SELECT COUNT(*) FROM enrollments WHERE class_id=? AND status='CONFIRMED'", id);
                    if (capacity < enrolled.intValue()) throw ApiException.conflict("Sĩ số mới không được thấp hơn số học viên đã đăng ký.");
                    long courseId = b.containsKey("course_id") ? idValue(b.get("course_id"), "course_id") : number(old.get("course_id")).longValue();
                    if (one(c, "SELECT course_id FROM courses WHERE course_id=?", courseId) == null) throw ApiException.missing("Khóa học");
                    Long teacherId = b.containsKey("teacher_id") ? nullableLong(b.get("teacher_id")) : nullableLong(old.get("teacher_id"));
                    if (teacherId != null && one(c, "SELECT teacher_id FROM teachers WHERE teacher_id=?", teacherId) == null) throw ApiException.missing("Giảng viên");
                    String code = b.containsKey("code") ? requiredText(b, "code").toUpperCase(Locale.ROOT) : str(old.get("code"));
                    String name = b.containsKey("name") ? requiredText(b, "name") : str(old.get("name"));
                    String status = b.containsKey("status") ? upper(requiredText(b, "status")) : upper(str(old.get("status"))); validateClassStatus(status);
                    if (!validTransition(upper(str(old.get("status"))), status)) throw ApiException.conflict("Chuyển trạng thái lớp không hợp lệ.");
                    execute(c, "UPDATE classes SET course_id=?,teacher_id=?,code=?,name=?,capacity=?,status=? WHERE class_id=?", courseId, teacherId, code, name, capacity, status, id);
                    Map<String,Object> updated = one(c, classSelect() + " WHERE cl.class_id=?", id); c.commit(); return result("class", updated);
                } catch (Exception ex) { rollback(c); throw ex; }
            }
        }
        throw new ApiException(404, "ENDPOINT_NOT_FOUND", "API endpoint không tồn tại.");
    }

    private Map<String,Object> classScheduleRoutes(Request r, User user) throws SQLException {
        requireRole(user, Security.Roles.ADMIN, Security.Roles.TRAINING);
        long classId = id(r.seg(1)); ensureExists("SELECT class_id FROM classes WHERE class_id=?", classId, "Lớp học");
        if (r.segs.size() == 3 && r.method("GET")) return result("items", many("SELECT schedule_id,class_id,day_of_week,start_time,end_time FROM class_schedules WHERE class_id=? ORDER BY day_of_week,start_time", classId));
        if (r.segs.size() == 3 && r.method("POST")) {
            Map<String,Object> b = r.body(); int day = positiveInt(b.get("day_of_week"), "day_of_week"); if (day > 7) throw ApiException.bad("day_of_week dùng chuẩn ISO 1–7.");
            Time start = time(b.get("start_time"), "start_time"), end = time(b.get("end_time"), "end_time"); if (!start.before(end)) throw ApiException.bad("Giờ kết thúc phải sau giờ bắt đầu.");
            Map<String,Object> cls = one("SELECT teacher_id FROM classes WHERE class_id=?", classId); Object teacherId = cls.get("teacher_id");
            Number overlap = scalar("SELECT COUNT(*) FROM class_schedules WHERE class_id=? AND day_of_week=? AND NOT(end_time<=? OR start_time>=?)", classId, day, start, end);
            if (overlap.intValue() > 0) throw ApiException.conflict("Lịch học bị trùng với lịch khác của lớp.");
            if (teacherId != null) {
                Number conflict = scalar("SELECT COUNT(*) FROM class_schedules cs JOIN classes c ON c.class_id=cs.class_id WHERE c.teacher_id=? AND c.class_id<>? AND cs.day_of_week=? AND NOT(cs.end_time<=? OR cs.start_time>=?)", teacherId, classId, day, start, end);
                if (conflict.intValue() > 0) throw ApiException.conflict("Giảng viên đã được xếp một lớp khác trong khung giờ này.");
            }
            long id = insert("INSERT INTO class_schedules(class_id,day_of_week,start_time,end_time) VALUES(?,?,?,?)", classId, day, start, end);
            r.responseStatus = 201; return result("schedule", one("SELECT schedule_id,class_id,day_of_week,start_time,end_time FROM class_schedules WHERE schedule_id=?", id));
        }
        if (r.segs.size() == 4 && r.method("DELETE")) {
            long scheduleId = id(r.seg(3)); Number marks = scalar("SELECT COUNT(*) FROM sessions s JOIN attendance a ON a.session_id=s.session_id WHERE s.class_id=?", classId);
            if (marks.intValue() > 0) throw ApiException.conflict("Không thể xóa lịch khi lớp đã có điểm danh.");
            int changed = execute("DELETE FROM class_schedules WHERE schedule_id=? AND class_id=?", scheduleId, classId); if (changed == 0) throw ApiException.missing("Lịch học");
            return result("ok", true);
        }
        throw new ApiException(404, "ENDPOINT_NOT_FOUND", "API endpoint không tồn tại.");
    }

    private Map<String,Object> enrollmentRoutes(Request r, User user) throws SQLException {
        if (r.is("enrollments") && r.method("POST")) return enroll(r, user, false);
        if (!r.method("GET")) throw new ApiException(404, "ENDPOINT_NOT_FOUND", "API endpoint không tồn tại.");
        if (r.segs.size() == 2) {
            long enrollmentId = id(r.seg(1)); Map<String,Object> row = one(enrollmentSelect() + " WHERE e.enrollment_id=?", enrollmentId);
            if (row == null) throw ApiException.missing("Đăng ký");
            if (Security.Roles.STUDENT.equals(user.role) && number(row.get("student_id")).longValue() != user.studentId) throw ApiException.forbidden();
            if (!Security.Roles.STUDENT.equals(user.role)) requireRole(user, Security.Roles.ADMIN, Security.Roles.TRAINING, Security.Roles.CASHIER);
            return result("enrollment", row);
        }
        if (!Arrays.asList(Security.Roles.ADMIN, Security.Roles.TRAINING).contains(user.role) && !Security.Roles.STUDENT.equals(user.role)) throw ApiException.forbidden();
        StringBuilder sql = new StringBuilder(enrollmentSelect() + " WHERE (?=0 OR e.student_id=?) AND (?=0 OR e.class_id=?) AND (?='' OR e.status=?)");
        long studentId = 0; if (!r.q("student_id").isEmpty()) studentId = id(r.q("student_id"));
        if (Security.Roles.STUDENT.equals(user.role)) studentId = user.studentId;
        long classId = r.q("class_id").isEmpty() ? 0 : id(r.q("class_id")); String status = upper(r.q("status"));
        sql.append(" ORDER BY e.enrollment_id DESC LIMIT ?");
        return result("items", many(sql.toString(), studentId, studentId, classId, classId, status, status, limit(r, 200)));
    }

    private Map<String,Object> enroll(Request r, User user, boolean selfService) throws SQLException {
        long studentId;
        if (selfService) {
            requireRole(user, Security.Roles.STUDENT); if (user.studentId <= 0) throw ApiException.conflict("Tài khoản chưa liên kết hồ sơ học viên.");
            if (r.body().containsKey("student_id")) throw ApiException.bad("Học viên tự đăng ký không được truyền student_id.");
            studentId = user.studentId;
        } else {
            requireRole(user, Security.Roles.ADMIN, Security.Roles.TRAINING);
            studentId = idValue(r.body().get("student_id"), "student_id");
        }
        long classId = idValue(r.body().get("class_id"), "class_id"); long enrollmentId;
        try (Connection c = db.open()) {
            c.setAutoCommit(false);
            try {
                Map<String,Object> cls = one(c, "SELECT cl.class_id,cl.status,cl.capacity,c.tuition_listed FROM classes cl JOIN courses c ON c.course_id=cl.course_id WHERE cl.class_id=? FOR UPDATE", classId);
                if (cls == null) throw ApiException.missing("Lớp học");
                if (!"OPEN".equals(upper(str(cls.get("status"))))) throw ApiException.conflict("Chỉ lớp OPEN mới nhận đăng ký.");
                Map<String,Object> student = one(c, "SELECT s.student_id,u.status FROM students s JOIN users u ON u.user_id=s.user_id WHERE s.student_id=?", studentId);
                if (student == null) throw ApiException.missing("Học viên");
                if (!"ACTIVE".equals(upper(str(student.get("status"))))) throw ApiException.conflict("Học viên cần tài khoản ACTIVE trước khi đăng ký.");
                Map<String,Object> duplicate = one(c, "SELECT enrollment_id FROM enrollments WHERE student_id=? AND class_id=? AND status='CONFIRMED' FOR UPDATE", studentId, classId);
                if (duplicate != null) throw ApiException.conflict("Học viên đã có đăng ký CONFIRMED trong lớp này.");
                Number count = scalar(c, "SELECT COUNT(*) FROM enrollments WHERE class_id=? AND status='CONFIRMED'", classId);
                if (count.intValue() >= number(cls.get("capacity")).intValue()) throw ApiException.conflict("Lớp đã đủ sĩ số.");
                BigDecimal due = decimal(cls.get("tuition_listed"));
                enrollmentId = insert(c, "INSERT INTO enrollments(student_id,class_id,status,enrolled_at) VALUES(?,?,'CONFIRMED',CURRENT_TIMESTAMP)", studentId, classId);
                insert(c, "INSERT INTO tuition_fees(enrollment_id,amount_due,status) VALUES(?,?,'UNPAID')", enrollmentId, due);
                c.commit();
            } catch (Exception ex) { rollback(c); throw ex; }
        }
        r.responseStatus = 201;
        return result("enrollment", one(enrollmentSelect() + " WHERE e.enrollment_id=?", enrollmentId));
    }

    private Map<String,Object> tuitionRoutes(Request r, User user) throws SQLException {
        requireRole(user, Security.Roles.ADMIN, Security.Roles.CASHIER);
        if (r.is("tuition-fees") && r.method("GET")) {
            StringBuilder sql = new StringBuilder(tuitionSelect() + " WHERE (?=0 OR s.student_id=?) AND (?=0 OR tf.enrollment_id=?) AND (?='' OR tf.status=?)");
            long studentId = r.q("student_id").isEmpty() ? 0 : id(r.q("student_id")); long enrollmentId = r.q("enrollment_id").isEmpty() ? 0 : id(r.q("enrollment_id"));
            String status = upper(r.q("status")); sql.append(" ORDER BY tf.tuition_id DESC LIMIT ?");
            return result("items", many(sql.toString(), studentId, studentId, enrollmentId, enrollmentId, status, status, limit(r, 200)));
        }
        if (r.segs.size() == 2 && r.method("GET")) {
            Map<String,Object> row = one(tuitionSelect() + " WHERE tf.tuition_id=?", id(r.seg(1))); if (row == null) throw ApiException.missing("Khoản học phí");
            return result("tuition_fee", row, "payments", many("SELECT payment_id,tuition_id,amount,method,request_id,receipt_code,paid_at FROM payments WHERE tuition_id=? ORDER BY paid_at DESC", id(r.seg(1))));
        }
        throw new ApiException(404, "ENDPOINT_NOT_FOUND", "API endpoint không tồn tại.");
    }

    private Map<String,Object> paymentRoutes(Request r, User user) throws SQLException {
        if (r.is("payments") && r.method("POST")) {
            requireRole(user, Security.Roles.ADMIN, Security.Roles.CASHIER);
            Map<String,Object> b = r.body(); long tuitionId = idValue(b.get("tuition_id"), "tuition_id");
            BigDecimal amount = money(b.get("amount"), "amount", true); if (amount.signum() <= 0) throw ApiException.bad("Số tiền thanh toán phải lớn hơn 0.");
            String method = requiredText(b, "method").toUpperCase(Locale.ROOT); if (!Arrays.asList("CASH", "BANK_TRANSFER", "CARD", "OTHER").contains(method)) throw ApiException.bad("Phương thức thanh toán phải là CASH, BANK_TRANSFER, CARD hoặc OTHER.");
            String requestId = text(b.get("request_id")); if (requestId.isEmpty()) requestId = r.header("Idempotency-Key");
            if (requestId.isEmpty() || requestId.length() > 100) throw ApiException.bad("Cần request_id hoặc header Idempotency-Key (tối đa 100 ký tự).");
            long paymentId; String receipt;
            try (Connection c = db.open()) {
                c.setAutoCommit(false);
                try {
                    Map<String,Object> duplicate = one(c, "SELECT payment_id,tuition_id,amount,method,request_id,receipt_code,paid_at FROM payments WHERE request_id=?", requestId);
                    if (duplicate != null) {
                        if (number(duplicate.get("tuition_id")).longValue() != tuitionId || decimal(duplicate.get("amount")).compareTo(amount) != 0 || !method.equals(upper(str(duplicate.get("method")))))
                            throw ApiException.conflict("Idempotency-Key đã được dùng cho yêu cầu thanh toán khác.");
                        c.commit(); return result("payment", duplicate, "idempotent_replay", true);
                    }
                    Map<String,Object> fee = one(c, "SELECT tuition_id,amount_due,status FROM tuition_fees WHERE tuition_id=? FOR UPDATE", tuitionId); if (fee == null) throw ApiException.missing("Khoản học phí");
                    BigDecimal due = decimal(fee.get("amount_due")); BigDecimal paid = decimal(scalar(c, "SELECT COALESCE(SUM(amount),0) FROM payments WHERE tuition_id=?", tuitionId));
                    BigDecimal balance = due.subtract(paid); if (amount.compareTo(balance) > 0) throw ApiException.conflict("Số tiền thanh toán vượt quá số còn nợ.");
                    receipt = "PHL-" + System.currentTimeMillis() + "-" + Security.randomToken().substring(0, 8).toUpperCase(Locale.ROOT);
                    paymentId = insert(c, "INSERT INTO payments(tuition_id,amount,method,request_id,receipt_code,paid_at) VALUES(?,?,?,?,?,CURRENT_TIMESTAMP)", tuitionId, amount, method, requestId, receipt);
                    String feeStatus = paid.add(amount).compareTo(due) == 0 ? "PAID" : "PARTIAL";
                    execute(c, "UPDATE tuition_fees SET status=? WHERE tuition_id=?", feeStatus, tuitionId);
                    c.commit();
                } catch (Exception ex) { rollback(c); throw ex; }
            }
            r.responseStatus = 201; return result("payment", one("SELECT payment_id,tuition_id,amount,method,request_id,receipt_code,paid_at FROM payments WHERE payment_id=?", paymentId), "balance", decimal(one(tuitionSelect() + " WHERE tf.tuition_id=?", tuitionId).get("balance")));
        }
        requireRole(user, Security.Roles.ADMIN, Security.Roles.CASHIER);
        if (r.is("payments") && r.method("GET")) {
            String sql = "SELECT p.payment_id,p.tuition_id,p.amount,p.method,p.request_id,p.receipt_code,p.paid_at,e.enrollment_id,s.student_id,s.full_name AS student_name FROM payments p JOIN tuition_fees tf ON tf.tuition_id=p.tuition_id JOIN enrollments e ON e.enrollment_id=tf.enrollment_id JOIN students s ON s.student_id=e.student_id WHERE (?=0 OR p.tuition_id=?) AND (?=0 OR s.student_id=?) ORDER BY p.paid_at DESC LIMIT ?";
            long tid = r.q("tuition_id").isEmpty() ? 0 : id(r.q("tuition_id")), sid = r.q("student_id").isEmpty() ? 0 : id(r.q("student_id"));
            return result("items", many(sql, tid, tid, sid, sid, limit(r, 200)));
        }
        throw new ApiException(404, "ENDPOINT_NOT_FOUND", "API endpoint không tồn tại.");
    }

    private Map<String,Object> sessionRoutes(Request r, User user) throws SQLException {
        if (r.segs.size() == 3 && "roster".equals(r.seg(2)) && r.method("GET")) return sessionRoster(r, user);
        if (r.segs.size() == 3 && "attendance".equals(r.seg(2)) && r.method("GET")) return sessionAttendance(r, user);
        if (r.segs.size() == 4 && "attendance".equals(r.seg(2)) && r.method("PUT")) return markAttendance(r, user);
        if (r.is("sessions") && r.method("GET")) {
            requireRole(user, Security.Roles.ADMIN, Security.Roles.TRAINING, Security.Roles.TEACHER);
            long classId = r.q("class_id").isEmpty() ? 0 : id(r.q("class_id"));
            if (Security.Roles.TEACHER.equals(user.role)) {
                if (user.teacherId <= 0) throw ApiException.forbidden();
                if (classId > 0) assertTeacherClass(user, classId);
                return result("items", many("SELECT s.session_id,s.class_id,s.session_date,s.start_time,s.end_time,cl.code AS class_code,cl.name AS class_name FROM sessions s JOIN classes cl ON cl.class_id=s.class_id WHERE cl.teacher_id=? AND (?=0 OR s.class_id=?) ORDER BY s.session_date DESC,s.start_time LIMIT ?", user.teacherId, classId, classId, limit(r, 200)));
            }
            return result("items", many("SELECT s.session_id,s.class_id,s.session_date,s.start_time,s.end_time,cl.code AS class_code,cl.name AS class_name FROM sessions s JOIN classes cl ON cl.class_id=s.class_id WHERE (?=0 OR s.class_id=?) ORDER BY s.session_date DESC,s.start_time LIMIT ?", classId, classId, limit(r, 200)));
        }
        if (r.is("sessions") && r.method("POST")) {
            requireRole(user, Security.Roles.ADMIN, Security.Roles.TRAINING);
            Map<String,Object> b = r.body(); long classId = idValue(b.get("class_id"), "class_id");
            Map<String,Object> cls = one("SELECT status FROM classes WHERE class_id=?", classId); if (cls == null) throw ApiException.missing("Lớp học");
            if ("CANCELLED".equals(upper(str(cls.get("status"))))) throw ApiException.conflict("Không thể tạo buổi cho lớp đã hủy.");
            Date day = date(b.get("session_date"), "session_date"); Time start = time(b.get("start_time"), "start_time"), end = time(b.get("end_time"), "end_time");
            if (!start.before(end)) throw ApiException.bad("Giờ kết thúc phải sau giờ bắt đầu.");
            if (hasSessionOverlap(classId, null, day, start, end)) throw ApiException.conflict("Buổi học trùng thời gian với buổi khác trong lớp.");
            long id = insert("INSERT INTO sessions(class_id,session_date,start_time,end_time) VALUES(?,?,?,?)", classId, day, start, end);
            r.responseStatus = 201; return result("session", one("SELECT session_id,class_id,session_date,start_time,end_time FROM sessions WHERE session_id=?", id));
        }
        if (r.segs.size() == 2 && r.method("PATCH")) {
            requireRole(user, Security.Roles.ADMIN, Security.Roles.TRAINING);
            long id = id(r.seg(1)); Map<String,Object> old = one("SELECT * FROM sessions WHERE session_id=?", id); if (old == null) throw ApiException.missing("Buổi học");
            Number marks = scalar("SELECT COUNT(*) FROM attendance WHERE session_id=?", id); if (marks.intValue() > 0) throw ApiException.conflict("Buổi đã có điểm danh nên không thể đổi lịch.");
            Map<String,Object> b = r.body(); long classId = b.containsKey("class_id") ? idValue(b.get("class_id"), "class_id") : number(old.get("class_id")).longValue();
            Date day = b.containsKey("session_date") ? date(b.get("session_date"), "session_date") : date(old.get("session_date"), "session_date");
            Time start = b.containsKey("start_time") ? time(b.get("start_time"), "start_time") : time(old.get("start_time"), "start_time");
            Time end = b.containsKey("end_time") ? time(b.get("end_time"), "end_time") : time(old.get("end_time"), "end_time");
            if (!start.before(end)) throw ApiException.bad("Giờ kết thúc phải sau giờ bắt đầu.");
            ensureExists("SELECT class_id FROM classes WHERE class_id=? AND status<>'CANCELLED'", classId, "Lớp học đang hoạt động");
            if (hasSessionOverlap(classId, id, day, start, end)) throw ApiException.conflict("Buổi học trùng thời gian với buổi khác trong lớp.");
            execute("UPDATE sessions SET class_id=?,session_date=?,start_time=?,end_time=? WHERE session_id=?", classId, day, start, end, id);
            return result("session", one("SELECT session_id,class_id,session_date,start_time,end_time FROM sessions WHERE session_id=?", id));
        }
        throw new ApiException(404, "ENDPOINT_NOT_FOUND", "API endpoint không tồn tại.");
    }

    private Map<String,Object> sessionRoster(Request r, User user) throws SQLException {
        Map<String,Object> session = one("SELECT session_id,class_id,session_date FROM sessions WHERE session_id=?", id(r.seg(1))); if (session == null) throw ApiException.missing("Buổi học");
        requireTeacherOrAdmin(user, number(session.get("class_id")).longValue());
        List<Map<String,Object>> roster = many("SELECT st.student_id,st.full_name,st.email,st.phone,a.status AS attendance_status,a.marked_at FROM enrollments e JOIN students st ON st.student_id=e.student_id LEFT JOIN attendance a ON a.session_id=? AND a.student_id=st.student_id WHERE e.class_id=? AND e.status='CONFIRMED' AND DATE(e.enrolled_at)<=? ORDER BY st.full_name",
            number(session.get("session_id")).longValue(), number(session.get("class_id")).longValue(), date(session.get("session_date"), "session_date"));
        return result("session", session, "students", roster);
    }

    private Map<String,Object> sessionAttendance(Request r, User user) throws SQLException {
        Map<String,Object> session = one("SELECT session_id,class_id FROM sessions WHERE session_id=?", id(r.seg(1))); if (session == null) throw ApiException.missing("Buổi học");
        requireTeacherOrAdmin(user, number(session.get("class_id")).longValue());
        return result("items", many("SELECT a.attendance_id,a.session_id,a.student_id,s.full_name AS student_name,a.status,a.marked_at FROM attendance a JOIN students s ON s.student_id=a.student_id WHERE a.session_id=? ORDER BY s.full_name", number(session.get("session_id")).longValue()));
    }

    private Map<String,Object> markAttendance(Request r, User user) throws SQLException {
        long sessionId = id(r.seg(1)), studentId = id(r.seg(3)); Map<String,Object> b = r.body(); String status = upper(requiredText(b, "status"));
        if (!Arrays.asList("PRESENT", "ABSENT_EXCUSED", "ABSENT_UNEXCUSED").contains(status)) throw ApiException.bad("Trạng thái điểm danh không hợp lệ.");
        Map<String,Object> session = one("SELECT session_id,class_id,session_date FROM sessions WHERE session_id=?", sessionId); if (session == null) throw ApiException.missing("Buổi học");
        long classId = number(session.get("class_id")).longValue(); requireRole(user, Security.Roles.ADMIN, Security.Roles.TEACHER); requireTeacherOrAdmin(user, classId);
        Map<String,Object> enrollment = one("SELECT enrollment_id FROM enrollments WHERE class_id=? AND student_id=? AND status='CONFIRMED' AND DATE(enrolled_at)<=?", classId, studentId, date(session.get("session_date"), "session_date"));
        if (enrollment == null) throw ApiException.conflict("Học viên không có đăng ký CONFIRMED hợp lệ tại ngày học.");
        execute("INSERT INTO attendance(session_id,student_id,status,marked_at) VALUES(?,?,?,CURRENT_TIMESTAMP) ON DUPLICATE KEY UPDATE status=VALUES(status),marked_at=CURRENT_TIMESTAMP", sessionId, studentId, status);
        return result("attendance", one("SELECT a.attendance_id,a.session_id,a.student_id,s.full_name AS student_name,a.status,a.marked_at FROM attendance a JOIN students s ON s.student_id=a.student_id WHERE a.session_id=? AND a.student_id=?", sessionId, studentId));
    }

    private Map<String,Object> attendanceRoutes(Request r, User user) throws SQLException {
        requireRole(user, Security.Roles.ADMIN, Security.Roles.TEACHER);
        long sessionId = r.q("session_id").isEmpty() ? 0 : id(r.q("session_id")); long studentId = r.q("student_id").isEmpty() ? 0 : id(r.q("student_id"));
        StringBuilder sql = new StringBuilder("SELECT a.attendance_id,a.session_id,a.student_id,a.status,a.marked_at,s.session_date,s.start_time,s.end_time,cl.class_id,cl.code AS class_code,cl.name AS class_name,st.full_name AS student_name FROM attendance a JOIN sessions s ON s.session_id=a.session_id JOIN classes cl ON cl.class_id=s.class_id JOIN students st ON st.student_id=a.student_id WHERE (?=0 OR a.session_id=?) AND (?=0 OR a.student_id=?)");
        List<Object> args = new ArrayList<Object>(Arrays.asList(sessionId, sessionId, studentId, studentId));
        if (Security.Roles.TEACHER.equals(user.role)) { if (user.teacherId <= 0) throw ApiException.forbidden(); sql.append(" AND cl.teacher_id=?"); args.add(user.teacherId); }
        sql.append(" ORDER BY s.session_date DESC,st.full_name LIMIT ?"); args.add(limit(r, 300));
        return result("items", many(sql.toString(), args.toArray()));
    }

    private Map<String,Object> attendanceSummary(Request r, User user) throws SQLException {
        requireRole(user, Security.Roles.ADMIN, Security.Roles.TEACHER); long classId = id(r.seg(1)); requireTeacherOrAdmin(user, classId);
        long studentId = id(r.q("student_id"));
        Map<String,Object> student = one("SELECT student_id,full_name FROM students WHERE student_id=?", studentId); if (student == null) throw ApiException.missing("Học viên");
        ensureExists("SELECT enrollment_id FROM enrollments WHERE class_id=? AND student_id=?", classId, "Đăng ký học viên vào lớp");
        Map<String,Object> totals = one("SELECT COUNT(*) AS marked_sessions,SUM(a.status='PRESENT') AS present_count,SUM(a.status='ABSENT_EXCUSED') AS excused_count,SUM(a.status='ABSENT_UNEXCUSED') AS unexcused_count,CASE WHEN COUNT(*)=0 THEN 0 ELSE ROUND(100.0*SUM(a.status='PRESENT')/COUNT(*),2) END AS attendance_rate FROM attendance a JOIN sessions s ON s.session_id=a.session_id WHERE s.class_id=? AND a.student_id=?", classId, studentId);
        List<Map<String,Object>> details = many("SELECT a.attendance_id,a.session_id,s.session_date,s.start_time,s.end_time,a.status,a.marked_at FROM attendance a JOIN sessions s ON s.session_id=a.session_id WHERE s.class_id=? AND a.student_id=? ORDER BY s.session_date,s.start_time", classId, studentId);
        return result("student", student, "summary", totals, "items", details);
    }

    private Map<String,Object> myAttendance(User user) throws SQLException {
        requireRole(user, Security.Roles.STUDENT); if (user.studentId <= 0) throw ApiException.forbidden();
        return result("items", many("SELECT a.attendance_id,a.session_id,a.student_id,a.status,a.marked_at,s.session_date,s.start_time,s.end_time,cl.class_id,cl.code AS class_code,cl.name AS class_name FROM attendance a JOIN sessions s ON s.session_id=a.session_id JOIN classes cl ON cl.class_id=s.class_id WHERE a.student_id=? ORDER BY s.session_date DESC,s.start_time", user.studentId));
    }

    private Map<String,Object> meProfile(Request r, User user) throws SQLException {
        requireRole(user, Security.Roles.STUDENT); if (user.studentId <= 0) throw ApiException.forbidden();
        if (r.method("GET")) {
            Map<String,Object> profile = one("SELECT s.student_id,s.full_name,s.email,s.phone,s.date_of_birth,s.gender,s.address,u.status AS account_status FROM students s JOIN users u ON u.user_id=s.user_id WHERE s.student_id=? AND s.user_id=?", user.studentId, user.id);
            if (profile == null) throw ApiException.missing("Hồ sơ học viên"); return result("profile", profile);
        }
        if (r.method("PUT") || r.method("PATCH")) {
            Map<String,Object> b = r.body(); if (b.containsKey("student_id") || b.containsKey("user_id")) throw ApiException.bad("Không thể thay đổi định danh liên kết.");
            if (b.containsKey("email")) throw ApiException.bad("Đổi email cần quy trình xác minh địa chỉ mới và hiện chưa được mở trong API v3.");
            String name = b.containsKey("full_name") ? requiredText(b, "full_name") : null;
            String phone = b.containsKey("phone") ? optionalText(b.get("phone")) : null;
            Date dob = b.containsKey("date_of_birth") ? dateOrNull(b.get("date_of_birth")) : null;
            String gender = b.containsKey("gender") ? optionalText(b.get("gender")) : null;
            String address = b.containsKey("address") ? optionalText(b.get("address")) : null;
            execute("UPDATE students SET full_name=COALESCE(?,full_name),phone=IF(?=1,?,phone),date_of_birth=IF(?=1,?,date_of_birth),gender=IF(?=1,?,gender),address=IF(?=1,?,address) WHERE student_id=? AND user_id=?",
                name, b.containsKey("phone") ? 1 : 0, phone, b.containsKey("date_of_birth") ? 1 : 0, dob, b.containsKey("gender") ? 1 : 0, gender, b.containsKey("address") ? 1 : 0, address, user.studentId, user.id);
            return result("profile", one("SELECT s.student_id,s.full_name,s.email,s.phone,s.date_of_birth,s.gender,s.address,u.status AS account_status FROM students s JOIN users u ON u.user_id=s.user_id WHERE s.student_id=? AND s.user_id=?", user.studentId, user.id));
        }
        throw new ApiException(405, "METHOD_NOT_ALLOWED", "Phương thức HTTP không được hỗ trợ cho hồ sơ cá nhân.");
    }

    private Map<String,Object> mySchedule(User user) throws SQLException {
        requireRole(user, Security.Roles.STUDENT); if (user.studentId <= 0) throw ApiException.forbidden();
        String sql = "SELECT e.enrollment_id,e.status AS enrollment_status,cl.class_id,cl.code AS class_code,cl.name AS class_name,c.course_id,c.code AS course_code,c.name AS course_name,t.full_name AS teacher_name,cs.day_of_week,cs.start_time AS weekly_start_time,cs.end_time AS weekly_end_time,s.session_id,s.session_date,s.start_time AS session_start_time,s.end_time AS session_end_time FROM enrollments e JOIN classes cl ON cl.class_id=e.class_id JOIN courses c ON c.course_id=cl.course_id LEFT JOIN teachers t ON t.teacher_id=cl.teacher_id LEFT JOIN class_schedules cs ON cs.class_id=cl.class_id LEFT JOIN sessions s ON s.class_id=cl.class_id AND s.session_date>=CURRENT_DATE WHERE e.student_id=? AND e.status='CONFIRMED' ORDER BY s.session_date,cs.day_of_week,cs.start_time";
        return result("items", many(sql, user.studentId));
    }

    private Map<String,Object> myTuition(User user) throws SQLException {
        requireRole(user, Security.Roles.STUDENT); if (user.studentId <= 0) throw ApiException.forbidden();
        List<Map<String,Object>> fees = many(tuitionSelect() + " WHERE s.student_id=? ORDER BY tf.tuition_id DESC", user.studentId);
        for (Map<String,Object> fee : fees) fee.put("payments", many("SELECT payment_id,tuition_id,amount,method,receipt_code,paid_at FROM payments WHERE tuition_id=? ORDER BY paid_at DESC", number(fee.get("tuition_id")).longValue()));
        return result("items", fees);
    }

    private User authenticate(Request r) throws SQLException {
        Security.Session session = sessions.find(r.bearer); if (session == null) throw ApiException.unauthorized();
        User user = loadUser(session.userId);
        if (user == null || !"ACTIVE".equals(user.status)) { sessions.revoke(r.bearer); throw ApiException.unauthorized(); }
        return user;
    }

    private User loadUser(long id) throws SQLException {
        Map<String,Object> row = one("SELECT u.user_id,u.email,u.role,u.status,s.student_id,t.teacher_id FROM users u LEFT JOIN students s ON s.user_id=u.user_id LEFT JOIN teachers t ON t.user_id=u.user_id WHERE u.user_id=?", id);
        if (row == null) return null;
        return new User(number(row.get("user_id")).longValue(), str(row.get("email")), Security.Roles.normalize(str(row.get("role"))), upper(str(row.get("status"))),
            row.get("student_id") == null ? 0 : number(row.get("student_id")).longValue(), row.get("teacher_id") == null ? 0 : number(row.get("teacher_id")).longValue());
    }

    private void requireTeacherOrAdmin(User user, long classId) throws SQLException {
        if (Security.Roles.ADMIN.equals(user.role)) return;
        requireRole(user, Security.Roles.TEACHER); assertTeacherClass(user, classId);
    }
    private void assertTeacherClass(User user, long classId) throws SQLException {
        if (user.teacherId <= 0 || one("SELECT class_id FROM classes WHERE class_id=? AND teacher_id=?", classId, user.teacherId) == null) throw ApiException.forbidden();
    }
    private static void requireRole(User user, String... roles) {
        for (String role : roles) if (role.equals(user.role)) return;
        throw ApiException.forbidden();
    }

    private boolean hasSessionOverlap(long classId, Long excludeId, Date day, Time start, Time end) throws SQLException {
        Number count;
        if (excludeId == null) count = scalar("SELECT COUNT(*) FROM sessions WHERE class_id=? AND session_date=? AND NOT(end_time<=? OR start_time>=?)", classId, day, start, end);
        else count = scalar("SELECT COUNT(*) FROM sessions WHERE class_id=? AND session_date=? AND session_id<>? AND NOT(end_time<=? OR start_time>=?)", classId, day, excludeId, start, end);
        return count.intValue() > 0;
    }

    private void saveOneTimeToken(Connection c, String table, String idColumn, long userId, String rawToken, int hours) throws SQLException {
        if (!("verification_tokens".equals(table) || "password_reset_tokens".equals(table))) throw new IllegalArgumentException("Invalid token table");
        execute(c, "UPDATE " + table + " SET used_at=CURRENT_TIMESTAMP WHERE user_id=? AND used_at IS NULL", userId);
        String interval = hours == 1 ? "1 HOUR" : "24 HOUR";
        execute(c, "INSERT INTO " + table + "(user_id,token,expires_at,used_at) VALUES(?,?,DATE_ADD(CURRENT_TIMESTAMP,INTERVAL " + interval + "),NULL)", userId, Security.sha256Hex(rawToken));
    }

    private void sendVerification(String email, String token) {
        if (!mailer.configured()) throw new ApiException(503, "EMAIL_NOT_CONFIGURED", "Email chưa được cấu hình; đặt API_SMTP_HOST và API_SMTP_FROM rồi thử lại.");
        try { mailer.send(email, "Xác minh tài khoản PHLingo", "Mở liên kết sau trong vòng 24 giờ để kích hoạt tài khoản:\n" + publicBase() + "/api/auth/verify-email?token=" + token + "\nNếu bạn không tạo tài khoản, hãy bỏ qua email này."); }
        catch (IOException ex) { System.err.println("Verification email was not delivered: " + ex.getClass().getSimpleName()); throw new ApiException(503, "EMAIL_UNAVAILABLE", "Không gửi được email xác minh; hãy thử lại sau."); }
    }
    private static String publicBase() { return setting("API_PUBLIC_BASE_URL", "http://localhost:8080").replaceAll("/+$", ""); }

    private boolean validTransition(String old, String next) {
        if (old.equals(next)) return true;
        if ("OPEN".equals(old)) return "IN_PROGRESS".equals(next) || "CANCELLED".equals(next);
        if ("IN_PROGRESS".equals(old)) return "COMPLETED".equals(next) || "CANCELLED".equals(next);
        return false;
    }
    private void validateClassStatus(String status) { if (!Arrays.asList("OPEN", "IN_PROGRESS", "COMPLETED", "CANCELLED").contains(upper(status))) throw ApiException.bad("Trạng thái lớp không hợp lệ."); }
    private void validateCourseStatus(String status) { if (!Arrays.asList("ACTIVE", "INACTIVE", "OPEN", "PUBLISHED").contains(upper(status))) throw ApiException.bad("Trạng thái khóa học không hợp lệ."); }
    private void ensureExists(String sql, long id, String name) throws SQLException { if (one(sql, id) == null) throw ApiException.missing(name); }

    private static String courseColumns() { return "c.course_id,c.code,c.name,c.tuition_listed,c.status,c.description,c.level,c.duration_hours"; }
    private static String studentSelect() { return "SELECT s.student_id,s.full_name,s.email,s.phone,s.user_id,s.date_of_birth,s.gender,s.address,u.status AS account_status FROM students s JOIN users u ON u.user_id=s.user_id"; }
    private static String classSelect() {
        return "SELECT cl.class_id,cl.course_id,c.code AS course_code,c.name AS course_name,c.tuition_listed,cl.teacher_id,t.full_name AS teacher_name,cl.code,cl.name,cl.capacity,cl.status,"
            + "(SELECT COUNT(*) FROM enrollments e WHERE e.class_id=cl.class_id AND e.status='CONFIRMED') AS enrolled_count,"
            + "GREATEST(cl.capacity-(SELECT COUNT(*) FROM enrollments e WHERE e.class_id=cl.class_id AND e.status='CONFIRMED'),0) AS seats_left FROM classes cl JOIN courses c ON c.course_id=cl.course_id LEFT JOIN teachers t ON t.teacher_id=cl.teacher_id";
    }
    private static String enrollmentSelect() {
        return "SELECT e.enrollment_id,e.student_id,s.full_name AS student_name,s.email AS student_email,e.class_id,cl.code AS class_code,cl.name AS class_name,e.status,e.enrolled_at,tf.tuition_id,tf.amount_due AS tuition_due,tf.status AS tuition_status "
            + "FROM enrollments e JOIN students s ON s.student_id=e.student_id JOIN classes cl ON cl.class_id=e.class_id LEFT JOIN tuition_fees tf ON tf.enrollment_id=e.enrollment_id";
    }
    private static String tuitionSelect() {
        return "SELECT tf.tuition_id,tf.enrollment_id,e.student_id,s.full_name AS student_name,s.email AS student_email,e.class_id,cl.code AS class_code,cl.name AS class_name,c.code AS course_code,c.name AS course_name,"
            + "tf.amount_due,COALESCE(p.paid_amount,0) AS paid_amount,GREATEST(tf.amount_due-COALESCE(p.paid_amount,0),0) AS balance,"
            + "CASE WHEN COALESCE(p.paid_amount,0)=0 THEN 'UNPAID' WHEN COALESCE(p.paid_amount,0)<tf.amount_due THEN 'PARTIAL' ELSE 'PAID' END AS status "
            + "FROM tuition_fees tf JOIN enrollments e ON e.enrollment_id=tf.enrollment_id JOIN students s ON s.student_id=e.student_id JOIN classes cl ON cl.class_id=e.class_id JOIN courses c ON c.course_id=cl.course_id "
            + "LEFT JOIN (SELECT tuition_id,SUM(amount) AS paid_amount FROM payments GROUP BY tuition_id) p ON p.tuition_id=tf.tuition_id";
    }

    private Map<String,Object> one(String sql, Object... args) throws SQLException { try (Connection c = db.open()) { return one(c, sql, args); } }
    private Map<String,Object> one(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) { bind(ps, args); try (ResultSet rs = ps.executeQuery()) { return rs.next() ? row(rs) : null; } }
    }
    private List<Map<String,Object>> many(String sql, Object... args) throws SQLException {
        try (Connection c = db.open(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, args); try (ResultSet rs = ps.executeQuery()) { List<Map<String,Object>> items = new ArrayList<Map<String,Object>>(); while (rs.next()) items.add(row(rs)); return items; }
        }
    }
    private Number scalar(String sql, Object... args) throws SQLException { try (Connection c = db.open()) { return scalar(c, sql, args); } }
    private Number scalar(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) { bind(ps, args); try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) return Long.valueOf(0); Object v = rs.getObject(1); return v instanceof Number ? (Number) v : Long.valueOf(v == null ? 0 : Long.parseLong(String.valueOf(v))); } }
    }
    private int execute(String sql, Object... args) throws SQLException { try (Connection c = db.open()) { return execute(c, sql, args); } }
    private int execute(Connection c, String sql, Object... args) throws SQLException { try (PreparedStatement ps = c.prepareStatement(sql)) { bind(ps, args); return ps.executeUpdate(); } }
    private long insert(String sql, Object... args) throws SQLException { try (Connection c = db.open()) { return insert(c, sql, args); } }
    private long insert(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            bind(ps, args); ps.executeUpdate(); try (ResultSet keys = ps.getGeneratedKeys()) { if (!keys.next()) throw new SQLException("No generated key returned"); return keys.getLong(1); }
        }
    }
    private static void bind(PreparedStatement ps, Object[] args) throws SQLException { for (int i=0; i<args.length; i++) ps.setObject(i + 1, args[i]); }
    private static Map<String,Object> row(ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData(); Map<String,Object> row = new LinkedHashMap<String,Object>();
        for (int i=1; i<=md.getColumnCount(); i++) row.put(md.getColumnLabel(i), rs.getObject(i)); return row;
    }
    private static void rollback(Connection c) { try { c.rollback(); } catch (SQLException ignored) { } }

    private static Map<String,Object> result(Object... pairs) {
        Map<String,Object> result = new LinkedHashMap<String,Object>();
        for (int i=0; i+1<pairs.length; i+=2) result.put(String.valueOf(pairs[i]), pairs[i+1]); return result;
    }
    private static String str(Object value) { return value == null ? "" : String.valueOf(value); }
    private static String text(Object value) { return value == null ? "" : String.valueOf(value).trim(); }
    private static String upper(String value) { return value == null ? "" : value.trim().toUpperCase(Locale.ROOT); }
    private static String optionalText(Object value) { String v = text(value); return v.isEmpty() ? null : v; }
    private static String requiredText(Map<String,Object> b, String key) {
        String value = text(b.get(key)); if (value.isEmpty()) throw ApiException.bad("Trường " + key + " không được để trống."); return value;
    }
    private static String requiredEmail(Map<String,Object> b, String key) { return validateEmail(requiredText(b, key)); }
    private static String optionalEmail(Object value) { String email = text(value); return email.isEmpty() ? "" : validateEmail(email); }
    private static String validateEmail(String raw) {
        String email = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (email.length() > 255 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw ApiException.bad("Email không hợp lệ.");
        return email;
    }
    private static void validatePassword(String password) {
        if (password == null || password.length() < 10 || password.length() > 128 || !password.matches("(?s).*[A-Za-z].*") || !password.matches("(?s).*\\d.*"))
            throw ApiException.bad("Mật khẩu cần từ 10 đến 128 ký tự, có ít nhất một chữ cái và một chữ số.");
    }
    private static BigDecimal money(Object value, String key, boolean required) {
        if (value == null || text(value).isEmpty()) { if (required) throw ApiException.bad("Thiếu trường " + key + "."); return BigDecimal.ZERO; }
        try { BigDecimal amount = value instanceof BigDecimal ? (BigDecimal) value : new BigDecimal(String.valueOf(value)); if (amount.scale() > 2) throw ApiException.bad(key + " chỉ hỗ trợ tối đa 2 chữ số thập phân."); return amount; }
        catch (NumberFormatException ex) { throw ApiException.bad(key + " phải là số hợp lệ."); }
    }
    private static BigDecimal decimal(Object value) {
        if (value == null) return BigDecimal.ZERO; if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return new BigDecimal(String.valueOf(value)); return new BigDecimal(String.valueOf(value));
    }
    private static Number number(Object value) {
        if (value instanceof Number) return (Number) value; if (value == null) return Long.valueOf(0);
        try { return Long.valueOf(String.valueOf(value)); } catch (NumberFormatException ex) { return new BigDecimal(String.valueOf(value)); }
    }
    private static long idValue(Object value, String name) { long n; try { n = number(value).longValue(); } catch (Exception ex) { throw ApiException.bad(name + " phải là số nguyên dương."); } if (n <= 0) throw ApiException.bad(name + " phải là số nguyên dương."); return n; }
    private static long id(String value) { return idValue(value, "id"); }
    private static Long nullableLong(Object value) { if (value == null || text(value).isEmpty()) return null; return Long.valueOf(idValue(value, "id")); }
    private static int positiveInt(Object value, String key) { long n = idValue(value, key); if (n > Integer.MAX_VALUE) throw ApiException.bad(key + " quá lớn."); return (int)n; }
    private static Integer optionalInt(Object value) { if (value == null || text(value).isEmpty()) return null; try { return Integer.valueOf(String.valueOf(value)); } catch (NumberFormatException ex) { throw ApiException.bad("Giá trị phải là số nguyên."); } }
    private static Date dateOrNull(Object value) { return value == null || text(value).isEmpty() ? null : date(value, "date_of_birth"); }
    private static Date date(Object value, String key) {
        if (value instanceof Date) return (Date)value; try { return Date.valueOf(LocalDate.parse(text(value))); }
        catch (DateTimeParseException ex) { throw ApiException.bad(key + " cần định dạng YYYY-MM-DD."); }
    }
    private static Time time(Object value, String key) {
        if (value instanceof Time) return (Time)value; String v = text(value); if (v.length() == 5) v += ":00";
        try { return Time.valueOf(LocalTime.parse(v)); } catch (DateTimeParseException ex) { throw ApiException.bad(key + " cần định dạng HH:mm hoặc HH:mm:ss."); }
    }
    private static Timestamp timestamp(Object value) {
        if (value instanceof Timestamp) return (Timestamp)value; String v = text(value).replace('T', ' ');
        try { return Timestamp.valueOf(v); } catch (IllegalArgumentException ex) { throw ApiException.bad("Thời hạn token trong cơ sở dữ liệu không hợp lệ."); }
    }
    private static int limit(Request r, int fallback) { int value = integer(r.q("limit"), fallback); return Math.max(1, Math.min(value, 500)); }
    private static int integer(String value, int fallback) { try { return Integer.parseInt(value); } catch (Exception ex) { return fallback; } }
    private static int integerSetting(String key, int fallback) { return integer(setting(key, String.valueOf(fallback)), fallback); }
    private static String setting(String name, String fallback) { String value = System.getenv(name); return value == null || value.trim().isEmpty() ? fallback : value.trim(); }

    private static void addCors(HttpExchange ex) {
        Headers h = ex.getResponseHeaders(); h.set("Access-Control-Allow-Origin", setting("API_CORS_ORIGIN", "*"));
        h.set("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS"); h.set("Access-Control-Allow-Headers", "Authorization,Content-Type,Idempotency-Key");
        h.set("Access-Control-Max-Age", "600"); h.set("Cache-Control", "no-store");
    }
    private static void sendError(HttpExchange ex, int status, String code, String message) throws IOException { send(ex, status, result("ok", false, "error", code, "message", message)); }
    private static void send(HttpExchange ex, int status, Map<String,Object> body) throws IOException {
        byte[] bytes = Json.stringify(body).getBytes(StandardCharsets.UTF_8); ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length); ex.getResponseBody().write(bytes);
    }
    private static void sendHtml(HttpExchange ex, int status, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8); ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff"); ex.getResponseHeaders().set("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; script-src 'unsafe-inline'; connect-src 'self'");
        ex.sendResponseHeaders(status, bytes.length); ex.getResponseBody().write(bytes);
    }
    private static String resetPasswordPage(String token) {
        String hidden = htmlEscape(token);
        return "<!doctype html><html lang=\"vi\"><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>Đặt lại mật khẩu PHLingo</title>"
            + "<body style=\"font:16px Arial,sans-serif;max-width:520px;margin:48px auto;padding:0 16px;color:#1f2937\"><h1>Đặt lại mật khẩu</h1>"
            + (token == null || token.trim().isEmpty() ? "<p>Liên kết thiếu token. Hãy yêu cầu email khôi phục mới.</p>" :
            "<form id=\"reset\"><input id=\"token\" type=\"hidden\" value=\"" + hidden + "\"><label>Mật khẩu mới<br><input id=\"password\" type=\"password\" minlength=\"10\" maxlength=\"128\" required style=\"width:100%;padding:10px;margin:8px 0 16px\"></label><br>"
            + "<label>Nhập lại mật khẩu<br><input id=\"confirm\" type=\"password\" minlength=\"10\" maxlength=\"128\" required style=\"width:100%;padding:10px;margin:8px 0 16px\"></label><br>"
            + "<button style=\"padding:10px 18px\">Lưu mật khẩu</button></form><p id=\"message\" role=\"status\"></p>"
            + "<script>document.getElementById('reset').addEventListener('submit',async function(e){e.preventDefault();var p=document.getElementById('password').value,c=document.getElementById('confirm').value,m=document.getElementById('message');if(p!==c){m.textContent='Mật khẩu xác nhận không khớp.';return;}try{var r=await fetch('/api/auth/reset-password',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({token:document.getElementById('token').value,new_password:p,confirm_password:c})});var d=await r.json();m.textContent=d.message||'Không thể đặt lại mật khẩu.';if(r.ok)document.getElementById('reset').remove();}catch(_){m.textContent='Không thể kết nối máy chủ.';}});</script>")
            + "</body></html>";
    }
    private static String htmlEscape(String value) { return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;"); }

    private static final class User {
        final long id; final String email, role, status; final long studentId, teacherId;
        User(long id, String email, String role, String status, long studentId, long teacherId) { this.id=id; this.email=email; this.role=role; this.status=status; this.studentId=studentId; this.teacherId=teacherId; }
        Map<String,Object> publicView() { return result("user_id", id, "email", email, "role", role, "status", status, "student_id", studentId == 0 ? null : studentId, "teacher_id", teacherId == 0 ? null : teacherId); }
    }

    private static final class Request {
        final HttpExchange exchange; final String method; final List<String> segs = new ArrayList<String>(); final Map<String,String> query = new LinkedHashMap<String,String>();
        final String bearer; final String bodyText; final Map<String,Object> body; int responseStatus; String html;
        Request(HttpExchange exchange) throws IOException {
            this.exchange = exchange; this.method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
            String path = exchange.getRequestURI().getPath(); String rest = path.startsWith("/api") ? path.substring(4) : path;
            for (String part : rest.split("/")) if (!part.isEmpty()) segs.add(decode(part));
            String rawQuery = exchange.getRequestURI().getRawQuery();
            if (rawQuery != null) for (String pair : rawQuery.split("&")) { int at=pair.indexOf('='); String k=decode(at<0?pair:pair.substring(0,at)), v=decode(at<0?"":pair.substring(at+1)); query.put(k,v); }
            String auth = exchange.getRequestHeaders().getFirst("Authorization"); this.bearer = auth != null && auth.regionMatches(true,0,"Bearer ",0,7) ? auth.substring(7).trim() : "";
            if (!Arrays.asList("GET", "DELETE", "OPTIONS").contains(method)) {
                ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] buffer = new byte[4096]; int total=0, n;
                try (InputStream in = exchange.getRequestBody()) { while ((n=in.read(buffer)) >= 0) { total += n; if (total > MAX_BODY) throw new ApiException(413,"BODY_TOO_LARGE","Yêu cầu vượt quá 1 MB."); out.write(buffer,0,n); } }
                bodyText = new String(out.toByteArray(), StandardCharsets.UTF_8); body = bodyText.trim().isEmpty() ? new LinkedHashMap<String,Object>() : Json.object(bodyText);
            } else { bodyText = ""; body = new LinkedHashMap<String,Object>(); }
        }
        String q(String key) { String value = query.get(key); return value == null ? "" : value.trim(); }
        String header(String key) { String value = exchange.getRequestHeaders().getFirst(key); return value == null ? "" : value.trim(); }
        Map<String,Object> body() { return body; }
        String seg(int i) { return i < segs.size() ? segs.get(i) : ""; }
        boolean first(String name) { return !segs.isEmpty() && name.equals(segs.get(0)); }
        boolean is(String... names) { if (segs.size() != names.length) return false; for (int i=0;i<names.length;i++) if (!names[i].equals(segs.get(i))) return false; return true; }
        boolean method(String name) { return name.equals(method); }
        private static String decode(String v) { try { return URLDecoder.decode(v.replace("+", "%2B"), "UTF-8"); } catch (Exception ex) { throw ApiException.bad("Đường dẫn hoặc query string không hợp lệ."); } }
    }
}
