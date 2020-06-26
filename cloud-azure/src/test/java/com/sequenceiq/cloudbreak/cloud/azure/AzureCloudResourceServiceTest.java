package com.sequenceiq.cloudbreak.cloud.azure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.resources.Deployment;
import com.microsoft.azure.management.resources.DeploymentOperation;
import com.microsoft.azure.management.resources.DeploymentOperations;
import com.microsoft.azure.management.resources.TargetResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@RunWith(MockitoJUnitRunner.class)
public class AzureCloudResourceServiceTest {

    private static final String MICROSOFT_COMPUTE_VIRTUAL_MACHINES = "Microsoft.Compute/virtualMachines";

    private static final String VM_NAME = "vmName";

    private static final String INSTANCE_1 = "instance-1";

    private static final String INSTANCE_2 = "instance-2";

    private static final String INSTANCE_3 = "instance-3";

    private static final String STACK_NAME = "stackName";

    private static final String STORAGE_1 = "storage_1";

    private static final String IP_1 = "ip_1";

    @Mock
    private AzureUtils azureUtils;

    @InjectMocks
    private AzureCloudResourceService underTest;

    @Spy
    private Deployment deployment;

    @Test
    public void getListWithKnownSuccededCloudResource() {
        TargetResource t = new TargetResource();
        t.withResourceType(MICROSOFT_COMPUTE_VIRTUAL_MACHINES);
        t.withResourceName(VM_NAME);
        PagedList<DeploymentOperation> operationList = Mockito.spy(PagedList.class);
        DeploymentOperations operations = Mockito.mock(DeploymentOperations.class);
        DeploymentOperation operation = Mockito.mock(DeploymentOperation.class);
        operationList.add(operation);

        when(deployment.deploymentOperations()).thenReturn(operations);
        when(deployment.deploymentOperations().list()).thenReturn(operationList);
        when(operation.targetResource()).thenReturn(t);
        when(operation.provisioningState()).thenReturn("succeeded");

        List<CloudResource> cloudResourceList = underTest.getCloudResources(deployment);

        assertEquals(1, cloudResourceList.size());
        CloudResource cloudResource = cloudResourceList.get(0);
        assertEquals(VM_NAME, cloudResource.getName());
        assertEquals(CommonStatus.CREATED, cloudResource.getStatus());
    }

    @Test
    public void getListWithKnownFailedCloudResource() {
        TargetResource t = new TargetResource();
        t.withResourceType(MICROSOFT_COMPUTE_VIRTUAL_MACHINES);
        t.withResourceName(VM_NAME);
        PagedList<DeploymentOperation> operationList = Mockito.spy(PagedList.class);
        DeploymentOperations operations = Mockito.mock(DeploymentOperations.class);
        DeploymentOperation operation = Mockito.mock(DeploymentOperation.class);
        operationList.add(operation);

        when(deployment.deploymentOperations()).thenReturn(operations);
        when(deployment.deploymentOperations().list()).thenReturn(operationList);
        when(operation.targetResource()).thenReturn(t);
        when(operation.provisioningState()).thenReturn("failed");

        List<CloudResource> cloudResourceList = underTest.getCloudResources(deployment);

        assertEquals(1, cloudResourceList.size());
        CloudResource cloudResource = cloudResourceList.get(0);
        assertEquals(VM_NAME, cloudResource.getName());
        assertEquals(CommonStatus.FAILED, cloudResource.getStatus());
    }

    @Test
    public void getListWithUnknownCloudResource() {
        TargetResource t = new TargetResource();
        t.withResourceType("unknown");
        t.withResourceName(VM_NAME);
        PagedList<DeploymentOperation> operationList = Mockito.spy(PagedList.class);
        DeploymentOperations operations = Mockito.mock(DeploymentOperations.class);
        DeploymentOperation operation = Mockito.mock(DeploymentOperation.class);
        operationList.add(operation);

        when(deployment.deploymentOperations()).thenReturn(operations);
        when(deployment.deploymentOperations().list()).thenReturn(operationList);
        when(operation.targetResource()).thenReturn(t);
        when(operation.provisioningState()).thenReturn("succeeded");

        List<CloudResource> cloudResourceList = underTest.getCloudResources(deployment);

        assertEquals(0, cloudResourceList.size());
    }

    @Test
    public void getInstanceCloudResourcesInstancesFound() {

        List<Group> groupList = new ArrayList<>();
        Group group = Mockito.mock(Group.class);
        CloudInstance instance = Mockito.mock(CloudInstance.class);
        InstanceTemplate instanceTemplate = Mockito.mock(InstanceTemplate.class);
        CloudResource vm1 = createCloudResource(INSTANCE_1, ResourceType.AZURE_INSTANCE);
        CloudResource vm2 = createCloudResource(INSTANCE_2, ResourceType.AZURE_INSTANCE);
        CloudResource vm3 = createCloudResource(INSTANCE_3, ResourceType.AZURE_INSTANCE);
        CloudResource storage1 = createCloudResource(STORAGE_1, ResourceType.AZURE_STORAGE);
        CloudResource ip1 = createCloudResource(IP_1, ResourceType.AZURE_PUBLIC_IP);
        List<CloudResource> cloudResourceList = List.of(vm1, vm2, vm3, storage1, ip1);

        when(instance.getTemplate()).thenReturn(instanceTemplate);
        when(instance.getTemplate().getPrivateId()).thenReturn(1L);
        when(instance.getTemplate().getStatus()).thenReturn(InstanceStatus.CREATE_REQUESTED);
        when(azureUtils.getPrivateInstanceId(any(), any(), any()))
                .thenReturn(INSTANCE_1)
                .thenReturn(INSTANCE_2)
                .thenReturn(INSTANCE_3);
        when(group.getInstances()).thenReturn(List.of(instance));
        groupList.add(group);

        List<CloudResource> resourceList = underTest.getInstanceCloudResources(STACK_NAME, cloudResourceList, groupList, "resourceGroupName");

        assertEquals(3, resourceList.size());
        assertEquals(INSTANCE_1, resourceList.get(0).getName());
        assertEquals(INSTANCE_2, resourceList.get(1).getName());
        assertEquals(INSTANCE_3, resourceList.get(2).getName());
    }

    @Test
    public void getInstanceCloudResourcesInstancesNotFound() {
        List<Group> groupList = new ArrayList<>();
        Group group = Mockito.mock(Group.class);
        CloudResource storage1 = createCloudResource(STORAGE_1, ResourceType.AZURE_STORAGE);
        CloudResource ip1 = createCloudResource(IP_1, ResourceType.AZURE_PUBLIC_IP);
        List<CloudResource> cloudResourceList = List.of(storage1, ip1);
        groupList.add(group);

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                () -> underTest.getInstanceCloudResources(STACK_NAME, cloudResourceList, groupList, "resourceGroupName"));

        assertTrue(exception.getMessage().contains("No VM resource found amongst cloud resources"));
    }

    private CloudResource createCloudResource(String name, ResourceType resourceType) {
        return createCloudResource(name, resourceType, CommonStatus.CREATED, null);
    }

    private CloudResource createCloudResource(String name, ResourceType resourceType, CommonStatus status, String instanceId) {
        return new CloudResource.Builder()
                .name(name)
                .status(status)
                .type(resourceType)
                .instanceId(instanceId)
                .params(Collections.emptyMap())
                .build();
    }

}