package com.sequenceiq.cloudbreak.cloud.aws.poller;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.dyngr.exception.PollerStoppedException;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsInstanceConnector;
import com.sequenceiq.cloudbreak.cloud.aws.common.poller.PollerUtil;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

@ExtendWith(MockitoExtension.class)
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
        CloudContext context = createCloudContext();
        CloudCredential cloudCredential = new CloudCredential(STACK_ID.toString(), "");
        AuthenticatedContext ac = new AuthenticatedContext(context, cloudCredential);

        List<CloudVmInstanceStatus> actual = underTest.waitFor(ac, Collections.emptyList(), Collections.emptySet(), "");

        assertEquals(0, actual.size());
    }

    private CloudContext createCloudContext() {
        return CloudContext.Builder.builder()
                .withId(STACK_ID)
                .withName("")
                .withCrn("")
                .withPlatform("")
                .withVariant("")
                .build();
    }

    @Test
    public void testWaitForWhenHasInstancesAndCompleted() {
        ReflectionTestUtils.setField(underTest, "pollingInterval", 1);
        ReflectionTestUtils.setField(underTest, "pollingAttempt", 1);
        CloudContext context = createCloudContext();
        CloudCredential cloudCredential = new CloudCredential(STACK_ID.toString(), "");
        AuthenticatedContext ac = new AuthenticatedContext(context, cloudCredential);
        CloudInstance cloudInstance = new CloudInstance("instanceId", null, null, "subnet-1", "az1");
        List<CloudInstance> instances = List.of(cloudInstance);

        CloudVmInstanceStatus cloudVmInstanceStatus = new CloudVmInstanceStatus(cloudInstance, InstanceStatus.CREATED);
        List<CloudVmInstanceStatus> vmInstanceStatuses = List.of(cloudVmInstanceStatus);

        when(awsInstanceConnector.check(ac, instances)).thenReturn(vmInstanceStatuses);

        List<CloudVmInstanceStatus> actual = underTest.waitFor(ac, instances, Set.of(InstanceStatus.CREATED), "");

        assertEquals(1, actual.size());
        assertEquals(vmInstanceStatuses, actual);
    }

    @Test
    public void testWaitForWhenHasInstancesAndNotCompleted() {
        ReflectionTestUtils.setField(underTest, "pollingInterval", 1);
        ReflectionTestUtils.setField(underTest, "pollingAttempt", 2);
        CloudContext context = createCloudContext();
        CloudCredential cloudCredential = new CloudCredential(STACK_ID.toString(), "");
        AuthenticatedContext ac = new AuthenticatedContext(context, cloudCredential);
        CloudInstance cloudInstance = new CloudInstance("instanceId", null, null, "subnet-1", "az1");
        List<CloudInstance> instances = List.of(cloudInstance);

        CloudVmInstanceStatus cloudVmInstanceStatus1 = new CloudVmInstanceStatus(cloudInstance, InstanceStatus.IN_PROGRESS);
        CloudVmInstanceStatus cloudVmInstanceStatus2 = new CloudVmInstanceStatus(cloudInstance, InstanceStatus.CREATED);

        when(awsInstanceConnector.check(ac, instances))
                .thenReturn(List.of(cloudVmInstanceStatus1))
                .thenReturn(List.of(cloudVmInstanceStatus2));

        List<CloudVmInstanceStatus> actual = underTest.waitFor(ac, instances, Set.of(InstanceStatus.CREATED), "");

        assertEquals(1, actual.size());
        assertEquals(cloudVmInstanceStatus2, actual.get(0));
    }

    @Test
    public void testWaitForWhenPollerStopExceptionHasNotCause() {
        ReflectionTestUtils.setField(underTest, "pollingInterval", 1);
        ReflectionTestUtils.setField(underTest, "pollingAttempt", 1);

        CloudCredential cloudCredential = new CloudCredential(STACK_ID.toString(), "");
        AuthenticatedContext ac = new AuthenticatedContext(createCloudContext(), cloudCredential);
        CloudInstance cloudInstance = new CloudInstance("instanceId", null, null, "subnet-1", "az1");
        List<CloudInstance> instances = List.of(cloudInstance);

        when(awsInstanceConnector.check(ac, instances)).thenReturn(List.of(new CloudVmInstanceStatus(cloudInstance, InstanceStatus.FAILED)));

        PollerStoppedException actual = assertThrows(PollerStoppedException.class, () -> underTest.waitFor(ac, instances, Set.of(InstanceStatus.STARTED), ""));

        Pattern regexp = Pattern.compile("unknown operation cannot be finished in time. Duration: .*. Instances: .*");
        assertTrue(regexp.matcher(actual.getMessage()).matches());
    }

    @Test
    public void testWaitForWhenPollerStopExceptionHasCause() {
        ReflectionTestUtils.setField(underTest, "pollingInterval", 1);
        ReflectionTestUtils.setField(underTest, "pollingAttempt", 1);

        CloudCredential cloudCredential = new CloudCredential(STACK_ID.toString(), "");
        AuthenticatedContext ac = new AuthenticatedContext(createCloudContext(), cloudCredential);
        CloudInstance cloudInstance = new CloudInstance("instanceId", null, null, "subnet-1", "az1");
        List<CloudInstance> instances = List.of(cloudInstance);

        when(awsInstanceConnector.check(ac, instances))
                .thenThrow(new RuntimeException("runtime ex"))
                .thenReturn(List.of(new CloudVmInstanceStatus(cloudInstance, InstanceStatus.FAILED)));

        PollerStoppedException actual = assertThrows(PollerStoppedException.class, () -> underTest.waitFor(ac, instances, Set.of(InstanceStatus.STARTED), ""));

        assertEquals("java.lang.RuntimeException: runtime ex", actual.getMessage());
    }

}
