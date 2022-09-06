package com.sequenceiq.periscope.service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.model.adjustment.MandatoryScalingAdjustmentParameters;

public interface MandatoryScalingAdjustmentService {

    void performMandatoryAdjustment(Cluster cluster, String pollingUserCrn, StackV4Response stackResponse,
            MandatoryScalingAdjustmentParameters adjustmentParameters);
}
