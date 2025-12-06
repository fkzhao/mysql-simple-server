package cc.fastsoft.db;

import cc.fastsoft.db.schema.Column;
import org.junit.jupiter.api.*;
import org.rocksdb.RocksDBException;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cc.fastsoft.db.schema.Column.ColumnType;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for database persistence and duplicate detection
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DatabasePersistenceTest {

    private static final String TEST_DB_PATH = "rocks_test.db";
    private static final String TEST_DB_NAME = "test_db";
    private static final String TEST_TABLE_NAME = "users";

    @BeforeAll
    void setUp() {
        // Clean up any existing test database
        deleteTestDatabase();
    }

    @AfterAll
    void tearDown() {
        // Clean up test database after all tests
        deleteTestDatabase();
    }

    @Test
    @Order(1)
    @DisplayName("Should create database and table on first run")
    void testFirstRun_CreateDatabaseAndTable() throws RocksDBException {
        try (DatabaseEngine engine = createTestEngine()) {
            // Verify no databases exist initially
            List<String> databases = engine.listDatabases();
            assertFalse(databases.contains(TEST_DB_NAME), "Database should not exist initially");

            // Create database
            assertDoesNotThrow(() -> engine.createDatabase(TEST_DB_NAME),
                    "Should successfully create database");

            // Verify database was created
            assertTrue(engine.databaseExists(TEST_DB_NAME), "Database should exist after creation");

            // Use database
            engine.useDatabase(TEST_DB_NAME);
            assertEquals(TEST_DB_NAME, engine.getCurrentDatabase(), "Current database should be test_db");

            // Create table
            List<Column> columns = createTestColumns();
            assertDoesNotThrow(() -> engine.createTable(TEST_TABLE_NAME, columns, List.of("id")),
                    "Should successfully create table");

            // Verify table was created
            assertTrue(engine.tableExists(TEST_TABLE_NAME), "Table should exist after creation");
        }
    }

    @Test
    @Order(2)
    @DisplayName("Should detect duplicate database in same session")
    void testSameSession_DetectDuplicateDatabase() throws RocksDBException {
        try (DatabaseEngine engine = createTestEngine()) {
            engine.useDatabase(TEST_DB_NAME);

            // Try to create duplicate database
            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> engine.createDatabase(TEST_DB_NAME),
                    "Should throw exception for duplicate database");

            assertTrue(exception.getMessage().contains("already exists"),
                    "Exception message should mention 'already exists'");
        }
    }

    @Test
    @Order(3)
    @DisplayName("Should detect duplicate table in same session")
    void testSameSession_DetectDuplicateTable() throws RocksDBException {
        try (DatabaseEngine engine = createTestEngine()) {
            engine.useDatabase(TEST_DB_NAME);

            // Try to create duplicate table
            List<Column> columns = createTestColumns();
            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> engine.createTable(TEST_TABLE_NAME, columns, List.of("id")),
                    "Should throw exception for duplicate table");

            assertTrue(exception.getMessage().contains("already exists"),
                    "Exception message should mention 'already exists'");
        }
    }

    @Test
    @Order(4)
    @DisplayName("Should insert and query data successfully")
    void testDataOperations_InsertAndQuery() throws RocksDBException {
        try (DatabaseEngine engine = createTestEngine()) {
            engine.useDatabase(TEST_DB_NAME);

            // Insert test data
            Map<String, Object> user1 = createTestUser(1L, "Alice", "alice@example.com");
            Map<String, Object> user2 = createTestUser(2L, "Bob", "bob@example.com");

            assertDoesNotThrow(() -> engine.insert(TEST_TABLE_NAME, user1),
                    "Should insert user1 successfully");
            assertDoesNotThrow(() -> engine.insert(TEST_TABLE_NAME, user2),
                    "Should insert user2 successfully");

            // Query data
            List<Map<String, Object>> users = engine.selectAll(TEST_TABLE_NAME);
            assertNotNull(users, "Query result should not be null");
            assertEquals(2, users.size(), "Should have 2 users");

            // Verify data
            assertTrue(users.stream().anyMatch(u -> "Alice".equals(u.get("name"))),
                    "Should contain Alice");
            assertTrue(users.stream().anyMatch(u -> "Bob".equals(u.get("name"))),
                    "Should contain Bob");
        }
    }

    @Test
    @Order(5)
    @DisplayName("Should load existing database on restart")
    void testRestart_LoadExistingDatabase() throws RocksDBException {
        // Create new engine instance (simulating restart)
        try (DatabaseEngine engine = createTestEngine()) {
            // Verify database is loaded
            assertTrue(engine.databaseExists(TEST_DB_NAME),
                    "Database should be loaded from RocksDB");

            List<String> databases = engine.listDatabases();
            assertTrue(databases.contains(TEST_DB_NAME),
                    "Database list should contain test_db");
        }
    }

    @Test
    @Order(6)
    @DisplayName("Should load existing table on restart")
    void testRestart_LoadExistingTable() throws RocksDBException {
        try (DatabaseEngine engine = createTestEngine()) {
            engine.useDatabase(TEST_DB_NAME);

            // Verify table is loaded
            assertTrue(engine.tableExists(TEST_TABLE_NAME),
                    "Table should be loaded from RocksDB");

            // Verify data persists
            List<Map<String, Object>> users = engine.selectAll(TEST_TABLE_NAME);
            assertEquals(2, users.size(), "Data should persist across restarts");
        }
    }

    @Test
    @Order(7)
    @DisplayName("Should detect duplicate database after restart")
    void testAfterRestart_DetectDuplicateDatabase() throws RocksDBException {
        try (DatabaseEngine engine = createTestEngine()) {
            // Try to create duplicate database after restart
            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> engine.createDatabase(TEST_DB_NAME),
                    "Should throw exception for duplicate database after restart");

            assertTrue(exception.getMessage().contains("already exists"),
                    "Exception message should mention 'already exists'");
        }
    }

    @Test
    @Order(8)
    @DisplayName("Should detect duplicate table after restart")
    void testAfterRestart_DetectDuplicateTable() throws RocksDBException {
        try (DatabaseEngine engine = createTestEngine()) {
            engine.useDatabase(TEST_DB_NAME);

            // Try to create duplicate table after restart
            List<Column> columns = createTestColumns();
            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> engine.createTable(TEST_TABLE_NAME, columns, List.of("id")),
                    "Should throw exception for duplicate table after restart");

            assertTrue(exception.getMessage().contains("already exists"),
                    "Exception message should mention 'already exists'");
        }
    }

    @Test
    @Order(9)
    @DisplayName("Should allow operations on existing data after restart")
    void testAfterRestart_QueryExistingData() throws RocksDBException {
        try (DatabaseEngine engine = createTestEngine()) {
            engine.useDatabase(TEST_DB_NAME);

            // Query existing data
            List<Map<String, Object>> users = engine.selectAll(TEST_TABLE_NAME);
            assertNotNull(users, "Should be able to query existing data");
            assertEquals(2, users.size(), "Should have 2 users from previous tests");

            // Select by primary key
            Map<String, Object> pk = Map.of("id", 1L);
            Map<String, Object> user = engine.selectByPrimaryKey(TEST_TABLE_NAME, pk);
            assertNotNull(user, "Should find user by primary key");
            assertEquals("Alice", user.get("name"), "Should retrieve correct user");
        }
    }

    @Test
    @Order(10)
    @DisplayName("Should handle multiple databases correctly")
    void testMultipleDatabases() throws RocksDBException {
        try (DatabaseEngine engine = createTestEngine()) {
            // Create another database
            String secondDb = "test_db_2";
            assertDoesNotThrow(() -> engine.createDatabase(secondDb),
                    "Should create second database");

            // Verify both databases exist
            List<String> databases = engine.listDatabases();
            assertTrue(databases.contains(TEST_DB_NAME), "First database should exist");
            assertTrue(databases.contains(secondDb), "Second database should exist");

            // Create table with same name in second database
            engine.useDatabase(secondDb);
            List<Column> columns = createTestColumns();
            assertDoesNotThrow(() -> engine.createTable(TEST_TABLE_NAME, columns, List.of("id")),
                    "Should create table with same name in different database");

            // Verify table isolation
            engine.useDatabase(TEST_DB_NAME);
            List<Map<String, Object>> users1 = engine.selectAll(TEST_TABLE_NAME);
            assertEquals(2, users1.size(), "First database should have 2 users");

            engine.useDatabase(secondDb);
            List<Map<String, Object>> users2 = engine.selectAll(TEST_TABLE_NAME);
            assertEquals(0, users2.size(), "Second database table should be empty");

            // Clean up
            engine.dropDatabase(secondDb);
        }
    }

    // Helper methods

    private DatabaseEngine createTestEngine() {
        // Override the default rocks.db path for testing
        System.setProperty("rocksdb.path", TEST_DB_PATH);
        return new DatabaseEngine();
    }

    private List<Column> createTestColumns() {
        return List.of(
                new Column("id", ColumnType.LONG),
                new Column("name", ColumnType.STRING),
                new Column("email", ColumnType.STRING)
        );
    }

    private Map<String, Object> createTestUser(Long id, String name, String email) {
        Map<String, Object> user = new HashMap<>();
        user.put("id", id);
        user.put("name", name);
        user.put("email", email);
        return user;
    }

    private void deleteTestDatabase() {
        File dbDir = new File(TEST_DB_PATH);
        if (dbDir.exists()) {
            deleteDirectory(dbDir);
        }
    }

    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
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

