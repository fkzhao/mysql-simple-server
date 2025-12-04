# MySQL Mock Server

A lightweight MySQL protocol mock server implemented with Netty for testing and development scenarios.

## Features

- ✅ Full MySQL handshake protocol support
- ✅ MySQL authentication mechanism (mysql_native_password)
- ✅ Basic SQL query support
- ✅ Compatible with DBeaver, MySQL CLI and other clients
- ✅ Asynchronous logging (based on Logback)
- ✅ Configurable port and authentication

## Supported SQL Commands

### Query Commands
- `SELECT 1` - Simple query test
- `SELECT @@variable_name` - System variable queries
- `SELECT DATABASE()` - Current database query
- `SHOW DATABASES` / `SHOW SCHEMAS` - Show database list
- `SHOW ENGINES` - Show storage engines (empty result set)
- `SHOW CHARSET` - Show character sets (empty result set)
- `SHOW COLLATION` - Show collations (empty result set)
- `SHOW PLUGINS` - Show plugins (empty result set)
- `SHOW VARIABLES` - Show variables (empty result set)

### Management Commands
- `SET ...` - SET commands (automatically return OK)
- `COM_PING` - Connection heartbeat
- `COM_QUIT` - Disconnect

## Tech Stack

- **Java 17**
- **Netty 4.2.7** - High-performance asynchronous network framework
- **Logback 1.4.14** - Logging framework
- **SLF4J 2.0.9** - Logging facade
- **Maven** - Build tool

## Quick Start

### Requirements

- JDK 17 or higher
- Maven 3.6+

### Build Project

```bash
mvn clean compile
```

### Start Server

```bash
mvn exec:java -Dexec.mainClass="cc.fastsoft.MysqlMockServer"
```

The server will start on port **2883**.

### Connect with MySQL Client

```bash
mysql -h127.0.0.1 -uroot -P2883 -p123456 --ssl-mode=DISABLED
```

**Default Connection Info:**
- Host: `127.0.0.1`
- Port: `2883`
- Username: `root`
- Password: `123456`

### Connect with DBeaver

1. Create a new MySQL connection
2. Configure connection parameters:
   - Server Host: `127.0.0.1`
   - Port: `2883`
   - Username: `root`
   - Password: `123456`
3. Add in **Driver properties**:
   - `useSSL=false`
4. Test connection and save

## Project Structure

```
mysql-simple-server/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── cc/fastsoft/
│   │   │       ├── MysqlMockServer.java          # Main server class
│   │   │       ├── protocol/
│   │   │       │   ├── Packet.java               # MySQL packet
│   │   │       │   └── codec/
│   │   │       │       ├── PacketDecoder.java    # Packet decoder
│   │   │       │       └── PacketEncoder.java    # Packet encoder
│   │   │       └── hander/
│   │   │           └── ServerHandler.java        # Business handler
│   │   └── resources/
│   │       └── logback.xml                       # Logging configuration
│   └── test/
├── logs/                                         # Log file directory
├── pom.xml                                       # Maven configuration
└── README.md                                     # Project documentation
```

## Logging Configuration

The logging configuration file is located at `src/main/resources/logback.xml`.

**Log Levels:**
- `cc.fastsoft` package: DEBUG
- `io.netty` package: INFO
- Root log level: INFO

**Log Output:**
- Console output: Real-time log viewing
- File output: Configured with daily rolling (logs directory)

Example of changing log level:

```xml
<logger name="cc.fastsoft" level="INFO"/>  <!-- Change to INFO level -->
```

## Example Queries

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
```

### Simple Query

```sql
SELECT 1;
```

Output:
```
+---+
| 1 |
+---+
| 1 |
+---+
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
```

## Custom Configuration

### Change Port

Modify in `MysqlMockServer.java`:

```java
int port = 2883;  // Change to another port
```

### Change Password

Modify in `ServerHandler.java`:

```java
private String password = "123456";  // Change to another password
```

### Add More Mock Databases

Modify in the `handleQuery` method of `ServerHandler.java`:

```java
String[] databases = {
    "information_schema", 
    "mysql", 
    "performance_schema", 
    "sys", 
    "test_db", 
    "my_database",
    "your_database"  // Add custom database
};
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

## FAQ

### Q: Getting SSL error when connecting?
**A:** Use `--ssl-mode=DISABLED` parameter to disable SSL:
```bash
mysql -h127.0.0.1 -uroot -P2883 -p123456 --ssl-mode=DISABLED
```

### Q: DBeaver connection failed?
**A:** Make sure to set `useSSL=false` in Driver properties

### Q: Port already in use?
**A:** Use the following command to find and kill the process:
```bash
lsof -ti:2883 | xargs kill -9
```

### Q: How to view logs?
**A:** Logs are output to console by default. Check the `logs/` directory for file logs (configured with daily rolling).

## Performance Optimization

1. **Adjust Worker Threads**: Modify the worker thread count in `MysqlMockServer.java`
   ```java
   EventLoopGroup workerGroup = new MultiThreadIoEventLoopGroup(16, NioIoHandler.newFactory());
   ```

2. **Async Logging**: Async logging is configured in logback.xml, adjust queue size as needed

3. **Connection Pooling**: For high concurrency scenarios, clients should use connection pooling

## Limitations

- ⚠️ This is a mock server and does not support real data storage
- ⚠️ Does not support complex SQL queries (JOIN, subqueries, etc.)
- ⚠️ Does not support transactions
- ⚠️ Does not support stored procedures and triggers
- ⚠️ Simplified authentication mechanism, for testing purposes only

## Contributing

Issues and Pull Requests are welcome!

## License

This project is licensed under the MIT License.

## Contact

- Author: fastsoft
- Project: [GitHub](https://github.com/your-username/mysql-simple-server)

## Changelog

### v1.0.0 (2025-12-04)
- ✅ Initial release
- ✅ Basic MySQL protocol support
- ✅ Common SHOW commands support
- ✅ Logback logging framework integration
- ✅ DBeaver and MySQL CLI client support

