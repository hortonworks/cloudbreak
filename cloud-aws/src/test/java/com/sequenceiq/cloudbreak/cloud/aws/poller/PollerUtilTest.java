package com.sequenceiq.cloudbreak.cloud.aws.poller;

import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.aws.AwsInstanceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

@RunWith(MockitoJUnitRunner.class)
public class PollerUtilTest {

    private static final Long STACK_ID = 12L;

    @Mock
    private AwsInstanceConnector awsInstanceConnector;

    @InjectMocks
    private PollerUtil underTest;

    @Test
    public void testWaitForWhenNoInstances() {
        ReflectionTestUtils.setField(underTest, "pollingInterval", 1);
        ReflectionTestUtils.setField(underTest, "pollingAttempt", 1);
        CloudContext cloudContext = new CloudContext(STACK_ID, "", "", "", "");
        CloudCredential cloudCredential = new CloudCredential(STACK_ID.toString(), "");
        AuthenticatedContext ac = new AuthenticatedContext(cloudContext, cloudCredential);

        List<CloudVmInstanceStatus> actual = underTest.waitFor(ac, Collections.emptyList(), Collections.emptySet());

        Assert.assertEquals(0, actual.size());
    }

    @Test
    public void testWaitForWhenHasInstancesAndCompleted() {
        ReflectionTestUtils.setField(underTest, "pollingInterval", 1);
        ReflectionTestUtils.setField(underTest, "pollingAttempt", 1);
        CloudContext cloudContext = new CloudContext(STACK_ID, "", "", "", "");
        CloudCredential cloudCredential = new CloudCredential(STACK_ID.toString(), "");
        AuthenticatedContext ac = new AuthenticatedContext(cloudContext, cloudCredential);
        CloudInstance cloudInstance = new CloudInstance("instanceId", null, null);
        List<CloudInstance> instances = List.of(cloudInstance);

        CloudVmInstanceStatus cloudVmInstanceStatus = new CloudVmInstanceStatus(cloudInstance, InstanceStatus.CREATED);
        List<CloudVmInstanceStatus> vmInstanceStatuses = List.of(cloudVmInstanceStatus);

        when(awsInstanceConnector.check(ac, instances)).thenReturn(vmInstanceStatuses);

        List<CloudVmInstanceStatus> actual = underTest.waitFor(ac, instances, Set.of(InstanceStatus.CREATED));

        Assert.assertEquals(1, actual.size());
        Assert.assertEquals(vmInstanceStatuses, actual);
    }

    @Test
    public void testWaitForWhenHasInstancesAndNotCompleted() {
        ReflectionTestUtils.setField(underTest, "pollingInterval", 1);
        ReflectionTestUtils.setField(underTest, "pollingAttempt", 2);
        CloudContext cloudContext = new CloudContext(STACK_ID, "", "", "", "");
        CloudCredential cloudCredential = new CloudCredential(STACK_ID.toString(), "");
        AuthenticatedContext ac = new AuthenticatedContext(cloudContext, cloudCredential);
        CloudInstance cloudInstance = new CloudInstance("instanceId", null, null);
        List<CloudInstance> instances = List.of(cloudInstance);

        CloudVmInstanceStatus cloudVmInstanceStatus1 = new CloudVmInstanceStatus(cloudInstance, InstanceStatus.IN_PROGRESS);
        CloudVmInstanceStatus cloudVmInstanceStatus2 = new CloudVmInstanceStatus(cloudInstance, InstanceStatus.CREATED);

        when(awsInstanceConnector.check(ac, instances))
                .thenReturn(List.of(cloudVmInstanceStatus1))
                .thenReturn(List.of(cloudVmInstanceStatus2));

        List<CloudVmInstanceStatus> actual = underTest.waitFor(ac, instances, Set.of(InstanceStatus.CREATED));

        Assert.assertEquals(1, actual.size());
        Assert.assertEquals(cloudVmInstanceStatus2, actual.get(0));
    }
}
