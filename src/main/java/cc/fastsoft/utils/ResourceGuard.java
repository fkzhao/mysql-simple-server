package cc.fastsoft.utils;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class ResourceGuard implements AutoCloseable {

    /** The object that serves as lock for count and the closed-flag. */
    private final SerializableObject lock;

    /** Number of clients that have ongoing access to the resource. */
    private volatile int leaseCount;

    /** This flag indicated if it is still possible to acquire access to the resource. */
    private volatile boolean closed;

    public ResourceGuard() {
        this.lock = new SerializableObject();
        this.leaseCount = 0;
        this.closed = false;
    }

    /**
     * Acquired access from one new client for the guarded resource.
     *
     * @throws IOException when the resource guard is already closed.
     */
    public Lease acquireResource() throws IOException {

        synchronized (lock) {
            if (closed) {
                throw new IOException("Resource guard was already closed.");
            }

            ++leaseCount;
        }

        return new Lease();
    }

    /**
     * Releases access for one client of the guarded resource. This method must only be called after
     * a matching call to {@link #acquireResource()}.
     */
    private void releaseResource() {

        synchronized (lock) {
            --leaseCount;

            if (closed && leaseCount == 0) {
                lock.notifyAll();
            }
        }
    }

    public void closeInterruptibly() throws InterruptedException {
        synchronized (lock) {
            closed = true;

            while (leaseCount > 0) {
                lock.wait();
            }
        }
    }

    /**
     * If the current thread is {@linkplain Thread#interrupt interrupted} while waiting for the
     * close method to complete, then it will continue to wait. When the thread does return from
     * this method its interrupt status will be set.
     */
    @SuppressWarnings("WeakerAccess")
    public void closeUninterruptibly() {

        boolean interrupted = false;
        synchronized (lock) {
            closed = true;

            while (leaseCount > 0) {

                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
        }

        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Closed the resource guard. This method will block until all calls to {@link
     * #acquireResource()} have seen their matching call to {@link #releaseResource()}.
     */
    @Override
    public void close() {
        closeUninterruptibly();
    }

    /** Returns true if the resource guard is closed, i.e. after {@link #close()} was called. */
    public boolean isClosed() {
        return closed;
    }

    /** Returns the current count of open leases. */
    public int getLeaseCount() {
        return leaseCount;
    }

    /**
     * A lease is issued by the {@link ResourceGuard} as result of calls to {@link
     * #acquireResource()}. The owner of the lease can release it via the {@link #close()} call.
     */
    public class Lease implements AutoCloseable {

        private final AtomicBoolean closed;

        private Lease() {
            this.closed = new AtomicBoolean(false);
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                releaseResource();
            }
        }
    }
}
