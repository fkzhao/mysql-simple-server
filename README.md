# MySQL Simple Server

A lightweight, high-performance MySQL protocol server with embedded database engine. Built with Netty for network handling and RocksDB for data persistence. Perfect for testing, development, and integration scenarios.

## ✨ Features

### Network & Protocol
- ✅ **Full MySQL Protocol Implementation** - Complete handshake, authentication, and command processing
- ✅ **Multiple Authentication Formats** - Supports all mysql_native_password wire formats
  - CLIENT_PLUGIN_AUTH_LENENC_CLIENT_DATA
  - CLIENT_SECURE_CONNECTION (new protocol with 1-byte length)
  - Legacy protocol (null-terminated)
- ✅ **Real-time Connection Monitoring** - Thread-safe tracking of active client connections
- ✅ **Client Compatibility** - Works with DBeaver, MySQL CLI, JDBC drivers, and other MySQL clients
- ✅ **High Performance** - Built on Netty's asynchronous I/O architecture

### Database Engine
- ✅ **Embedded Database** - RocksDB-backed storage engine with ACID properties
- ✅ **Multi-Database Support** - Create, use, and manage multiple databases
- ✅ **Table Management** - Create tables with schema definitions and primary keys
- ✅ **Full CRUD Operations** - INSERT, SELECT, UPDATE, DELETE with SQL syntax
- ✅ **SQL Parser** - JSQLParser-based SQL statement parsing and execution
- ✅ **WHERE Clause Support** - Filter data with equality conditions
- ✅ **Column Aliasing** - Support for AS clause in SELECT statements
- ✅ **LIMIT Support** - Restrict result set size
- ✅ **Data Persistence** - All data persists across server restarts

### Development & Debugging
- ✅ **Comprehensive Logging** - Detailed debug logging with Logback framework
- ✅ **Packet-level Debugging** - Log all MySQL protocol packets (in/out)
- ✅ **Unit Tests** - Comprehensive test coverage for SQL operations
- ✅ **Example Code** - Multiple working examples included

## 📋 Supported SQL Commands

### Data Manipulation Language (DML)

#### SELECT
```sql
-- Select all columns
SELECT * FROM users

-- Select specific columns
SELECT id, name FROM users

-- Column aliasing
SELECT id AS user_id, name AS user_name FROM users

-- WHERE clause (equality)
SELECT * FROM users WHERE id = 1

-- LIMIT clause
SELECT * FROM users LIMIT 10
```

#### INSERT
```sql
-- Insert with explicit columns
INSERT INTO users (id, name, age) VALUES (1, 'Alice', 30)

-- Insert all columns
INSERT INTO users VALUES (2, 'Bob', 25)
```

#### UPDATE
```sql
-- Update with WHERE clause
UPDATE users SET age = 31 WHERE id = 1

-- Update multiple columns
UPDATE users SET name = 'John', age = 35 WHERE id = 2

-- Update all rows (no WHERE)
UPDATE users SET active = true
```

#### DELETE
```sql
-- Delete with WHERE clause
DELETE FROM users WHERE id = 1

-- Delete all rows
DELETE FROM users
```

### Data Definition Language (DDL)

#### Database Operations
```sql
-- Create database
CREATE DATABASE mydb

-- Use database
USE mydb

-- Show databases
SHOW DATABASES

-- Drop database
DROP DATABASE mydb
```

#### Table Operations
```sql
-- Create table (via DatabaseEngine API)
engine.createTable("users", columns, primaryKeyColumns)

-- Show tables (via DatabaseEngine API)
engine.getMetadataManager().listTables()
```

### System Commands
```sql
-- System variable queries
SELECT @@version
SELECT @@version_comment
SELECT DATABASE()

-- Show commands
SHOW ENGINES
SHOW CHARSET
SHOW COLLATION
SHOW PLUGINS
SHOW VARIABLES
SHOW VARIABLES LIKE 'character%'

-- SET commands
SET autocommit = 1
SET NAMES utf8mb4

-- Connection management
COM_PING          -- Heartbeat test
COM_QUIT          -- Close connection
COM_INIT_DB       -- Switch database
```

## 🛠 Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 17 | Programming language |
| Netty | 4.2.7.Final | High-performance asynchronous network framework |
| RocksDB | 8.10.0 | Embedded key-value storage engine |
| JSQLParser | 5.3 | SQL parsing library |
| Logback | 1.5.21 | Logging framework |
| SLF4J | 2.0.9 | Logging facade |
| MySQL Connector/J | 8.4.0 | JDBC driver (for examples) |
| JUnit Jupiter | 5.10.1 | Testing framework |
| Maven | 3.6+ | Build and dependency management |

## 🚀 Quick Start

### Prerequisites

- JDK 17 or higher
- Maven 3.6+
- (Optional) MySQL CLI client for testing

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/mysql-simple-server.git
   cd mysql-simple-server
   ```

2. **Build the project**
   ```bash
   mvn clean compile
   ```

3. **Run the server**
   ```bash
   mvn exec:java -Dexec.mainClass="cc.fastsoft.MysqlServer"
   ```

The server will start on port **2883**:
```
2025-12-08 10:00:00.123 [main] INFO cc.fastsoft.MysqlServer - MySQL Mock Server started on port 2883
```

### Connecting to the Server

#### Option 1: MySQL CLI

```bash
mysql -h127.0.0.1 -uroot -P2883 -p123456 --ssl-mode=DISABLED
```

Example session:
```sql
mysql> SHOW DATABASES;
+--------------------+
| Database           |
+--------------------+
| information_schema |
| mysql              |
| performance_schema |
| sys                |
| test_db            |
| my_database        |
+--------------------+
6 rows in set (0.01 sec)

mysql> USE test_db;
Database changed

mysql> SELECT * FROM users;
+----+-------+-----+
| id | name  | age |
+----+-------+-----+
|  1 | Alice |  30 |
|  2 | Bob   |  25 |
+----+-------+-----+
2 rows in set (0.02 sec)
```

#### Option 2: DBeaver

1. Create a new MySQL connection
2. Configure connection parameters:
   - **Server Host:** `127.0.0.1`
   - **Port:** `2883`
   - **Username:** `root`
   - **Password:** `123456`
3. In **Driver properties** tab, add:
   - Key: `useSSL`, Value: `false`
   - Key: `allowPublicKeyRetrieval`, Value: `true`
4. Test connection and save

#### Option 3: JDBC (Java)

```java
String url = "jdbc:mysql://127.0.0.1:2883/?useSSL=false&allowPublicKeyRetrieval=true";
String user = "root";
String password = "123456";

try (Connection conn = DriverManager.getConnection(url, user, password);
     Statement stmt = conn.createStatement()) {
    
    // Execute query
    ResultSet rs = stmt.executeQuery("SELECT * FROM users");
    
    // Process results
    while (rs.next()) {
        System.out.println("ID: " + rs.getInt("id") + 
                         ", Name: " + rs.getString("name"));
    }
}
```

See `src/main/java/cc/fastsoft/example/Example.java` for a complete example.

### Default Configuration

| Parameter | Value | Description |
|-----------|-------|-------------|
| Host | `127.0.0.1` | Server bind address |
| Port | `2883` | Server port |
| Username | `root` | Default username |
| Password | `123456` | Default password |
| SSL | Disabled | SSL/TLS encryption |
| Database Path | `rocks.db` | RocksDB data directory |

## 📁 Project Structure

```
mysql-simple-server/
├── src/
│   ├── main/
│   │   ├── java/cc/fastsoft/
│   │   │   ├── MysqlServer.java                  # Main server entry point
│   │   │   ├── db/                               # Database Engine
│   │   │   │   ├── DatabaseEngine.java           # Main database coordinator
│   │   │   │   ├── core/                         # Core database modules
│   │   │   │   │   ├── DatabaseManager.java      # Database-level operations
│   │   │   │   │   ├── MetadataManager.java      # Schema metadata management
│   │   │   │   │   ├── StorageManager.java       # Data storage operations
│   │   │   │   │   ├── KeyEncoder.java           # Key encoding for RocksDB
│   │   │   │   │   └── RowCodec.java             # Row serialization/deserialization
│   │   │   │   └── schema/                       # Schema definitions
│   │   │   │       ├── TableSchema.java          # Table schema model
│   │   │   │       └── Column.java               # Column definition
│   │   │   ├── sql/                              # SQL Processing
│   │   │   │   ├── SqlParse.java                 # SQL parser and executor
│   │   │   │   └── SqlData.java                  # Query result data structure
│   │   │   ├── jdbc/                             # MySQL Protocol Implementation
│   │   │   │   ├── ServerHandler.java            # Connection lifecycle handler
│   │   │   │   ├── ConnectContext.java           # Per-connection state
│   │   │   │   ├── hander/                       # Protocol phase handlers
│   │   │   │   │   ├── HandshakeHandler.java     # Handshake phase
│   │   │   │   │   ├── AuthHandler.java          # Authentication phase
│   │   │   │   │   ├── CommandHandler.java       # Command routing
│   │   │   │   │   └── QueryHandler.java         # Query execution
│   │   │   │   └── protocol/                     # Protocol utilities
│   │   │   │       ├── PacketHelper.java         # Packet construction utilities
│   │   │   │       ├── Constants.java            # MySQL protocol constants
│   │   │   │       ├── packet/                   # Packet definitions
│   │   │   │       │   ├── HandshakePacket.java
│   │   │   │       │   ├── AuthPacket.java
│   │   │   │       │   ├── OkPacket.java
│   │   │   │       │   └── ErrPacket.java
│   │   │   │       └── codec/                    # Packet codecs
│   │   │   │           ├── PacketDecoder.java    # Decode MySQL packets
│   │   │   │           └── PacketEncoder.java    # Encode MySQL packets
│   │   │   ├── storage/                          # Storage backends
│   │   │   │   └── rocksdb/
│   │   │   │       └── RocksDbHandle.java        # RocksDB wrapper
│   │   │   ├── utils/                            # Utilities
│   │   │   │   ├── IOUtils.java
│   │   │   │   ├── OperatingSystem.java
│   │   │   │   ├── Preconditions.java
│   │   │   │   └── StringUtils.java
│   │   │   └── example/                          # Examples
│   │   │       └── Example.java                  # JDBC connection example
│   │   └── resources/
│   │       └── logback.xml                       # Logging configuration
│   └── test/
│       └── java/cc/fastsoft/
│           ├── db/
│           │   └── DatabasePersistenceTest.java  # Database engine tests
│           └── sql/
│               └── SqlParseTest.java             # SQL parser tests
├── rocks.db/                                      # RocksDB data directory (runtime)
├── logs/                                          # Log files (runtime)
├── pom.xml                                        # Maven configuration
├── README.md                                      # This file
└── SQL_IMPLEMENTATION.md                          # SQL features documentation
```

## 🏗 Architecture

### Component Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    MySQL Client (DBeaver, CLI, JDBC)        │
└──────────────────────┬──────────────────────────────────────┘
                       │ MySQL Protocol (TCP/IP)
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                    Netty Network Layer                       │
│  ┌────────────────┬────────────────┬─────────────────────┐  │
│  │ PacketDecoder  │ ServerHandler  │  PacketEncoder      │  │
│  └────────────────┴────────────────┴─────────────────────┘  │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                 Protocol Handlers                            │
│  ┌──────────────┬──────────────┬──────────────┬──────────┐  │
│  │ Handshake    │ Auth         │ Command      │ Query    │  │
│  │ Handler      │ Handler      │ Handler      │ Handler  │  │
│  └──────────────┴──────────────┴──────────────┴──────────┘  │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                    SQL Processing Layer                      │
│  ┌─────────────────────────────────────────────────────┐    │
│  │              SqlParse (JSQLParser)                  │    │
│  │  • SELECT  • INSERT  • UPDATE  • DELETE            │    │
│  └─────────────────────────────────────────────────────┘    │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                  Database Engine Layer                       │
│  ┌──────────────┬─────────────────┬────────────────────┐    │
│  │ Database     │ Metadata        │ Storage            │    │
│  │ Manager      │ Manager         │ Manager            │    │
│  └──────────────┴─────────────────┴────────────────────┘    │
│  ┌──────────────┬─────────────────────────────────────┐     │
│  │ KeyEncoder   │ RowCodec                             │     │
│  └──────────────┴─────────────────────────────────────┘     │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                    RocksDB Storage                           │
│  • Key-Value Store  • ACID Properties  • Persistence        │
└─────────────────────────────────────────────────────────────┘
```

### Key Components

| Component | Description |
|-----------|-------------|
| **MysqlServer** | Main server class, initializes Netty pipeline |
| **ServerHandler** | Manages connection lifecycle and state transitions |
| **ConnectContext** | Per-connection state (scramble, capabilities, connection ID) |
| **HandshakeHandler** | Sends initial handshake packet to client |
| **AuthHandler** | Validates mysql_native_password authentication |
| **CommandHandler** | Routes COM_* commands to appropriate handlers |
| **QueryHandler** | Executes SQL queries and generates result sets |
| **SqlParse** | Parses and executes SQL statements using JSQLParser |
| **DatabaseEngine** | Coordinates all database operations |
| **DatabaseManager** | Manages database-level operations (CREATE/DROP/USE) |
| **MetadataManager** | Manages table schemas and metadata |
| **StorageManager** | Handles data CRUD operations via RocksDB |

## 📊 Connection Monitoring

### Active Connection Tracking

The server automatically tracks active client connections using thread-safe counters.

```java
// Get current active connection count
int activeConnections = ServerHandler.getActiveConnectionCount();
logger.info("Active connections: {}", activeConnections);
```

**Features:**
- Automatic increment on connection establishment
- Automatic decrement on connection close
- Thread-safe implementation with AtomicInteger
- Real-time logging of connection events

**Log Output:**
```
2025-12-08 10:15:30.123 [netty-thread-1] INFO ServerHandler - Client connected: /127.0.0.1:54321, active connections: 1
2025-12-08 10:15:45.456 [netty-thread-2] INFO ServerHandler - Client disconnected: /127.0.0.1:54321, active connections: 0
```

## 🧪 Testing

### Run All Tests

```bash
mvn test
```

### Run Specific Test

```bash
# Database engine tests
mvn test -Dtest=DatabasePersistenceTest

# SQL parser tests
mvn test -Dtest=SqlParseTest
```

### Test Coverage

| Test Suite | Coverage |
|------------|----------|
| **DatabasePersistenceTest** | Database/table creation, CRUD operations, persistence |
| **SqlParseTest** | SELECT, INSERT, UPDATE, DELETE with various clauses |

### Example Tests

```java
// Test SELECT with WHERE clause
@Test
public void testSelectWithWhereStatement() throws Exception {
    String sql = "SELECT * FROM users WHERE id = 1";
    SqlData result = SqlParse.parseSql(sql, engine);
    
    assertEquals(1, result.getRows().size());
    assertEquals(1, result.getRows().get(0).get("id"));
}

// Test UPDATE statement
@Test
public void testUpdateStatement() throws Exception {
    String sql = "UPDATE users SET age = 31 WHERE id = 1";
    SqlParse.parseSql(sql, engine);
    
    // Verify update
    Map<String, Object> pk = Map.of("id", 1);
    Map<String, Object> row = engine.selectByPrimaryKey("users", pk);
    assertEquals(31, row.get("age"));
}
```

## 📖 Examples

### Example 1: SQL Parser Demo

Run the comprehensive SQL demo:

```bash
mvn exec:java -Dexec.mainClass="cc.fastsoft.sql.SqlParseExample"
```

This demonstrates:
- Database and table creation
- Multiple INSERT operations
- Various SELECT queries (*, columns, aliases, WHERE, LIMIT)
- UPDATE operations
- DELETE operations

### Example 2: JDBC Connection

See `src/main/java/cc/fastsoft/example/Example.java`:

```java
public class Example {
    public static void main(String[] args) {
        String url = "jdbc:mysql://127.0.0.1:2883/?useSSL=false";
        String user = "root";
        String password = "123456";
        
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            Statement stmt = conn.createStatement();
            
            // Execute query
            ResultSet rs = stmt.executeQuery("SHOW DATABASES");
            while (rs.next()) {
                System.out.println("Database: " + rs.getString(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
```

### Example 3: Programmatic Database API

```java
DatabaseEngine engine = new DatabaseEngine();

// Create database
engine.createDatabase("myapp");
engine.useDatabase("myapp");

// Define schema
List<Column> columns = List.of(
    new Column("id", ColumnType.INT),
    new Column("name", ColumnType.STRING),
    new Column("email", ColumnType.STRING),
    new Column("age", ColumnType.INT)
);
engine.createTable("users", columns, List.of("id"));

// Insert data
Map<String, Object> user = new HashMap<>();
user.put("id", 1);
user.put("name", "Alice");
user.put("email", "alice@example.com");
user.put("age", 30);
engine.insert("users", user);

// Query data
List<Map<String, Object>> results = engine.selectAll("users");
for (Map<String, Object> row : results) {
    System.out.println(row);
}

engine.close();
```

## ⚙️ Configuration

### Server Port

Edit `MysqlServer.java`:

```java
private static final int PORT = 2883; // Change to your desired port
```

### Authentication

Edit `AuthHandler.java`:

```java
private static final String VALID_PASSWORD = "123456"; // Change password
```

### RocksDB Path

Set via system property:

```java
System.setProperty("rocksdb.path", "/path/to/data");
```

Or pass as JVM argument:

```bash
mvn exec:java -Dexec.mainClass="cc.fastsoft.MysqlServer" -Drocksdb.path=/path/to/data
```

### Logging Level

Edit `src/main/resources/logback.xml`:

```xml
<logger name="cc.fastsoft" level="DEBUG"/>  <!-- Change to INFO, WARN, ERROR -->
```

## 🔍 Troubleshooting

### Connection Issues

**Problem:** Client cannot connect

**Solutions:**
1. Check if server is running: `netstat -an | grep 2883`
2. Verify firewall allows connections on port 2883
3. Use correct connection parameters (host, port, username, password)
4. Disable SSL: Add `--ssl-mode=DISABLED` (CLI) or `useSSL=false` (JDBC)

### Authentication Failures

**Problem:** Authentication failed for user 'root'

**Solutions:**
1. Verify password is `123456`
2. Check client supports `mysql_native_password`
3. Review authentication logs in `logs/application.log`
4. Add `allowPublicKeyRetrieval=true` to JDBC URL

### SQL Errors

**Problem:** SQL command returns error

**Solutions:**
1. Check SQL syntax is correct
2. Verify database and table exist
3. Ensure you've called `USE database` before table operations
4. Review supported SQL commands in this README
5. Check logs for detailed error messages

### Data Persistence Issues

**Problem:** Data lost after restart

**Solutions:**
1. Verify RocksDB path is correct
2. Check disk space availability
3. Ensure RocksDB directory has write permissions
4. Review logs for RocksDB errors

## 🚧 Limitations

### SQL Support
- Only equality operators in WHERE clause (no >, <, >=, <=, LIKE, IN)
- No JOIN support
- No aggregate functions (COUNT, SUM, AVG, etc.)
- No GROUP BY, HAVING, ORDER BY
- No subqueries
- No transactions (BEGIN, COMMIT, ROLLBACK)
- No CREATE TABLE via SQL (use API)

### Performance
- Full table scans for WHERE clause evaluation
- No secondary indexes (only primary key)
- In-memory filtering (not pushed to storage layer)

### Protocol
- Only supports mysql_native_password authentication
- No SSL/TLS encryption
- No prepared statements
- No stored procedures

## 🛣 Roadmap

### Short-term (v1.1)
- [ ] Add comparison operators in WHERE (>, <, >=, <=, !=)
- [ ] Implement LIKE operator
- [ ] Add IN clause support
- [ ] Implement ORDER BY
- [ ] Add aggregate functions (COUNT, SUM, AVG, MIN, MAX)

### Mid-term (v1.5)
- [ ] JOIN operations (INNER, LEFT, RIGHT)
- [ ] GROUP BY and HAVING
- [ ] Secondary indexes
- [ ] Transaction support
- [ ] CREATE TABLE via SQL

### Long-term (v2.0)
- [ ] SSL/TLS support
- [ ] Prepared statements
- [ ] Stored procedures
- [ ] Replication support
- [ ] Cluster mode

## 📝 Documentation

- **README.md** - This file, main documentation
- **SQL_IMPLEMENTATION.md** - Detailed SQL feature documentation
- **Javadoc** - Generate with `mvn javadoc:javadoc`

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Guidelines

- Follow existing code style and formatting
- Add unit tests for new features
- Update documentation as needed
- Keep commits atomic and well-described
- Ensure all tests pass before submitting PR

## 📄 License

This project is open source and available under the MIT License.

## 🙏 Acknowledgments

- **Netty** - High-performance network framework
- **RocksDB** - Embedded database engine
- **JSQLParser** - SQL parsing library
- **MySQL** - Protocol specification

## 📧 Contact

For questions, issues, or suggestions:
- Open an issue on GitHub
- Contact the maintainers

## 🌟 Show Your Support

If you find this project helpful, please give it a ⭐️ on GitHub!

---

**Built with ❤️ by the MySQL Simple Server team**

