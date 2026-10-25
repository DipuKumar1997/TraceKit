package dev.tracekit.core;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

/** Framework-independent tracing engine. Tracing failures never propagate to the application. */
public final class TraceEngine {
    public record Frame(String traceId, String spanId, String parentSpanId, int depth, String method,
                        long startNanos, boolean sampled) {}

    private static final ThreadLocal<Deque<Frame>> STACK = ThreadLocal.withInitial(ArrayDeque::new);
    private final ValueRenderer renderer;
    private final Redactor redactor;
    private final List<TraceSink> sinks;
    private final double sampleRate;
    private final long slowThresholdNanos;

    public TraceEngine(ValueRenderer renderer, Redactor redactor, List<TraceSink> sinks) {
        this(renderer, redactor, sinks, 1.0, 0);
    }

    /**
     * @param sampleRate 0..1, decided once per root call; children inherit the decision
     * @param slowThresholdNanos if &gt; 0, only EXIT events of calls at least this slow (and errors) are emitted
     */
    public TraceEngine(ValueRenderer renderer, Redactor redactor, List<TraceSink> sinks,
                       double sampleRate, long slowThresholdNanos) {
        this.renderer = renderer; this.redactor = redactor; this.sinks = List.copyOf(sinks);
        this.sampleRate = sampleRate; this.slowThresholdNanos = slowThresholdNanos;
    }

    public Frame enter(String method, String[] names, Object[] args, boolean withArgs) {
        Deque<Frame> st = STACK.get();
        Frame parent = st.peek();
        boolean sampled = parent != null ? parent.sampled()
                : sampleRate >= 1.0 || (sampleRate > 0 && ThreadLocalRandom.current().nextDouble() < sampleRate);
        Frame f = new Frame(parent == null ? id() : parent.traceId(), id(),
                parent == null ? null : parent.spanId(), parent == null ? 0 : parent.depth() + 1,
                method, System.nanoTime(), sampled);
        st.push(f);
        if (!sampled || slowThresholdNanos > 0) return f;
        List<String> rendered = List.of();
        try { if (withArgs && args != null) rendered = renderArgs(names, args); } catch (RuntimeException ignored) { }
        emit(event(TraceEvent.Type.ENTER, f, rendered, null, null, 0));
        return f;
    }

    public void exitOk(Frame f, Object result, boolean withResult) {
        pop(f);
        if (!f.sampled()) return;
        long dur = System.nanoTime() - f.startNanos();
        if (slowThresholdNanos > 0 && dur < slowThresholdNanos) return;
        String r = null;
        try { if (withResult) r = renderer.render(result); } catch (RuntimeException ignored) { }
        emit(event(TraceEvent.Type.EXIT, f, List.of(), r, null, dur));
    }

    public void exitError(Frame f, Throwable t) {
        pop(f);
        if (!f.sampled()) return;
        String msg = t.getClass().getName() + (t.getMessage() == null ? "" : ": " + t.getMessage());
        String err;
        try { err = renderer.text(msg); } catch (RuntimeException e) { err = t.getClass().getName(); }
        emit(event(TraceEvent.Type.ERROR, f, List.of(), null, err, System.nanoTime() - f.startNanos()));
    }

    /** Capture the current trace context so work run on another thread nests under the current call. */
    public Runnable wrap(Runnable task) {
        Frame ctx = STACK.get().peek();
        if (ctx == null) return task;
        return () -> { push(ctx); try { task.run(); } finally { pop(ctx); } };
    }

    public <V> Callable<V> wrap(Callable<V> task) {
        Frame ctx = STACK.get().peek();
        if (ctx == null) return task;
        return () -> { push(ctx); try { return task.call(); } finally { pop(ctx); } };
    }

    private static void push(Frame f) { STACK.get().push(f); }

    private List<String> renderArgs(String[] names, Object[] args) {
        List<String> out = new ArrayList<>(args.length);
        for (int i = 0; i < args.length; i++) {
            String n = names != null && i < names.length && names[i] != null ? names[i] : "arg" + i;
            out.add(n + "=" + (redactor.isSensitiveName(n) ? "***" : renderer.render(args[i])));
        }
        return out;
    }

    private TraceEvent event(TraceEvent.Type t, Frame f, List<String> args, String res, String err, long dur) {
        return new TraceEvent(t, Instant.now(), f.traceId(), f.spanId(), f.parentSpanId(), f.depth(),
                Thread.currentThread().getName(), f.method(), args, res, err, dur);
    }

    private void emit(TraceEvent e) {
        for (TraceSink s : sinks) { try { s.accept(e); } catch (RuntimeException ignored) { } }
    }

    private static void pop(Frame f) {
        Deque<Frame> st = STACK.get();
        st.remove(f);
        if (st.isEmpty()) STACK.remove();
    }

    private static String id() { return String.format("%016x", ThreadLocalRandom.current().nextLong()); }
}
