package mrl.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Implementation of {@link IThreadLogger} with Apache Commons logger.
 */
public class ApacheCommonsThreadLogger implements IThreadLogger {

    /**
     * Debug factory method<br/>
     * <b>Note:</b> This method should not be called with the same parameter in the same thread twice.
     * TODO @BrainX: Write an actual factory class for this logger
     *
     * @param clazz
     * @return A new instance of {@link ApacheCommonsThreadLogger}
     */
    public static ApacheCommonsThreadLogger getLogger(Class clazz) {
        return new ApacheCommonsThreadLogger(clazz);
    }

    private Log logger;
    protected StringBuilder messageBuilder;

    protected ApacheCommonsThreadLogger(Class clazz) {
        messageBuilder = new StringBuilder();
        messageBuilder.append("\n");
        logger = LogFactory.getLog(clazz);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void log(Object message) {
        messageBuilder.append(message).append("\n");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush() {
        //TODO @BrainX: Do something about default log level
//        logger.debug(messageBuilder);
        messageBuilder = new StringBuilder();
        messageBuilder.append("\n");
    }
}
