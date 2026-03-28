package com.example.SE2.configs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DatabaseInitializer {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    public static void createDatabaseIfNotExists(String url, String username, String password) {
        // Extract database name from URL: jdbc:postgresql://host:port/dbname
        String dbName = url.substring(url.lastIndexOf("/") + 1);
        String baseUrl = url.substring(0, url.lastIndexOf("/")) + "/postgres";

        try (Connection conn = DriverManager.getConnection(baseUrl, username, password);
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery(
                    "SELECT 1 FROM pg_database WHERE datname = '" + dbName.replace("'", "''") + "'");

            if (!rs.next()) {
                stmt.execute("CREATE DATABASE \"" + dbName + "\"");
                log.info("Database '{}' created successfully", dbName);
            } else {
                log.info("Database '{}' already exists", dbName);
            }
        } catch (Exception e) {
            log.error("Failed to initialize database: {}", e.getMessage());
        }
    }
}