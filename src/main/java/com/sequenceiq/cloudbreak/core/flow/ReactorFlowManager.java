package com.sequenceiq.cloudbreak.core.flow;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;
import com.sequenceiq.cloudbreak.core.flow.context.TerminationContext;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionRequest;
import com.sequenceiq.cloudbreak.service.stack.event.StackDeleteRequest;

import reactor.core.Reactor;
import reactor.event.Event;

/**
 * Flow manager implementation backed by Reactor.
 * This class is the flow state machine and mediates between the states and reactor events
 */
@Service
public class ReactorFlowManager implements FlowManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReactorFlowManager.class);

    private Map<Class, Transition> transitionMap = new HashMap();

    @Autowired
    private Reactor reactor;

    @Autowired
    private ErrorHandlerAwareFlowEventFactory eventFactory;

    private String transitionKey(Class handler, boolean success) {
        LOGGER.debug("Transitioning from handler {}. Scenario {}", handler, success ? "SUCCESS" : "ERROR");
        String transitionKey = null;
        if (transitionMap.containsKey(handler)) {
            if (success) {
                transitionKey = transitionMap.get(handler).getNext();
            } else {
                transitionKey = transitionMap.get(handler).getFailure();
            }
            LOGGER.debug("Transitioning to [ {} ] from handler [ {} ] ", transitionKey, handler);
        } else {
            LOGGER.debug("There is no registered transition from handler {}", handler);
        }
        return transitionKey;
    }

    @Override
    public void registerTransition(Class handlerClass, Transition transition) {
        transitionMap.put(handlerClass, transition);
    }

    @Override
    public void triggerProvisioning(Object object) {
        ProvisionRequest provisionRequest = (ProvisionRequest) object;
        ProvisioningContext context = FlowContextFactory.createProvisioningSetupContext(provisionRequest.getCloudPlatform(),
                provisionRequest.getStackId());
        String nameOfPhase = FlowInitializer.Phases.PROVISIONING_SETUP.name();
        reactor.notify(nameOfPhase, eventFactory.createEvent(context, nameOfPhase));
    }

    @Override
    public void triggerNext(Class sourceHandlerClass, Object payload, boolean success) {
        String key = transitionKey(sourceHandlerClass, success);
        if (null != key) {
            Event event = eventFactory.createEvent(payload, key);
            reactor.notify(key, event);
        } else {
            LOGGER.debug("The handler {} has no transitions.", sourceHandlerClass);
        }
    }

    @Override
    public void triggerClusterInstall(Object object) {
        ProvisionRequest provisionRequest = (ProvisionRequest) object;
        ProvisioningContext context = FlowContextFactory.createProvisioningSetupContext(provisionRequest.getCloudPlatform(),
                provisionRequest.getStackId());
        String nameOfPhase = FlowInitializer.Phases.CLUSTER_CREATION.name();
        reactor.notify(nameOfPhase, eventFactory.createEvent(context, nameOfPhase));
    }

    @Override
    public void triggerTermination(Object object) {
        StackDeleteRequest deleteRequest = (StackDeleteRequest) object;
        TerminationContext context = new TerminationContext(deleteRequest.getStackId(), deleteRequest.getCloudPlatform());
        String nameOfPhase = FlowInitializer.Phases.TERMINATION.name();
        reactor.notify(nameOfPhase, eventFactory.createEvent(context, nameOfPhase));
    }

    public static class TransitionFactory {
        public static Transition createTransition(String current, String next, String failure) {
            return new Transition(current, next, failure);
        }
    }

}

