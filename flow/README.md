# Flow

Cloudbreak Flow engine is responsible for manage the different execution flows of the cloudbreak's microservices using the Spring StateMachine project and
Reactor events.

* Sprint StateMachine: https://projects.spring.io/spring-statemachine/ Version: 1.0.1.RELEASE
* Project Reactor: https://projectreactor.io/docs Version: 2.0.7.RELEASE

If you would like to use the flow in your application, you have to implement the following interfaces:
- ApplicationFlowInformation
    - You have to define here the application's restartable flows, which will be restarted after your application restarted
    - You have to define here the application's parallel flows for a resource, which can be triggered even if other flows are running on that resource.
- The application's flow configurations

## Flow configuration:
You must implement the following interfaces and abstract classes per flow:
- FlowState: define the states of the flow
- FlowEvent: define the events between the flow states
- AbstractFlowConfiguration: configure the statemachine with its states and its transitions
  - have to define the flow transitions
    - source state
    - target state
    - event
    - failure state
    - failure event
  - have to define the FlowEdgeConfig
    - init state
    - final state
    - defaultFailureState: all the failure events from the states will lead to this state,
    - failHandled event)
- AbstractAction implementations for the different flow states.

You can implement the following interfaces for the flow to be more flexible:
- Create your own context inherit from CommonContext
- PayloadConverter which can be used to convert different payloads to the one which is appropriate for the state's action
- FlowTriggerCondition: you can implement your own flow trigger condition with which you can decide if a flow is triggerable for the specified resource or not

## General hints:
- The state's actions execution time recommended to be short: mainly administration purpose: database updates, notifications, etc.
- The state's actions triggers reactor handlers which executes long term operations, tasks.
- The reactor handlers sends back flow control events which will lead the flow to its next state
- Reactor handlers should send back the flow control event with the event headers copied from the handler's trigger event.

## General usage of a flow:
- Trigger the flow with its trigger event.
  - flow will be initialized, step into the init state and execute the action of it
  - on all the states
    - from the action the trigger event will be sent to its reactor handler
    - the handler will send the next flow control event: failure or success
    - in case of failure the general behavior is to handle the error and finish the flow
  - from the last state the finalize event will be sent automatically and the flow will be finished

## Example "Hello world" flow:
- HelloWordState: FlowState implementation
- HelloWorldEvent: FlowEvent implementation
- HelloWorldFlowConfig: flow configuration for the "Hello world" flow
- HelloWorldActions: Bean configurations for the abstract actions connected to the flow states
- HelloWorldContext: flow context for the "Hello world" flow
- Flow control event: HelloWorldFlowTriggerEvent, HelloWorldLongLastingTaskSuccessResponse, HelloWorldLongLastingTaskFailureResponse
- Reactor handler events: HelloWorldLongLAstingTaskTriggerEvent. Will send the flow control events with

## Restartable flows:

Documentation is under construction...

## Parallel states

Currently not supported, but spring statemachine supports fork and join states

## Distributed flows

Flows currently are not per state distributed. Spring statemachine has distributed implementation but not released yet: https://docs.spring.io/spring-statemachine/docs/2.1.3.RELEASE/reference/#sm-distributed
