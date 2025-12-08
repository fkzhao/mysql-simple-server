package cc.fastsoft.jdbc;

import cc.fastsoft.jdbc.protocol.MysqlPassword;

import java.security.SecureRandom;

import static cc.fastsoft.jdbc.protocol.MysqlPassword.SCRAMBLE_LENGTH;

public class ConnectContext {
    private int connectionId = 0;
    private String userName;
    private String database;
    private int clientCapabilities = 0;

    public ConnectContext() {

    }

    public int getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(int connectionId) {
        this.connectionId = connectionId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public boolean isAuthenticated() {
        return userName != null && !userName.isEmpty();
    }

    public int getClientCapabilities() {
        return clientCapabilities;
    }

    public void setClientCapabilities(int clientCapabilities) {
        this.clientCapabilities = clientCapabilities;
    }
}
