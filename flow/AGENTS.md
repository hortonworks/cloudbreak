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

## 🔗 Resources
- [TRAINING - Flow engine](https://cloudera.atlassian.net/wiki/spaces/ENG/pages/859504641/TRAINING+-+Flow+engine)
- [Spring StateMachine Project](https://projects.spring.io/spring-statemachine/)
