package dev.tracekit.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

/** Default sink: formats events and writes them through SLF4J (works with Logback, Log4j2, ...). */
public final class Slf4jSink implements TraceSink {
    private final Logger logger;
    private final EventFormatter formatter;
    private final Level level;

    public Slf4jSink(String loggerName, EventFormatter formatter, String level) {
        this.logger = LoggerFactory.getLogger(loggerName);
        this.formatter = formatter;
        this.level = Level.valueOf(level.toUpperCase());
    }

    @Override public void accept(TraceEvent e) {
        if (logger.isEnabledForLevel(level)) logger.atLevel(level).log(formatter.format(e));
    }
}
