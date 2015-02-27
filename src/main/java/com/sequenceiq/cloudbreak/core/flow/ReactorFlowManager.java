package com.sequenceiq.cloudbreak.core.flow;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.service.stack.event.ProvisionRequest;

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
        String transition = null;
        if (transitionMap.containsKey(handler)) {
            if (success) {
                transition = transitionMap.get(handler).getNext();
            } else {
                transition = transitionMap.get(handler).getFailure();
            }
            LOGGER.debug("Transitioning to [ {} ] from handler [ {} ] ", transition, handler);
        } else {
            LOGGER.debug("There is no registered transition from handler {}", handler);
        }
        return transition;
    }

    @Override
    public void registerTransition(Class handlerClass, Transition transition) {
        transitionMap.put(handlerClass, transition);
    }

    @Override
    public void triggerProvisioning(Object object) {
        ProvisionRequest provisionRequest = (ProvisionRequest) object;
        ProvisioningContext context = ProvisioningContextFactory.createProvisioningSetupContext(provisionRequest.getCloudPlatform(),
                provisionRequest.getStackId());
        reactor.notify(FlowInitializer.Phases.PROVISIONING_SETUP.name(), eventFactory.createEvent(context, FlowInitializer.Phases.PROVISIONING_SETUP.name()));
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

    public static class TransitionFactory {
        public static Transition createTransition(String current, String next, String failure) {
            return new Transition(current, next, failure);
        }
    }

}

