# Contributing
1. Fork, create a branch, run `mvn clean verify` (Java 17+).
2. Keep the public API small: anything public in `dev.tracekit.core` is a compatibility promise.
3. Add tests for every behaviour change; never log real secrets in tests.
4. Describe user-visible changes in CHANGELOG.md. Versioning follows SemVer.
