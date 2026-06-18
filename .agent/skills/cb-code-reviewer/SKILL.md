---
name: cb-code-reviewer
description: Review Cloudbreak pull requests the way the team actually reviews them, distilled from the team's real review history. Use when reviewing a PR/diff, or to self-review before requesting review.
---

# Cloudbreak Code Reviewer

A practical review playbook reflecting how the Cloudbreak team actually reviews on `cloudbreak/cloudbreak`. Apply it when reviewing a PR or self-reviewing before pushing. Reviews are written in English, predominantly phrased as questions. For the hard project mandates (commit format, `String` over `Enum`, `boolean` over `Boolean`, `jakarta.*`, import order) see root `AGENTS.md`; this skill is the *judgement* layered on top.

The dimensions reviewers raise most, in order: **reuse/duplication, refactoring, logging, exception-handling, removing unnecessary changes, and flow-engine correctness**.

## How to review

1. **Scope first.** Flag anything not needed for the ticket — removing unnecessary/unrelated changes is one of the most frequent comments. If the PR mixes a refactor (renames, reformatting, new-lines for params) with the feature, ask to split it into a separate PR.
2. **Go file by file** against the dimensions below.
3. **Phrase findings as questions/requests**, not commands — the house style leans heavily on questions. "Shouldn't we…", "Can we…", "Could you… please?", "Are you sure…?". Mark minor items `nit:`. Use ```` ```suggestion ```` blocks for concrete fixes.
4. **Separate blockers from nits.** Correctness bugs, auth holes, breaking changes, flow deadlocks, missing tests for new logic → request changes. Naming, log level, indentation → nit.
5. **Link, don't assert.** Point at the existing pattern with an exact `master` link (`https://github.infra.cloudera.com/cloudbreak/cloudbreak/blob/master/…#Lnn`) or the Jira ticket that explains the history.

## Review dimensions — grouped roughly by how often the team raises them

> Logging (dimension 7) is actually a top-3 theme — don't let its position here under-rate it; it's the single most common ask from some reviewers.

### 1. Reuse over duplication *(most frequent)*
- "This duplicates `com.sequenceiq.cloudbreak.util.VersionComparator`, please use it instead."
- "This is the same as `findByCrnAndDeletedIsNull(...)` — please use the existing method." / "use `findFirstByDatalakeIdIsOrderByIdDesc` instead."
- "I would use an existing constant: `…AwsPlatformResources#POSTGRES`." / "could be simplified using `AzureListResultFactory`."
- "Why fetch `saltSecurityConfig` again? We already have `stack.getSecurityConfig().getSaltSecurityConfig()` here." — reuse objects already in scope.
- Always scan repo/DAO methods, util classes, constants, and SPI/common helpers before accepting new bespoke logic.

### 2. Refactor / keep methods small / self-documenting
- "This method seems too long — can we refactor the two request instantiations into separate methods?" / "extract the try-catch into a separate method."
- Prefer **interface + per-type implementations over a huge `switch`-case** (mirror the `ResourceBuilders` pattern; inject all impls, dispatch by supported type).
- **Comments → extract a well-named method:** "Please remove these comments. If you feel a comment is necessary, consider extracting the logic into a method with a descriptive name instead."
- **No magic literals:** "could you place `selinux-supported` into a constant and use it everywhere?" / "`only` deserves a constant with a more descriptive name." Replace hardcoded strings/values (and magic strings duplicating a config property) with named constants.
- Throw a **specific** exception instead of a generic runtime so callers don't need `instanceof`.
- **Use `Optional` well, but don't over-use it:** prefer `Optional.ofNullable(x).map(...).orElse(...)` over `.get()`; but "Optional made this simple method hard to read — return a plain String / empty list" when there's no real absence to model.

### 3. Surgical diff — remove the unnecessary
- "Unnecessary change, please remove (and the related test changes too)." / "Please revert this change, it modifies the original logic." / "Please don't do this." / "not used, please remove."
- No committed commented-out code blocks. No gratuitous reordering or re-wrapping of parameters.

### 4. Exception & error handling
- Use the right base class: **`EventSenderAwareHandler` is deprecated → use `ExceptionCatcherEventHandler`.** With it, failing the flow is automatic — an exception (or the emitted failure event) is handled by the parent, so don't hand-roll `flow.setFlowFailed(...)`.
- "Shouldn't we handle the possible class-cast exception here?" / "throw an exception when the type is Unknown instead of silently continuing."
- **Exception masking:** a `finally`/wrapper that throws will swallow the original exception — wrap it. "Can we have a log here as the original exception will be wrapped?"

### 5. Validation & defensive checks
- **Reuse existing validators:** "DistroxService already has a `validate` method for this — why is it at the start of the controller method?" Don't scatter ad-hoc checks.
- **Empty/blank inputs:** "the regex `*` matches `""` and there's no `@Size(min=1)` — is an empty value valid? If not add `@NotEmpty`/`@Size(min=1)`."
- **Future-proof for internal callers:** internal services (e.g. COD via internal endpoints) drive Datahub-like workloads — keep validation working for them too.
- If a method returns a **default on empty/invalid input**, its name should say so.

### 6. Flow engine *(reviewed heavily — see `flow/AGENTS.md`)*
- **Decouple handlers from the next state:** handlers should emit a generic `…_FINISHED_EVENT`; only the `FlowConfiguration` should know the next state, so adding a state doesn't force handler edits.
- **Every dispatched event needs a `@Component` handler**, or the flow waits forever.
- **Right event types:** use `StackEvent`, not raw `BaseFlowEvent`; put handler request events under `/request`; add a proper `DetailedStackStatus` (e.g. `DISK_METADATA_SYNC_FAILED(Status.AVAILABLE)`).
- **Idempotency on rerun:** "if we rerun this state (e.g. pod eviction) after some instances were already updated, will the next run cause issues? Is this step/handler idempotent?"
- **Flowchain failure propagates:** "If a flow fails in a flowchain that should fail the whole flowchain — if it works differently that's a bug in the flow framework."
- Don't match flow state by **state-name suffix** — use the flow/edge config.
- Carry the flow identifier through poller/attempt results.

### 7. Logging & debuggability
- Require logs on new flows/handlers/jobs: "Can we have happy-path logs too — what resources/tag keys are updated?" / "info log with the calculated params for easier debugging."
- **Level discipline:** "INFO would be better", "WARN instead of debug".
- Honest messages: "this log is misleading — say it's a dry-run"; fix copy-paste (`cloudbreak` → `redbeams`, a Redbeams handler must not log a FreeIPA message).
- **Don't duplicate MDC in the message** — fields already in the MDC (e.g. `resourceCrn`) are on every line, so don't repeat them in the message text.

### 8. Naming & typos
- Clarity: "`Migration` could mean a lot of things — is this clear enough?"; map fields named `additionalParcelUrlsByParcelName`-style; rename a method when its name no longer matches what it does (e.g. `…ByCrns` → `…ByEnvCrn`).
- Typos in identifiers/messages get flagged every time (`retieve`→`retrieve`, `insternal`→`internal`).

### 9. Null-safety
- "Are you sure `volumeSet.getVolumes()` will never be null?"
- **NPE via auto-unboxing:** a nullable `Boolean` field unboxed into a `boolean` param → use `Boolean.TRUE.equals(x)`.
- Add null checks (`.getDefaultTags()`); know when a getter already throws so a check is redundant.

### 10. API & model design
- Request objects over long param lists: "could you use a request object, for future proof?"
- **Enums over bare booleans/strings:** "`syncResources(stack, false)` — false what? Use an enum `DRY_RUN`/`PERSIST` or two methods."
- `toString` on new DTOs; verify `@Schema` descriptions and whether API annotations are required; lift shared annotations to class level.

### 11. Concurrency & thread-safety
- **Concurrent flows:** "this won't work if multiple flows execute at the same time — it's not thread-safe." Shared/static mutable state and caches must tolerate parallel flow execution.
- Reuse thread infrastructure: "can't we extract this executor factory and reuse it? Only the thread-name prefix differs."
- Remove dead thread-local plumbing (e.g. an unused `ThreadBasedUserCrnProvider.getUserCrn()` propagated through events).
- (See also `ConcurrentModificationException` under Correctness bugs.)

### 12. Database & migrations
- Idempotent + reversible: `ADD COLUMN IF NOT EXISTS …`, include `@UNDO` (per `core/AGENTS.md`).
- Justify indexes: "What are the current indices? Why isn't `idx_…` sufficient?"
- Watch constraint/index renames other code still references by the old name.
- Prod awareness: "due to a previously reverted-but-executed SQL on production we can't take the safer multi-step approach."

### 13. Performance, scale & limits
- Bound batch endpoints: "Hundreds of CRNs could cause timeouts/resource exhaustion — add a max-size check (50/100) and `BadRequestException`."
- **Don't block controller/Tomcat threads** with long cloud calls — push to a flow; otherwise the client times out and the thread is locked.
- Avoid double O(n) scans; consider caching where provider data rarely changes.

### 14. Correctness bugs (always block)
- **Spring AOP proxy limits:** `@Retryable`/`@Transactional`/`@Cacheable` do **not** work on `private` or self-invoked methods.
- **`ConcurrentModificationException`:** mutating a list while iterating → `List.copyOf(...)`.
- **Inconsistent resource state** when a multi-step cloud op fails partway (attach ok, detach throws).
- **Wrong identifiers:** `vol.getDevice()` (Linux path) where a GCP disk name is expected → always 404.
- **Locale bugs:** bare `toLowerCase()` → `toLowerCase(Locale.ROOT)`; watch id collisions when stripping characters.

### 15. Security & authorization
- **Per-resource auth on batch endpoints:** use `@CheckPermissionByResourceCrnList`, not account-level `DESCRIBE`, or any user can reach any resource via the bulk path.
- **Authorization framework needs the CRN:** if `@CheckPermissionByResourceCrn` relies on a `@ResourceCrn`/`@ResourceName` parameter, confirm it's actually present and correct — "how can the framework authorize if the crn is missing?" Watch default/unregistered CRNs that aren't in UMS.
- **`@Cacheable` eviction keys:** if a method caches on `(secret, field)` but eviction keys only on `secret`, the cache won't evict correctly.
- **Don't write arbitrary customer input to resource attributes** — "super dangerous"; validate, and prefer a dry-run-first sync that computes intended updates.
- Don't expose internal DNS names / config to the public; such config shouldn't live in the Java class. Keep secrets in Vault/Pillars.

### 16. Breaking changes, rollout & follow-up
- Stricter validation is a breaking change: "this allowlist rejects prod configs with `!@#$` that the old denylist allowed — data audit, or release-note it?"
- **Gate behind rollout:** "Please disable the feature / periodic rotations in `application.yml` until the rollout plan is finalized."
- **Defer scope cleanly:** "Please create a Jira ticket to handle new/terminated resources" rather than expanding this PR.
- **User-facing flow notifications:** "What's missing: step-by-step progress events ('Creating snapshot…'), and a failure notification saying which step failed and what state the resource is in."

### 17. Formatting & PR hygiene (nits)
- Wrong YAML/`.sls`/`application.yml` indentation is flagged every time; fix pre-existing wrong indentation nearby when you touch it.
- Commit message & PR description quality: include the feature name (e.g. "Kraft migration"), keep description accurate/updated.
- New logic needs a unit test — frequently the sole condition on an approval ("LGTM, but please add UT coverage for the null/non-null branches"). See `.agent/WORKFLOW.md` (reproduction/UT first).

## Reviewer emphasis archetypes
Different senior reviewers weight different things; expect any of these lenses on a PR, so cover them all when self-reviewing:
- **The reuse/architecture lens** — existing constants & utils, magic literals → named constants, flow-engine correctness (`ExceptionCatcherEventHandler`, decoupled FINISHED events, idempotent reruns), refactoring, no commented-out code, DB indices.
- **The operability lens** — logging presence & levels, null-safety, controller-thread/layering, interface-over-switch refactors, splitting unrelated refactors into separate PRs, UT coverage.
- **The surgical/authz lens** — removing unnecessary/unrelated changes, reusing existing repo methods, **authorization** correctness, thread-safety, exact code placement (with `master` links), naming, indentation, rollout gating (`application.yml`), follow-up Jira tickets.
- **The correctness lens** — deep bugs (AOP proxies, CME, NPE, resource-state, exception masking), authorization holes, input-validation/security, breaking-change/limits, concrete ```suggestion``` fixes with file:line evidence.

## Verdict & phrasing
- **Request changes / block:** correctness bug, auth bypass, flow deadlock, unhandled breaking change, missing UT for new logic, feature not gated for rollout, wrong commit format.
- **Comment / nit:** naming, log level, indentation, refactor suggestions, "consider…".
- **Approve (LGTM):** the house approval is short — "LGTM", "LGTM, thanks". It's normal to **approve with a non-blocking ask** ("LGTM, but please add UT coverage…") or **approve with an FYI** ("I approved it, just letting you know it wasn't necessary"). Lead a changes-requested summary with what's good, then the asks ("Good improvements overall — a few items to address before merging").
