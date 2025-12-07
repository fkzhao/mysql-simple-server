package cc.fastsoft.jdbc.protocol;

public class Constants {
    public static final int CLIENT_LONG_PASSWORD                    = 1;
    public static final int CLIENT_FOUND_ROWS                       = 1 << 1;
    public static final int CLIENT_LONG_FLAG                        = 1 << 2;
    public static final int CLIENT_CONNECT_WITH_DB                  = 1 << 3;
    public static final int CLIENT_LOCAL_FILES                      = 1 << 7;
    public static final int CLIENT_PROTOCOL_41                      = 1 << 9;   // must have
    public static final int CLIENT_INTERACTIVE                      = 1 << 10;
    public static final int CLIENT_SSL                              = 1 << 11;
    public static final int CLIENT_TRANSACTIONS                      = 1 << 13;
    public static final int CLIENT_SECURE_CONNECTION                = 1 << 15;
    public static final int CLIENT_MULTI_STATEMENTS                 = 1 << 16;
    public static final int CLIENT_MULTI_RESULTS                    = 1 << 17;
    public static final int CLIENT_PS_MULTI_RESULTS                 = 1 << 18;
    public static final int CLIENT_PLUGIN_AUTH                      = 1 << 19;
    public static final int CLIENT_CONNECT_ATTRS                    = 1 << 20;
    public static final int CLIENT_PLUGIN_AUTH_LENENC_CLIENT_DATA   = 1 << 21;
    public static final int CLIENT_CAN_HANDLE_EXPIRED_PASSWORDS     = 1 << 22;
    public static final int CLIENT_SESSION_TRACK                    = 1 << 23;
    public static final int CLIENT_DEPRECATE_EOF                    = 1 << 24;
    public static final String MYSQL_NATIVE_PASSWORD                = "mysql_native_password";
    public static final String CACHING_SHA2_PASSWORD                = "caching_sha2_password";

    // Server status flags
    public static final int SERVER_STATUS_IN_TRANS              = 1;
    public static final int SERVER_STATUS_AUTOCOMMIT            = 1 << 1;
    public static final int SERVER_MORE_RESULTS_EXISTS          = 1 << 3;
    public static final int SERVER_STATUS_NO_GOOD_INDEX_USED    = 1 << 4;
    public static final int SERVER_STATUS_NO_INDEX_USED         = 1 << 5;
    public static final int SERVER_STATUS_CURSOR_EXISTS         = 1 << 6;
    public static final int SERVER_STATUS_LAST_ROW_SENT         = 1 << 7;
    public static final int SERVER_STATUS_DB_DROPPED            = 1 << 8;
    public static final int SERVER_STATUS_NO_BACKSLASH_ESCAPES  = 1 << 9;
    public static final int SERVER_STATUS_METADATA_CHANGED      = 1 << 10;
    public static final int SERVER_QUERY_WAS_SLOW               = 1 << 11;
    public static final int SERVER_PS_OUT_PARAMS                = 1 << 12;
    public static final int SERVER_STATUS_IN_TRANS_READONLY     = 1 << 13;
    public static final int SERVER_SESSION_STATE_CHANGED        = 1 << 14;

    // Command types
    public static final byte COM_SLEEP               = 0x00;
    public static final byte COM_QUIT                = 0x01;
    public static final byte COM_INIT_DB             = 0x02;
    public static final byte COM_QUERY               = 0x03;
    public static final byte COM_FIELD_LIST          = 0x04;
    public static final byte COM_CREATE_DB           = 0x05;
    public static final byte COM_DROP_DB             = 0x06;
    public static final byte COM_REFRESH             = 0x07;
    public static final byte COM_SHUTDOWN            = 0x08;
    public static final byte COM_STATISTICS          = 0x09;
    public static final byte COM_PROCESS_INFO        = 0x0A;
    public static final byte COM_CONNECT             = 0x0B;
    public static final byte COM_PROCESS_KILL        = 0x0C;
    public static final byte COM_DEBUG               = 0x0D;
    public static final byte COM_PING                = 0x0E;
    public static final byte COM_TIME                = 0x0F;
    public static final byte COM_DELAYED_INSERT      = 0x10;
    public static final byte COM_CHANGE_USER         = 0x11;
    public static final byte COM_BINLOG_DUMP         = 0x12;
    public static final byte COM_TABLE_DUMP          = 0x13;
    public static final byte COM_CONNECT_OUT         = 0x14;
    public static final byte COM_REGISTER_SLAVE      = 0x15;
    public static final byte COM_STMT_PREPARE        = 0x16;
    public static final byte COM_STMT_EXECUTE        = 0x17;
    public static final byte COM_STMT_SEND_LONG_DATA = 0x18;
    public static final byte COM_STMT_CLOSE          = 0x19;
    public static final byte COM_STMT_RESET          = 0x1A;
    public static final byte COM_SET_OPTION          = 0x1B;
    public static final byte COM_STMT_FETCH          = 0x1C;
    public static final byte COM_DAEMON              = 0x1D;
    public static final byte COM_BINLOG_DUMP_GTID    = 0x1E;
    public static final byte COM_RESET_CONNECTION    = 0x1F;

    // MySQL column types
    public static final byte MYSQL_TYPE_DECIMAL      = 0x00;
    public static final byte MYSQL_TYPE_TINY         = 0x01;
    public static final byte MYSQL_TYPE_SHORT        = 0x02;
    public static final byte MYSQL_TYPE_LONG         = 0x03;
    public static final byte MYSQL_TYPE_FLOAT        = 0x04;
    public static final byte MYSQL_TYPE_DOUBLE       = 0x05;
    public static final byte MYSQL_TYPE_NULL         = 0x06;
    public static final byte MYSQL_TYPE_TIMESTAMP    = 0x07;
    public static final byte MYSQL_TYPE_LONGLONG     = 0x08;
    public static final byte MYSQL_TYPE_INT24        = 0x09;
    public static final byte MYSQL_TYPE_DATE         = 0x0A;
    public static final byte MYSQL_TYPE_TIME         = 0x0B;
    public static final byte MYSQL_TYPE_DATETIME     = 0x0C;
    public static final byte MYSQL_TYPE_YEAR         = 0x0D;
    public static final byte MYSQL_TYPE_NEWDATE      = 0x0E;
    public static final byte MYSQL_TYPE_VARCHAR      = 0x0F;
    public static final byte MYSQL_TYPE_BIT          = 0x10;
    public static final byte MYSQL_TYPE_TIMESTAMP2   = 0x11;
    public static final byte MYSQL_TYPE_DATETIME2    = 0x12;
    public static final byte MYSQL_TYPE_TIME2        = 0x13;
    public static final byte MYSQL_TYPE_JSON         = (byte) 0xF5;
    public static final byte MYSQL_TYPE_NEWDECIMAL   = (byte) 0xF6;
    public static final byte MYSQL_TYPE_ENUM         = (byte) 0xF7;
    public static final byte MYSQL_TYPE_SET          = (byte) 0xF8;
    public static final byte MYSQL_TYPE_TINY_BLOB    = (byte) 0xF9;
    public static final byte MYSQL_TYPE_MEDIUM_BLOB  = (byte) 0xFA;
    public static final byte MYSQL_TYPE_LONG_BLOB    = (byte) 0xFB;
    public static final byte MYSQL_TYPE_BLOB         = (byte) 0xFC;
    public static final byte MYSQL_TYPE_VAR_STRING   = (byte) 0xFD;
    public static final byte MYSQL_TYPE_STRING       = (byte) 0xFE;
    public static final byte MYSQL_TYPE_GEOMETRY     = (byte) 0xFF;
}
