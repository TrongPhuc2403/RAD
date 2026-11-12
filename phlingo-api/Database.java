package phlingo.api;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

final class Database {
    private final String url, user, password;
    Database() {
        String host = setting("API_DB_HOST", "db.host", "localhost");
        String port = setting("API_DB_PORT", "db.port", "3306");
        String name = setting("API_DB_NAME", "db.name", "phlingo_academy");
        user = setting("API_DB_USER", "db.user", "root");
        password = setting("API_DB_PASSWORD", "db.password", "");
        url = "jdbc:mysql://" + host + ":" + port + "/" + name
            + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh&connectTimeout=3000&socketTimeout=10000";
        try { Class.forName("com.mysql.cj.jdbc.Driver"); }
        catch (ClassNotFoundException ex) { throw new IllegalStateException("Không tìm thấy MySQL Connector/J trong lib.", ex); }
    }
    private static String setting(String env, String prop, String fallback) {
        String value = System.getenv(env); if (value == null || value.trim().isEmpty()) value = System.getProperty(prop);
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
    Connection open() throws SQLException { return DriverManager.getConnection(url, user, password); }
    void ping() throws SQLException { try (Connection c = open(); java.sql.Statement s = c.createStatement()) { s.execute("SELECT 1"); } }
}
