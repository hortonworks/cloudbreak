# Flow

Cloudbreak Flow engine is responsible for manage the different execution flows of Cloudbreak's microservices using the Spring StateMachine project and
async event handling.

* Spring StateMachine: https://projects.spring.io/spring-statemachine/ Version: 1.0.1.RELEASE

If you would like to use the Flow engine in your application, you must implement the following interfaces:
- [`ApplicationFlowInformation`](flow/src/main/java/com/sequenceiq/flow/core/ApplicationFlowInformation.java)
    - You have to define here the application's _restartable_ flows, which will be restarted after your application is restarted
    - You have to define here the application's _parallel_ flows for a resource, which can be triggered even if other flows are running on that resource.
- The application's flow configurations.

## Flow configuration:
You must implement the following interfaces and abstract classes per flow:
- [`FlowState`](flow/src/main/java/com/sequenceiq/flow/core/FlowState.java): define the states of the flow
- [`FlowEvent`](flow/src/main/java/com/sequenceiq/flow/core/FlowEvent.java): define the events between the flow states
- [`AbstractFlowConfiguration`](flow/src/main/java/com/sequenceiq/flow/core/config/AbstractFlowConfiguration.java): configure the statemachine with its states and its transitions
  - have to define the flow transitions
    - source state
    - target state
    - event
    - failure state
    - failure event
  - have to define the `FlowEdgeConfig`
    - init state
    - final state
    - defaultFailureState: all the failure events from the states will lead to this state,
    - `failureHandled` event
- `AbstractAction` implementations for the different flow states.

You can implement the following interfaces for the flow to be more flexible:
- Create your own context, by extending `CommonContext`.
- Implementing `PayloadConverter` which can be used to convert different payloads to the one which is appropriate for the state's action.
- FlowTriggerCondition: you can implement your own flow trigger condition which can decide if a flow is triggerable for the specified resource or not.

## General hints:
- The state's actions execution time should be short and should mainly perform administrative actions: database updates, notifications, etc.
- The state's actions trigger _reactor handlers_ which execute long term operations or tasks.
- The reactor handlers send back flow control events which will transition the flow to its next state
- Reactor handlers should send back the flow control event with the event headers copied from the handler's trigger event.

## General usage of a flow:
- Trigger the flow with its trigger event.
  - The flow will be initialized, then step into the initial state, and execute its action.
  - For every `State` in the flow:
    - The `Action` associated with the state will send the trigger `Event` to its Reactor `Handler`.
    - The handler will send the next flow control event: failure or success
    - In case of failure the general behavior is to handle the error and finish the flow
  - The last state of the flow will automatically send a finalize event and the flow will be finished.

# Example "Hello world" flow:
## Flow states
Let's define flow states for our flow. You have to implement the *FlowState* interface and define your state enums.

We will have the following states:
* `INIT_STATE`
* `HELLO_WORLD_START_STATE`
* `HELLO_WORLD_FINISHED_STATE`
* `HELLO_WORLD_FAILED_STATE`
* `FINAL_STATE`

In code:
```java
public enum HelloWorldState implements FlowState {
   INIT_STATE,
   HELLO_WORLD_START_STATE,
   HELLO_WORLD_FINISHED_STATE,
   HELLO_WORLD_FAILED_STATE,
   FINAL_STATE
}
```

You can define restart actions for your flow, which will be used when you restart a failed flow. 

For example, in the [Data Lake service](datalake/src/main/java/com/sequenceiq/datalake) we are using `FillInMemoryStateStoreRestartAction`. 
It will update the data lake’s status in the `DatalakeInMemoryStateStore` to `CANCELLED` or `POLLABLE` based on the status. 
We are using this to cancel running flows. If you have a data lake install flow, and it is polling Cloudbreak for stack status, then if you send a terminate 
request we cancel the installation flow. It happens through this in-memory store. 

You can override `restartAction` this way:
```java
   @Override
   public Class<? extends RestartAction> restartAction() {
       return FillInMemoryStateStoreRestartAction.class;
   }
```

## Flow events
`FlowEvent`s are sent to the flow engine, which changes its state based on these events.
```java
public enum HelloWorldEvent implements FlowEvent {
   HELLOWORLD_TRIGGER_EVENT,
   HELLOWORLD_FINISHED_EVENT,
   FINALIZE_HELLOWORLD_EVENT,
   HELLOWORLD_SOMETHING_WENT_WRONG,
   HELLOWORLD_FAILHANDLED_EVENT;

   private String selector;

   HelloWorldEvent() {
       this.selector = name();
   }

   @Override
   public String event() {
       return selector;
   }
}
```

## Flow config
We have to define which state follows which state. To do that, we implement a Flow config, which extends from `AbstractFlowConfiguration`. 
If you want it to be retryable, then implement `RetryableFlowConfiguration`.

First define transition with `Transition.Builder` class. It is a generic class with two type. First type is the state enum, second type is the event enum. 
Define the default failure event, in our case it is `HELLOWORLD_SOMETHING_WENT_WRONG`. Then you can define state changes in your flow. Every state changes needs 
a from - to on which event and what event will be triggered on failure. It can be the `defaultFailureEvent` or you can define a specific failure event like 
`HELLOWORLD_FIRST_STEP_WENT_WRONG_EVENT` in the second step.

You have to define flow edge config, it will define what is your init and final state, the default failure state and the fail handled event. 
We need fail handled event for restart. If this event arrives it means we handled failure correctly so it can be restarted.

`getEvents` method should return your events, `getInitEvents` should return your initial event(s) and `getRetryableEvent` returns the event we can recover from.
```java
@Component
public class HelloWorldFlowConfig extends AbstractFlowConfiguration<HelloWorldState, HelloWorldEvent> implements RetryableFlowConfiguration<HelloWorldEvent> {
   private static final List<Transition<HelloWorldState, HelloWorldEvent>> TRANSITIONS = new Transition.Builder<HelloWorldState, HelloWorldEvent>()
           .defaultFailureEvent(HELLOWORLD_SOMETHING_WENT_WRONG)

           .from(INIT_STATE)
           .to(HELLO_WORLD_FIRST_STEP_STATE)
           .event(HELLOWORLD_TRIGGER_EVENT)
           .noFailureEvent()

           .from(HELLO_WORLD_FIRST_STEP_STATE)
           .to(HELLO_WORLD_SECOND_STEP_STATE)
           .event(HELLOWORLD_FIRST_STEP_FINISHED_EVENT)
           .failureEvent(HELLOWORLD_FIRST_STEP_WENT_WRONG_EVENT)

           .from(HELLO_WORLD_SECOND_STEP_STATE)
           .to(HELLO_WORLD_FINISHED_STATE)
           .event(HELLOWORLD_SECOND_STEP_FINISHED_EVENT)
           .defaultFailureEvent()

           .from(HELLO_WORLD_FINISHED_STATE)
           .to(FINAL_STATE)
           .event(FINALIZE_HELLOWORLD_EVENT)
           .defaultFailureEvent()
           .build();

   private static final FlowEdgeConfig<HelloWorldState, HelloWorldEvent> EDGE_CONFIG = new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, HELLO_WORLD_FAILED_STATE, HELLOWORLD_FAILHANDLED_EVENT);

   public HelloWorldFlowConfig() {
       super(HelloWorldState.class, HelloWorldEvent.class);
   }

   @Override
   public HelloWorldEvent[] getEvents() {
       return HelloWorldEvent.values();
   }

   @Override
   public HelloWorldEvent[] getInitEvents() {
       return new HelloWorldEvent[] {
               HELLOWORLD_TRIGGER_EVENT
       };
   }

   @Override
   public String getDisplayName() {
       return "Hello world";
   }

   @Override
   public HelloWorldEvent getRetryableEvent() {
       return HELLOWORLD_FAILHANDLED_EVENT;
   }

   @Override
   protected List<Transition<HelloWorldState, HelloWorldEvent>> getTransitions() {
       return TRANSITIONS;
   }

   @Override
   protected FlowEdgeConfig<HelloWorldState, HelloWorldEvent> getEdgeConfig() {
       return EDGE_CONFIG;
   }
}
```
## Actions
We will implement the actions for flow states. Let’s see first state `HELLO_WORLD_FIRST_STEP_STATE`. In flow config we have the following lines: 
```java
.from(INIT_STATE)
.to(HELLO_WORLD_FIRST_STEP_STATE)
.event(HELLOWORLD_TRIGGER_EVENT)
```
So flow will move from `INIT_STATE` to `HELLO_WORLD_FIRST_STEP_STATE` when `HELLOWORLD_TRIGGER_EVENT` arrives. This is how you can move from one state to another. 
As you can remember in `FlowEdgeConfig` we defined the start event for our new flow:  
```java
@Override
public HelloWorldEvent[] getInitEvents() {
   return new HelloWorldEvent[] {
           HELLOWORLD_TRIGGER_EVENT
   };
}
```
We will implement actions in `HelloWorldActions` class. You can see below we have a Bean with name `HELLO_WORLD_FIRST_STEP_STATE`. This is the first action in 
our flow. In doExecute you can do the implementation for this state. This time we just print a log message and send an 
`HelloWorldFirstStepLongLastingTaskTriggerEvent`, but why we do this? This is where handlers come into picture.
```java
@Configuration
public class HelloWorldActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(HelloWorldActions.class);

    @Bean(name = "HELLO_WORLD_FIRST_STEP_STATE")
    public Action<?, ?> firstStep() {
        return new AbstractHelloWorldAction<>(HelloWorldFlowTrigger.class) {
            @Override
            protected void doExecute(HelloWorldContext context, HelloWorldFlowTrigger payload, Map<Object, Object> variables) {
                LOGGER.info("Hello world first step in progress, we are sending an event for a handler, because it is a long lasting task");
                HelloWorldFirstStepLongLastingTaskTriggerEvent taskTriggerEvent = new HelloWorldFirstStepLongLastingTaskTriggerEvent(payload.getResourceId());
                sendEvent(context, taskTriggerEvent);
            }
        };
    }

    @Bean(name = "HELLO_WORLD_FIRST_STEP_FAILED_STATE")
    public Action<?, ?> firstStepFailedAction() {
        return new AbstractHelloWorldAction<>(HelloWorldFirstStepLongLastingTaskFailureResponse.class) {

            @Override
            protected void doExecute(HelloWorldContext context, HelloWorldFirstStepLongLastingTaskFailureResponse payload, Map<Object, Object> variables) {
                sendEvent(context, HELLOWORLD_FAILHANDLED_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "HELLO_WORLD_SECOND_STEP_STATE")
    public Action<?, ?> secondStep() {
        return new AbstractHelloWorldAction<>(HelloWorldFirstStepLongLastingTaskSuccessResponse.class) {
            @Override
            protected void doExecute(HelloWorldContext context, HelloWorldFirstStepLongLastingTaskSuccessResponse payload, Map<Object, Object> variables) {
                LOGGER.info("Hello world second step in progress..");
                HelloWorldSecondStepSuccessful helloWorldSecondStepSuccessful = new HelloWorldSecondStepSuccessful(payload.getResourceId());
                sendEvent(context, HELLOWORLD_SECOND_STEP_FINISHED_EVENT.event(), helloWorldSecondStepSuccessful);
            }
        };
    }

    @Bean(name = "HELLO_WORLD_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractHelloWorldAction<>(HelloWorldSecondStepSuccessful.class) {
            @Override
            protected void doExecute(HelloWorldContext context, HelloWorldSecondStepSuccessful payload, Map<Object, Object> variables) {
                LOGGER.info("Hello world finished!");
                sendEvent(context, FINALIZE_HELLOWORLD_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "HELLO_WORLD_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractHelloWorldAction<>(HelloWorldFailedEvent.class) {

            @Override
            protected void doExecute(HelloWorldContext context, HelloWorldFailedEvent payload, Map<Object, Object> variables) {
                sendEvent(context, HELLOWORLD_FAILHANDLED_EVENT.event(), payload);
            }
        };
    }

    private abstract static class AbstractHelloWorldAction<P extends Payload> extends AbstractAction<HelloWorldState, HelloWorldEvent, HelloWorldContext, P> {

        protected AbstractHelloWorldAction(Class<P> payloadClass) {
            super(payloadClass);
        }

        @Override
        protected HelloWorldContext createFlowContext(FlowParameters flowParameters, StateContext<HelloWorldState, HelloWorldEvent> stateContext, P payload) {
            return new HelloWorldContext(flowParameters);
        }

        @Override
        protected Object getFailurePayload(P payload, Optional<HelloWorldContext> flowContext, Exception ex) {
            return new HelloWorldFailedEvent(payload.getResourceId(), ex);
        }
    }
}
```
`getFailurePayload` method is called when somehow an unexpected exception happens in your action's `doExecute`. It should return with your failure payload, 
usually it contains an exception.

## Handlers
If you have to do something that can take a long time, like a remote call or polling something, then you shouldn’t do it in an action's `doExecute` method. 
If you do this then it will consume one reactor thread and after a while it can happen there will be no available threads for Reactor to process events for new 
flows. For this reason you should create a Handler. Handlers should extend from `ExceptionCatcherEventHandler`. 
You have to implement selector method to define which event your handler will listen on. In our case this event is 
`HelloWorldFirstStepLongLastingTaskTriggerEvent`. You have to define a default failure event also, it will be sent if some unexpected and unhandled exception 
happening in your handler. In `doAccept` you can do your long lasting actions like polling or rest calls and you have to return with an event you want to send 
back to reactor. In this scenario we send a `HelloWorldFirstStepLongLastingTaskSuccessResponse` in successful case, and a 
`HelloWorldFirstStepLongLastingTaskFailureResponse` in failed case. It is very common to put results into these responses for next flow steps.
```java
@Component
public class HelloWorldFirstStepLongLastingTaskHandler extends ExceptionCatcherEventHandler<HelloWorldFirstStepLongLastingTaskTriggerEvent> {
   private static final Logger LOGGER = LoggerFactory.getLogger(HelloWorldFirstStepLongLastingTaskHandler.class);

   @Override
   public String selector() {
       return HelloWorldFirstStepLongLastingTaskTriggerEvent.class.getSimpleName();
   }

   @Override
   protected Selectable defaultFailureEvent(Long resourceId, Exception e) {
       return new HelloWorldFirstStepLongLastingTaskFailureResponse(resourceId, e);
   }

   @Override
   protected Selectable doAccept(HandlerEvent event) {
       HelloWorldFirstStepLongLastingTaskTriggerEvent helloWorldReactorEvent = event.getData();
       Long resourceId = helloWorldReactorEvent.getResourceId();
       try {
           LOGGER.info("Long lasting task execution...");
           return new HelloWorldFirstStepLongLastingTaskSuccessResponse(resourceId);
       } catch (RuntimeException ex) {
           LOGGER.info("Long lasting task execution failed. Cause: ", ex);
           return new HelloWorldFirstStepLongLastingTaskFailureResponse(resourceId, ex);
       }
   }
}
```

You can check the second step is `HELLO_WORLD_SECOND_STEP_STATE` in our flow. It is waiting for a `HelloWorldFirstStepLongLastingTaskSuccessResponse` payload, 
we sent from handler previously. It is a very simple step also, it’s just print a log, then sends a `HELLOWORLD_SECOND_STEP_FINISHED_EVENT`, so our flow can 
step into `HELLO_WORLD_FINISHED_STATE` where our flow ends.

## Restartable flows:

Documentation is under construction...

## Parallel states

Currently not supported, but spring statemachine supports fork and join states

## Distributed flows

Flows currently are not per state distributed. Spring statemachine has distributed implementation but not released yet: https://docs.spring.io/spring-statemachine/docs/2.1.3.RELEASE/reference/#sm-distributed

# Flow Chains

Cloudbreak’s flow engine has a concept for executing/connecting flows and executing them in the desired order with shared context that’s called flow chain.
Flow chains are not just able to connect different flows but also connect/execute other flow chains which makes it possible to orchestrate quite complex workflows with dynamic set up. In order to do this the `com.sequenceiq.flow.core.chain.FlowEventChainFactory` interface needs to be implemented that orchestrate the contained flows, flow chains and similarly to the flows it is also event driven, just it configures the shared context with the necessary flows and chains dynamically.

## Flow and flow chain visualization

As the Flows and their configuration are statically defined within the code base, those configs could be used to generate graphs as visualization of the underlying workflows for easier understanding. Unfortunately Flow Chains are the opposite, they are so dynamic that only with covering unit tests we found a way to build the underlying state machine and workflows to be able to have visualization about them. This way not all of them has a graph yet.
[There is a drive folder that's intended to be a store of flow and flow chain graphs by Cloudbreak versions.](https://drive.google.com/drive/folders/1SGUvGccDCvg9A4stpk8h54wCyxlBoPo9?usp=drive_link)

So if you were able to generate for a fresh CB version with the following mechanism, then do not hesitate to copy the generated pictures to a new folder in the Drive folder.  

## Flow and Flow Chain graph generation

### Generation for Flows
A separate Make goal has been created to do this from the Cloudbreak project root. Of course if a new module/micros-service is introduced then it needs to updated after creating the generator caller class in the new module.
```
make generate-flow-graphs
```

### Generation for Flow Chains
Some of the most simple flow chains covered by the previous step by the more dynamic ones the unit test of the module need to be executed which could simply be done by a gradle clean build for example.

### Generate pictures from the previously generated DOT graph descriptors
The previously mentioned steps only generate the graph descriptor files in DOT format and to have real visualization the following Make goal from the Cloudbreak project root should be executed:
```
make generate-flow-graph-pictures
```

### Recommended steps for generation
As the Gradle tasks may erase the build directories of the modules the following execution order makes more sense:
```
./gradlew clean build
make generate-flow-graphs
make generate-flow-graph-pictures
```

### How to generate graph at the end of your FlowChainEventFactory’s unit tests
A basic Util class has been introduced to do this on the easiest way. It looks like the following at the end of one of the unit tests. Example: `com.sequenceiq.cloudbreak.core.flow2.chain.MultiHostgroupDownscaleFlowEventChainFactoryTest`
```
FlowChainConfigGraphGeneratorUtil.generateFor(
underTest,            //The FlowEventChainFactory instance with the built context
FLOW_CONFIGS_PACKAGE, //The package where the relevant Flow and Flow Chain configs are
flowTriggerQueue,     //The flow trigger queue which is orchestrated by the tested factory during the test run
"FULL_DOWNSCALE");    //File name suffix to be able to differentiate graphs with the same factory but different context setup
```