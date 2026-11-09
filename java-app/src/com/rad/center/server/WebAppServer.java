package com.rad.center.server;

import com.rad.center.dao.CenterRepository;
import com.rad.center.model.*;
import com.rad.center.service.CenterService;
import com.rad.center.service.ValidationException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * HTTP REST API & Static Web Server tích hợp trực tiếp với CenterService & CenterRepository.
 * Cho phép chạy giao diện Web hiện đại tại http://localhost:8080 với đầy đủ kiểm tra nghiệp vụ phía Server.
 */
public class WebAppServer {
    private final int port;
    private final CenterService service;
    private final CenterRepository repo;
    private HttpServer server;

    public WebAppServer(int port, CenterService service) {
        this.port = port;
        this.service = service;
        this.repo = CenterRepository.getInstance();
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/", new ApiHandler());
        server.createContext("/", new StaticFileHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("TDTU Language Center Web & API Server started at: http://localhost:" + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private class ApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, X-User");

            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
                ex.sendResponseHeaders(204, -1);
                return;
            }

            String path = ex.getRequestURI().getPath();
            String method = ex.getRequestMethod().toUpperCase();
            Map<String, String> params = parseParams(readBody(ex));

            // Đồng bộ phiên người dùng từ header X-User nếu có
            String headerUser = ex.getRequestHeaders().getFirst("X-User");
            if (headerUser != null && !headerUser.trim().isEmpty()) {
                for (User u : repo.getUsers()) {
                    if (u.getUsername().equalsIgnoreCase(headerUser.trim())) {
                        service.setCurrentUser(u);
                        break;
                    }
                }
            }

            try {
                if ("/api/bootstrap".equals(path) && "GET".equals(method)) {
                    sendJson(ex, 200, buildBootstrapJson());
                } else if ("/api/login".equals(path) && "POST".equals(method)) {
                    User u = service.login(params.get("username"), params.get("password"));
                    sendJson(ex, 200, "{\"ok\":true,\"user\":" + toJson(u) + ",\"data\":" + buildBootstrapJson() + "}");
                } else if ("/api/users".equals(path) && "POST".equals(method)) {
                    String action = params.get("action");
                    if ("update".equalsIgnoreCase(action)) {
                        int stId = -1;
                        String stIdParam = params.get("studentId");
                        if ("__AUTO_CREATE__".equalsIgnoreCase(stIdParam) || "-99".equals(stIdParam)) {
                            stId = -99;
                        } else if (stIdParam != null && !stIdParam.isEmpty()) {
                            stId = parseInt(stIdParam, -1);
                        }
                        User u = service.updateUser(
                                parseInt(params.get("userId"), 0),
                                params.get("fullName"),
                                params.get("role"),
                                params.get("status"),
                                params.get("email"),
                                params.get("phone"),
                                stId
                        );
                        sendJson(ex, 200, "{\"ok\":true,\"user\":" + toJson(u) + ",\"data\":" + buildBootstrapJson() + "}");
                    } else if ("reset_password".equalsIgnoreCase(action)) {
                        User u = service.resetUserPassword(
                                parseInt(params.get("userId"), 0),
                                params.get("newPassword")
                        );
                        sendJson(ex, 200, "{\"ok\":true,\"user\":" + toJson(u) + ",\"data\":" + buildBootstrapJson() + "}");
                    } else if ("change_password".equalsIgnoreCase(action)) {
                        User u = service.changeOwnPassword(
                                params.get("currentPassword"),
                                params.get("newPassword")
                        );
                        sendJson(ex, 200, "{\"ok\":true,\"user\":" + toJson(u) + ",\"data\":" + buildBootstrapJson() + "}");
                    } else if ("register_student".equalsIgnoreCase(action)) {
                        User u = service.registerStudentAccount(
                                params.get("username"),
                                params.get("fullName"),
                                params.get("email"),
                                params.get("phone"),
                                params.get("dob"),
                                params.get("password")
                        );
                        sendJson(ex, 200, "{\"ok\":true,\"emailSent\":true,\"user\":" + toJson(u) + ",\"data\":" + buildBootstrapJson() + "}");
                    } else if ("resend_otp".equalsIgnoreCase(action)) {
                        String em = params.get("email");
                        if (em == null || em.trim().isEmpty()) em = params.get("usernameOrEmail");
                        User u = service.resendVerificationOtp(em);
                        sendJson(ex, 200, "{\"ok\":true,\"emailSent\":true,\"user\":" + toJson(u) + ",\"data\":" + buildBootstrapJson() + "}");
                    } else if ("verify_email".equalsIgnoreCase(action)) {
                        String em = params.get("email");
                        if (em == null || em.trim().isEmpty()) em = params.get("usernameOrEmail");
                        if (em == null || em.trim().isEmpty()) em = params.get("username");
                        User u = service.verifyStudentEmail(
                                em,
                                params.get("token")
                        );
                        sendJson(ex, 200, "{\"ok\":true,\"user\":" + toJson(u) + ",\"data\":" + buildBootstrapJson() + "}");
                    } else if ("request_reset_otp".equalsIgnoreCase(action)) {
                        String em = params.get("email");
                        if (em == null || em.trim().isEmpty()) em = params.get("usernameOrEmail");
                        User u = service.requestPasswordResetOtp(em);
                        sendJson(ex, 200, "{\"ok\":true,\"emailSent\":true,\"user\":" + toJson(u) + ",\"data\":" + buildBootstrapJson() + "}");
                    } else if ("reset_by_token".equalsIgnoreCase(action)) {
                        String em = params.get("email");
                        if (em == null || em.trim().isEmpty()) em = params.get("usernameOrEmail");
                        User u = service.resetPasswordByToken(
                                em,
                                params.get("token"),
                                params.get("newPassword")
                        );
                        sendJson(ex, 200, "{\"ok\":true,\"user\":" + toJson(u) + ",\"data\":" + buildBootstrapJson() + "}");
                    } else {
                        int stId = 0;
                        String stIdParam = params.get("studentId");
                        if ("__AUTO_CREATE__".equalsIgnoreCase(stIdParam) || "-99".equals(stIdParam)) {
                            stId = -99;
                        } else if (stIdParam != null && !stIdParam.isEmpty()) {
                            stId = parseInt(stIdParam, 0);
                        }
                        User u = service.addUser(
                                params.get("username"),
                                params.get("password"),
                                params.get("fullName"),
                                params.get("role"),
                                params.get("status"),
                                params.get("email"),
                                params.get("phone"),
                                stId
                        );
                        sendJson(ex, 200, "{\"ok\":true,\"user\":" + toJson(u) + ",\"data\":" + buildBootstrapJson() + "}");
                    }
                } else if ("/api/reset".equals(path) && "POST".equals(method)) {
                    repo.resetToSampleData();
                    sendJson(ex, 200, "{\"ok\":true,\"message\":\"Đã khôi phục dữ liệu mẫu chuẩn kịch bản Demo!\",\"data\":" + buildBootstrapJson() + "}");
                } else if ("/api/students".equals(path) && "POST".equals(method)) {
                    String action = params.get("action");
                    if ("update".equalsIgnoreCase(action)) {
                        Student s = service.updateStudent(
                                parseInt(params.get("studentId"), 0),
                                params.get("fullName"), params.get("dob"), params.get("gender"),
                                params.get("phone"), params.get("email"), params.get("address"), params.get("status")
                        );
                        sendJson(ex, 200, "{\"ok\":true,\"student\":" + toJson(s) + ",\"data\":" + buildBootstrapJson() + "}");
                    } else if ("delete".equalsIgnoreCase(action)) {
                        int sid = parseInt(params.get("studentId"), 0);
                        service.deleteStudent(sid);
                        sendJson(ex, 200, "{\"ok\":true,\"message\":\"Đã xóa hồ sơ học viên và đồng bộ các phân hệ liên quan!\",\"data\":" + buildBootstrapJson() + "}");
                    } else {
                        Student s = service.addStudent(
                                params.get("studentCode"), params.get("fullName"), params.get("dob"),
                                params.get("gender"), params.get("phone"), params.get("email"), params.get("address")
                        );
                        sendJson(ex, 200, "{\"ok\":true,\"student\":" + toJson(s) + ",\"data\":" + buildBootstrapJson() + "}");
                    }
                } else if ("/api/teachers".equals(path) && "POST".equals(method)) {
                    String action = params.get("action");
                    if ("update".equalsIgnoreCase(action)) {
                        Teacher t = service.updateTeacher(
                                parseInt(params.get("teacherId"), 0),
                                params.get("fullName"), params.get("email"),
                                params.get("phone"), params.get("specialty"), params.get("status")
                        );
                        sendJson(ex, 200, "{\"ok\":true,\"teacher\":" + toJson(t) + ",\"data\":" + buildBootstrapJson() + "}");
                    } else {
                        Teacher t = service.addTeacher(
                                params.get("teacherCode"), params.get("fullName"),
                                params.get("email"), params.get("phone"), params.get("specialty")
                        );
                        sendJson(ex, 200, "{\"ok\":true,\"teacher\":" + toJson(t) + ",\"data\":" + buildBootstrapJson() + "}");
                    }
                } else if ("/api/courses".equals(path) && "POST".equals(method)) {
                    String action = params.get("action");
                    if ("update".equalsIgnoreCase(action)) {
                        Course c = service.updateCourse(
                                parseInt(params.get("courseId"), 0),
                                params.get("courseName"), params.get("description"),
                                params.get("duration"), parseLong(params.get("tuition"), 0L),
                                params.get("entryLevel"), params.get("status")
                        );
                        sendJson(ex, 200, "{\"ok\":true,\"course\":" + toJson(c) + ",\"data\":" + buildBootstrapJson() + "}");
                    } else {
                        Course c = service.addCourse(
                                params.get("courseCode"), params.get("courseName"), params.get("description"),
                                params.get("duration"), parseLong(params.get("tuition"), 0L),
                                params.get("entryLevel"), params.get("status")
                        );
                        sendJson(ex, 200, "{\"ok\":true,\"course\":" + toJson(c) + ",\"data\":" + buildBootstrapJson() + "}");
                    }
                } else if ("/api/classes".equals(path) && "POST".equals(method)) {
                    String action = params.get("action");
                    if ("update".equalsIgnoreCase(action)) {
                        ClassRoom cl = service.updateClassRoom(
                                parseInt(params.get("classId"), 0),
                                parseInt(params.get("courseId"), 1),
                                parseInt(params.get("teacherId"), 1),
                                parseInt(params.get("capacity"), 20),
                                params.get("startDate"), params.get("endDate"),
                                params.get("scheduleSummary"), params.get("room"), params.get("status")
                        );
                        sendJson(ex, 200, "{\"ok\":true,\"classRoom\":" + toJson(cl) + ",\"data\":" + buildBootstrapJson() + "}");
                    } else {
                        ClassRoom cl = service.addClassRoom(
                                params.get("classCode"),
                                parseInt(params.get("courseId"), 1),
                                parseInt(params.get("teacherId"), 1),
                                parseInt(params.get("capacity"), 20),
                                params.get("startDate"), params.get("endDate"),
                                params.get("scheduleSummary"), params.get("room"), params.get("status")
                        );
                        sendJson(ex, 200, "{\"ok\":true,\"classRoom\":" + toJson(cl) + ",\"data\":" + buildBootstrapJson() + "}");
                    }
                } else if ("/api/enrollments".equals(path) && "POST".equals(method)) {
                    String action = params.get("action");
                    if ("cancel".equalsIgnoreCase(action)) {
                        service.cancelEnrollment(parseInt(params.get("enrollmentId"), 0), params.get("reason"));
                        sendJson(ex, 200, "{\"ok\":true,\"data\":" + buildBootstrapJson() + "}");
                    } else if ("delete".equalsIgnoreCase(action)) {
                        service.deleteCancelledEnrollment(parseInt(params.get("enrollmentId"), 0));
                        sendJson(ex, 200, "{\"ok\":true,\"data\":" + buildBootstrapJson() + "}");
                    } else {
                        Enrollment e = service.enrollStudent(
                                parseInt(params.get("studentId"), 0),
                                parseInt(params.get("classId"), 0)
                        );
                        sendJson(ex, 200, "{\"ok\":true,\"enrollment\":" + toJson(e) + ",\"data\":" + buildBootstrapJson() + "}");
                    }
                } else if ("/api/payments".equals(path) && "POST".equals(method)) {
                    Payment p = service.recordPayment(
                            parseInt(params.get("tuitionId"), 0),
                            parseLong(params.get("amount"), 0L),
                            params.get("method"),
                            params.get("note")
                    );
                    sendJson(ex, 200, "{\"ok\":true,\"payment\":" + toJson(p) + ",\"data\":" + buildBootstrapJson() + "}");
                } else if ("/api/sessions".equals(path) && "POST".equals(method)) {
                    Session s = service.addSession(
                            parseInt(params.get("classId"), 0),
                            params.get("topic"),
                            params.get("sessionDate"),
                            params.get("startTime"),
                            params.get("endTime")
                    );
                    sendJson(ex, 200, "{\"ok\":true,\"session\":" + toJson(s) + ",\"data\":" + buildBootstrapJson() + "}");
                } else if ("/api/attendance".equals(path) && "POST".equals(method)) {
                    Attendance a = service.markAttendance(
                            parseInt(params.get("sessionId"), 0),
                            parseInt(params.get("studentId"), 0),
                            params.get("status"),
                            params.get("note")
                    );
                    sendJson(ex, 200, "{\"ok\":true,\"attendance\":" + toJson(a) + ",\"data\":" + buildBootstrapJson() + "}");
                } else {
                    sendJson(ex, 404, "{\"ok\":false,\"ruleCode\":\"HTTP-404\",\"message\":\"API endpoint không tồn tại: " + esc(path) + "\"}");
                }
            } catch (ValidationException vex) {
                sendJson(ex, 400, "{\"ok\":false,\"ruleCode\":\"" + esc(vex.getRuleCode()) + "\",\"message\":\"" + esc(vex.getMessage()) + "\"}");
            } catch (Exception err) {
                sendJson(ex, 500, "{\"ok\":false,\"ruleCode\":\"SYS-500\",\"message\":\"" + esc(err.getMessage()) + "\"}");
            }
        }
    }

    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String uriPath = ex.getRequestURI().getPath();
            if ("/".equals(uriPath) || uriPath.isEmpty()) {
                uriPath = "/index.html";
            }
            File rootDir = new File("../prototype");
            if (!rootDir.exists()) {
                rootDir = new File("prototype");
            }
            File target = new File(rootDir, uriPath.substring(1)).getCanonicalFile();
            if (!target.exists() || target.isDirectory()) {
                target = new File(rootDir, "index.html");
            }
            if (!target.exists()) {
                byte[] msg = "404 Not Found".getBytes(StandardCharsets.UTF_8);
                ex.sendResponseHeaders(404, msg.length);
                ex.getResponseBody().write(msg);
                ex.close();
                return;
            }
            String mime = "text/html; charset=utf-8";
            String name = target.getName().toLowerCase();
            if (name.endsWith(".css")) mime = "text/css; charset=utf-8";
            else if (name.endsWith(".js")) mime = "application/javascript; charset=utf-8";
            else if (name.endsWith(".png")) mime = "image/png";
            else if (name.endsWith(".svg")) mime = "image/svg+xml";
            else if (name.endsWith(".json")) mime = "application/json; charset=utf-8";

            byte[] bytes = Files.readAllBytes(target.toPath());
            ex.getResponseHeaders().set("Content-Type", mime);
            if (name.endsWith(".png") || name.endsWith(".svg")) {
                ex.getResponseHeaders().set("Cache-Control", "public, max-age=86400");
            } else {
                ex.getResponseHeaders().set("Cache-Control", "no-cache");
            }
            ex.sendResponseHeaders(200, bytes.length);
            OutputStream os = ex.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    private void sendJson(HttpExchange ex, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        OutputStream os = ex.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private String readBody(HttpExchange ex) throws IOException {
        InputStream is = ex.getRequestBody();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[2048];
        int r;
        while ((r = is.read(buf)) != -1) {
            bos.write(buf, 0, r);
        }
        return new String(bos.toByteArray(), StandardCharsets.UTF_8);
    }

    private Map<String, String> parseParams(String body) {
        Map<String, String> map = new HashMap<String, String>();
        if (body == null || body.trim().isEmpty()) return map;
        String trimmed = body.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            // Simple flat JSON object parser
            String content = trimmed.substring(1, trimmed.length() - 1);
            StringBuilder token = new StringBuilder();
            boolean inQuotes = false;
            boolean escaped = false;
            for (int i = 0; i < content.length(); i++) {
                char ch = content.charAt(i);
                if (escaped) {
                    token.append(ch);
                    escaped = false;
                } else if (ch == '\\') {
                    escaped = true;
                } else if (ch == '"') {
                    inQuotes = !inQuotes;
                    token.append(ch);
                } else if (ch == ',' && !inQuotes) {
                    putJsonPair(map, token.toString());
                    token.setLength(0);
                } else {
                    token.append(ch);
                }
            }
            if (token.length() > 0) {
                putJsonPair(map, token.toString());
            }
        } else {
            String[] pairs = trimmed.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf('=');
                if (idx > 0) {
                    try {
                        String k = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
                        String v = URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
                        map.put(k, v);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return map;
    }

    private void putJsonPair(Map<String, String> map, String pair) {
        int colon = pair.indexOf(':');
        if (colon <= 0) return;
        String k = cleanJsonVal(pair.substring(0, colon));
        String v = cleanJsonVal(pair.substring(colon + 1));
        map.put(k, v);
    }

    private String cleanJsonVal(String raw) {
        String s = raw.trim();
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            s = s.substring(1, s.length() - 1);
        }
        if ("null".equals(s)) return "";
        return s.replace("\\\"", "\"").replace("\\n", "\n");
    }

    private int parseInt(String s, int def) {
        if (s == null || s.trim().isEmpty()) return def;
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    private long parseLong(String s, long def) {
        if (s == null || s.trim().isEmpty()) return def;
        try { return Long.parseLong(s.trim()); } catch (Exception e) { return def; }
    }

    public String buildBootstrapJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"users\":[");
        List<User> users = repo.getUsers();
        for (int i = 0; i < users.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson(users.get(i)));
        }
        sb.append("],\"students\":[");
        List<Student> students = repo.getStudents();
        for (int i = 0; i < students.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson(students.get(i)));
        }
        sb.append("],\"teachers\":[");
        List<Teacher> teachers = repo.getTeachers();
        for (int i = 0; i < teachers.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson(teachers.get(i)));
        }
        sb.append("],\"courses\":[");
        List<Course> courses = repo.getCourses();
        for (int i = 0; i < courses.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson(courses.get(i)));
        }
        sb.append("],\"classes\":[");
        List<ClassRoom> classes = repo.getClasses();
        for (int i = 0; i < classes.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson(classes.get(i)));
        }
        sb.append("],\"enrollments\":[");
        List<Enrollment> enrollments = repo.getEnrollments();
        for (int i = 0; i < enrollments.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson(enrollments.get(i)));
        }
        sb.append("],\"tuitionFees\":[");
        List<TuitionFee> fees = repo.getTuitionFees();
        for (int i = 0; i < fees.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson(fees.get(i)));
        }
        sb.append("],\"payments\":[");
        List<Payment> payments = repo.getPayments();
        for (int i = 0; i < payments.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson(payments.get(i)));
        }
        sb.append("],\"sessions\":[");
        List<Session> sessions = repo.getSessions();
        for (int i = 0; i < sessions.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson(sessions.get(i)));
        }
        sb.append("],\"attendances\":[");
        List<Attendance> attendances = repo.getAttendances();
        for (int i = 0; i < attendances.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson(attendances.get(i)));
        }
        sb.append("]}");
        return sb.toString();
    }

    private String toJson(User u) {
        return "{\"userId\":" + u.getUserId()
                + ",\"username\":\"" + esc(u.getUsername()) + "\""
                + ",\"fullName\":\"" + esc(u.getFullName()) + "\""
                + ",\"role\":\"" + esc(u.getRole()) + "\""
                + ",\"roleDisplay\":\"" + esc(u.getRoleDisplay()) + "\""
                + ",\"status\":\"" + esc(u.getStatus()) + "\""
                + ",\"email\":\"" + esc(u.getEmail()) + "\""
                + ",\"phone\":\"" + esc(u.getPhone()) + "\""
                + ",\"studentId\":" + u.getStudentId()
                + ",\"createdAt\":\"" + esc(u.getCreatedAt()) + "\""
                + ",\"lastLogin\":\"" + esc(u.getLastLogin()) + "\""
                + ",\"passwordHash\":\"" + esc(u.getPasswordHash()) + "\"}";
    }

    private String toJson(Student s) {
        return "{\"studentId\":" + s.getStudentId()
                + ",\"studentCode\":\"" + esc(s.getStudentCode()) + "\""
                + ",\"fullName\":\"" + esc(s.getFullName()) + "\""
                + ",\"dob\":\"" + esc(s.getDob()) + "\""
                + ",\"gender\":\"" + esc(s.getGender()) + "\""
                + ",\"phone\":\"" + esc(s.getPhone()) + "\""
                + ",\"email\":\"" + esc(s.getEmail()) + "\""
                + ",\"address\":\"" + esc(s.getAddress()) + "\""
                + ",\"status\":\"" + esc(s.getStatus()) + "\""
                + ",\"createdAt\":\"" + esc(s.getCreatedAt()) + "\"}";
    }

    private String toJson(Teacher t) {
        return "{\"teacherId\":" + t.getTeacherId()
                + ",\"teacherCode\":\"" + esc(t.getTeacherCode()) + "\""
                + ",\"fullName\":\"" + esc(t.getFullName()) + "\""
                + ",\"email\":\"" + esc(t.getEmail()) + "\""
                + ",\"phone\":\"" + esc(t.getPhone()) + "\""
                + ",\"specialty\":\"" + esc(t.getSpecialization()) + "\""
                + ",\"status\":\"" + esc(t.getStatus()) + "\"}";
    }

    private String toJson(Course c) {
        return "{\"courseId\":" + c.getCourseId()
                + ",\"courseCode\":\"" + esc(c.getCourseCode()) + "\""
                + ",\"courseName\":\"" + esc(c.getCourseName()) + "\""
                + ",\"description\":\"" + esc(c.getDescription()) + "\""
                + ",\"duration\":\"" + esc(c.getDuration()) + "\""
                + ",\"tuition\":" + c.getTuition()
                + ",\"entryLevel\":\"" + esc(c.getEntryLevel()) + "\""
                + ",\"status\":\"" + esc(c.getStatus()) + "\"}";
    }

    private String toJson(ClassRoom c) {
        return "{\"classId\":" + c.getClassId()
                + ",\"classCode\":\"" + esc(c.getClassCode()) + "\""
                + ",\"courseId\":" + c.getCourseId()
                + ",\"teacherId\":" + c.getTeacherId()
                + ",\"capacity\":" + c.getCapacity()
                + ",\"startDate\":\"" + esc(c.getStartDate()) + "\""
                + ",\"endDate\":\"" + esc(c.getEndDate()) + "\""
                + ",\"scheduleSummary\":\"" + esc(c.getScheduleSummary()) + "\""
                + ",\"room\":\"" + esc(c.getRoom()) + "\""
                + ",\"status\":\"" + esc(c.getStatus()) + "\"}";
    }

    private String toJson(Enrollment e) {
        return "{\"enrollmentId\":" + e.getEnrollmentId()
                + ",\"enrollmentCode\":\"" + esc(e.getEnrollmentCode()) + "\""
                + ",\"studentId\":" + e.getStudentId()
                + ",\"classId\":" + e.getClassId()
                + ",\"enrollDate\":\"" + esc(e.getEnrollmentDate()) + "\""
                + ",\"enrolledBy\":\"" + esc(e.getReceivedBy()) + "\""
                + ",\"status\":\"" + esc(e.getStatus()) + "\""
                + ",\"cancelReason\":\"" + esc(e.getCancelReason()) + "\"}";
    }

    private String toJson(TuitionFee tf) {
        return "{\"tuitionId\":" + tf.getTuitionId()
                + ",\"enrollmentId\":" + tf.getEnrollmentId()
                + ",\"amountDue\":" + tf.getAmountDue()
                + ",\"createdDate\":\"" + esc(tf.getCreatedAt()) + "\""
                + ",\"status\":\"" + esc(tf.getStatus()) + "\"}";
    }

    private String toJson(Payment p) {
        return "{\"paymentId\":" + p.getPaymentId()
                + ",\"paymentCode\":\"" + esc(p.getPaymentCode()) + "\""
                + ",\"tuitionId\":" + p.getTuitionId()
                + ",\"amount\":" + p.getAmount()
                + ",\"paymentDate\":\"" + esc(p.getPaymentDate()) + "\""
                + ",\"method\":\"" + esc(p.getMethod()) + "\""
                + ",\"receivedBy\":\"" + esc(p.getReceivedBy()) + "\""
                + ",\"note\":\"" + esc(p.getNote()) + "\"}";
    }

    private String toJson(Session s) {
        return "{\"sessionId\":" + s.getSessionId()
                + ",\"classId\":" + s.getClassId()
                + ",\"sessionNumber\":" + s.getSessionNumber()
                + ",\"topic\":\"" + esc(s.getTopic()) + "\""
                + ",\"sessionDate\":\"" + esc(s.getSessionDate()) + "\""
                + ",\"startTime\":\"" + esc(s.getStartTime()) + "\""
                + ",\"endTime\":\"" + esc(s.getEndTime()) + "\""
                + ",\"status\":\"" + esc(s.getStatus()) + "\"}";
    }

    private String toJson(Attendance a) {
        return "{\"attendanceId\":" + a.getAttendanceId()
                + ",\"sessionId\":" + a.getSessionId()
                + ",\"studentId\":" + a.getStudentId()
                + ",\"status\":\"" + esc(a.getStatus()) + "\""
                + ",\"markedBy\":\"" + esc(a.getMarkedBy()) + "\""
                + ",\"markedAt\":\"" + esc(a.getMarkedAt()) + "\""
                + ",\"note\":\"" + esc(a.getNote()) + "\"}";
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", "\\n");
    }
}
