# Architecture Decision Records
- ADR-001 Annotation is `@Traced` (not `@Logs`/`@Trace`): descriptive, avoids APM-vendor collisions. Hard to change after release.
- ADR-002 Core has no Spring dependency; Spring AOP via `MethodInterceptor` + `Advisor` (no aspectjweaver required).
- ADR-003 groupId `dev.tracekit` is a PLACEHOLDER. Verify namespace ownership (Maven Central Portal) before publishing; rename packages then, not later.
- ADR-004 Values are rendered to bounded, redacted strings synchronously on the caller thread, before any async hand-off.
- ADR-005 `toString()` on application types is off by default.
- ADR-006 Local-variable tracing = separate optional agent (Byte Buddy + ASM visitor), Milestone 3. Not included yet.
- ADR-007 Java 17 baseline, Spring Boot 3.3.x tested. Boot 4 unverified.
