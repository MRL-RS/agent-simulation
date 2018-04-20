package mrl.common;

import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

/**
 * User: roohola
 * Date: 3/26/11
 * Time: 8:21 PM
 */
public class ExceptionFilterConsoleAppender extends org.apache.log4j.ConsoleAppender {
    // ...

    @Override
    public void append(LoggingEvent event) {

        ThrowableInformation ti = event.getThrowableInformation();
        if (!event.getLoggerName().startsWith("mrl") && !event.getLevel().equals(Level.ERROR) && !event.getLevel().equals(Level.FATAL)) {
            return;
        }

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