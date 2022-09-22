package com.sequenceiq.cloudbreak.cloud.aws.common.util;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceLifeCycle;

import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceLifecycleType;

@Service
public class AwsLifeCycleMapper {

    public CloudInstanceLifeCycle getLifeCycle(Instance instance) {
        return InstanceLifecycleType.SPOT.equals(instance.instanceLifecycle())
                ? CloudInstanceLifeCycle.SPOT
                : CloudInstanceLifeCycle.NORMAL;
    }
}
