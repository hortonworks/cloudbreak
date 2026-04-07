---
name: cb-ci-local
description: Local build and test ergonomics for Cloudbreak—Gradle commands, module-scoped tests, checkstyle, and how they relate to GitHub Actions PR workflows.
---

# Cloudbreak CI (Local)

Use this skill to **mirror PR CI locally** before push, or to debug failures quickly. Java **21**, **Gradle**, **Spring Boot 3.3** per root `GEMINI.md`.

## Commands that cover most PR feedback

| Action | Command |
|--------|---------|
| Clean build (full, heavy) | `./gradlew clean build` |
| **Module unit tests** (preferred during dev) | `./gradlew :<module>:test` |
| Single test class | `./gradlew :<module>:test --tests com.example.FullClassName` |
| Single test method | `./gradlew :<module>:test --tests com.example.ClassName.methodName` |
| Checkstyle (when enabled for module) | `./gradlew :<module>:checkstyleMain` and/or `checkstyleTest` |

Replace `<module>` with the Gradle project name (directory name for most modules: `core`, `datalake`, `environment`, `integration-test`, etc.).

## Module discovery

- **Domain map**: root `GEMINI.md` (“Logical Domain Mapping”).
- **Deep context**: read `<module>/GEMINI.md` when present before large test runs.

## Checkstyle

- Root config: **`config/checkstyle/checkstyle.xml`** (applied from root `build.gradle`).
- Not every submodule opts in the same way; if `./gradlew :foo:checkstyleMain` is not found, run **`./gradlew tasks --group=verification`** in that module or use root aggregate tasks if your branch defines them.

## Relationship to GitHub Actions

- **`.github/workflows/pull-request.yaml`** runs on **`pull_request`** and includes **Jira update** and **unit test** jobs (and others as defined in the file).
- Local **`./gradlew :<module>:test`** is the fastest way to match **“unit test failed in CI”** for a specific module before re-pushing.

## Integration tests

- The **`integration-test`** module is separate from per-module unit tests; it has its own README and setup (see **`integration-test/README.md`** for local env, UMS, and long-running scenarios).
- Do not assume `integration-test` runs in every developer’s default loop unless the ticket requires it.

## Troubleshooting (short)

- **Flaky test** — Re-run with `--tests` narrowed; check for timing, shared state, or missing mocks in the failing module’s `src/test/java`.
- **OutOfMemoryError** — Increase Gradle JVM args only if needed (`org.gradle.jvmargs` in `gradle.properties`); prefer fixing leaks or oversized fixtures.
- **Dependency resolution** — Internal artifacts may require **`README.md` / `README-devnotes.md`** credentials in `~/.gradle/gradle.properties`.

## Pairing

- **`cb-junior-dev`** — Repro test first; then full **`:module:test`** before PR.
- **`cb-developer`** — After tests pass, run janitorial checks if you touched style-sensitive areas.
