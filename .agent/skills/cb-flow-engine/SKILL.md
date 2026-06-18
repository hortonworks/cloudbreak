---
name: cb-flow-engine
description: Add or modify a Cloudbreak flow — state machine config, states/events enums, actions, handlers, and flow chains — without deadlocking the engine. Use when implementing any long-running async operation (provisioning, upgrade, repair, scaling, rotation) in core/datalake/environment/freeipa/redbeams.
---

# Cloudbreak Flow Engine — adding a flow

The flow engine (Spring StateMachine + Reactor event bus) drives every long-running operation. The reference concepts live in **`flow/AGENTS.md`**; this skill is the **step-by-step procedure** for adding one correctly, plus the mistakes reviewers catch most.

A flow is a state machine. **Actions** run on entering a state (short, admin-only — DB update + notify), then dispatch a work event to a **Handler** (long-running: cloud calls, polling). The handler emits a `*_FINISHED`/`*_FAILED` event that drives the next transition. Nothing blocks the caller thread.

## Package layout & naming (follow exactly)

Flows live under `<module>/src/main/java/.../<feature>/flow/<feature_name>/`. Use a real example as a template: **`environment/.../flow/upgrade/ccm/`** (`UpgradeCcm*`).

```
<feature>/flow/<name>/
├── config/<Name>FlowConfig.java        // @Component, extends AbstractFlowConfiguration, declares transitions
├── event/
│   ├── <Name>Event.java                // payload, extends BaseNamedFlowEvent (or BaseFlowEvent/StackEvent)
│   ├── <Name>FailedEvent.java          // extends BaseFailedFlowEvent
│   ├── <Name>StateSelectors.java       // enum implements FlowEvent — transition triggers
│   └── <Name>HandlerSelectors.java     // enum implements FlowEvent — routes actions → handlers
├── handler/<Step>Handler.java          // @Component, extends ExceptionCatcherEventHandler
├── <Name>State.java                    // enum implements FlowState
├── Abstract<Name>Action.java           // abstract, extends AbstractAction
└── <Name>Actions.java                  // @Configuration, one @Bean(name="STATE") Action per state
```

Naming is mechanical and reviewers expect it: state `FOO_STATE` ↔ trigger `FOO_EVENT` ↔ handler selector `FOO_HANDLER`. Config = `<Name>FlowConfig`, actions container = `<Name>Actions`, base action = `Abstract<Name>Action`.

## Procedure

1. **State enum** (`implements FlowState`): list `INIT_STATE`, one state per step, a `*_FINISHED_STATE`, a `*_FAILED_STATE`, `FINAL_STATE`. Implement `restartAction()` returning the module's fill-in-memory-state-store restart action so the flow survives an app restart.

2. **Two event enums** (`implements FlowEvent`, `event()` returns `name()`):
   - `<Name>StateSelectors` — transition triggers including `FINISH_*`, `FINALIZE_*`, `FAILED_*`, `HANDLED_FAILED_*`.
   - `<Name>HandlerSelectors` — one entry per async work unit (action → handler routing).

3. **Event classes:** payload extends `BaseNamedFlowEvent` (carry `resourceId`, `resourceName`, `resourceCrn`); use a builder + `@JsonDeserialize`/`@JsonCreator` so events survive serialization across a restart. Failed event extends `BaseFailedFlowEvent`. Use the **right base** — `StackEvent` not raw `BaseFlowEvent` where a stack id is in play.

4. **FlowConfig** (`@Component extends AbstractFlowConfiguration<State, Selectors>`): declare transitions with `new Transition.Builder<>().defaultFailureEvent(FAILED_*).from(A).to(B).event(A_TO_B_EVENT)...build()` and `getEdgeConfig()` → `new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, <FAILED>_STATE, HANDLED_FAILED_*)`. Implement `RetryableFlowConfiguration` if the failure state should be retryable.

5. **Actions** (`<Name>Actions` is `@Configuration`; one `@Bean(name = "FOO_STATE")` per state, each `new Abstract<Name>Action<>(Payload.class){ doExecute(...) }`): in `doExecute` do **only** admin work (status update + user notification) then `sendEvent(context, FOO_HANDLER.selector(), payload)`. Keep it short — no cloud calls here.

6. **Handlers** (`@Component extends ExceptionCatcherEventHandler<Payload>`): see below — this is where the bugs are.

7. **Register & verify:** every dispatched selector needs a `@Component` handler or the flow **waits forever**. Then generate graphs (step at the end) and add a unit/integration test.

## Handlers: use `ExceptionCatcherEventHandler` (not the deprecated one)

**`EventSenderAwareHandler` is deprecated** — older flows (incl. the UpgradeCcm example) still use it, but new handlers MUST extend `ExceptionCatcherEventHandler<T>`. The difference is the whole point:

```java
@Component
public class ValidateFooHandler extends ExceptionCatcherEventHandler<FooEvent> {
    @Override public String selector() { return FOO_VALIDATION_HANDLER.name(); }

    @Override                       // RETURN the next event — framework sends it for you
    protected Selectable doAccept(HandlerEvent<FooEvent> event) {
        FooEvent data = event.getData();
        // ... long-running work (cloud calls, polling) ...
        return FooEvent.builder().withSelector(FOO_NEXT_EVENT.selector())
            .withResourceCrn(data.getResourceCrn()).withResourceId(data.getResourceId()).build();
    }

    @Override                       // any exception in doAccept lands here automatically
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<FooEvent> event) {
        return new FooFailedEvent(event.getData(), e, FooStatus.FOO_VALIDATION_FAILED);
    }
}
```

You **return** the next `Selectable`; the framework sends it and routes any thrown exception to `defaultFailureEvent`. Do **not** hand-roll `try/catch` + `eventSender().sendEvent(...)` (that's the deprecated pattern and it lets exceptions get masked).

## Failure-mode checklist (what reviewers block on)

- **Deadlock:** every event you dispatch has a matching `@Component` handler whose `selector()` returns it, and every state has an outgoing transition. A missing handler/transition = flow stuck forever.
- **The failure state needs its own outgoing transition.** A state exists in the machine only if it's the source or target of some transition. `FlowEdgeConfig` auto-registers the `failureState` as the *target* of failure transitions, but you must still declare its *outgoing* edge to `FINAL_STATE` or the flow hangs after failing:
  ```java
  .from(MY_FAILURE_STATE).to(FINAL_STATE).event(FAILURE_HANDLED_EVENT).noFailureEvent()
  ```
  In `new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, failureState, failureHandledEvent)`, the 4th arg is the event that drives `failureState → FINAL_STATE` — and that transition must appear in the `TRANSITIONS` list.
- **Decouple handlers from the next state:** a handler should emit a generic `*_FINISHED_EVENT`; only the `FlowConfig` decides what comes next. Don't hardcode the next *state* in a handler — adding a step shouldn't force handler edits.
- **Actually fail the flow** on unrecoverable errors — return a proper `*FailedEvent`; don't swallow and continue. Add a real `DetailedStackStatus`/domain status (e.g. `FOO_FAILED(Status.AVAILABLE)`) and update it in the action.
- **Idempotent reruns:** if the flow restarts mid-way (pod eviction, app restart) the state may re-run — make each handler safe to execute twice (don't re-create what exists, tolerate already-done).
- **Flowchain failure propagates:** in a flow chain, a failing flow must fail the whole chain; if it doesn't, that's a framework-level bug, not something to work around.
- **Don't match state by name suffix** — drive transitions from the config/edges, not string parsing of state names.
- **Carry the flow identifier** through poller/attempt results so retries and logging stay correlated.
- **Logging:** new flows/handlers need happy-path logs (what resource/params) and honest failure logs with MDC `resourceCrn` — reviewers ask for these every time.

## Failure cleanup before the final failure state

When a failure needs long-running rollback (delete a half-created LB, release cloud resources) before the flow ends, route to an **intermediate cleanup state** instead of straight to the failed state — set that cleanup state as the `failureState` in `FlowEdgeConfig`. Then:

1. The cleanup **action** dispatches a request to a **handler** (cloud calls belong in handlers, never actions).
2. The cleanup handler must **never** emit the flow's `defaultFailureEvent` — `FlowEdgeConfig` routes that event back to the same cleanup state, creating a self-loop. Always emit a `*_COMPLETE` event (even on cleanup failure), log the error, and let the flow proceed to the final failed state.
3. Override `getFailurePayload(payload, flowContext, ex)` in the cleanup action to emit that same `*_COMPLETE` event — a safety net if the action itself throws.
4. Pass the original exception forward via a flow variable so the final failed-state action can report it.

## Conditional branching & flow variables

To skip states (e.g. skip a cloud step on a platform where it doesn't apply): in the action's `doExecute(...)`, check the condition and `sendEvent` a *different* event that has its own transition to the later state. Communicate decisions downstream through the `variables` map that every action receives — `variables.put("TEST_RESOURCE_CREATED", true)` in one action, `variables.get(...)` in a later one — rather than re-deriving them.

## Flow chains

To run flows in sequence, implement `FlowEventChainFactory<TriggerEvent>` (`@Component`): `initEvent()` returns the chain trigger; `createFlowTriggerEventQueue(event)` returns a `FlowTriggerEventQueue` holding an ordered `Queue<Selectable>` of each flow's trigger. See `core/.../SetDefaultJavaVersionFlowChainFactory`.

## Verify the flow

- **Graphs:** `make generate-flow-graphs` (and `make generate-flow-graph-pictures`) regenerate DOT graphs per module via each module's `FlowOfflineStateGraphGenerator`. Run after adding/changing a flow and commit the updated graph.
- **Chain test:** in a `*FlowEventChainFactoryTest`, call `FlowChainConfigGraphGeneratorUtil.generateFor(underTest, "com.sequenceiq.flow.config", queue, "SCENARIO")` to validate the chain wiring (see `UpscaleFlowEventChainFactoryTest`).
- **Integration test:** drive the real state machine with `@ActiveProfiles("integration-test")` + a `@TestConfiguration` importing your `Actions`/`FlowConfig`/handlers, mocking external services with `@MockitoBean` (the deprecated `@MockBean` is being phased out). **Trigger** via `flowManager.notify(event.selector(), event)` inside `ThreadBasedUserCrnProvider.doAs(...)`; **wait** by polling `flowRegister.get(flowIdentifier.getPollableId())` until null; **assert** mock interactions with `InOrder` and that `flowRegister.getRunningFlowIds().isEmpty()`. Gotchas: `Resource` entities need non-null attributes (`resource.setAttributes(new Json("{}"))`) for `ResourceToCloudResourceConverter`, and `NodeValidator.checkForRecentHeartbeat()` runs on trigger so stub it with `doNothing()`. See `UpgradeCcmFlowIntegrationTest` and **cb-testing**.
