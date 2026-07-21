# PR workflow checks catalog

Workflow: `.github/workflows/pull-request.yaml` (`cloudbreak-pull-request-test`)

## Jobs

| Job | Action / script | Typical failure signal |
|-----|-----------------|------------------------|
| Jira Update | `.github/actions/pull-request/jira-update` | Missing/invalid `CB-XXXXX` in commits |
| Unit Test | `unit-test` → `unit-test-steps_gradle_build.sh` | JUnit failure; dirty tree after build |
| Unit Test (comment) | Jacoco PR comment in same action | Changed/overall coverage &lt; **55%** |
| Bootstrap Cert Filter Test | inline | `ImportCertsTest`, `Sync.java --check` drift |
| Checkstyle Main | `checkstyle-main` | `[ant:checkstyle]`, `checkstyleMain FAILED` |
| Checkstyle Test | `checkstyle-test` | Same, under `src/test` |
| Spotbugs Main | `spotbugs-main` | `SpotBugs`, bug pattern (e.g. `NP_NULL_ON_SOME_PATH`) |
| Spotbugs Test | `spotbugs-test` | Same, test scope |
| Swagger Compatibility | `swagger-compatiblity` | OpenAPI breaking change |
| Component Test | `component-test` | Testcontainers / component failure |
| Config Change Integration Test | `config-change-integration-test` | Config migration assertion |
| Schema Compatibility Test | `schema-compatibility-test` | DB schema compat (90m, large runner) |
| Integration Test | `integration-test` | Assertion, timeout, env setup |
| FedRAMP Integration Test | `fedramp-integration-test` | Same (FedRAMP env) |
| Real UMS Test | `real-ums-test` | UMS integration failure |
| Aws Test | `aws-test` | AWS integration failure |
| Auto Approve | `auto-approve` | Dependabot / cloudbreak-jenkins only (not a blocker) |

## Changed-module scoping (checkstyle / spotbugs only)

From `.github/actions/pull-request/changed-modules.sh`:

- Diff against merge-base with `origin/$BRANCH` (default `master`).
- Emits `:module:checkstyleMain` (or `spotbugs*`) per changed top-level Gradle module.
- **Full-repo fallback** when diff touches `build.gradle`, `settings.gradle`, `gradle.properties`, `dependencies.gradle`, or `config/checkstyle/` / `config/spotbugs/`.

Unit Test always runs **all** modules, not changed-modules scoped.

## Triage and local repro by failure type

### Unit test

**Log clues:** `There were failing tests`, `TEST-*.xml`, `java.lang.AssertionError`, `BUILD FAILED` after `:module:test`.

```bash
# Single test (from log)
./gradlew :core:test --tests 'com.example.FooTest' --no-daemon

# Match CI (all modules, no static analysis)
./gradlew -Penv=jenkins test jacocoTestReport \
  -x checkstyleMain -x checkstyleTest -x spotbugsMain -x spotbugsTest \
  --parallel --no-daemon
```

**Reports:** `module/build/test-results/test/`, `module/build/reports/tests/test/`

**Common fixes:** mock setup, wrong exception type, `EnforceAuthorizationAnnotationsTest`, uncommitted generated output (`git status` after build).

**CI command (exact):**

```bash
./gradlew -Penv=jenkins -b build.gradle \
  test jacocoTestReport \
  -x checkstyleMain -x checkstyleTest -x spotbugsMain -x spotbugsTest \
  --no-daemon --quiet --parallel \
  -Dorg.gradle.jvmargs="-Xmx4096m -XX:MaxMetaspaceSize=256m -XX:+HeapDumpOnOutOfMemoryError"
```

Post-step: fail if `git status --porcelain` is non-empty.

### Coverage (Jacoco on PR)

**Log clues:** PR comment *Cloudbreak code coverage report*; gates **55%** overall and on changed files (see `unit-test/action.yaml`).

```bash
./gradlew :core:test :core:jacocoTestReport --no-daemon
# open core/build/reports/jacoco/test/html/index.html
```

**Common fixes:** add tests for new branches; generated code already excluded in CI.

### Checkstyle

```bash
export BRANCH=master
source .github/actions/pull-request/changed-modules.sh
./gradlew $(changed_module_tasks checkstyleMain) --no-daemon   # or checkstyleTest
```

**Config:** `config/checkstyle/` — import order per root `AGENTS.md`.

### SpotBugs

```bash
export BRANCH=master
source .github/actions/pull-request/changed-modules.sh
./gradlew $(changed_module_tasks spotbugsMain) --no-daemon   # or spotbugsTest
```

**Reports:** `module/build/reports/spotbugs/main.html` (or `test`).

### Integration / AWS / FedRAMP / Real UMS / Schema / Config-change

Long runs, very large logs. **Pull one failed job at a time**, then search for the first real failure:

```bash
gh run view "$RUN_ID" --repo cloudbreak/cloudbreak --log-failed --job <job-id> 2>&1 \
  | rg -n 'FAILED|There were failing tests|AssertionError|java.lang.|BUILD FAILED|Tests run:' | head -40
```

**Log clues:** `Tests run:`, `<<< FAILURE!`, `There were failing tests`, `*IT` / `*Test` class names, timeout/OOM, env setup errors before tests run.

**Report:** job name + **test class and method** (or IT suite from YAML under `integration-test/src/main/resources/testsuites/`).

**Local repro:** run the specific IT class if logs name it; otherwise check the composite action script for the Gradle/suite invocation (e.g. `.github/actions/pull-request/integration-test/`).

**Flaky infra:** if only aws/fedramp jobs fail intermittently with env/timeout errors, note possible flake but still cite the assertion.

### Generated / dirty tree

**Log clue:** `There are local changes in git, which are autogenerated and not added in PR.`

Run the generating task locally and commit, or stop unintentional regeneration.

## Separate workflows (usually not PR blockers)

- `cloudbreak-coverage` (`.github/workflows/coverage.yaml`) — Sonar on push to master
- `integration-test-code-coverage.yaml` — integration coverage

Only triage these if the user points at them explicitly.
