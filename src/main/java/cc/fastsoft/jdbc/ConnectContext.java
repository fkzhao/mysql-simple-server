package cc.fastsoft.jdbc;

import java.security.SecureRandom;

public class ConnectContext {
    final byte[] scramble;
    private int connectionId = 0;

    public ConnectContext() {
        this.scramble = new byte[20];
        new SecureRandom().nextBytes(this.scramble);
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
