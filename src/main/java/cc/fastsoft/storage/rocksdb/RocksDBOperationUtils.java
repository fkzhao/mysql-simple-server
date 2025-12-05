package cc.fastsoft.storage.rocksdb;

import cc.fastsoft.utils.IOUtils;
import cc.fastsoft.utils.OperatingSystem;
import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.DBOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static  cc.fastsoft.utils.Preconditions.checkNotNull;
import static  cc.fastsoft.utils.Preconditions.checkState;

/**
 * Utils for RocksDB Operations.
 */
public class RocksDBOperationUtils {

    private static final Logger LOG = LoggerFactory.getLogger(RocksDBOperationUtils.class);

    public static void addColumnFamilyOptionsToCloseLater(
            List<ColumnFamilyOptions> columnFamilyOptions, ColumnFamilyHandle columnFamilyHandle) {
        try {
            // IMPORTANT NOTE: Do not call ColumnFamilyHandle#getDescriptor() just to judge if it
            // return null and then call it again when it return is not null. That will cause
            // task manager native memory used by RocksDB can't be released timely after job
            // restart.
            // The problem can find in : https://issues.apache.org/jira/browse/FLINK-21986
            if (columnFamilyHandle != null) {
                ColumnFamilyDescriptor columnFamilyDescriptor = columnFamilyHandle.getDescriptor();
                if (columnFamilyDescriptor != null) {
                    columnFamilyOptions.add(columnFamilyDescriptor.getOptions());
                }
            }
        } catch (RocksDBException e) {
            // ignore
        }
    }

    public static RocksDB openDB(
            String path,
            List<ColumnFamilyDescriptor> columnFamilyDescriptors,
            List<ColumnFamilyHandle> columnFamilyHandles,
            DBOptions dbOptions,
            boolean isReadOnly)
            throws IOException {
        RocksDB dbRef;
        try {
            if (isReadOnly) {
                dbRef =
                        RocksDB.openReadOnly(
                                checkNotNull(dbOptions),
                                checkNotNull(path),
                                columnFamilyDescriptors,
                                columnFamilyHandles);
            } else {
                dbRef =
                        RocksDB.open(
                                checkNotNull(dbOptions),
                                checkNotNull(path),
                                columnFamilyDescriptors,
                                columnFamilyHandles);
            }
        } catch (RocksDBException e) {
            columnFamilyDescriptors.forEach((cfd) -> IOUtils.closeQuietly(cfd.getOptions()));

            // improve error reporting on Windows
            throwExceptionIfPathLengthExceededOnWindows(path, e);

            throw new IOException("Error while opening RocksDB instance.", e);
        }

        checkState(
                columnFamilyDescriptors.size() == columnFamilyHandles.size(),
                "Not all requested column family handles have been created");
        return dbRef;
    }

    private static void throwExceptionIfPathLengthExceededOnWindows(String path, Exception cause)
            throws IOException {
        // max directory path length on Windows is 247.
        // the maximum path length is 260, subtracting one file name length (12 chars) and one NULL
        // terminator.
        final int maxWinDirPathLen = 247;

        if (path.length() > maxWinDirPathLen && OperatingSystem.isWindows()) {
            throw new IOException(
                    String.format(
                            "The directory path length (%d) is longer than the directory path length limit for Windows (%d): %s",
                            path.length(), maxWinDirPathLen, path),
                    cause);
        }
    }
}
