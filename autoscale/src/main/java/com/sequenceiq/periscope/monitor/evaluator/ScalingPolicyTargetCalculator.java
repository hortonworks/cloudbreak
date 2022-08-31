package com.sequenceiq.periscope.monitor.evaluator;

import static java.lang.Math.ceil;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.monitor.event.ScalingEvent;
import com.sequenceiq.periscope.utils.ClusterUtils;

@Component
public class ScalingPolicyTargetCalculator {

    public Integer getDesiredAbsoluteNodeCount(ScalingEvent event, int hostGroupNodeCount) {
        ScalingPolicy policy = event.getAlert().getScalingPolicy();
        int scalingAdjustment = policy.getScalingAdjustment();
        int desiredAbsoluteHostGroupNodeCount;
        switch (policy.getAdjustmentType()) {
            case NODE_COUNT:
                desiredAbsoluteHostGroupNodeCount = hostGroupNodeCount + scalingAdjustment;
                break;
            case PERCENTAGE:
                desiredAbsoluteHostGroupNodeCount = hostGroupNodeCount
                        + (int) (ceil(hostGroupNodeCount * ((double) scalingAdjustment / ClusterUtils.MAX_CAPACITY)));
                break;
            case EXACT:
                desiredAbsoluteHostGroupNodeCount = policy.getScalingAdjustment();
                break;
            default:
                desiredAbsoluteHostGroupNodeCount = hostGroupNodeCount;
        }
        int minSize = ScalingConstants.DEFAULT_HOSTGROUP_MIN_SIZE;
        return desiredAbsoluteHostGroupNodeCount < minSize ? minSize : desiredAbsoluteHostGroupNodeCount;
    }
}