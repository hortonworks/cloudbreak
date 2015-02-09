package com.sequenceiq.cloudbreak.core.flow;

public interface FlowManager {

    String transition(Class handlerClass, boolean success);

    void registerTransition(Class handlerClass, Transition transition);

    boolean hasTransitions(Class handlerClass);
}
