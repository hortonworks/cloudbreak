package com.sequenceiq.cloudbreak.core.flow;

/**
 * Contract for flow management operations.
 * A <b>flow<b/> represents a succession of <b>phases<b/>, each performed on a different thread.
 */
public interface FlowManager {

    void registerTransition(Class handlerClass, Transition transition);

    void triggerProvisioning(Object object);

    void triggerNext(Class sourceHandlerClass, Object payload, boolean success);
}
