package dev.tracekit.spring;

import dev.tracekit.core.TraceEngine;
import dev.tracekit.core.Traced;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TracedMethodInterceptor implements MethodInterceptor {
    private static final String[] NONE = new String[0];
    private final TraceEngine engine;
    private final ParameterNameDiscoverer names = new DefaultParameterNameDiscoverer();
    private final Map<Method, String[]> nameCache = new ConcurrentHashMap<>();

    private final List<String> include, exclude;

    public TracedMethodInterceptor(TraceEngine engine) { this(engine, List.of(), List.of()); }

    public TracedMethodInterceptor(TraceEngine engine, List<String> include, List<String> exclude) {
        this.engine = engine; this.include = include; this.exclude = exclude;
    }

    private boolean accepts(Class<?> c) {
        String n = c.getName();
        for (String x : exclude) if (n.startsWith(x)) return false;
        if (include.isEmpty()) return true;
        for (String i : include) if (n.startsWith(i)) return true;
        return false;
    }

    @Override public Object invoke(MethodInvocation inv) throws Throwable {
        Method m = inv.getMethod();
        Class<?> target = inv.getThis() != null ? AopUtils.getTargetClass(inv.getThis()) : m.getDeclaringClass();
        if (!accepts(target)) return inv.proceed();
        Traced t = AnnotatedElementUtils.findMergedAnnotation(m, Traced.class);
        if (t == null) t = AnnotatedElementUtils.findMergedAnnotation(target, Traced.class);
        boolean args = t == null || t.args();
        boolean res = (t == null || t.result()) && m.getReturnType() != void.class;
        String[] pn = nameCache.computeIfAbsent(m, k -> { String[] n = names.getParameterNames(k); return n == null ? NONE : n; });

        TraceEngine.Frame f = engine.enter(target.getSimpleName() + "#" + m.getName(), pn, inv.getArguments(), args);
        try {
            Object r = inv.proceed();
            engine.exitOk(f, r, res);
            return r;
        } catch (Throwable e) {
            engine.exitError(f, e);
            throw e;
        }
    }
}
