package dev.tracekit.core;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EngineFeaturesTest {
    private final Redactor red = new DefaultRedactor(DefaultRedactor.DEFAULT_KEYS);
    private final ValueRenderer r = new ValueRenderer(red, 100, 3, 3, false);

    @Test void zeroSampleRateEmitsNothing() {
        List<TraceEvent> ev = new ArrayList<>();
        TraceEngine e = new TraceEngine(r, red, List.of(ev::add), 0.0, 0);
        var f = e.enter("A#a", null, new Object[0], true);
        e.exitOk(f, 1, true);
        assertEquals(0, ev.size());
    }

    @Test void slowModeOnlyEmitsSlowCallsAndErrors() {
        List<TraceEvent> ev = new ArrayList<>();
        TraceEngine e = new TraceEngine(r, red, List.of(ev::add), 1.0, 1_000_000_000L);
        e.exitOk(e.enter("A#fast", null, new Object[0], true), 1, true);
        assertEquals(0, ev.size());
        e.exitError(e.enter("A#bad", null, new Object[0], true), new RuntimeException("x"));
        assertEquals(1, ev.size());
        assertEquals(TraceEvent.Type.ERROR, ev.get(0).type());
    }

    @Test void wrapPropagatesContextAcrossThreads() throws Exception {
        List<TraceEvent> ev = Collections.synchronizedList(new ArrayList<>());
        TraceEngine e = new TraceEngine(r, red, List.of(ev::add));
        var parent = e.enter("A#parent", null, new Object[0], true);
        Thread t = new Thread(e.wrap(() -> e.exitOk(e.enter("B#child", null, new Object[0], true), 1, true)));
        t.start(); t.join();
        e.exitOk(parent, null, false);
        TraceEvent childEnter = ev.stream().filter(x -> x.method().equals("B#child")).findFirst().orElseThrow();
        assertEquals(parent.traceId(), childEnter.traceId());
        assertEquals(parent.spanId(), childEnter.parentSpanId());
    }
}
