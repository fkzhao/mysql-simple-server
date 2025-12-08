package cc.fastsoft.sql;

import cc.fastsoft.db.DatabaseEngine;
import cc.fastsoft.db.schema.Column;

import java.util.List;

/**
 * Example demonstrating SQL parsing and execution
 */
public class SqlParseExample {
    public static void main(String[] args) {
        // Set a custom path for the RocksDB instance
        System.setProperty("rocksdb.path", "example_rocks.db");

        try (DatabaseEngine engine = new DatabaseEngine()) {
            System.out.println("=== MySQL Simple Server - SQL Example ===\n");

            // Create database
            engine.createDatabase("demo_db");
            engine.useDatabase("demo_db");
            System.out.println("✓ Created database: demo_db");

            // Create table
            List<Column> columns = List.of(
                    new Column("id", Column.ColumnType.INT),
                    new Column("name", Column.ColumnType.STRING),
                    new Column("email", Column.ColumnType.STRING),
                    new Column("age", Column.ColumnType.INT)
            );
            engine.createTable("users", columns, List.of("id"));
            System.out.println("✓ Created table: users");
            System.out.println();

            // INSERT operations
            System.out.println("--- INSERT Operations ---");
            String[] insertSqls = {
                "INSERT INTO users (id, name, email, age) VALUES (1, 'Alice Smith', 'alice@example.com', 30)",
                "INSERT INTO users (id, name, email, age) VALUES (2, 'Bob Jones', 'bob@example.com', 25)",
                "INSERT INTO users (id, name, email, age) VALUES (3, 'Charlie Brown', 'charlie@example.com', 35)",
                "INSERT INTO users (id, name, email, age) VALUES (4, 'Diana Prince', 'diana@example.com', 28)"
            };

            for (String sql : insertSqls) {
                SqlParse.parseSql(sql, engine);
                System.out.println("✓ " + sql);
            }
            System.out.println();

            // SELECT ALL
            System.out.println("--- SELECT * FROM users ---");
            String selectAllSql = "SELECT * FROM users";
            SqlData result = SqlParse.parseSql(selectAllSql, engine);
            printResult(result);
            System.out.println();

            // SELECT with specific columns
            System.out.println("--- SELECT id, name FROM users ---");
            String selectColumnsSql = "SELECT id, name FROM users";
            result = SqlParse.parseSql(selectColumnsSql, engine);
            printResult(result);
            System.out.println();

            // SELECT with alias
            System.out.println("--- SELECT id AS user_id, name AS user_name FROM users ---");
            String selectAliasSql = "SELECT id AS user_id, name AS user_name FROM users";
            result = SqlParse.parseSql(selectAliasSql, engine);
            printResult(result);
            System.out.println();

            // SELECT with WHERE
            System.out.println("--- SELECT * FROM users WHERE id = 2 ---");
            String selectWhereSql = "SELECT * FROM users WHERE id = 2";
            result = SqlParse.parseSql(selectWhereSql, engine);
            printResult(result);
            System.out.println();

            // SELECT with LIMIT
            System.out.println("--- SELECT * FROM users LIMIT 2 ---");
            String selectLimitSql = "SELECT * FROM users LIMIT 2";
            result = SqlParse.parseSql(selectLimitSql, engine);
            printResult(result);
            System.out.println();

            // UPDATE operation
            System.out.println("--- UPDATE users SET age = 31 WHERE id = 1 ---");
            String updateSql = "UPDATE users SET age = 31 WHERE id = 1";
            SqlParse.parseSql(updateSql, engine);
            System.out.println("✓ Updated user with id = 1");

            // Verify update
            String verifyUpdateSql = "SELECT * FROM users WHERE id = 1";
            result = SqlParse.parseSql(verifyUpdateSql, engine);
            printResult(result);
            System.out.println();

            // DELETE operation
            System.out.println("--- DELETE FROM users WHERE id = 4 ---");
            String deleteSql = "DELETE FROM users WHERE id = 4";
            SqlParse.parseSql(deleteSql, engine);
            System.out.println("✓ Deleted user with id = 4");

            // Verify delete
            System.out.println("\n--- SELECT * FROM users (after delete) ---");
            result = SqlParse.parseSql(selectAllSql, engine);
            printResult(result);

            System.out.println("\n=== Example completed successfully! ===");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void printResult(SqlData data) {
        if (data.getColumns().isEmpty() && data.getRows().isEmpty()) {
            System.out.println("(No results to display)");
            return;
        }

        List<String> columns = data.getColumns();

        // Print header
        System.out.print("| ");
        for (String col : columns) {
            System.out.printf("%-20s | ", col);
        }
        System.out.println();

        // Print separator
        System.out.print("|-");
        for (int i = 0; i < columns.size(); i++) {
            System.out.print("---------------------|-");
        }
        System.out.println();

        // Print rows
        for (var row : data.getRows()) {
            System.out.print("| ");
            for (String col : columns) {
                Object value = row.get(col);
                System.out.printf("%-20s | ", value != null ? value.toString() : "NULL");
            }
            System.out.println();
        }

        System.out.println("\nTotal rows: " + data.getRows().size());
    }
}

