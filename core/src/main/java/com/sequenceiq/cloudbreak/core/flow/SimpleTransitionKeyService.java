package com.sequenceiq.cloudbreak.core.flow;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SimpleTransitionKeyService implements TransitionKeyService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleTransitionKeyService.class);
    private Map<Class, Transition> transitionMap = new HashMap();

    @Override
    public String successKey(Class handlerClass) {
        Transition transition = transitionForClass(handlerClass);
        LOGGER.info("Transitioning from phase [{}] to [{}]", transition.getCurrent(), transition.getNext());
        return transition.getNext();
    }

    @Override
    public String failureKey(Class handlerClass) {
        Transition transition = transitionForClass(handlerClass);
        LOGGER.info("Transitioning from phase [{}] to [{}]", transition.getCurrent(), transition.getFailure());
        return transition.getFailure();
    }

    @Override
    public void registerTransition(Class handlerClass, Transition transition) {
        LOGGER.debug("Registering transition. Handler: {}, Transition: {}", handlerClass, transition);
        transitionMap.put(handlerClass, transition);
    }

    private Transition transitionForClass(Class handlerClass) {
        LOGGER.debug("Retrieving transition for class: {}", handlerClass);
        Transition transition = null;
        if (transitionMap.containsKey(handlerClass)) {
            transition = transitionMap.get(handlerClass);
        } else {
            LOGGER.debug("There's no registered transition for class: {}", handlerClass);
            throw new IllegalStateException("There's no registered transition for handler class" + handlerClass);
        }
        return transition;
    }

    public static class TransitionFactory {
        public static Transition createTransition(String current, String next, String failure) {
            if (current == null || next == null || failure == null) {
                throw new IllegalStateException("Invalid transition definition");
            }
            return new Transition(current, next, failure);
        }
    }

}
