# MySQL Mock Server

A lightweight, high-performance MySQL protocol mock server implemented with Netty. Designed for testing, development, and integration scenarios where a full MySQL database is not required.

## ✨ Features

- ✅ **Full MySQL Protocol Support** - Complete handshake and authentication implementation
- ✅ **Multiple Authentication Formats** - Supports all mysql_native_password wire formats
  - CLIENT_PLUGIN_AUTH_LENENC_CLIENT_DATA
  - CLIENT_SECURE_CONNECTION (new protocol with 1-byte length)
  - Legacy protocol (null-terminated)
- ✅ **Real-time Connection Monitoring** - Thread-safe tracking of active client connections
- ✅ **Basic SQL Command Support** - Common queries and management commands
- ✅ **Client Compatibility** - Works with DBeaver, MySQL CLI, JDBC drivers, and other MySQL clients
- ✅ **High Performance** - Built on Netty's asynchronous I/O architecture
- ✅ **Comprehensive Logging** - Detailed debug logging with Logback framework
- ✅ **Configurable** - Easy to customize port, password, and mock data

## 📋 Supported SQL Commands

### Query Commands
- **`SELECT 1`** - Simple query test
- **`SELECT @@variable_name`** - System variable queries (version, version_comment, etc.)
- **`SELECT DATABASE()`** - Current database query
- **`SHOW DATABASES` / `SHOW SCHEMAS`** - Display mock database list
- **`SHOW ENGINES`** - Show storage engines (returns empty result set)
- **`SHOW CHARSET`** - Show character sets (returns empty result set)
- **`SHOW COLLATION`** - Show collations (returns empty result set)
- **`SHOW PLUGINS`** - Show plugins (returns empty result set)
- **`SHOW VARIABLES`** - Show system variables (returns empty result set)

### Management Commands
- **`SET ...`** - SET commands (automatically returns OK packet)
- **`USE database_name`** - Switch database (returns OK packet)
- **`COM_PING`** - Connection heartbeat test
- **`COM_QUIT`** - Graceful connection termination

> **Note:** This is a mock server that returns predefined responses. It does not perform actual SQL execution or data storage.

## 🛠 Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 17 | Programming language |
| Netty | 4.2.7.Final | High-performance asynchronous network framework |
| Logback | 1.5.21 | Logging framework |
| SLF4J | 2.0.9 | Logging facade |
| MySQL Connector/J | 8.4.0 | JDBC driver (for examples) |
| RocksDB | 10.4.2 | Embedded key-value storage |
| Maven | 3.6+ | Build and dependency management |

## 🚀 Quick Start

### Prerequisites

- JDK 17 or higher
- Maven 3.6+
- (Optional) MySQL CLI client for testing

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/mysql-simple-server.git
   cd mysql-simple-server
   ```

2. **Build the project**
   ```bash
   mvn clean compile
   ```

3. **Run the server**
   ```bash
   mvn exec:java -Dexec.mainClass="cc.fastsoft.MysqlMockServer"
   ```

The server will start on port **2883** and you'll see:
```
2025-12-05 10:00:00.123 [main] INFO cc.fastsoft.MysqlMockServer - MySQL Mock Server started on port 2883
```

### Connecting to the Server

#### Option 1: MySQL CLI

```bash
mysql -h127.0.0.1 -uroot -P2883 -p123456 --ssl-mode=DISABLED
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

Connection conn = DriverManager.getConnection(url, user, password);
// Use the connection...
```

See `src/main/java/cc/fastsoft/example/Example.java` for a complete example.

### Default Configuration

| Parameter | Value |
|-----------|-------|
| Host | `127.0.0.1` |
| Port | `2883` |
| Username | `root` |
| Password | `123456` |
| SSL | Disabled |

## 📁 Project Structure

```
mysql-simple-server/
├── src/
│   ├── main/
│   │   ├── java/cc/fastsoft/
│   │   │   ├── MysqlMockServer.java              # Main server class
│   │   │   ├── jdbc/
│   │   │   │   ├── ServerHandler.java            # Connection handler (with connection tracking)
│   │   │   │   ├── ConnectContext.java           # Connection context (scramble, connection ID)
│   │   │   │   ├── hander/
│   │   │   │   │   ├── HandshakeHandler.java     # MySQL handshake phase
│   │   │   │   │   ├── AuthHandler.java          # Authentication (mysql_native_password)
│   │   │   │   │   ├── CommandHandler.java       # SQL command routing
│   │   │   │   │   └── QueryHandler.java         # Query result generation
│   │   │   │   └── protocol/
│   │   │   │       ├── Packet.java               # MySQL packet wrapper
│   │   │   │       ├── PacketHelper.java         # Packet utilities
│   │   │   │       ├── Constants.java            # Protocol constants
│   │   │   │       └── codec/
│   │   │   │           ├── PacketDecoder.java    # Decode MySQL packets
│   │   │   │           └── PacketEncoder.java    # Encode MySQL packets
│   │   │   ├── storage/
│   │   │   │   └── rocksdb/
│   │   │   │       └── RocksDbHandle.java        # RocksDB storage (optional)
│   │   │   ├── utils/
│   │   │   │   ├── IOUtils.java                  # I/O utilities
│   │   │   │   ├── OperatingSystem.java          # OS detection
│   │   │   │   └── Preconditions.java            # Validation utilities
│   │   │   └── example/
│   │   │       └── Example.java                  # JDBC connection example
│   │   └── resources/
│   │       └── logback.xml                       # Logging configuration
│   └── test/
│       └── java/
├── logs/                                         # Log files (generated at runtime)
├── pom.xml                                       # Maven project configuration
├── README.md                                     # This file
└── FIX_SUMMARY.md                                # Authentication fix documentation
```

### Key Components

| Component | Description |
|-----------|-------------|
| **MysqlMockServer** | Main server class, sets up Netty pipeline |
| **ServerHandler** | Connection handler with state management and connection tracking |
| **ConnectContext** | Stores per-connection data (scramble, connection ID) |
| **HandshakeHandler** | Sends MySQL handshake packet with server capabilities |
| **AuthHandler** | Handles mysql_native_password authentication (3 wire formats) |
| **CommandHandler** | Routes SQL commands to appropriate handlers |
| **QueryHandler** | Generates result sets for queries |
| **PacketDecoder/Encoder** | Converts between bytes and Packet objects |

## 📊 Connection Monitoring

### Active Connection Tracking

The server automatically tracks the number of active client connections using a thread-safe `AtomicInteger`.

**Features:**
- Automatic increment when a client connects (`channelActive`)
- Automatic decrement when a client disconnects (`channelInactive`)
- Thread-safe counter for concurrent connections
- Real-time logging of connection events

**Getting Active Connection Count:**

```java
// Get current active connection count anywhere in your code
int activeConnections = ServerHandler.getActiveConnectionCount();
logger.info("Current active connections: {}", activeConnections);
```

**Connection Events in Logs:**

```
2025-12-05 10:15:30.123 [nioEventLoopGroup-3-1] INFO  c.f.jdbc.ServerHandler - Creating new connection handler. Active connections: 1
2025-12-05 10:15:30.456 [nioEventLoopGroup-3-1] INFO  c.f.jdbc.ServerHandler - Client connected: /127.0.0.1:54321. Total active connections: 1
2025-12-05 10:16:45.789 [nioEventLoopGroup-3-1] INFO  c.f.jdbc.ServerHandler - Client disconnected: /127.0.0.1:54321. Remaining active connections: 0
```

### Connection Context

Each connection has its own `ConnectContext` containing:
- **Connection ID** - Unique incremental ID
- **Scramble** - 20-byte random challenge for authentication
- **Authentication State** - Tracked in ServerHandler

## 📝 Logging Configuration

The logging configuration file is located at `src/main/resources/logback.xml`.

**Log Levels:**
- `cc.fastsoft` package: DEBUG (detailed protocol messages)
- `io.netty` package: INFO
- Root log level: INFO

**Log Output:**
- Console output: Real-time log viewing with color coding
- File output: Daily rolling files in `logs/` directory

**Key Log Messages:**
- Handshake: scramble generation, capability flags
- Authentication: wire format used, verification steps (SHA-1 calculations)
- Commands: SQL received, result sets sent
- Connections: connect/disconnect events, active count

Example of changing log level:

```xml
<logger name="cc.fastsoft" level="INFO"/>  <!-- Change to INFO to reduce verbosity -->
```

## 💡 Example Queries

### Show Database List

```sql
SHOW DATABASES;
```

Output:
```
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
6 rows in set (0.00 sec)
```

### Simple Query Test

```sql
SELECT 1 AS value;
```

Output:
```
+-------+
| value |
+-------+
|     1 |
+-------+
1 row in set (0.00 sec)
```

### System Variable Query

```sql
SELECT @@version_comment;
```

Output:
```
+-------------------+
| @@version_comment |
+-------------------+
| MySQL Mock Server |
+-------------------+
1 row in set (0.00 sec)
```

### Multiple Queries

```sql
SELECT DATABASE();
USE test_db;
SELECT 'Hello, World!' AS message;
```

Output:
```
+------------+
| DATABASE() |
+------------+
| NULL       |
+------------+

Database changed

+-----------------+
| message         |
+-----------------+
| Hello, World!   |
+-----------------+
```

## ⚙️ Custom Configuration

### Change Server Port

Modify in `MysqlMockServer.java`:

```java
int port = 2883;  // Change to your desired port
```

### Change Authentication Password

Modify the password in `ServerHandler` constructor:

```java
this.authHandler = new AuthHandler("your_password", ctx.getScramble());
```

### Add Custom Mock Databases

Modify in `QueryHandler.java` to add more databases to the mock list:

```java
private static final String[] MOCK_DATABASES = {
    "information_schema",
    "mysql",
    "performance_schema",
    "sys",
    "test_db",
    "my_database",
    "your_custom_db",    // Add your database
    "another_db"         // Add another
};
```

### Adjust Worker Threads

For higher concurrency, modify the worker thread pool size in `MysqlMockServer.java`:

```java
EventLoopGroup workerGroup = new MultiThreadIoEventLoopGroup(16, NioIoHandler.newFactory());
// Increase from 8 to 16 or more based on your needs
```

## Development Guide

### Adding New SQL Command Support

Add in the `handleQuery` method of `ServerHandler.java`:

```java
} else if (sqlUpper.startsWith("YOUR COMMAND")) {
    // Handle your command
    sendSimpleResultSet(ctx, columnNames, rows);
}
```

### Debugging Tips

1. **Enable Verbose Logging**: Set log level to DEBUG
2. **Packet Analysis**: Use Wireshark or tcpdump to analyze MySQL protocol
3. **Use Breakpoints**: Set breakpoints on key methods in your IDE

## ❓ FAQ

### Q: Getting SSL error when connecting?
**A:** Use `--ssl-mode=DISABLED` parameter to disable SSL:
```bash
mysql -h127.0.0.1 -uroot -P2883 -p123456 --ssl-mode=DISABLED
```
For JDBC, add to connection URL: `useSSL=false`

### Q: Authentication failed with JDBC?
**A:** Make sure you're using the correct connection URL format:
```java
String url = "jdbc:mysql://127.0.0.1:2883/?useSSL=false&allowPublicKeyRetrieval=true";
```
The server supports all mysql_native_password wire formats.

### Q: DBeaver connection timeout or failed?
**A:** Check the following:
1. Set `useSSL=false` in Driver properties
2. Set `allowPublicKeyRetrieval=true` in Driver properties
3. Make sure the server is running on port 2883
4. Check firewall settings

### Q: Port already in use?
**A:** Find and kill the process using port 2883:
```bash
# macOS/Linux
lsof -ti:2883 | xargs kill -9

# Or check what's using the port
lsof -i:2883
```

### Q: How to view logs?
**A:** Logs are output to:
- **Console**: Real-time output when running the server
- **File**: Check the `logs/` directory (daily rolling logs)

Enable DEBUG logging in `logback.xml` for more details.

### Q: How to monitor active connections?
**A:** Check the server logs for connection events:
```
Client connected: /127.0.0.1:54321. Total active connections: 5
Client disconnected: /127.0.0.1:54321. Remaining active connections: 4
```
Or programmatically:
```java
int count = ServerHandler.getActiveConnectionCount();
```

### Q: Does it support transactions?
**A:** No, this is a mock server. It returns predefined responses and doesn't support:
- Transactions (BEGIN, COMMIT, ROLLBACK)
- Complex queries (JOINs, subqueries)
- Data persistence
- Stored procedures or triggers

### Q: Can I use this in production?
**A:** No, this is designed for **testing and development only**. For production, use a real MySQL database.

## ⚡ Performance Optimization

### Server-Side Optimizations

1. **Adjust Worker Threads**
   
   Modify the worker thread pool size in `MysqlMockServer.java`:
   ```java
   EventLoopGroup workerGroup = new MultiThreadIoEventLoopGroup(16, NioIoHandler.newFactory());
   // Default is 8, increase for higher concurrency
   ```

2. **Async Logging**
   
   Async logging is configured in `logback.xml`. Adjust the queue size:
   ```xml
   <appender name="ASYNC" class="ch.qos.logback.classic.AsyncAppender">
       <queueSize>512</queueSize>  <!-- Increase if needed -->
   </appender>
   ```

3. **Reduce Log Level**
   
   For production-like testing, reduce logging verbosity:
   ```xml
   <logger name="cc.fastsoft" level="INFO"/>  <!-- Change from DEBUG -->
   ```

### Client-Side Optimizations

1. **Connection Pooling**
   
   Use connection pooling for high-concurrency scenarios:
   ```java
   // Example with HikariCP
   HikariConfig config = new HikariConfig();
   config.setJdbcUrl("jdbc:mysql://127.0.0.1:2883/?useSSL=false");
   config.setUsername("root");
   config.setPassword("123456");
   config.setMaximumPoolSize(20);
   HikariDataSource ds = new HikariDataSource(config);
   ```

2. **Reuse Connections**
   
   Don't create a new connection for each query:
   ```java
   // Bad
   for (int i = 0; i < 1000; i++) {
       Connection conn = DriverManager.getConnection(url, user, pass);
       // ... query ...
       conn.close();
   }
   
   // Good
   Connection conn = DriverManager.getConnection(url, user, pass);
   for (int i = 0; i < 1000; i++) {
       // ... query ...
   }
   conn.close();
   ```

### Monitoring

Monitor active connections to detect leaks:
```java
int activeConnections = ServerHandler.getActiveConnectionCount();
if (activeConnections > threshold) {
    logger.warn("High connection count: {}", activeConnections);
}
```

## ⚠️ Limitations

This is a **mock server** designed for testing and development. It has the following limitations:

### Data & Storage
- ❌ **No data persistence** - All data is in-memory and predefined
- ❌ **No real database storage** - RocksDB integration is available but optional
- ❌ **No table creation** - Cannot execute CREATE TABLE, ALTER TABLE, etc.

### SQL Support
- ❌ **No complex queries** - JOINs, subqueries, GROUP BY, HAVING not supported
- ❌ **No INSERT/UPDATE/DELETE** - Only returns OK packets, no actual data modification
- ❌ **Limited WHERE clauses** - Cannot filter results dynamically

### Transaction & Concurrency
- ❌ **No transaction support** - BEGIN, COMMIT, ROLLBACK are ignored
- ❌ **No isolation levels** - All queries are independent
- ❌ **No locking mechanisms** - No row-level or table-level locks

### Advanced Features
- ❌ **No stored procedures** - Cannot execute or create stored procedures
- ❌ **No triggers** - No trigger support
- ❌ **No views** - Cannot create or query views
- ❌ **No prepared statements** - Only text protocol supported
- ❌ **No replication** - Single server only

### Authentication & Security
- ⚠️ **Simplified authentication** - Only mysql_native_password supported
- ⚠️ **No user management** - Single hardcoded user (root/123456)
- ⚠️ **No SSL/TLS** - Must disable SSL on client side
- ⚠️ **Testing only** - Do not use in production environments

### Use Cases

✅ **Good for:**
- Integration testing
- Development mockups
- Protocol testing
- Client compatibility testing
- Educational purposes

❌ **Not suitable for:**
- Production environments
- Data persistence requirements
- Complex query testing
- Performance benchmarking of real MySQL

## Contributing

Issues and Pull Requests are welcome!

## License

This project is licensed under the MIT License.

## Contact

- Author: fastsoft
- Project: [GitHub](https://github.com/your-username/mysql-simple-server)

## 📜 Changelog

### v1.1.0 (2025-12-05)
- ✅ **New Feature:** Real-time connection monitoring with thread-safe counter
- ✅ **New Feature:** Connection context with unique connection IDs
- ✅ **Enhancement:** Improved HandshakeHandler with proper MySQL capability flags
- ✅ **Enhancement:** AuthHandler now supports all mysql_native_password wire formats:
  - CLIENT_PLUGIN_AUTH_LENENC_CLIENT_DATA (length-encoded format)
  - CLIENT_SECURE_CONNECTION (new protocol: 1-byte length + data)
  - Legacy protocol (null-terminated format)
- ✅ **Fix:** Resolved JDBC authentication issues with proper wire format handling
- ✅ **Fix:** Corrected RocksDB import path errors
- ✅ **Enhancement:** Added comprehensive debug logging throughout the protocol stack
- ✅ **Documentation:** Complete README overhaul with detailed examples

### v1.0.0 (2025-12-04)
- ✅ Initial release
- ✅ Basic MySQL protocol support (handshake, authentication, command phase)
- ✅ Common SHOW commands support (DATABASES, VARIABLES, etc.)
- ✅ Logback logging framework integration with async logging
- ✅ DBeaver and MySQL CLI client compatibility
- ✅ Netty-based asynchronous I/O architecture

