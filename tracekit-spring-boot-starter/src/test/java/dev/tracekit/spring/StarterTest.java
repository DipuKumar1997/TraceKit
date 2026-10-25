package dev.tracekit.spring;

import dev.tracekit.core.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.*;

class StarterTest {
    static final List<TraceEvent> EVENTS = new CopyOnWriteArrayList<>();

    public static class Svc {
        @Traced public int add(int a, int b) { return a + b; }
        @Traced public void login(String user, String password) { }
        @Traced public void fail() { throw new IllegalStateException("boom"); }
        public int untraced() { return 1; }
    }

    @Configuration(proxyBeanMethods = false)
    static class Cfg {
        @Bean Svc svc() { return new Svc(); }
        @Bean TraceSink sink() { return EVENTS::add; }
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AopAutoConfiguration.class, TraceKitAutoConfiguration.class))
            .withUserConfiguration(Cfg.class);

    @Test void tracesArgsAndResult() {
        EVENTS.clear();
        runner.run(ctx -> {
            assertThat(ctx.getBean(Svc.class).add(2, 3)).isEqualTo(5);
            assertThat(EVENTS).hasSize(2);
            assertThat(EVENTS.get(0).args()).containsExactly("a=2", "b=3");
            assertThat(EVENTS.get(1).result()).isEqualTo("5");
        });
    }

    @Test void redactsPasswordArgument() {
        EVENTS.clear();
        runner.run(ctx -> {
            ctx.getBean(Svc.class).login("bob", "hunter2");
            assertThat(EVENTS.get(0).args()).containsExactly("user=bob", "password=***");
        });
    }

    @Test void reportsAndRethrowsExceptions() {
        EVENTS.clear();
        runner.run(ctx -> {
            assertThatThrownBy(() -> ctx.getBean(Svc.class).fail()).isInstanceOf(IllegalStateException.class);
            assertThat(EVENTS.get(1).type()).isEqualTo(TraceEvent.Type.ERROR);
        });
    }

    @Test void untracedMethodsProduceNoEvents() {
        EVENTS.clear();
        runner.run(ctx -> { ctx.getBean(Svc.class).untraced(); assertThat(EVENTS).isEmpty(); });
    }

    @Test void canBeDisabled() {
        EVENTS.clear();
        runner.withPropertyValues("tracekit.enabled=false").run(ctx -> {
            ctx.getBean(Svc.class).add(1, 1);
            assertThat(EVENTS).isEmpty();
        });
    }

    @Test void excludedPackagesAreNotTraced() {
        EVENTS.clear();
        runner.withPropertyValues("tracekit.exclude-packages=dev.tracekit.spring").run(ctx -> {
            ctx.getBean(Svc.class).add(1, 1);
            assertThat(EVENTS).isEmpty();
        });
    }

    @Test void includeListRestrictsTracing() {
        EVENTS.clear();
        runner.withPropertyValues("tracekit.include-packages=com.other").run(ctx -> {
            ctx.getBean(Svc.class).add(1, 1);
            assertThat(EVENTS).isEmpty();
        });
    }
}
