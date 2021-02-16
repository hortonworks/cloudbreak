package com.sequenceiq.flow.core.config;

/**
 * Marker interface for flow configurations.
 * In case of a flow transition steps are ordered and dp not have branches, isFlowOrdered method can be set to true.
 * By enabling this setting a progress map with states - percentage pairs will be filled, that can be used the follow
 * the progress of the flow.
 */
public interface OrderedFlowConfiguration {
    /**
     * Override this if the flow config is ordered (flow steps are in order and no conditional steps)
     */
    default boolean isFlowOrdered() {
        return false;
    }
}
