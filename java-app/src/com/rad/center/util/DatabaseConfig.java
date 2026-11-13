package com.rad.center.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {
    private static String host = setting("API_DB_HOST", "db.host", "127.0.0.1");
    private static String port = setting("API_DB_PORT", "db.port", "3306");
    private static String dbName = setting("API_DB_NAME", "db.name", "phlingo_academy");
    private static String user = setting("API_DB_USER", "db.user", "root");
    private static String password = setting("API_DB_PASSWORD", "db.password", "");
    private static boolean mysqlAvailable = false;

    private static String setting(String envKey, String propKey, String fallback) {
        String v = System.getenv(envKey);
        if (v == null || v.trim().isEmpty()) v = System.getProperty(propKey);
        return (v == null || v.trim().isEmpty()) ? fallback : v.trim();
    }

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ignored) {
            try {
                Class.forName("com.mysql.jdbc.Driver");
            } catch (ClassNotFoundException ignored2) {
            }
        }
        if (!checkConnection() && "3306".equals(port)) {
            // Tự động thử cổng 3307 (XAMPP MariaDB khi MySQL80 đang chiếm 3306)
            port = "3307";
            if (!checkConnection()) {
                port = "3306";
            }
        }
    }

    public static boolean checkConnection() {
        String url = "jdbc:mysql://" + host + ":" + port + "/" + dbName
                + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh&connectTimeout=1500&socketTimeout=3000";
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            mysqlAvailable = (conn != null && !conn.isClosed());
        } catch (SQLException e) {
            mysqlAvailable = false;
        }
        return mysqlAvailable;
    }

    public static Connection getConnection() throws SQLException {
        String url = "jdbc:mysql://" + host + ":" + port + "/" + dbName
                + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh&connectTimeout=2000";
        return DriverManager.getConnection(url, user, password);
    }

    public static boolean isMysqlAvailable() {
        return mysqlAvailable;
    }

    public static void configure(String newHost, String newPort, String newDbName, String newUser, String newPass) {
        host = newHost;
        port = newPort;
        dbName = newDbName;
        user = newUser;
        password = newPass;
        checkConnection();
    }

    public static String getConnectionSummary() {
        if (mysqlAvailable) {
            return "MySQL/MariaDB JDBC (" + host + ":" + port + "/" + dbName + ")";
        }
        return "Local Persistent DB (data/language_center_store.db)";
    }

    public static String getHost() { return host; }
    public static String getPort() { return port; }
    public static String getDbName() { return dbName; }
    public static String getUser() { return user; }
    public static String getPassword() { return password; }
}
