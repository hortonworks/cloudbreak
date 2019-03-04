package com.sequenceiq.cloudbreak.service.stack.repair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.handler.InstanceStateQuery;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

@RunWith(MockitoJUnitRunner.class)
public class UnhealthyInstancesFinalizerTest {

    @Mock
    private CredentialToCloudCredentialConverter credentialConverter;

    @Mock
    private InstanceMetaDataToCloudInstanceConverter cloudInstanceConverter;

    @Mock
    private InstanceStateQuery instanceStateQuery;

    @InjectMocks
    private UnhealthyInstancesFinalizer underTest;

    @Test
    public void shouldFinalizeInstancesMarkedAsTerminated() {
        Stack stack = TestUtil.stack(Status.AVAILABLE, TestUtil.awsCredential());

        CloudCredential cloudCredential = mock(CloudCredential.class);
        when(credentialConverter.convert(stack.getCredential())).thenReturn(cloudCredential);

        String instanceId1 = "i-0f1e0605506aaaaaa";
        String instanceId2 = "i-0f1e0605506bbbbbb";

        Set<InstanceMetaData> candidateUnhealthyInstances = new HashSet<>();
        setupInstanceMetaData(instanceId1, candidateUnhealthyInstances);
        setupInstanceMetaData(instanceId2, candidateUnhealthyInstances);

        List<CloudInstance> cloudInstances = new ArrayList<>();
        CloudInstance cloudInstance1 = setupCloudInstance(instanceId1, cloudInstances);
        CloudInstance cloudInstance2 = setupCloudInstance(instanceId2, cloudInstances);
        when(cloudInstanceConverter.convert(candidateUnhealthyInstances)).thenReturn(cloudInstances);

        List<CloudVmInstanceStatus> cloudVmInstanceStatusList = new ArrayList<>();
        setupCloudVmInstanceStatus(cloudInstance1, InstanceStatus.STOPPED, cloudVmInstanceStatusList);
        setupCloudVmInstanceStatus(cloudInstance2, InstanceStatus.TERMINATED, cloudVmInstanceStatusList);
        when(instanceStateQuery.getCloudVmInstanceStatuses(eq(cloudCredential), any(CloudContext.class), eq(cloudInstances))).
                thenReturn(cloudVmInstanceStatusList);

        Set<String> unhealthyInstances = underTest.finalizeUnhealthyInstances(stack, candidateUnhealthyInstances);

        assertEquals(1L, unhealthyInstances.size());
        assertTrue(unhealthyInstances.contains(instanceId2));
    }

    @Test
    public void shouldFinalizeInstancesThatAreNotFound() {
        Stack stack = TestUtil.stack(Status.AVAILABLE, TestUtil.awsCredential());

        CloudCredential cloudCredential = mock(CloudCredential.class);
        when(credentialConverter.convert(stack.getCredential())).thenReturn(cloudCredential);

        String instanceId1 = "i-0f1e0605506aaaaaa";
        String instanceId2 = "i-0f1e0605506bbbbbb";

        Set<InstanceMetaData> candidateUnhealthyInstances = new HashSet<>();
        setupInstanceMetaData(instanceId1, candidateUnhealthyInstances);
        setupInstanceMetaData(instanceId2, candidateUnhealthyInstances);

        List<CloudInstance> cloudInstances = new ArrayList<>();
        CloudInstance cloudInstance1 = setupCloudInstance(instanceId1, cloudInstances);
        when(cloudInstanceConverter.convert(candidateUnhealthyInstances)).thenReturn(cloudInstances);

        List<CloudVmInstanceStatus> cloudVmInstanceStatusList = new ArrayList<>();
        setupCloudVmInstanceStatus(cloudInstance1, InstanceStatus.TERMINATED, cloudVmInstanceStatusList);
        when(instanceStateQuery.getCloudVmInstanceStatuses(eq(cloudCredential), any(CloudContext.class), eq(cloudInstances))).
                thenReturn(cloudVmInstanceStatusList);

        Set<String> unhealthyInstances = underTest.finalizeUnhealthyInstances(stack, candidateUnhealthyInstances);

        assertEquals(2L, unhealthyInstances.size());
        assertTrue(unhealthyInstances.contains(instanceId1));
        assertTrue(unhealthyInstances.contains(instanceId2));
    }

    private void setupCloudVmInstanceStatus(
            CloudInstance cloudInstance1, InstanceStatus stopped, List<CloudVmInstanceStatus> cloudVmInstanceStatusList) {
        CloudVmInstanceStatus status1 = mock(CloudVmInstanceStatus.class);
        when(status1.getCloudInstance()).thenReturn(cloudInstance1);
        when(status1.getStatus()).thenReturn(stopped);
        cloudVmInstanceStatusList.add(status1);
    }

    private CloudInstance setupCloudInstance(String instanceId, List<CloudInstance> cloudInstances) {
        CloudInstance cloudInstance = mock(CloudInstance.class);
        when(cloudInstance.getInstanceId()).thenReturn(instanceId);
        cloudInstances.add(cloudInstance);
        return cloudInstance;
    }

    private void setupInstanceMetaData(String instanceId, Set<InstanceMetaData> candidateUnhealthyInstances) {
        InstanceMetaData imd1 = mock(InstanceMetaData.class);
        when(imd1.getInstanceId()).thenReturn(instanceId);
        candidateUnhealthyInstances.add(imd1);
    }
}
