package dev.tracekit.core;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class DefaultRedactor implements Redactor {
    public static final List<String> DEFAULT_KEYS =
            List.of("password", "passwd", "secret", "token", "authorization", "apikey", "api-key", "api_key", "credential", "cookie");
    private static final Pattern BEARER = Pattern.compile("(?i)(bearer\\s+)[A-Za-z0-9._~+/=-]+");
    private final List<String> keys;
    private final Pattern kv;

    public DefaultRedactor(Collection<String> keys) {
        this.keys = keys.stream().map(k -> k.toLowerCase(Locale.ROOT)).toList();
        String alt = String.join("|", this.keys.stream().map(Pattern::quote).toList());
        this.kv = alt.isEmpty() ? null
                : Pattern.compile("(?i)(\\w*(?:" + alt + ")\\w*\\s*[=:]\\s*)(\"[^\"]*\"|[^\\s,;&})\\]]+)");
    }

    @Override public boolean isSensitiveName(String name) {
        if (name == null) return false;
        String n = name.toLowerCase(Locale.ROOT);
        for (String k : keys) if (n.contains(k)) return true;
        return false;
    }

    @Override public String redactText(String text) {
        String out = BEARER.matcher(text).replaceAll("$1***");
        return kv == null ? out : kv.matcher(out).replaceAll("$1***");
    }
}
