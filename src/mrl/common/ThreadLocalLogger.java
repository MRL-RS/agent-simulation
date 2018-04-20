package mrl.common;

public class ThreadLocalLogger extends ThreadLocal<IThreadLogger> {
    private static ThreadLocalLogger threadLocalLogger = new ThreadLocalLogger();

    @Override
    public void set(IThreadLogger object) {
        super.set(object);
    }

    @Override
    public IThreadLogger get() {
        return super.get();
    }

    public static void setThreadLogger(IThreadLogger object) {
        threadLocalLogger.set(object);
    }

    public static IThreadLogger getThreadLogger() {
        return threadLocalLogger.get();
    }
}
