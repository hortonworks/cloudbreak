package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.common.api.type.ResourceType.AZURE_AVAILABILITY_SET;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_DATABASE;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_DATABASE_SECURITY_ALERT_POLICY;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_DISK;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_DNS_ZONE_GROUP;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_INSTANCE;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_LOAD_BALANCER;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_MANAGED_IMAGE;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_NETWORK;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_NETWORK_INTERFACE;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_PRIVATE_DNS_ZONE;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_PRIVATE_ENDPOINT;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_PUBLIC_IP;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_RESOURCE_GROUP;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_SECURITY_GROUP;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_STORAGE;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_SUBNET;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_VIRTUAL_NETWORK_LINK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import com.azure.core.http.rest.PagedIterable;
import com.azure.resourcemanager.compute.models.ManagedDiskParameters;
import com.azure.resourcemanager.compute.models.OSDisk;
import com.azure.resourcemanager.compute.models.StorageAccountTypes;
import com.azure.resourcemanager.compute.models.StorageProfile;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import com.azure.resourcemanager.network.models.Subnet;
import com.azure.resourcemanager.resources.models.Deployment;
import com.azure.resourcemanager.resources.models.DeploymentOperation;
import com.azure.resourcemanager.resources.models.DeploymentOperations;
import com.azure.resourcemanager.resources.models.TargetResource;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureListResult;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureListResultFactory;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureExceptionHandler;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
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

    @Mock
    private AuthenticatedContext ac;

    @Mock
    private AzureClient azureClient;

    @InjectMocks
    private AzureCloudResourceService underTest;

    @Spy
    private Deployment deployment;

    @Mock
    private AzureListResultFactory azureListResultFactory;

    @Spy
    private AzureExceptionHandler azureExceptionHandler;

    @BeforeEach
    void init() {
        lenient().when(azureListResultFactory.create(any())).thenAnswer((Answer<AzureListResult>) invocationOnMock -> {
            PagedIterable pagedIterable = invocationOnMock.getArgument(0, PagedIterable.class);
            return new AzureListResult(pagedIterable, azureExceptionHandler);
        });
        lenient().when(azureListResultFactory.list(any())).thenCallRealMethod();
    }

    @Test
    public void getListWithKnownSucceededCloudResources() {
        ArrayList<DeploymentOperation> deploymentOperations = getDeploymentOperations();
        PagedIterable<DeploymentOperation> operationList = mock(PagedIterable.class);
        when(operationList.stream()).thenReturn(deploymentOperations.stream());
        DeploymentOperations operations = mock(DeploymentOperations.class);
        when(deployment.deploymentOperations()).thenReturn(operations);
        when(deployment.deploymentOperations().list()).thenReturn(operationList);

        List<CloudResource> cloudResourceList = underTest.getDeploymentCloudResources(deployment);

        assertEquals(15, cloudResourceList.size());
        assertEquals(AZURE_AVAILABILITY_SET, cloudResourceList.get(0).getType());
        assertEquals(AZURE_INSTANCE, cloudResourceList.get(1).getType());
        assertEquals(AZURE_SECURITY_GROUP, cloudResourceList.get(2).getType());
        assertEquals(AZURE_PUBLIC_IP, cloudResourceList.get(3).getType());
        assertEquals(AZURE_NETWORK_INTERFACE, cloudResourceList.get(4).getType());
        assertEquals(AZURE_NETWORK, cloudResourceList.get(5).getType());
        assertEquals(AZURE_PRIVATE_ENDPOINT, cloudResourceList.get(6).getType());
        assertEquals(AZURE_DATABASE, cloudResourceList.get(7).getType());
        assertEquals(AZURE_DATABASE_SECURITY_ALERT_POLICY, cloudResourceList.get(8).getType());
        assertEquals(AZURE_MANAGED_IMAGE, cloudResourceList.get(9).getType());
        assertEquals(AZURE_DISK, cloudResourceList.get(10).getType());
        assertEquals(AZURE_STORAGE, cloudResourceList.get(11).getType());
        assertEquals(AZURE_PRIVATE_DNS_ZONE, cloudResourceList.get(12).getType());
        assertEquals(AZURE_VIRTUAL_NETWORK_LINK, cloudResourceList.get(13).getType());
        assertEquals(AZURE_LOAD_BALANCER, cloudResourceList.get(14).getType());
        assertTrue(cloudResourceList.stream().noneMatch(cloudResource -> cloudResource.getType() == AZURE_DNS_ZONE_GROUP));
        assertTrue(cloudResourceList.stream().allMatch(cloudResource -> CommonStatus.CREATED.equals(cloudResource.getStatus())));
        assertTrue(cloudResourceList.stream().allMatch(cloudResource -> cloudResource.getName().equals(VM_NAME)));
    }

    private ArrayList<DeploymentOperation> getDeploymentOperations() {
        List<String> resources = List.of(
                "Microsoft.Compute/availabilitySets",
                "Microsoft.Compute/virtualMachines",
                "Microsoft.Network/networkSecurityGroups",
                "Microsoft.Network/publicIPAddresses",
                "Microsoft.Network/networkInterfaces",
                "Microsoft.Network/virtualNetworks",
                "Microsoft.Network/privateEndpoints",
                "Microsoft.DBforPostgreSQL/servers",
                "Microsoft.DBforPostgreSQL/servers/securityAlertPolicies",
                "Microsoft.Compute/images",
                "Microsoft.Compute/disks",
                "Microsoft.Storage/storageAccounts",
                "Microsoft.Network/privateDnsZones",
                "Microsoft.Network/privateDnsZones/virtualNetworkLinks",
                "Microsoft.Network/loadBalancers",
                "Microsoft.Network/privateEndpoints/privateDnsZoneGroups");

        ArrayList<DeploymentOperation> deploymentOperations = new ArrayList<>();

        resources.forEach(resource -> {
            TargetResource t = new TargetResource();
            t.withResourceType(resource);
            t.withResourceName(VM_NAME);
            DeploymentOperation operation = mock(DeploymentOperation.class);
            when(operation.targetResource()).thenReturn(t);
            when(operation.provisioningState()).thenReturn("succeeded");
            deploymentOperations.add(operation);
        });
        return deploymentOperations;
    }

    @Test
    public void getListWithKnownFailedCloudResource() {
        TargetResource t = new TargetResource();
        t.withResourceType(MICROSOFT_COMPUTE_VIRTUAL_MACHINES);
        t.withResourceName(VM_NAME);
        PagedIterable<DeploymentOperation> operationList = mock(PagedIterable.class);
        DeploymentOperations operations = mock(DeploymentOperations.class);
        DeploymentOperation operation = mock(DeploymentOperation.class);
        when(operationList.stream()).thenReturn(Stream.of(operation));

        when(deployment.deploymentOperations()).thenReturn(operations);
        when(deployment.deploymentOperations().list()).thenReturn(operationList);
        when(operation.targetResource()).thenReturn(t);
        when(operation.provisioningState()).thenReturn("failed");

        List<CloudResource> cloudResourceList = underTest.getDeploymentCloudResources(deployment);

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
        PagedIterable<DeploymentOperation> operationList = mock(PagedIterable.class);
        DeploymentOperations operations = mock(DeploymentOperations.class);
        DeploymentOperation operation = mock(DeploymentOperation.class);
        when(operationList.stream()).thenReturn(Stream.of(operation));

        when(deployment.deploymentOperations()).thenReturn(operations);
        when(deployment.deploymentOperations().list()).thenReturn(operationList);
        when(operation.targetResource()).thenReturn(t);
        when(operation.provisioningState()).thenReturn("succeeded");

        List<CloudResource> cloudResourceList = underTest.getDeploymentCloudResources(deployment);

        assertEquals(0, cloudResourceList.size());
    }

    @Test
    public void getInstanceCloudResourcesInstancesFound() {

        List<Group> groupList = new ArrayList<>();
        Group group = mock(Group.class);
        InstanceTemplate instanceTemplate = mock(InstanceTemplate.class);
        CloudInstance instance = new CloudInstance("i-1", instanceTemplate, mock(InstanceAuthentication.class), "subnet-1", "az-1");
        CloudResource vm1 = createCloudResource(INSTANCE_1, ResourceType.AZURE_INSTANCE);
        CloudResource vm2 = createCloudResource(INSTANCE_2, ResourceType.AZURE_INSTANCE);
        CloudResource vm3 = createCloudResource(INSTANCE_3, ResourceType.AZURE_INSTANCE);
        CloudResource storage1 = createCloudResource(STORAGE_1, ResourceType.AZURE_STORAGE);
        CloudResource ip1 = createCloudResource(IP_1, ResourceType.AZURE_PUBLIC_IP);
        List<CloudResource> cloudResourceList = List.of(vm1, vm2, vm3, storage1, ip1);

        when(instance.getTemplate().getStatus()).thenReturn(InstanceStatus.CREATE_REQUESTED);
        when(azureUtils.getFullInstanceId(any(), any(), any(), any()))
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
    public void getAttachedOsDiskResourcesFound() {
        CloudResource vm1 = createCloudResource(INSTANCE_1, ResourceType.AZURE_INSTANCE);
        CloudResource vm2 = createCloudResource(INSTANCE_2, ResourceType.AZURE_INSTANCE);
        CloudResource vm3 = createCloudResource(INSTANCE_3, ResourceType.AZURE_INSTANCE);
        AzureListResult<VirtualMachine> virtualMachines = mock(AzureListResult.class);
        VirtualMachine vm = mock(VirtualMachine.class);
        StorageProfile storageProfile = mock(StorageProfile.class);
        OSDisk osDisk = mock(OSDisk.class);
        ManagedDiskParameters managedDiskParameters = mock(ManagedDiskParameters.class);
        when(virtualMachines.getStream()).thenAnswer(invocation -> Stream.of(vm));

        when(vm.name()).thenReturn(INSTANCE_1);
        when(vm.storageProfile()).thenReturn(storageProfile);
        when(vm.storageProfile().osDisk()).thenReturn(osDisk);
        when(osDisk.managedDisk()).thenReturn(managedDiskParameters);
        when(osDisk.diskSizeGB()).thenReturn(200);
        when(managedDiskParameters.storageAccountType()).thenReturn(StorageAccountTypes.STANDARD_SSD_LRS);
        when(managedDiskParameters.id()).thenReturn("diskId1");
        when(osDisk.name()).thenReturn("diskName1");

        when(azureClient.getVirtualMachines("resourceGroupName")).thenReturn(virtualMachines);

        List<CloudResource> osDiskResources = underTest.getAttachedOsDiskResources(List.of(vm1, vm2, vm3), "resourceGroupName", azureClient);

        assertEquals(1, osDiskResources.size());
        CloudResource diskResource = osDiskResources.get(0);
        VolumeSetAttributes volumeSetAttributes = diskResource.getParameter("attributes", VolumeSetAttributes.class);
        assertEquals("diskName1", diskResource.getName());
        assertEquals("diskId1", diskResource.getReference());
        assertEquals("test", diskResource.getGroup());
        assertEquals(200, volumeSetAttributes.getVolumes().get(0).getSize());
        assertEquals("StandardSSD_LRS", volumeSetAttributes.getVolumes().get(0).getType());
    }

    private CloudResource createCloudResource(String name, ResourceType resourceType) {
        return createCloudResource(name, resourceType, CommonStatus.CREATED, null);
    }

    private CloudResource createCloudResource(String name, ResourceType resourceType, CommonStatus status, String instanceId) {
        return CloudResource.builder()
                .withName(name)
                .withStatus(status)
                .withType(resourceType)
                .withInstanceId(instanceId)
                .withGroup("test")
                .build();
    }

    @Test
    public void testCollectAndSaveNetworkAndSubnetNoSubnetsProvided() {
        String resourceGroupName = "testResourceGroup";
        String virtualNetwork = "testVNet";
        List<String> subnetNameList = new ArrayList<>();
        PersistenceNotifier notifier = mock(PersistenceNotifier.class);
        CloudContext cloudContext = mock(CloudContext.class);
        when(cloudContext.getPlatform()).thenReturn(Platform.platform("AZURE"));
        Network network = mock(Network.class);
        AzureClient client = mock(AzureClient.class);

        Subnet mockedSubnet = mock(Subnet.class);
        when(mockedSubnet.name()).thenReturn("defaultSubnet");
        com.azure.resourcemanager.network.models.Network mockedNetwork = mock(com.azure.resourcemanager.network.models.Network.class);
        when(mockedSubnet.parent()).thenReturn(mockedNetwork);
        when(mockedNetwork.name()).thenReturn(virtualNetwork);
        Map subnets = mock(HashMap.class);
        when(client.getSubnets(resourceGroupName, virtualNetwork)).thenReturn(subnets);
        when(subnets.values()).thenReturn(List.of(mockedSubnet));

        List<CloudResource> resources = underTest.collectAndSaveNetworkAndSubnet(
                resourceGroupName, virtualNetwork, notifier, cloudContext, subnetNameList, network, client);

        assertEquals(3, resources.size());
        assertEquals("testResourceGroup", resources.get(0).getName());
        assertEquals(AZURE_RESOURCE_GROUP, resources.get(0).getType());
        assertEquals("testVNet", resources.get(1).getName());
        assertEquals(AZURE_NETWORK, resources.get(1).getType());
        assertEquals("defaultSubnet", resources.get(2).getName());
        assertEquals(AZURE_SUBNET, resources.get(2).getType());
    }

    @Test
    public void testCollectAndSaveNetworkAndSubnetWithProvidedSubnets() {
        String resourceGroupName = "testResourceGroup";
        String virtualNetwork = "testVNet";
        List<String> subnetNameList = List.of("subnet1", "subnet2");
        PersistenceNotifier notifier = mock(PersistenceNotifier.class);
        CloudContext cloudContext = mock(CloudContext.class);
        when(cloudContext.getPlatform()).thenReturn(Platform.platform("AZURE"));
        Network network = mock(Network.class);
        AzureClient client = mock(AzureClient.class);

        when(azureUtils.getCustomNetworkId(network)).thenReturn(virtualNetwork);
        when(azureUtils.getCustomResourceGroupName(network)).thenReturn(resourceGroupName);

        List<CloudResource> resources = underTest.collectAndSaveNetworkAndSubnet(
                resourceGroupName, virtualNetwork, notifier, cloudContext, subnetNameList, network, client);

        assertEquals(4, resources.size());
        assertEquals("testResourceGroup", resources.get(0).getName());
        assertEquals(AZURE_RESOURCE_GROUP, resources.get(0).getType());
        assertEquals("testVNet", resources.get(1).getName());
        assertEquals(AZURE_NETWORK, resources.get(1).getType());
        assertEquals("subnet1", resources.get(2).getName());
        assertEquals(AZURE_SUBNET, resources.get(2).getType());
        assertEquals("subnet2", resources.get(3).getName());
        assertEquals(AZURE_SUBNET, resources.get(3).getType());
    }

    @Test
    public void testCollectAndSaveNetworkAndSubnetVerifyNotifierCalls() {
        String resourceGroupName = "testResourceGroup";
        String virtualNetwork = "testVNet";
        List<String> subnetNameList = List.of("subnet1");
        PersistenceNotifier notifier = mock(PersistenceNotifier.class);
        CloudContext cloudContext = mock(CloudContext.class);
        when(cloudContext.getPlatform()).thenReturn(Platform.platform("AZURE"));
        Network network = mock(Network.class);
        AzureClient client = mock(AzureClient.class);

        when(azureUtils.getCustomNetworkId(network)).thenReturn(virtualNetwork);
        when(azureUtils.getCustomResourceGroupName(network)).thenReturn(resourceGroupName);

        underTest.collectAndSaveNetworkAndSubnet(
                resourceGroupName, virtualNetwork, notifier, cloudContext, subnetNameList, network, client);

        verify(notifier, times(3)).notifyAllocation(any(CloudResource.class), eq(cloudContext));
    }
}