package dev.tracekit.core;

public final class ConsoleFormatter implements EventFormatter {
    @Override public String format(TraceEvent e) {
        String indent = "  ".repeat(e.depth());
        String head = "[" + e.traceId().substring(0, 8) + "/" + e.spanId().substring(0, 4) + "] " + indent;
        String ms = String.format("%.3f ms", e.durationNanos() / 1_000_000.0);
        return switch (e.type()) {
            case ENTER -> head + "-> " + e.method() + "(" + String.join(", ", e.args()) + ")";
            case EXIT -> head + "<- " + e.method() + (e.result() != null ? " = " + e.result() : "") + " (" + ms + ")";
            case ERROR -> head + "!! " + e.method() + " threw " + e.error() + " (" + ms + ")";
        };
    }
}
