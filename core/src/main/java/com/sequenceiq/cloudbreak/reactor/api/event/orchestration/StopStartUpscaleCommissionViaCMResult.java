package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StopStartUpscaleCommissionViaCMRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class StopStartUpscaleCommissionViaCMResult extends AbstractClusterScaleResult<StopStartUpscaleCommissionViaCMRequest> {

    // TODO CB-14929: Include additional information about success / failure, nodes etc - so that the next step can take corrective action.
    public StopStartUpscaleCommissionViaCMResult(StopStartUpscaleCommissionViaCMRequest request) {
        super(request);
    }
}
