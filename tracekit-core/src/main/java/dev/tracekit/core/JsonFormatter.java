package dev.tracekit.core;

import java.util.List;

/** Dependency-free single-line JSON writer. */
public final class JsonFormatter implements EventFormatter {
    @Override public String format(TraceEvent e) {
        StringBuilder b = new StringBuilder("{");
        kv(b, "type", e.type().name()); kv(b, "ts", e.timestamp().toString());
        kv(b, "traceId", e.traceId()); kv(b, "spanId", e.spanId()); kv(b, "parentSpanId", e.parentSpanId());
        b.append("\"depth\":").append(e.depth()).append(',');
        kv(b, "thread", e.thread()); kv(b, "method", e.method());
        b.append("\"args\":[");
        List<String> a = e.args();
        for (int i = 0; i < a.size(); i++) { if (i > 0) b.append(','); b.append(q(a.get(i))); }
        b.append("],");
        kv(b, "result", e.result()); kv(b, "error", e.error());
        b.append("\"durationNanos\":").append(e.durationNanos()).append('}');
        return b.toString();
    }

    private static void kv(StringBuilder b, String k, String v) { b.append('"').append(k).append("\":").append(q(v)).append(','); }

    static String q(String s) {
        if (s == null) return "null";
        StringBuilder b = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                default -> { if (c < 0x20) b.append(String.format("\\u%04x", (int) c)); else b.append(c); }
            }
        }
        return b.append('"').toString();
    }
}
