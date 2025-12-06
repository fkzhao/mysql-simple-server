package cc.fastsoft.storage.rocksdb;

import cc.fastsoft.utils.IOUtils;
import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.DBOptions;
import org.rocksdb.RocksDB;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RocksDbHandle implements AutoCloseable {
    static {
        RocksDB.loadLibrary();
    }

    private final boolean isReadOnly;

    private final DBOptions dbOptions;

    private final String dbPath;

    private RocksDB db;

    private ColumnFamilyHandle defaultColumnFamilyHandle;

    private final ColumnFamilyOptions defaultColumnFamilyOptions;

    public RocksDbHandle(
            File instanceRocksDBPath,
            DBOptions dbOptions,
            ColumnFamilyOptions defaultColumnFamilyOptions,
            boolean isReadOnly) {
        this.dbPath = instanceRocksDBPath.getAbsolutePath();
        this.dbOptions = dbOptions;
        this.defaultColumnFamilyOptions = defaultColumnFamilyOptions;
        this.isReadOnly = isReadOnly;
    }

    public RocksDbHandle(
            File instanceRocksDBPath,
            DBOptions dbOptions,
            ColumnFamilyOptions defaultColumnFamilyOptions) {
        this(instanceRocksDBPath, dbOptions, defaultColumnFamilyOptions, false);
    }

    public void openDB() throws IOException {
        loadDb();
    }

    private void loadDb() throws IOException {
        // we only have one column family, default column family
        List<ColumnFamilyDescriptor> columnFamilyDescriptors =
                Collections.singletonList(
                        new ColumnFamilyDescriptor(
                                RocksDB.DEFAULT_COLUMN_FAMILY, defaultColumnFamilyOptions
                        )
                );
        List<ColumnFamilyHandle> defaultCfHandle = new ArrayList<>(1);
        db = RocksDBOperationUtils.openDB(
                dbPath,
                columnFamilyDescriptors,
                defaultCfHandle,
                dbOptions,
                isReadOnly
        );
        // remove the default column family which is located at the first index
        defaultColumnFamilyHandle = defaultCfHandle.remove(0);
    }

    public RocksDB getDb() {
        return db;
    }

    public ColumnFamilyHandle getDefaultColumnFamilyHandle() {
        return defaultColumnFamilyHandle;
    }

    @Override
    public void close() {
        IOUtils.closeQuietly(defaultColumnFamilyHandle);
        IOUtils.closeQuietly(db);
        // Making sure the already created column family options will be closed
        IOUtils.closeQuietly(defaultColumnFamilyOptions);
    }
}
