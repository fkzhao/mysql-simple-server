package cc.fastsoft.jdbc;

import cc.fastsoft.jdbc.protocol.MysqlPassword;

import java.security.SecureRandom;

import static cc.fastsoft.jdbc.protocol.MysqlPassword.SCRAMBLE_LENGTH;

public class ConnectContext {
    private static final int SCRAMBLE_LENGTH = 20;
    final byte[] scramble;
    private int connectionId = 0;

    public ConnectContext() {
        this.scramble =  MysqlPassword.createRandomString(SCRAMBLE_LENGTH);;
    }

    public byte[] getScramble() {
        return scramble;
    }

    public int getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(int connectionId) {
        this.connectionId = connectionId;
    }
}
