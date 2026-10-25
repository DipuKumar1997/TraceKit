package dev.tracekit.spring;

import dev.tracekit.core.DefaultRedactor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "tracekit")
public class TraceKitProperties {
    /** Master switch. */
    private boolean enabled = true;
    /** Output format: console or json. */
    private String format = "console";
    /** SLF4J level used by the default sink. */
    private String level = "INFO";
    private int maxStringLength = 500;
    private int maxCollectionSize = 10;
    private int maxDepth = 3;
    /** Call toString() on application objects. Off by default (side effects, cost, leaks). */
    private boolean useToString = false;
    /** Case-insensitive substrings marking names/keys as sensitive. */
    /** If non-empty, only classes whose name starts with one of these prefixes are traced. */
    private List<String> includePackages = new ArrayList<>();
    /** Classes whose name starts with one of these prefixes are never traced. */
    private List<String> excludePackages = new ArrayList<>();
    /** Fraction (0..1) of root calls traced. */
    private double sampleRate = 1.0;
    /** If &gt; 0: only report calls at least this slow (ms), plus errors. */
    private long slowThresholdMs = 0;
    private List<String> redactKeys = new ArrayList<>(DefaultRedactor.DEFAULT_KEYS);

    public boolean isEnabled() { return enabled; } public void setEnabled(boolean v) { enabled = v; }
    public String getFormat() { return format; } public void setFormat(String v) { format = v; }
    public String getLevel() { return level; } public void setLevel(String v) { level = v; }
    public int getMaxStringLength() { return maxStringLength; } public void setMaxStringLength(int v) { maxStringLength = v; }
    public int getMaxCollectionSize() { return maxCollectionSize; } public void setMaxCollectionSize(int v) { maxCollectionSize = v; }
    public int getMaxDepth() { return maxDepth; } public void setMaxDepth(int v) { maxDepth = v; }
    public boolean isUseToString() { return useToString; } public void setUseToString(boolean v) { useToString = v; }
    public List<String> getRedactKeys() { return redactKeys; } public void setRedactKeys(List<String> v) { redactKeys = v; }
    public List<String> getIncludePackages() { return includePackages; } public void setIncludePackages(List<String> v) { includePackages = v; }
    public List<String> getExcludePackages() { return excludePackages; } public void setExcludePackages(List<String> v) { excludePackages = v; }
    public double getSampleRate() { return sampleRate; } public void setSampleRate(double v) { sampleRate = v; }
    public long getSlowThresholdMs() { return slowThresholdMs; } public void setSlowThresholdMs(long v) { slowThresholdMs = v; }
}
