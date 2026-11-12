package phlingo.api;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Minimal SMTP client using only JDK sockets; configure API_SMTP_* to enable email workflows. */
final class Mailer {
    private final String host = env("API_SMTP_HOST", "");
    private final int port = integer("API_SMTP_PORT", 587);
    private final String username = env("API_SMTP_USER", "");
    private final String password = env("API_SMTP_PASSWORD", "");
    private final String from = env("API_SMTP_FROM", username);
    private final boolean startTls = bool("API_SMTP_STARTTLS", port != 465);
    private final boolean implicitTls = bool("API_SMTP_IMPLICIT_TLS", port == 465);

    boolean configured() { return !host.trim().isEmpty() && !from.trim().isEmpty(); }
    void send(String recipient, String subject, String body) throws IOException {
        if (!configured()) throw new IOException("Email chưa được cấu hình; cần API_SMTP_HOST và API_SMTP_FROM.");
        if (recipient == null || recipient.indexOf('\r') >= 0 || recipient.indexOf('\n') >= 0) throw new IOException("Địa chỉ nhận email không hợp lệ.");
        Socket socket = new Socket(); socket.connect(new InetSocketAddress(host, port), 7000); socket.setSoTimeout(10000);
        if (implicitTls) socket = tls(socket, host);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII));
        try {
            expect(readReply(in), 220); command(out, in, "EHLO phlingo.local", 250);
            if (startTls && !implicitTls) {
                command(out, in, "STARTTLS", 220); socket = tls(socket, host);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
                out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII));
                command(out, in, "EHLO phlingo.local", 250);
            }
            if (!implicitTls && !startTls) throw new IOException("SMTP email containing account links requires TLS.");
            if (!username.isEmpty()) {
                command(out, in, "AUTH LOGIN", 334);
                command(out, in, Base64.getEncoder().encodeToString(username.getBytes(StandardCharsets.UTF_8)), 334);
                command(out, in, Base64.getEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8)), 235);
            }
            command(out, in, "MAIL FROM:<" + safeAddress(from) + ">", 250);
            command(out, in, "RCPT TO:<" + safeAddress(recipient) + ">", 250, 251);
            command(out, in, "DATA", 354);
            String encodedBody = Base64.getMimeEncoder(76, "\r\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(body.getBytes(StandardCharsets.UTF_8));
            String encodedSubject = Base64.getEncoder().encodeToString(subject.getBytes(StandardCharsets.UTF_8));
            out.write("From: " + safeAddress(from) + "\r\nTo: " + safeAddress(recipient) + "\r\nSubject: =?UTF-8?B?" + encodedSubject + "?=\r\n");
            out.write("MIME-Version: 1.0\r\nContent-Type: text/plain; charset=UTF-8\r\nContent-Transfer-Encoding: base64\r\n\r\n");
            out.write(encodedBody); out.write("\r\n.\r\n"); out.flush(); expect(readReply(in), 250);
            command(out, in, "QUIT", 221);
        } finally { try { socket.close(); } catch (IOException ignored) { } }
    }
    private static Socket tls(Socket plain, String host) throws IOException {
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket ssl = (SSLSocket) factory.createSocket(plain, host, plain.getPort(), true);
        java.util.Set<String> supported = new java.util.HashSet<String>(java.util.Arrays.asList(ssl.getSupportedProtocols()));
        java.util.List<String> enabled = new java.util.ArrayList<String>();
        if (supported.contains("TLSv1.3")) enabled.add("TLSv1.3");
        if (supported.contains("TLSv1.2")) enabled.add("TLSv1.2");
        if (enabled.isEmpty()) throw new IOException("JRE không hỗ trợ TLS 1.2 trở lên.");
        ssl.setEnabledProtocols(enabled.toArray(new String[enabled.size()]));
        javax.net.ssl.SSLParameters parameters = ssl.getSSLParameters(); parameters.setEndpointIdentificationAlgorithm("HTTPS"); ssl.setSSLParameters(parameters);
        ssl.startHandshake(); return ssl;
    }
    private static String safeAddress(String address) throws IOException {
        if (address == null || address.trim().isEmpty() || address.indexOf('\r') >= 0 || address.indexOf('\n') >= 0 || address.indexOf('<') >= 0 || address.indexOf('>') >= 0)
            throw new IOException("Địa chỉ email SMTP không hợp lệ.");
        return address.trim();
    }
    private static void command(BufferedWriter out, BufferedReader in, String text, int expected, int... also) throws IOException {
        out.write(text); out.write("\r\n"); out.flush(); expect(readReply(in), expected, also);
    }
    private static String readReply(BufferedReader in) throws IOException {
        StringBuilder reply = new StringBuilder(); String line;
        do { line = in.readLine(); if (line == null) throw new IOException("SMTP server closed the connection."); reply.append(line).append('\n'); }
        while (line.length() >= 4 && line.charAt(3) == '-');
        return reply.toString();
    }
    private static void expect(String reply, int expected, int... also) throws IOException {
        int code; try { code = Integer.parseInt(reply.substring(0, 3)); } catch (Exception ex) { throw new IOException("SMTP returned an invalid response."); }
        if (code == expected) return; for (int allowed : also) if (code == allowed) return;
        throw new IOException("SMTP rejected a command with status " + code + ".");
    }
    private static String env(String name, String fallback) { String v = System.getenv(name); return v == null || v.trim().isEmpty() ? fallback : v.trim(); }
    private static int integer(String name, int fallback) { try { return Integer.parseInt(env(name, String.valueOf(fallback))); } catch (NumberFormatException ex) { return fallback; } }
    private static boolean bool(String name, boolean fallback) { String v = env(name, String.valueOf(fallback)); return "true".equalsIgnoreCase(v) || "1".equals(v); }
}
