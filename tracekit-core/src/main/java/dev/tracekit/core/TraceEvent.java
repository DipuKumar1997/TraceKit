package dev.tracekit.core;

import java.time.Instant;
import java.util.List;

/** Immutable trace event. All values are already rendered, redacted and bounded. */
public record TraceEvent(Type type, Instant timestamp, String traceId, String spanId, String parentSpanId,
                         int depth, String thread, String method, List<String> args,
                         String result, String error, long durationNanos) {
    public enum Type { ENTER, EXIT, ERROR }
}
