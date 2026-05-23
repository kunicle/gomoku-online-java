package com.omok.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    // Database connection settings
    // serverTimezone=Asia/Seoul: Synchronizes timezone between MySQL and Java
    private static final String URL      = "jdbc:mysql://localhost:3306/omok_db?serverTimezone=Asia/Seoul";
    private static final String USER     = "root";
    private static final String PASSWORD = "test1234";

    // Singleton pattern: Only one instance exists throughout the program
    private static DBConnection instance;

    // Holds the actual connection object to the database
    // SQL queries are executed through this connection
    private Connection connection;

    // Private constructor: Prevents external instantiation via new DBConnection()
    // Core of singleton pattern - forces access only through getInstance()
    private DBConnection() throws SQLException {
        this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // The only way to obtain a DBConnection from outside
    // Creates a new instance if none exists or if the connection is closed
    // Returns the existing instance otherwise - maintains only one DB connection
    public static DBConnection getInstance() throws SQLException {
        if (instance == null || instance.connection.isClosed()){
            instance = new DBConnection();
        }
        return instance;
    }

    // Returns the actual Connection object
    // Used by UserDAO, GameDAO, etc. to execute SQL
    // Usage: Connection conn = DBConnection.getInstance().getConnection();
    public Connection getConnection() {
        return connection;
    }
}
