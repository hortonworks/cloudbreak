package com.sequenceiq.cloudbreak.core.flow;

import com.sequenceiq.cloudbreak.api.model.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupAdjustmentJson;

/**
 * Contract for flow management operations.
 * A <b>flow<b/> represents a succession of <b>phases<b/>, each performed on a different thread.
 */
public interface FlowManager {
    void triggerProvisioning(Long stackId);

    void triggerStackStart(Long stackId);

    void triggerStackStop(Long stackId);

    void triggerStackUpscale(Long stackId, InstanceGroupAdjustmentJson instanceGroupAdjustment);

    void triggerStackDownscale(Long stackId, InstanceGroupAdjustmentJson instanceGroupAdjustment);

    void triggerStackSync(Long stackId);

    void triggerClusterInstall(Long stackId);

    void triggerClusterStart(Long stackId);

    void triggerClusterStop(Long stackId);

    void triggerClusterUpscale(Long stackId, HostGroupAdjustmentJson hostGroupAdjustment);

    void triggerClusterDownscale(Long stackId, HostGroupAdjustmentJson hostGroupAdjustment);

    void triggerClusterSync(Long stackId);

    void triggerFullSync(Long stackId);


    void triggerClusterReInstall(Object object);

    void triggerTermination(Object object);

    void triggerForcedTermination(Object object);

    void triggerClusterTermination(Object object);

    void triggerStackRemoveInstance(Object object);

    void triggerClusterUserNamePasswordUpdate(Object object);
}
