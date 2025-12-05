package cc.fastsoft.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class Example {
    public static void main(String[] args) {
        // MySQL connection parameters
        String url = "jdbc:mysql://127.0.0.1:2883/?useSSL=false&allowPublicKeyRetrieval=true";
        String user = "root";
        String password = "123456";

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            // Load MySQL JDBC driver (optional for newer versions)
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Establish connection
            System.out.println("Connecting to MySQL server at " + url + "...");
            conn = DriverManager.getConnection(url, user, password);
            System.out.println("Connected successfully!");

            // Create statement
            stmt = conn.createStatement();

            // Execute SHOW DATABASES
            System.out.println("\nExecuting: SHOW DATABASES");
            rs = stmt.executeQuery("SHOW DATABASES");

            // Print results
            System.out.println("\nDatabases:");
            System.out.println("==========");
            while (rs.next()) {
                String database = rs.getString(1);
                System.out.println(database);
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Close resources
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) {
                    conn.close();
                    System.out.println("\nConnection closed.");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
