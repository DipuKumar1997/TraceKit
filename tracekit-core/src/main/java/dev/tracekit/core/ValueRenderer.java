package dev.tracekit.core;

import java.lang.reflect.Array;
import java.time.temporal.TemporalAccessor;
import java.util.*;

/**
 * Renders values into bounded, redacted, control-character-free strings.
 * Never calls toString() on application types unless useToString is true.
 */
public final class ValueRenderer {
    private final Redactor redactor;
    private final int maxLength, maxItems, maxDepth;
    private final boolean useToString;

    public ValueRenderer(Redactor redactor, int maxLength, int maxItems, int maxDepth, boolean useToString) {
        this.redactor = redactor; this.maxLength = maxLength; this.maxItems = maxItems;
        this.maxDepth = maxDepth; this.useToString = useToString;
    }

    public String render(Object value) {
        StringBuilder sb = new StringBuilder();
        try { write(sb, value, 0, Collections.newSetFromMap(new IdentityHashMap<>())); }
        catch (RuntimeException e) { sb.append("<render-failed:").append(e.getClass().getSimpleName()).append('>'); }
        return finish(sb.toString());
    }

    /** Redact, sanitize and truncate arbitrary text (e.g. exception messages). */
    public String text(String raw) { return finish(raw == null ? "null" : raw); }

    private String finish(String s) {
        String cleaned = sanitize(redactor.redactText(s));
        return cleaned.length() > maxLength ? cleaned.substring(0, maxLength) + "...(truncated)" : cleaned;
    }

    static String sanitize(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\n') b.append("\\n");
            else if (c == '\r') b.append("\\r");
            else if (c == '\t') b.append("\\t");
            else if (c < 0x20 || c == 0x7f) b.append(String.format("\\u%04x", (int) c));
            else b.append(c);
        }
        return b.toString();
    }

    private void write(StringBuilder sb, Object o, int depth, Set<Object> seen) {
        if (sb.length() > maxLength * 2) return;
        if (o == null) { sb.append("null"); return; }
        Class<?> c = o.getClass();
        if (o instanceof String || o instanceof Boolean || o instanceof Character) { sb.append(o); return; }
        if (o instanceof Enum<?> e) { sb.append(e.name()); return; }
        if (c.getName().startsWith("java.") && (o instanceof Number || o instanceof UUID || o instanceof TemporalAccessor)) {
            sb.append(o); return;
        }
        if (o instanceof Class<?> k) { sb.append(k.getName()); return; }
        boolean container = o instanceof Collection || o instanceof Map || c.isArray();
        if (container) {
            if (!seen.add(o)) { sb.append("<cycle>"); return; }
            try {
                if (depth >= maxDepth) { sb.append("..."); return; }
                if (o instanceof Collection<?> col) writeIter(sb, "[", "]", col.iterator(), col.size(), depth, seen);
                else if (o instanceof Map<?, ?> m) writeMap(sb, m, depth, seen);
                else {
                    int n = Array.getLength(o);
                    List<Object> view = new ArrayList<>(Math.min(n, maxItems));
                    for (int i = 0; i < Math.min(n, maxItems); i++) view.add(Array.get(o, i));
                    writeIter(sb, "[", "]", view.iterator(), n, depth, seen);
                }
            } finally { seen.remove(o); }
            return;
        }
        if (useToString) sb.append(o); else sb.append(c.getSimpleName()).append('@').append(Integer.toHexString(System.identityHashCode(o)));
    }

    private void writeIter(StringBuilder sb, String open, String close, Iterator<?> it, int size, int depth, Set<Object> seen) {
        sb.append(open);
        int i = 0;
        while (it.hasNext() && i < maxItems) {
            if (i++ > 0) sb.append(", ");
            write(sb, it.next(), depth + 1, seen);
        }
        if (size > maxItems) sb.append(", ...(+").append(size - maxItems).append(')');
        sb.append(close);
    }

    private void writeMap(StringBuilder sb, Map<?, ?> m, int depth, Set<Object> seen) {
        sb.append('{');
        int i = 0;
        for (Map.Entry<?, ?> e : m.entrySet()) {
            if (i >= maxItems) { sb.append(", ...(+").append(m.size() - maxItems).append(')'); break; }
            if (i++ > 0) sb.append(", ");
            write(sb, e.getKey(), depth + 1, seen);
            sb.append('=');
            if (e.getKey() instanceof String k && redactor.isSensitiveName(k)) sb.append("***");
            else write(sb, e.getValue(), depth + 1, seen);
        }
        sb.append('}');
    }
}
