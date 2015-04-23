package com.sequenceiq.cloudbreak.core.flow;

public interface TransitionKeyService {

    String successKey(Class handlerClass);

    String failureKey(Class handlerClass);

    void registerTransition(Class handlerClass, Transition transition);

}
