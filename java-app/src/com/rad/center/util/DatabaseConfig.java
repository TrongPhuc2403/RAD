package com.rad.center.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {
    private static String host = System.getProperty("db.host", "localhost");
    private static String port = System.getProperty("db.port", "3306");
    private static String dbName = System.getProperty("db.name", "language_center_db");
    private static String user = System.getProperty("db.user", "root");
    private static String password = System.getProperty("db.password", "");
    private static boolean mysqlAvailable = false;

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ignored) {
            try {
                Class.forName("com.mysql.jdbc.Driver");
            } catch (ClassNotFoundException ignored2) {
            }
        }
        checkConnection();
    }

    public static boolean checkConnection() {
        String url = "jdbc:mysql://" + host + ":" + port + "/" + dbName
                + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&connectTimeout=1200&socketTimeout=2500";
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            mysqlAvailable = (conn != null && !conn.isClosed());
        } catch (SQLException e) {
            mysqlAvailable = false;
        }
        return mysqlAvailable;
    }

    public static Connection getConnection() throws SQLException {
        String url = "jdbc:mysql://" + host + ":" + port + "/" + dbName
                + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&connectTimeout=1500";
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
            return "MySQL JDBC (" + host + ":" + port + "/" + dbName + ")";
        }
        return "Local Persistent DB (data/language_center_store.db + SQL Sync)";
    }

    public static String getHost() { return host; }
    public static String getPort() { return port; }
    public static String getDbName() { return dbName; }
    public static String getUser() { return user; }
    public static String getPassword() { return password; }
}

