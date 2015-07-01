package com.sequenceiq.cloudbreak.core.flow;

/**
 * Contract for flow management operations.
 * A <b>flow<b/> represents a succession of <b>phases<b/>, each performed on a different thread.
 */
public interface FlowManager {

    void triggerProvisioning(Object object);

    void triggerNext(Class sourceHandlerClass, Object payload, boolean success);

    void triggerClusterInstall(Object object);

    void triggerClusterReInstall(Object object);

    void triggerTermination(Object object);

    void triggerStackStop(Object object);

    void triggerStackStart(Object object);

    void triggerStackStopRequested(Object object);

    void triggerClusterStartRequested(Object object);

    void triggerClusterStop(Object object);

    void triggerClusterStart(Object object);

    void triggerStackUpscale(Object object);

    void triggerStackDownscale(Object object);

    void triggerStackRemoveInstance(Object object);

    void triggerClusterUpscale(Object object);

    void triggerClusterDownscale(Object object);

    void triggerUpdateAllowedSubnets(Object object);

    void triggerClusterSync(Object object);

    void triggerStackSync(Object object);

}
