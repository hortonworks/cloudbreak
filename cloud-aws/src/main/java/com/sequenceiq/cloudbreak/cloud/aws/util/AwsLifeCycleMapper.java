package com.sequenceiq.cloudbreak.cloud.aws.util;

import org.springframework.stereotype.Service;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceLifecycleType;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceLifeCycle;

@Service
public class AwsLifeCycleMapper {

    public CloudInstanceLifeCycle getLifeCycle(Instance instance) {
        return InstanceLifecycleType.Spot.toString().equals(instance.getInstanceLifecycle())
                ? CloudInstanceLifeCycle.SPOT
                : CloudInstanceLifeCycle.NORMAL;
    }
}
