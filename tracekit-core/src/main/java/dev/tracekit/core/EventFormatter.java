package dev.tracekit.core;

/** Turns an event into text. Kept separate from event collection. */
public interface EventFormatter { String format(TraceEvent event); }
