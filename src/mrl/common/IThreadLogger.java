package mrl.common;

/**
 * Interface for a Thread-aware logger with manual flush capability.
 *
 * @author BrainX
 * @version 1.0
 */
public interface IThreadLogger {
    /**
     * Logs a message using the underlying implementation.<br/>
     * Log level is defined in the implementation and is preferably FATAL <br/>
     * <b>Note:</b> The message is not actually passed to the underlying implementation until {@link #flush()} is called.
     *
     * @param message Message to be logged
     */
    public void log(Object message);

    /**
     * Flushes all previously registered messages through {@link #log(Object)} to the underlying implementation.
     */
    public void flush();
}
