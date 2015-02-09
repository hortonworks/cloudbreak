package com.sequenceiq.cloudbreak.core.flow;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SimpleFlowManager implements FlowManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleFlowManager.class);
    private Map<Class, Transition> transitionMap = new HashMap();

    @Override
    public String transition(Class handler, boolean success) {
        String transition = null;

        if (hasTransitions(handler)) {
            if (success) {
                transition = transitionMap.get(handler).getNext();
            } else {
                transition = transitionMap.get(handler).getFailure();
            }
            LOGGER.debug("Transitioning to [ {} ] > from handler [ {} ] ", transition, handler);
        } else {
            LOGGER.debug("There is no registered transition from handler {}", handler);
        }
        return transition;
    }

    @Override
    public void registerTransition(Class handlerClass, Transition transition) {
        transitionMap.put(handlerClass, transition);
    }

    @Override public boolean hasTransitions(Class handlerClass) {
        return transitionMap.containsKey(handlerClass);
    }

    public static class TransitionFactory {
        public static Transition createTransition(String current, String next, String failure) {
            Transition transition = new Transition(current, next, failure);
            return transition;
        }
    }

}

