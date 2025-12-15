package cc.fastsoft.kv;

import javax.annotation.Nonnull;
import java.io.IOException;

public interface KvBatchWriter extends AutoCloseable{

    /** Put a key-value pair. */
    void put(@Nonnull byte[] key, @Nonnull byte[] value) throws IOException;

    /** Delete a key-value pair by the given key. */
    void delete(@Nonnull byte[] key) throws IOException;

    /** Flush the written key-value pair. */
    void flush() throws IOException;
}
