package com.example.model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import com.example.util.Constants;

public class DatabaseConnector {
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(Constants.DB_URL, Constants.DB_USER, Constants.DB_PASSWORD);
    }
}
