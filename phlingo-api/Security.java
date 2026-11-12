package phlingo.api;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class Security {
    private static final SecureRandom RANDOM = new SecureRandom();
    private Security() { }

    static String hashPassword(String password) {
        byte[] salt = new byte[16]; RANDOM.nextBytes(salt);
        int iterations = 160000;
        byte[] derived = derive(password.toCharArray(), salt, iterations);
        return "pbkdf2_sha256$" + iterations + "$" + Base64.getEncoder().encodeToString(salt) + "$" + Base64.getEncoder().encodeToString(derived);
    }
    static boolean matchesPassword(String password, String encoded) {
        if (password == null || encoded == null) return false;
        try {
            if (encoded.startsWith("pbkdf2_sha256$")) {
                String[] p = encoded.split("\\$", -1); if (p.length != 4) return false;
                int iterations = Integer.parseInt(p[1]); if (iterations < 10000 || iterations > 1000000) return false;
                byte[] salt = Base64.getDecoder().decode(p[2]); byte[] expected = Base64.getDecoder().decode(p[3]);
                return MessageDigest.isEqual(expected, derive(password.toCharArray(), salt, iterations));
            }
            // Compatible with older demo records that used one-round SHA-256.
            if (encoded.matches("(?i)[0-9a-f]{64}")) return MessageDigest.isEqual(encoded.toLowerCase().getBytes(StandardCharsets.US_ASCII), sha256Hex(password).getBytes(StandardCharsets.US_ASCII));
        } catch (Exception ignored) { }
        return false;
    }
    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch (Exception ex) { throw new IllegalStateException("PBKDF2 không khả dụng trong JRE hiện tại.", ex); }
        finally { spec.clearPassword(); java.util.Arrays.fill(password, '\0'); }
    }
    static String randomToken() { byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    static String sha256Hex(String text) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(64); for (byte b : bytes) out.append(String.format("%02x", b & 0xff)); return out.toString();
        } catch (Exception ex) { throw new IllegalStateException("SHA-256 is unavailable.", ex); }
    }

    static final class Session {
        final long userId; final long expiresAt;
        Session(long userId, long expiresAt) { this.userId = userId; this.expiresAt = expiresAt; }
    }
    static final class Sessions {
        private final Map<String, Session> active = new ConcurrentHashMap<String, Session>();
        String create(long userId) {
            String token = randomToken(); active.put(sha256Hex(token), new Session(userId, System.currentTimeMillis() + 12L * 60L * 60L * 1000L)); return token;
        }
        Session find(String token) {
            if (token == null || token.isEmpty()) return null;
            String key = sha256Hex(token); Session s = active.get(key);
            if (s != null && s.expiresAt <= System.currentTimeMillis()) { active.remove(key); return null; }
            return s;
        }
        void revoke(String token) { if (token != null) active.remove(sha256Hex(token)); }
        void revokeUser(long userId) { for (Map.Entry<String, Session> entry : active.entrySet()) if (entry.getValue().userId == userId) active.remove(entry.getKey(), entry.getValue()); }
    }

    static final class Roles {
        static final String ADMIN = "ADMIN", TRAINING = "TRAINING_STAFF", CASHIER = "CASHIER", TEACHER = "TEACHER", STUDENT = "STUDENT";
        private Roles() { }
        static String normalize(String raw) {
            if (raw == null) return "";
            String v = raw.trim().toUpperCase(java.util.Locale.ROOT).replace('-', '_').replace(' ', '_');
            if (v.equals("QT") || v.equals("ADMINISTRATOR")) return ADMIN;
            if (v.equals("DT") || v.equals("TRAINING")) return TRAINING;
            if (v.equals("TN") || v.equals("CASHIER_STAFF")) return CASHIER;
            if (v.equals("GV") || v.equals("INSTRUCTOR")) return TEACHER;
            if (v.equals("HV") || v.equals("LEARNER")) return STUDENT;
            return v;
        }
        static boolean valid(String role) { String r = normalize(role); return ADMIN.equals(r) || TRAINING.equals(r) || CASHIER.equals(r) || TEACHER.equals(r) || STUDENT.equals(r); }
    }
}
