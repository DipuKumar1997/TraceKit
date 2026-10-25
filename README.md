# TraceKit (v0.1.0-SNAPSHOT)

Annotation-driven method tracing for Spring Boot: arguments, return values, exceptions, duration, nesting (trace/span ids),
with redaction and size limits. Local-first, no external services.

## Build & try
    mvn clean install            # builds + runs tests, installs to ~/.m2

In a consumer Spring Boot 3 app:

    <dependency>
      <groupId>dev.tracekit</groupId>
      <artifactId>tracekit-spring-boot-starter</artifactId>
      <version>0.1.0-SNAPSHOT</version>
    </dependency>

    @Service
    public class OrderService {
        @Traced
        public Order place(String user, String password, List<Item> items) { ... }
    }

Output (logger `tracekit`, INFO):

    [3f9a1c22/91ab] -> OrderService#place(user=bob, password=***, items=[Item@1b2, ...])
    [3f9a1c22/91ab] <- OrderService#place = Order@7c1 (2.114 ms)

## Properties (all optional)
| Property | Default |
|---|---|
| tracekit.enabled | true |
| tracekit.format | console (or json) |
| tracekit.level | INFO |
| tracekit.max-string-length | 500 |
| tracekit.max-collection-size | 10 |
| tracekit.max-depth | 3 |
| tracekit.use-to-string | false |
| tracekit.include-packages / exclude-packages | empty (prefix match on class name) |
| tracekit.sample-rate | 1.0 (per root call) |
| tracekit.slow-threshold-ms | 0 (off; >0 reports only slow calls + errors) |
| tracekit.redact-keys | password, secret, token, authorization, api-key, ... |

Custom extension: declare your own `TraceSink`, `EventFormatter`, `Redactor` or `ValueRenderer` bean; defaults back off.

## Limitations (by design of proxy-based AOP)
- Only Spring-managed beans; self-invocation (`this.foo()`), private and final methods are NOT traced.
- Nesting uses a ThreadLocal. Spring `@Async`/task executors are covered by the bundled `TaskDecorator`; hand-made threads need `engine.wrap(runnable)`.
- Parameter names need `-parameters` (Spring Boot parent enables it), else `arg0`, `arg1`.
- Redaction is best-effort pattern matching, not a compliance guarantee. Logging personal data has legal risk even locally.
- Local-variable tracing is NOT implemented (planned optional agent, Milestone 3).

## Try the demo
    mvn clean install
    cd examples/demo-app && mvn spring-boot:run
    curl 'localhost:8080/checkout?user=bob&password=hunter2'

## Publishing (see docs/ADR.md ADR-003)
Replace CHANGE_ME in the root pom, verify your groupId, push to GitHub, tag `v0.1.0`; JitPack builds from the tag (`jitpack.yml`).
Maven Central: verify namespace in the Central Portal, then `mvn -Prelease deploy` with credentials/GPG in your own `settings.xml`.

## Not done
Local-variable agent (Milestone 3), JMH benchmarks, OpenTelemetry bridge, Boot 4 compatibility testing.
