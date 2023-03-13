package com.sequenceiq.cloudbreak.cloud.aws.common.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceLifeCycle;

import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceLifecycleType;

class AwsLifeCycleMapperTest {

    private AwsLifeCycleMapper underTest = new AwsLifeCycleMapper();

    @Test
    void spotLifeCycle() {
        Instance spotInstance = Instance.builder().instanceLifecycle(InstanceLifecycleType.SPOT).build();

        CloudInstanceLifeCycle lifeCycle = underTest.getLifeCycle(spotInstance);

        Assertions.assertThat(lifeCycle).isEqualTo(CloudInstanceLifeCycle.SPOT);
    }

    @Test
    void scheduledLifeCycle() {
        Instance scheduledInstance = Instance.builder().instanceLifecycle(InstanceLifecycleType.SCHEDULED).build();

        CloudInstanceLifeCycle lifeCycle = underTest.getLifeCycle(scheduledInstance);

        Assertions.assertThat(lifeCycle).isEqualTo(CloudInstanceLifeCycle.NORMAL);
    }

    @Test
    void nullLifeCycle() {
        Instance onDemandInstance = Instance.builder().instanceLifecycle((String) null).build();

        CloudInstanceLifeCycle lifeCycle = underTest.getLifeCycle(onDemandInstance);

        Assertions.assertThat(lifeCycle).isEqualTo(CloudInstanceLifeCycle.NORMAL);
    }

}
