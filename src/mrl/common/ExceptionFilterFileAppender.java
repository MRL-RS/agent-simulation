package mrl.common;

import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

/**
 * User: roohola
 * Date: 3/26/11
 * Time: 8:21 PM
 */
public class ExceptionFilterFileAppender extends DailyRollingFileAppender {
    // ...

    @Override
    public void append(LoggingEvent event) {
        if (!event.getLoggerName().startsWith("mrl")) {
            return;
        }
        ThrowableInformation ti = event.getThrowableInformation();

        if (ti != null) {
            Throwable t = ti.getThrowable();
            if (t != null) {
                if (t instanceof CommandException)
                    return;
            }
        }

        super.append(event);
    }

}