# Flow Module Mandates

This module handles asynchronous execution flows using Spring StateMachine and Reactor-based event handling. It is the core engine for long-running background tasks in Cloudbreak.

## 🧂 Core Concepts
- **FlowConfiguration**: Defines the state machine transitions, initial/final states, and error handling. Extends `AbstractFlowConfiguration`.
- **FlowState**: Enum representing a specific stage in the flow.
- **FlowEvent**: Enum representing triggers that drive transitions between states.
- **Action**: Logic executed when entering a state. Must be short-lived.
- **Handler**: Component that executes long-running tasks (e.g., API calls, polling). Extends `ExceptionCatcherEventHandler`.
- **Flow Chain**: Orchestrates multiple flows in a specific order with shared context.

## ⚖ Execution Rules
- **Short Actions**: `AbstractAction.doExecute` must remain short and perform only administrative tasks (DB updates, notifications).
- **Long-Running Tasks**: MUST be executed in **Handlers** triggered by reactor events.
- **Async Pattern**: 
    1. **Action** sends a trigger event to a **Handler**.
    2. **Handler** executes the long task.
    3. **Handler** sends back a success/failure event to transition the state machine to the next **State**.
- **Context**: Use `CommonContext` (or a subclass) to share data between actions in a flow.

## 📂 Key Entry Points
- **Restart Logic**: `com.sequenceiq.flow.core.ApplicationFlowInformation` defines which flows are restartable after app crashes.
- **Base Interfaces**: 
    - `com.sequenceiq.flow.core.FlowState`
    - `com.sequenceiq.flow.core.FlowEvent`
- **Configuration**: `com.sequenceiq.flow.core.config.AbstractFlowConfiguration`

## 📊 Observability & Tooling
- **Graph Generation**: Use `make generate-flow-graphs` and `make generate-flow-graph-pictures` to visualize flow states.
- **Unit Testing**: Use `FlowChainConfigGraphGeneratorUtil` in tests to verify flow chain orchestration and generate visual graphs for verification.
- **Visuals**: Flow graphs are stored in DOT format and can be converted to pictures for easier debugging.

## ⚠️ Common Pitfalls

### State Registration in the State Machine
A state only exists in the Spring StateMachine if it appears as a source or target in at least one **transition**. The `failureState` declared in `FlowEdgeConfig` is the target of auto-generated failure transitions, BUT if that failure state has no **outgoing** transition defined in the `TRANSITIONS` list, the flow will get stuck after entering it (it cannot proceed to `FINAL_STATE`). Always add an explicit transition:
```java
.from(MY_FAILURE_STATE).to(FINAL_STATE).event(FAILURE_HANDLED_EVENT).noFailureEvent()
```

### FlowEdgeConfig Semantics
```java
new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, failureState, failureHandledEvent)
```
- `failureState`: where `defaultFailureEvent()` routes on unhandled errors from any state.
- `failureHandledEvent`: the event that transitions FROM `failureState` TO `FINAL_STATE`. This transition must exist in the `TRANSITIONS` list.
- The `failureState` itself is only auto-registered as a **target** of failure transitions — you still need to define its outgoing edge.

### Failure Cleanup States (Intermediate Cleanup Before Final Failure)
When failure cleanup requires cloud API calls or other long-running work:
1. Route failures to an intermediate cleanup state (set it as `failureState` in `FlowEdgeConfig`).
2. The cleanup action dispatches a request to a **handler** (cloud calls belong in handlers, not actions).
3. The cleanup handler must **never** emit the flow's `defaultFailureEvent` — doing so creates a self-loop since `FlowEdgeConfig` routes that event back to the same cleanup state. Instead, always emit a "complete" event (even on failure), log the error, and let the flow proceed to the final failure state.
4. Override `getFailurePayload()` in the cleanup action to emit the same "complete" event — safety net if the action itself throws.
5. Pass the original exception via a flow variable so the downstream failed-state action can read it.

### Conditional Branching in Actions
To skip states (e.g., skip cloud operations for non-applicable platforms):
- In the action's `doExecute()`, check the condition and send a different event that has its own transition to a later state.
- Use flow variables (`setVariable`/`getVariable` on `FlowParameters`) to communicate decisions to downstream actions (e.g., `TEST_RESOURCE_CREATED = true`).

## 🧪 Flow Integration Testing

### Pattern
Use Spring context with real flow engine, mocked services:
```java
@ActiveProfiles("integration-test")
@ExtendWith(SpringExtension.class)
class MyFlowIntegrationTest {

    @Inject private FlowRegister flowRegister;
    @Inject private FreeIpaFlowManager flowManager; // or equivalent

    @MockitoBean private StackService stackService;
    // ... other mocked dependencies

    @Profile("integration-test")
    @TestConfiguration
    @Import({
        MyActions.class,
        MyHandler1.class, MyHandler2.class,
        MyFlowConfig.class,
        FlowIntegrationTestConfig.class,
        // converters/utils used by handlers
    })
    static class Config { }
}
```

### Key mechanics
- **Trigger**: call `flowManager.notify(event.selector(), event)` wrapped in `ThreadBasedUserCrnProvider.doAs(...)`.
- **Wait**: poll `flowRegister.get(flowIdentifier.getPollableId())` until null (flow finished) with a timeout loop.
- **Assert**: verify mock interactions in order (`InOrder`), check `flowRegister.getRunningFlowIds().isEmpty()`.

### Common test gotchas
- `Resource` entities passed through `ResourceToCloudResourceConverter` require non-null `attributes` — use `resource.setAttributes(new Json("{}"))`.
- Use `@MockitoBean` (from `org.springframework.test.context.bean.override.mockito`) instead of deprecated `@MockBean`.
- Mock `NodeValidator.checkForRecentHeartbeat()` with `doNothing()` — it runs on flow trigger.

## 🔗 Resources
- [TRAINING - Flow engine](https://cloudera.atlassian.net/wiki/spaces/ENG/pages/859504641/TRAINING+-+Flow+engine)
- [Spring StateMachine Project](https://projects.spring.io/spring-statemachine/)
