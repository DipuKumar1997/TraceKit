package dev.tracekit.core;

/** Sensitive-data SPI. */
public interface Redactor {
    boolean isSensitiveName(String name);
    String redactText(String text);
}
