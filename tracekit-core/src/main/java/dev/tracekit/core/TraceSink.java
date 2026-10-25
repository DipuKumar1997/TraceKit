package dev.tracekit.core;

/** Output destination SPI. Implementations must not throw and should be fast. */
public interface TraceSink { void accept(TraceEvent event); }
