package mrl.common;

/**
 * An extension of {@link ApacheCommonsThreadLogger} which embeds time passed since last log in the log message as well.
 *
 * @see ApacheCommonsThreadLogger
 * @see IThreadLogger
 */
public class TimestampThreadLogger extends ApacheCommonsThreadLogger {

    /**
     * Debug factory method<br/>
     * <b>Note:</b> This method should not be called with the same parameter in the same thread twice.
     * TODO @BrainX: Write an actual factory class for this logger
     *
     * @return A new instance of {@link ApacheCommonsThreadLogger}
     */
    public static TimestampThreadLogger getCurrentThreadLogger() {
        if (ThreadLocalLogger.getThreadLogger() == null || !(ThreadLocalLogger.getThreadLogger() instanceof TimestampThreadLogger)) {
            ThreadLocalLogger.setThreadLogger(new TimestampThreadLogger(TimestampThreadLogger.class));
//            System.out.println("Created a thread-specific logger for Thread : " + Thread.currentThread().getName());
        }
        return (TimestampThreadLogger) ThreadLocalLogger.getThreadLogger();
    }

    private long timestamp;

    protected TimestampThreadLogger(Class clazz) {
        super(clazz);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void log(Object message) {
        if (timestamp == 0) {
            timestamp = System.nanoTime();
        }
        long duration = System.nanoTime() - timestamp;
        messageBuilder.append("[" + Thread.currentThread().getName() + "] [" + (duration / 1000000) + "ms] [" + (timestamp / 1000000) + "ms] \t\t" + message).append("\n");

        timestamp = System.nanoTime();
    }

    @Override
    public void flush() {
//        super.flush();
        timestamp = 0;
    }
}
