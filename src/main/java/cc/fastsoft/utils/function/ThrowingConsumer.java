package cc.fastsoft.utils.function;

import java.util.function.Consumer;

public interface ThrowingConsumer <T, E extends Throwable> {

    /**
     * Performs this operation on the given argument.
     *
     * @param t the input argument
     * @throws E on errors during consumption
     */
    void accept(T t) throws E;

    /**
     * Converts a {@link ThrowingConsumer} into a {@link Consumer} which throws all checked
     * exceptions as unchecked.
     *
     * @param throwingConsumer to convert into a {@link Consumer}
     * @return {@link Consumer} which throws all checked exceptions as unchecked.
     */
    static <T, E extends Throwable> Consumer<T> unchecked(ThrowingConsumer<T, E> throwingConsumer) {
        return t -> {
            try {
                throwingConsumer.accept(t);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        };
    }
}
