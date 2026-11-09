package com.rad.center.util;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * SmtpMailer — Dịch vụ gửi Email xác thực OTP qua Gmail SMTP SSL (Port 465)
 * Đính kèm inline logo chính thức Trường Đại học Tôn Đức Thắng (Content-ID: <tdtu_logo>).
 */
public class SmtpMailer {

    public static final String SMTP_HOST = "smtp.gmail.com";
    public static final int SMTP_PORT = 465;
    public static final String SENDER_EMAIL = "phucn4910@gmail.com";
    public static final String SENDER_APP_PASSWORD = "pevwoqlyqxhlkgcq";
    public static final String PROJECT_SENDER_NAME = "PHLINGO ACADEMY — Đại học Tôn Đức Thắng";

    private static final String FONT_STACK = "'Segoe UI', -apple-system, BlinkMacSystemFont, Roboto, Helvetica, Arial, sans-serif";

    public static void sendVerificationOtpEmail(String recipientEmail, String fullName, String username, String studentCode, String otpCode) throws Exception {
        String subject = "[PHLINGO ACADEMY] Mã xác thực kích hoạt tài khoản: " + otpCode;
        String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String cleanName = cleanDisplayName(fullName);
        String html = buildVerificationHtml(recipientEmail, cleanName, username, studentCode, otpCode, nowStr);
        sendHtmlEmail(recipientEmail, subject, html);
    }

    public static void sendPasswordResetOtpEmail(String recipientEmail, String fullName, String username, String otpCode) throws Exception {
        String subject = "[PHLINGO ACADEMY] Mã xác thực đặt lại mật khẩu: " + otpCode;
        String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String cleanName = cleanDisplayName(fullName);
        String html = buildPasswordResetHtml(recipientEmail, cleanName, username, otpCode, nowStr);
        sendHtmlEmail(recipientEmail, subject, html);
    }

    private static String cleanDisplayName(String fullName) {
        if (fullName == null) return "Học viên";
        return fullName.replaceAll("\\s*\\(HV\\d+\\)\\s*", "").trim();
    }

    private static byte[] loadTdtuLogoBytes() {
        String[] candidates = new String[]{
            "prototype/assets/tdtu-logo.png",
            "../prototype/assets/tdtu-logo.png",
            "d:/working/Learning/RAD/prototype/assets/tdtu-logo.png"
        };
        for (String path : candidates) {
            try {
                File f = new File(path);
                if (f.exists() && f.isFile()) {
                    return Files.readAllBytes(f.toPath());
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    public static void sendHtmlEmail(String toEmail, String subject, String htmlBody) throws Exception {
        String cleanTo = toEmail == null ? "" : toEmail.trim();
        if (cleanTo.isEmpty() || !cleanTo.contains("@")) {
            throw new IllegalArgumentException("Địa chỉ email người nhận không hợp lệ: " + toEmail);
        }

        byte[] logoBytes = loadTdtuLogoBytes();
        String boundary = "----=_Part_Phlingo_" + System.currentTimeMillis();

        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try (SSLSocket socket = (SSLSocket) factory.createSocket(SMTP_HOST, SMTP_PORT)) {
            socket.setSoTimeout(15000);
            socket.startHandshake();

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), false);

            expectCode(in, "220");

            sendCmd(out, "EHLO phlingo.academy");
            expectCode(in, "250");

            sendCmd(out, "AUTH LOGIN");
            expectCode(in, "334");

            sendCmd(out, Base64.getEncoder().encodeToString(SENDER_EMAIL.getBytes(StandardCharsets.UTF_8)));
            expectCode(in, "334");

            sendCmd(out, Base64.getEncoder().encodeToString(SENDER_APP_PASSWORD.getBytes(StandardCharsets.UTF_8)));
            expectCode(in, "235");

            sendCmd(out, "MAIL FROM:<" + SENDER_EMAIL + ">");
            expectCode(in, "250");

            sendCmd(out, "RCPT TO:<" + cleanTo + ">");
            expectCode(in, "250");

            sendCmd(out, "DATA");
            expectCode(in, "354");

            String encodedFromName = encodeMimeHeaderUtf8(PROJECT_SENDER_NAME);
            String encodedSubject = encodeMimeHeaderUtf8(subject);
            String encodedHtml = Base64.getMimeEncoder(76, new byte[]{'\r', '\n'})
                    .encodeToString(htmlBody.getBytes(StandardCharsets.UTF_8));

            StringBuilder data = new StringBuilder();
            data.append("From: ").append(encodedFromName).append(" <").append(SENDER_EMAIL).append(">\r\n");
            data.append("To: <").append(cleanTo).append(">\r\n");
            data.append("Reply-To: ").append(SENDER_EMAIL).append("\r\n");
            data.append("Subject: ").append(encodedSubject).append("\r\n");
            data.append("MIME-Version: 1.0\r\n");

            if (logoBytes != null && logoBytes.length > 0) {
                String encodedLogo = Base64.getMimeEncoder(76, new byte[]{'\r', '\n'}).encodeToString(logoBytes);
                data.append("Content-Type: multipart/related; boundary=\"").append(boundary).append("\"\r\n");
                data.append("\r\n");

                // Part 1: HTML
                data.append("--").append(boundary).append("\r\n");
                data.append("Content-Type: text/html; charset=UTF-8\r\n");
                data.append("Content-Transfer-Encoding: base64\r\n");
                data.append("\r\n");
                data.append(encodedHtml).append("\r\n");

                // Part 2: Inline TDTU Logo PNG
                data.append("--").append(boundary).append("\r\n");
                data.append("Content-Type: image/png; name=\"tdtu-logo.png\"\r\n");
                data.append("Content-Transfer-Encoding: base64\r\n");
                data.append("Content-ID: <tdtu_logo>\r\n");
                data.append("Content-Disposition: inline; filename=\"tdtu-logo.png\"\r\n");
                data.append("\r\n");
                data.append(encodedLogo).append("\r\n");

                data.append("--").append(boundary).append("--\r\n");
            } else {
                data.append("Content-Type: text/html; charset=UTF-8\r\n");
                data.append("Content-Transfer-Encoding: base64\r\n");
                data.append("\r\n");
                data.append(encodedHtml).append("\r\n");
            }

            data.append(".\r\n");

            out.print(data.toString());
            out.flush();
            expectCode(in, "250");

            sendCmd(out, "QUIT");
        }
    }

    private static void sendCmd(PrintWriter out, String cmd) {
        out.print(cmd + "\r\n");
        out.flush();
    }

    private static void expectCode(BufferedReader in, String expectedPrefix) throws Exception {
        String line;
        StringBuilder fullResp = new StringBuilder();
        while ((line = in.readLine()) != null) {
            fullResp.append(line).append(" ");
            if (line.length() >= 4 && line.charAt(3) == ' ') {
                break;
            }
            if (line.length() == 3) {
                break;
            }
        }
        String resp = fullResp.toString().trim();
        if (!resp.startsWith(expectedPrefix)) {
            throw new RuntimeException("SMTP Error (expected " + expectedPrefix + "): " + resp);
        }
    }

    private static String encodeMimeHeaderUtf8(String text) {
        String b64 = Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
        return "=?UTF-8?B?" + b64 + "?=";
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String buildHeaderHtml() {
        return "<tr>" +
            "<td style=\"background-color:#003366;height:4px;font-size:0;line-height:0;\">&nbsp;</td>" +
          "</tr>" +
          "<tr>" +
            "<td style=\"padding:24px 36px 20px 36px;border-bottom:1px solid #E2E8F0;background-color:#FFFFFF;\">" +
              "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">" +
                "<tr>" +
                  "<td width=\"76\" valign=\"middle\" style=\"padding-right:16px;\">" +
                    "<img src=\"cid:tdtu_logo\" alt=\"TDTU Logo\" width=\"68\" style=\"display:block;width:68px;height:auto;border:0;\" />" +
                  "</td>" +
                  "<td valign=\"middle\" style=\"font-family:" + FONT_STACK + ";\">" +
                    "<div style=\"font-size:11px;font-weight:700;color:#64748B;text-transform:uppercase;letter-spacing:0.06em;margin-bottom:3px;\">" +
                      "TRƯỜNG ĐẠI HỌC TÔN ĐỨC THẮNG · KHOA CÔNG NGHỆ THÔNG TIN" +
                    "</div>" +
                    "<div style=\"font-size:17px;font-weight:700;color:#0F172A;letter-spacing:-0.01em;\">" +
                      "Trung tâm Ngoại ngữ PHLINGO ACADEMY" +
                    "</div>" +
                  "</td>" +
                "</tr>" +
              "</table>" +
            "</td>" +
          "</tr>";
    }

    private static String buildFooterHtml() {
        return "<tr>" +
            "<td style=\"background-color:#F8FAFC;padding:20px 36px;border-top:1px solid #E2E8F0;font-family:" + FONT_STACK + ";font-size:12px;color:#64748B;line-height:1.6;\">" +
              "<div style=\"font-weight:600;color:#334155;margin-bottom:2px;\">" +
                "Trung tâm Ngoại ngữ PHLINGO ACADEMY — Trường Đại học Tôn Đức Thắng" +
              "</div>" +
              "<div>Địa chỉ: Số 19 Nguyễn Hữu Thọ, Phường Tân Phong, Quận 7, TP. Hồ Chí Minh</div>" +
              "<div>Học phần Phân tích và Thiết kế Yêu cầu (502050) · Nhóm thực hiện: Nguyễn Trọng Phúc, Lê Khải Hoàn, Trần Hữu Long</div>" +
            "</td>" +
          "</tr>";
    }

    private static String buildVerificationHtml(String email, String fullName, String username, String studentCode, String otpCode, String nowStr) {
        return "<!DOCTYPE html>" +
            "<html lang=\"vi\"><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\"></head>" +
            "<body style=\"margin:0;padding:0;background-color:#F1F5F9;font-family:" + FONT_STACK + ";color:#1E293B;-webkit-font-smoothing:antialiased;\">" +
            "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:#F1F5F9;padding:36px 16px;\">" +
              "<tr><td align=\"center\">" +
                "<table role=\"presentation\" width=\"580\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:580px;width:100%;background-color:#FFFFFF;border-radius:10px;overflow:hidden;border:1px solid #CBD5E1;box-shadow:0 4px 16px rgba(15,23,42,0.05);\">" +
                  buildHeaderHtml() +
                  "<tr><td style=\"padding:32px 36px;font-family:" + FONT_STACK + ";\">" +
                    "<h1 style=\"margin:0 0 16px 0;font-size:20px;font-weight:700;color:#0F172A;letter-spacing:-0.01em;\">" +
                      "Xác thực địa chỉ email tài khoản Học viên" +
                    "</h1>" +
                    "<p style=\"margin:0 0 12px 0;font-size:14.5px;line-height:1.6;color:#1E293B;\">" +
                      "Xin chào <strong>" + esc(fullName) + "</strong>," +
                    "</p>" +
                    "<p style=\"margin:0 0 24px 0;font-size:14px;line-height:1.65;color:#475569;\">" +
                      "Cảm ơn bạn đã đăng ký tài khoản tại <strong>Trung tâm Ngoại ngữ PHLINGO ACADEMY</strong>. Để hoàn tất bước xác minh địa chỉ email và kích hoạt tài khoản, vui lòng nhập mã xác thực gồm 6 chữ số dưới đây vào màn hình kích hoạt:" +
                    "</p>" +

                    /* Clean, Unified-Font OTP Box */
                    "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:#F8FAFC;border:1px solid #CBD5E1;border-radius:8px;margin:0 0 24px 0;\">" +
                      "<tr><td align=\"center\" style=\"padding:22px 20px;font-family:" + FONT_STACK + ";\">" +
                        "<div style=\"font-size:11px;font-weight:700;text-transform:uppercase;letter-spacing:0.08em;color:#64748B;margin-bottom:8px;\">" +
                          "MÃ XÁC THỰC KÍCH HOẠT (OTP)" +
                        "</div>" +
                        "<div style=\"font-family:" + FONT_STACK + ";font-size:32px;font-weight:700;letter-spacing:10px;color:#003366;line-height:1.2;padding-left:10px;\">" +
                          esc(otpCode) +
                        "</div>" +
                        "<div style=\"margin-top:8px;font-size:12.5px;color:#64748B;\">" +
                          "Mã có hiệu lực trong vòng 10 phút và chỉ sử dụng 01 lần." +
                        "</div>" +
                      "</td></tr>" +
                    "</table>" +

                    /* Clean Account Details Table */
                    "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"border:1px solid #E2E8F0;border-radius:8px;overflow:hidden;margin-bottom:24px;font-family:" + FONT_STACK + ";font-size:13.5px;\">" +
                      "<tr>" +
                        "<td colspan=\"2\" style=\"background-color:#F8FAFC;padding:10px 16px;font-weight:700;color:#0F172A;font-size:12px;text-transform:uppercase;letter-spacing:0.04em;border-bottom:1px solid #E2E8F0;\">" +
                          "Thông tin tài khoản đăng ký" +
                        "</td>" +
                      "</tr>" +
                      "<tr>" +
                        "<td style=\"padding:10px 16px;color:#64748B;width:40%;border-bottom:1px solid #F1F5F9;\">Họ và tên học viên</td>" +
                        "<td style=\"padding:10px 16px;font-weight:600;color:#0F172A;border-bottom:1px solid #F1F5F9;\">" + esc(fullName) + "</td>" +
                      "</tr>" +
                      "<tr>" +
                        "<td style=\"padding:10px 16px;color:#64748B;border-bottom:1px solid #F1F5F9;\">Mã hồ sơ học viên</td>" +
                        "<td style=\"padding:10px 16px;font-weight:600;color:#003366;border-bottom:1px solid #F1F5F9;\">" + esc(studentCode) + "</td>" +
                      "</tr>" +
                      "<tr>" +
                        "<td style=\"padding:10px 16px;color:#64748B;border-bottom:1px solid #F1F5F9;\">Tên đăng nhập</td>" +
                        "<td style=\"padding:10px 16px;font-weight:600;color:#0F172A;border-bottom:1px solid #F1F5F9;\">" + esc(username) + "</td>" +
                      "</tr>" +
                      "<tr>" +
                        "<td style=\"padding:10px 16px;color:#64748B;border-bottom:1px solid #F1F5F9;\">Email xác thực</td>" +
                        "<td style=\"padding:10px 16px;font-weight:600;color:#0F172A;border-bottom:1px solid #F1F5F9;\">" + esc(email) + "</td>" +
                      "</tr>" +
                      "<tr>" +
                        "<td style=\"padding:10px 16px;color:#64748B;\">Thời gian cấp mã</td>" +
                        "<td style=\"padding:10px 16px;color:#334155;\">" + esc(nowStr) + "</td>" +
                      "</tr>" +
                    "</table>" +

                    "<p style=\"margin:0;font-size:13px;color:#64748B;line-height:1.6;\">" +
                      "Nếu bạn không thực hiện yêu cầu đăng ký này, vui lòng bỏ qua email. Vì lý do bảo mật, tuyệt đối không chia sẻ mã xác thực cho bất kỳ ai." +
                    "</p>" +
                  "</td></tr>" +
                  buildFooterHtml() +
                "</table>" +
              "</td></tr>" +
            "</table>" +
            "</body></html>";
    }

    private static String buildPasswordResetHtml(String email, String fullName, String username, String otpCode, String nowStr) {
        return "<!DOCTYPE html>" +
            "<html lang=\"vi\"><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\"></head>" +
            "<body style=\"margin:0;padding:0;background-color:#F1F5F9;font-family:" + FONT_STACK + ";color:#1E293B;-webkit-font-smoothing:antialiased;\">" +
            "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:#F1F5F9;padding:36px 16px;\">" +
              "<tr><td align=\"center\">" +
                "<table role=\"presentation\" width=\"580\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:580px;width:100%;background-color:#FFFFFF;border-radius:10px;overflow:hidden;border:1px solid #CBD5E1;box-shadow:0 4px 16px rgba(15,23,42,0.05);\">" +
                  buildHeaderHtml() +
                  "<tr><td style=\"padding:32px 36px;font-family:" + FONT_STACK + ";\">" +
                    "<h1 style=\"margin:0 0 16px 0;font-size:20px;font-weight:700;color:#0F172A;letter-spacing:-0.01em;\">" +
                      "Yêu cầu đặt lại mật khẩu tài khoản" +
                    "</h1>" +
                    "<p style=\"margin:0 0 12px 0;font-size:14.5px;line-height:1.6;color:#1E293B;\">" +
                      "Xin chào <strong>" + esc(fullName) + "</strong>," +
                    "</p>" +
                    "<p style=\"margin:0 0 24px 0;font-size:14px;line-height:1.65;color:#475569;\">" +
                      "Hệ thống <strong>Trung tâm Ngoại ngữ PHLINGO ACADEMY</strong> vừa nhận được yêu cầu đặt lại mật khẩu cho tài khoản <strong>" + esc(username) + "</strong> (gắn với email <strong>" + esc(email) + "</strong>). Vui lòng nhập mã xác thực gồm 6 chữ số dưới đây để thiết lập mật khẩu mới:" +
                    "</p>" +

                    "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:#F8FAFC;border:1px solid #CBD5E1;border-radius:8px;margin:0 0 24px 0;\">" +
                      "<tr><td align=\"center\" style=\"padding:22px 20px;font-family:" + FONT_STACK + ";\">" +
                        "<div style=\"font-size:11px;font-weight:700;text-transform:uppercase;letter-spacing:0.08em;color:#64748B;margin-bottom:8px;\">" +
                          "MÃ XÁC THỰC ĐẶT LẠI MẬT KHẨU (OTP)" +
                        "</div>" +
                        "<div style=\"font-family:" + FONT_STACK + ";font-size:32px;font-weight:700;letter-spacing:10px;color:#003366;line-height:1.2;padding-left:10px;\">" +
                          esc(otpCode) +
                        "</div>" +
                        "<div style=\"margin-top:8px;font-size:12.5px;color:#64748B;\">" +
                          "Mã có hiệu lực trong vòng 10 phút · Cấp lúc " + esc(nowStr) +
                        "</div>" +
                      "</td></tr>" +
                    "</table>" +

                    "<p style=\"margin:0;font-size:13px;color:#64748B;line-height:1.6;\">" +
                      "Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này. Tài khoản của bạn vẫn được bảo mật an toàn." +
                    "</p>" +
                  "</td></tr>" +
                  buildFooterHtml() +
                "</table>" +
              "</td></tr>" +
            "</table>" +
            "</body></html>";
    }
}
