package cc.fastsoft.sql;

import cc.fastsoft.db.DatabaseEngine;
import cc.fastsoft.db.schema.Column;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SqlParseTest {
    private DatabaseEngine engine;
    private static final String TEST_DB = "test_db";
    private static final String TEST_TABLE = "users";

    @BeforeEach
    public void setUp() throws Exception {
        // Set up a temporary RocksDB for testing
        System.setProperty("rocksdb.path", "test_rocks.db");
        engine = new DatabaseEngine();

        // Create database and table
        engine.createDatabase(TEST_DB);
        engine.useDatabase(TEST_DB);

        List<Column> columns = List.of(
                new Column("id", Column.ColumnType.INT),
                new Column("name", Column.ColumnType.STRING),
                new Column("age", Column.ColumnType.INT)
        );
        List<String> pkColumns = List.of("id");

        engine.createTable(TEST_TABLE, columns, pkColumns);
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (engine != null) {
            engine.close();
        }
        // Clean up test database directory
        System.clearProperty("rocksdb.path");
        deleteDirectory(new java.io.File("test_rocks.db"));
    }

    private void deleteDirectory(java.io.File directory) {
        if (directory.exists()) {
            java.io.File[] files = directory.listFiles();
            if (files != null) {
                for (java.io.File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }

    @Test
    public void testInsertStatement() throws Exception {
        String sql = "INSERT INTO users (id, name, age) VALUES (1, 'Alice', 30)";
        SqlData result = SqlParse.parseSql(sql, engine);

        assertNotNull(result);
        assertTrue(result.getColumns().isEmpty());
        assertTrue(result.getRows().isEmpty());

        // Verify the data was inserted
        Map<String, Object> pk = new HashMap<>();
        pk.put("id", 1);
        Map<String, Object> row = engine.selectByPrimaryKey(TEST_TABLE, pk);
        assertNotNull(row);
        assertEquals("Alice", row.get("name"));
        assertEquals(30, row.get("age"));
    }

    @Test
    public void testSelectAllStatement() throws Exception {
        // Insert test data
        Map<String, Object> row1 = new HashMap<>();
        row1.put("id", 1);
        row1.put("name", "Alice");
        row1.put("age", 30);
        engine.insert(TEST_TABLE, row1);

        Map<String, Object> row2 = new HashMap<>();
        row2.put("id", 2);
        row2.put("name", "Bob");
        row2.put("age", 25);
        engine.insert(TEST_TABLE, row2);

        // Test SELECT *
        String sql = "SELECT * FROM users";
        SqlData result = SqlParse.parseSql(sql, engine);

        assertNotNull(result);
        assertEquals(3, result.getColumns().size());
        assertEquals(2, result.getRows().size());
    }

    @Test
    public void testSelectWithColumnsStatement() throws Exception {
        // Insert test data
        Map<String, Object> row1 = new HashMap<>();
        row1.put("id", 1);
        row1.put("name", "Alice");
        row1.put("age", 30);
        engine.insert(TEST_TABLE, row1);

        // Test SELECT specific columns
        String sql = "SELECT id, name FROM users";
        SqlData result = SqlParse.parseSql(sql, engine);

        assertNotNull(result);
        assertEquals(2, result.getColumns().size());
        assertTrue(result.getColumns().contains("id"));
        assertTrue(result.getColumns().contains("name"));
        assertEquals(1, result.getRows().size());
    }

    @Test
    public void testSelectWithWhereStatement() throws Exception {
        // Insert test data
        Map<String, Object> row1 = new HashMap<>();
        row1.put("id", 1);
        row1.put("name", "Alice");
        row1.put("age", 30);
        engine.insert(TEST_TABLE, row1);

        Map<String, Object> row2 = new HashMap<>();
        row2.put("id", 2);
        row2.put("name", "Bob");
        row2.put("age", 25);
        engine.insert(TEST_TABLE, row2);

        // Test SELECT with WHERE
        String sql = "SELECT * FROM users WHERE id = 1";
        SqlData result = SqlParse.parseSql(sql, engine);

        assertNotNull(result);
        assertEquals(1, result.getRows().size());
        assertEquals(1, result.getRows().get(0).get("id"));
        assertEquals("Alice", result.getRows().get(0).get("name"));
    }

    @Test
    public void testSelectWithLimitStatement() throws Exception {
        // Insert test data
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", i);
            row.put("name", "User" + i);
            row.put("age", 20 + i);
            engine.insert(TEST_TABLE, row);
        }

        // Test SELECT with LIMIT
        String sql = "SELECT * FROM users LIMIT 3";
        SqlData result = SqlParse.parseSql(sql, engine);

        assertNotNull(result);
        assertEquals(3, result.getRows().size());
    }

    @Test
    public void testUpdateStatement() throws Exception {
        // Insert test data
        Map<String, Object> row1 = new HashMap<>();
        row1.put("id", 1);
        row1.put("name", "Alice");
        row1.put("age", 30);
        engine.insert(TEST_TABLE, row1);

        // Test UPDATE
        String sql = "UPDATE users SET age = 31 WHERE id = 1";
        SqlData result = SqlParse.parseSql(sql, engine);

        assertNotNull(result);
        assertTrue(result.getColumns().isEmpty());
        assertTrue(result.getRows().isEmpty());

        // Verify the data was updated
        Map<String, Object> pk = new HashMap<>();
        pk.put("id", 1);
        Map<String, Object> row = engine.selectByPrimaryKey(TEST_TABLE, pk);
        assertNotNull(row);
        assertEquals(31, row.get("age"));
    }

    @Test
    public void testDeleteStatement() throws Exception {
        // Insert test data
        Map<String, Object> row1 = new HashMap<>();
        row1.put("id", 1);
        row1.put("name", "Alice");
        row1.put("age", 30);
        engine.insert(TEST_TABLE, row1);

        Map<String, Object> row2 = new HashMap<>();
        row2.put("id", 2);
        row2.put("name", "Bob");
        row2.put("age", 25);
        engine.insert(TEST_TABLE, row2);

        // Test DELETE
        String sql = "DELETE FROM users WHERE id = 1";
        SqlData result = SqlParse.parseSql(sql, engine);

        assertNotNull(result);
        assertTrue(result.getColumns().isEmpty());
        assertTrue(result.getRows().isEmpty());

        // Verify the data was deleted
        List<Map<String, Object>> allRows = engine.selectAll(TEST_TABLE);
        assertEquals(1, allRows.size());
        assertEquals(2, allRows.get(0).get("id"));
    }
}

