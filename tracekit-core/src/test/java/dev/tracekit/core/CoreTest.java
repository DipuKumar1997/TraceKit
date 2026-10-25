package dev.tracekit.core;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CoreTest {
    private final Redactor red = new DefaultRedactor(DefaultRedactor.DEFAULT_KEYS);
    private final ValueRenderer r = new ValueRenderer(red, 100, 3, 3, false);

    @Test void truncatesCollectionsAndStrings() {
        assertEquals("[1, 2, 3, ...(+2)]", r.render(List.of(1, 2, 3, 4, 5)));
        assertTrue(r.render("x".repeat(500)).endsWith("...(truncated)"));
    }

    @Test void handlesCycles() {
        List<Object> l = new ArrayList<>(); l.add(l);
        assertEquals("[<cycle>]", r.render(l));
    }

    @Test void redactsMapKeysAndText() {
        assertEquals("{password=***}", r.render(Map.of("password", "hunter2")));
        assertFalse(r.text("login password=hunter2 ok").contains("hunter2"));
        assertFalse(r.text("Authorization: Bearer abc.def.ghi").contains("abc.def"));
    }

    @Test void escapesLogInjection() {
        assertEquals("a\\nb", r.render("a\nb"));
    }

    @Test void doesNotCallToStringOnAppTypes() {
        Object o = new Object() { @Override public String toString() { throw new IllegalStateException(); } };
        assertDoesNotThrow(() -> r.render(o));
    }

    @Test void engineNestsAndSurvivesBadSink() {
        List<TraceEvent> ev = new ArrayList<>();
        TraceEngine e = new TraceEngine(r, red, List.of(x -> { throw new RuntimeException(); }, ev::add));
        var outer = e.enter("A#a", new String[]{"p"}, new Object[]{"v"}, true);
        var inner = e.enter("B#b", null, new Object[]{1}, true);
        e.exitOk(inner, 5, true);
        e.exitError(outer, new IllegalStateException("boom"));
        assertEquals(4, ev.size());
        assertEquals(outer.spanId(), ev.get(1).parentSpanId());
        assertEquals(1, ev.get(1).depth());
        assertEquals("arg0=1", ev.get(1).args().get(0));
        assertEquals(TraceEvent.Type.ERROR, ev.get(3).type());
    }

    @Test void formattersProduceOutput() {
        TraceEvent ev = new TraceEvent(TraceEvent.Type.EXIT, java.time.Instant.EPOCH, "0123456789abcdef", "fedcba9876543210",
                null, 0, "main", "A#a", List.of(), "ok\"", null, 1_500_000);
        assertTrue(new ConsoleFormatter().format(ev).contains("= ok\""));
        assertTrue(new JsonFormatter().format(ev).contains("\"result\":\"ok\\\"\""));
    }
}
