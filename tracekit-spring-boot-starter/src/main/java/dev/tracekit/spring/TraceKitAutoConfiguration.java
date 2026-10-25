package dev.tracekit.spring;

import dev.tracekit.core.*;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.core.task.TaskDecorator;

@AutoConfiguration(afterName = "org.springframework.boot.autoconfigure.aop.AopAutoConfiguration")
@ConditionalOnProperty(prefix = "tracekit", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(TraceKitProperties.class)
public class TraceKitAutoConfiguration {

    @Bean @ConditionalOnMissingBean
    public Redactor traceKitRedactor(TraceKitProperties p) { return new DefaultRedactor(p.getRedactKeys()); }

    @Bean @ConditionalOnMissingBean
    public ValueRenderer traceKitValueRenderer(TraceKitProperties p, Redactor r) {
        return new ValueRenderer(r, p.getMaxStringLength(), p.getMaxCollectionSize(), p.getMaxDepth(), p.isUseToString());
    }

    @Bean @ConditionalOnMissingBean
    public EventFormatter traceKitFormatter(TraceKitProperties p) {
        return "json".equalsIgnoreCase(p.getFormat()) ? new JsonFormatter() : new ConsoleFormatter();
    }

    @Bean @ConditionalOnMissingBean(TraceSink.class)
    public TraceSink traceKitSink(EventFormatter f, TraceKitProperties p) { return new Slf4jSink("tracekit", f, p.getLevel()); }

    @Bean @ConditionalOnMissingBean
    public TraceEngine traceKitEngine(ValueRenderer v, Redactor r, ObjectProvider<TraceSink> sinks, TraceKitProperties p) {
        return new TraceEngine(v, r, sinks.orderedStream().toList(), p.getSampleRate(), p.getSlowThresholdMs() * 1_000_000L);
    }

    @Bean @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public Advisor traceKitAdvisor(TraceEngine engine, TraceKitProperties p) {
        ComposablePointcut pc = new ComposablePointcut(new AnnotationMatchingPointcut(Traced.class, true))
                .union(AnnotationMatchingPointcut.forMethodAnnotation(Traced.class));
        return new DefaultPointcutAdvisor(pc, new TracedMethodInterceptor(engine, p.getIncludePackages(), p.getExcludePackages()));
    }

    /** Propagates trace context into Spring @Async / task executors. */
    @Bean @ConditionalOnMissingBean(TaskDecorator.class)
    public TaskDecorator traceKitTaskDecorator(TraceEngine engine) { return engine::wrap; }
}
