package com.sequenceiq.cloudbreak.cloud.aws.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceLifecycleType;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceLifeCycle;

class AwsLifeCycleMapperTest {

    private AwsLifeCycleMapper underTest = new AwsLifeCycleMapper();

    @Test
    void spotLifeCycle() {
        Instance spotInstance = new Instance().withInstanceLifecycle(InstanceLifecycleType.Spot);

        CloudInstanceLifeCycle lifeCycle = underTest.getLifeCycle(spotInstance);

        Assertions.assertThat(lifeCycle).isEqualTo(CloudInstanceLifeCycle.SPOT);
    }

    @Test
    void scheduledLifeCycle() {
        Instance scheduledInstance = new Instance().withInstanceLifecycle(InstanceLifecycleType.Scheduled);

        CloudInstanceLifeCycle lifeCycle = underTest.getLifeCycle(scheduledInstance);

        Assertions.assertThat(lifeCycle).isEqualTo(CloudInstanceLifeCycle.NORMAL);
    }

    @Test
    void nullLifeCycle() {
        Instance onDemandInstance = new Instance().withInstanceLifecycle((String) null);

        CloudInstanceLifeCycle lifeCycle = underTest.getLifeCycle(onDemandInstance);

        Assertions.assertThat(lifeCycle).isEqualTo(CloudInstanceLifeCycle.NORMAL);
    }

}