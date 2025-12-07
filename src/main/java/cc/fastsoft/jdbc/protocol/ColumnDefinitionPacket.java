package cc.fastsoft.jdbc.protocol;

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

/**
 * MySQL Column Definition Packet
 *
 * Used in result set to describe column metadata
 *
 * Packet Format:
 * - length-encoded string: catalog
 * - length-encoded string: schema
 * - length-encoded string: table
 * - length-encoded string: org_table
 * - length-encoded string: name
 * - length-encoded string: org_name
 * - 1 byte: 0x0C (filler)
 * - 2 bytes: character set
 * - 4 bytes: column length
 * - 1 byte: column type
 * - 2 bytes: flags
 * - 1 byte: decimals
 * - 2 bytes: 0x00 (filler)
 */
public class ColumnDefinitionPacket extends MysqlPacket {

    private static final byte FILLER_1 = 0x0C;

    private String catalog;
    private String schema;
    private String table;
    private String orgTable;
    private String name;
    private String orgName;
    private int characterSet;
    private long columnLength;
    private byte columnType;
    private int flags;
    private byte decimals;

    public ColumnDefinitionPacket() {
        this((byte) 0);
    }

    public ColumnDefinitionPacket(byte sequenceId) {
        super(sequenceId);
        this.catalog = "def";
        this.schema = "";
        this.table = "";
        this.orgTable = "";
        this.name = "";
        this.orgName = "";
        this.characterSet = 33; // utf8_general_ci
        this.columnLength = 0;
        this.columnType = Constants.MYSQL_TYPE_VAR_STRING;
        this.flags = 0;
        this.decimals = 0;
    }

    public ColumnDefinitionPacket(String name, byte columnType, long columnLength) {
        this();
        this.name = name;
        this.orgName = name;
        this.columnType = columnType;
        this.columnLength = columnLength;
    }

    @Override
    public void write(ByteBuf buffer) {
        // Catalog (length-encoded string)
        PacketHelper.writeLengthEncodedString(buffer, catalog);

        // Schema (length-encoded string)
        PacketHelper.writeLengthEncodedString(buffer, schema);

        // Table (length-encoded string)
        PacketHelper.writeLengthEncodedString(buffer, table);

        // Org table (length-encoded string)
        PacketHelper.writeLengthEncodedString(buffer, orgTable);

        // Name (length-encoded string)
        PacketHelper.writeLengthEncodedString(buffer, name);

        // Org name (length-encoded string)
        PacketHelper.writeLengthEncodedString(buffer, orgName);

        // Filler
        buffer.writeByte(FILLER_1);

        // Character set
        buffer.writeShortLE(characterSet);

        // Column length
        buffer.writeIntLE((int) columnLength);

        // Column type
        buffer.writeByte(columnType);

        // Flags
        buffer.writeShortLE(flags);

        // Decimals
        buffer.writeByte(decimals);

        // Filler (2 bytes of 0x00)
        buffer.writeShort(0);
    }

    @Override
    public void read(ByteBuf buffer) {
        // Catalog
        catalog = PacketHelper.readLengthEncodedString(buffer);

        // Schema
        schema = PacketHelper.readLengthEncodedString(buffer);

        // Table
        table = PacketHelper.readLengthEncodedString(buffer);

        // Org table
        orgTable = PacketHelper.readLengthEncodedString(buffer);

        // Name
        name = PacketHelper.readLengthEncodedString(buffer);

        // Org name
        orgName = PacketHelper.readLengthEncodedString(buffer);

        // Filler
        buffer.readByte();

        // Character set
        characterSet = buffer.readUnsignedShortLE();

        // Column length
        columnLength = buffer.readUnsignedIntLE();

        // Column type
        columnType = buffer.readByte();

        // Flags
        flags = buffer.readUnsignedShortLE();

        // Decimals
        decimals = buffer.readByte();

        // Filler (2 bytes)
        buffer.readShort();
    }

    @Override
    public int getPayloadLength() {
        int length = 0;
        length += PacketHelper.getLengthEncodedStringLength(catalog);
        length += PacketHelper.getLengthEncodedStringLength(schema);
        length += PacketHelper.getLengthEncodedStringLength(table);
        length += PacketHelper.getLengthEncodedStringLength(orgTable);
        length += PacketHelper.getLengthEncodedStringLength(name);
        length += PacketHelper.getLengthEncodedStringLength(orgName);
        length += 1; // filler
        length += 2; // character set
        length += 4; // column length
        length += 1; // column type
        length += 2; // flags
        length += 1; // decimals
        length += 2; // filler
        return length;
    }

    // Getters and Setters

    public String getCatalog() {
        return catalog;
    }

    public void setCatalog(String catalog) {
        this.catalog = catalog;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getOrgTable() {
        return orgTable;
    }

    public void setOrgTable(String orgTable) {
        this.orgTable = orgTable;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public int getCharacterSet() {
        return characterSet;
    }

    public void setCharacterSet(int characterSet) {
        this.characterSet = characterSet;
    }

    public long getColumnLength() {
        return columnLength;
    }

    public void setColumnLength(long columnLength) {
        this.columnLength = columnLength;
    }

    public byte getColumnType() {
        return columnType;
    }

    public void setColumnType(byte columnType) {
        this.columnType = columnType;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public byte getDecimals() {
        return decimals;
    }

    public void setDecimals(byte decimals) {
        this.decimals = decimals;
    }

    @Override
    public String toString() {
        return "ColumnDefinitionPacket{" +
                "sequenceId=" + sequenceId +
                ", name='" + name + '\'' +
                ", columnType=" + columnType +
                ", columnLength=" + columnLength +
                ", characterSet=" + characterSet +
                ", flags=" + flags +
                '}';
    }
}

