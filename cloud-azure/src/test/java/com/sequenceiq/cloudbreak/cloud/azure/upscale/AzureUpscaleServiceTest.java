package com.sequenceiq.cloudbreak.cloud.azure.upscale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.microsoft.azure.Page;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.resources.Deployment;
import com.microsoft.azure.management.resources.DeploymentExportResult;
import com.microsoft.rest.RestException;
import com.sequenceiq.cloudbreak.cloud.azure.AzureDiskType;
import com.sequenceiq.cloudbreak.cloud.azure.AzureStorage;
import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.connector.resource.AzureComputeResourceService;
import com.sequenceiq.cloudbreak.cloud.azure.template.AzureTemplateDeploymentService;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureStackView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.transform.CloudResourceHelper;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@RunWith(MockitoJUnitRunner.class)
public class AzureUpscaleServiceTest {

    private static final Map<String, Object> PARAMETERS = Collections.emptyMap();

    private static final String STACK_NAME = "Test Cluster";

    private static final String RESOURCE_GROUP = "resource group";

    private static final List<CloudResource> NEW_INSTANCES = List.of(mock(CloudResource.class));

    private static final List<CloudResource> NETWORK_RESOURCES = List.of(mock(CloudResource.class));

    private static final String INSTANCE_ID = "instanceId";

    private static final String TEMPLATE = "template";

    @InjectMocks
    private AzureUpscaleService underTest;

    @Mock
    private AzureTemplateDeploymentService azureTemplateDeploymentService;

    @Mock
    private AzureUtils azureUtils;

    @Mock
    private AzureStorage azureStorage;

    @Mock
    private CloudResourceHelper cloudResourceHelper;

    @Mock
    private AzureComputeResourceService azureComputeResourceService;

    @Mock
    private AzureClient client;

    @Mock
    private CloudStack stack;

    @Mock
    private AzureStackView azureStackView;

    @Mock
    private Deployment templateDeployment;

    @Before
    public void before() {
        when(azureStackView.getStorageAccounts()).thenReturn(Map.of("storageAccount", AzureDiskType.STANDARD_SSD_LRS));
        when(azureUtils.getStackName(any(CloudContext.class))).thenReturn(STACK_NAME);
        when(azureUtils.getResourceGroupName(any(CloudContext.class), eq(stack))).thenReturn(RESOURCE_GROUP);
        when(stack.getParameters()).thenReturn(Collections.emptyMap());
        when(azureStorage.isEncrytionNeeded(any())).thenReturn(true);
    }

    @Test
    public void testUpscaleThenThereAreNewInstancesRequired() {
        CloudContext cloudContext = createCloudContext();
        AuthenticatedContext ac = new AuthenticatedContext(cloudContext, null);
        CloudResource template = createCloudResource(TEMPLATE, ResourceType.ARM_TEMPLATE);
        List<CloudResource> resources = List.of(createCloudResource("volumes", ResourceType.AZURE_VOLUMESET), template);
        List<Group> scaledGroups = createScaledGroups();
        PagedList<VirtualMachine> virtualMachines = createVirtualMachinesWithOneInstance();

        when(cloudResourceHelper.getScaledGroups(stack)).thenReturn(scaledGroups);
        when(azureTemplateDeploymentService.getTemplateDeployment(client, stack, ac, azureStackView)).thenReturn(templateDeployment);
        when(templateDeployment.exportTemplate()).thenReturn(mock(DeploymentExportResult.class));
        when(azureUtils.getInstanceCloudResources(cloudContext, templateDeployment, scaledGroups)).thenReturn(NEW_INSTANCES);
        when(cloudResourceHelper.getNetworkResources(resources)).thenReturn(NETWORK_RESOURCES);
        when(client.getVirtualMachines(RESOURCE_GROUP)).thenReturn(virtualMachines);

        List<CloudResourceStatus> actual = underTest.upscale(ac, stack, resources, azureStackView, client);

        assertFalse(actual.isEmpty());
        assertEquals(template, actual.get(0).getCloudResource());
        assertEquals(ResourceStatus.IN_PROGRESS, actual.get(0).getStatus());

        verify(cloudResourceHelper).getScaledGroups(stack);
        verify(azureTemplateDeploymentService).getTemplateDeployment(client, stack, ac, azureStackView);
        verify(templateDeployment).exportTemplate();
        verify(azureUtils).getInstanceCloudResources(cloudContext, templateDeployment, scaledGroups);
        verify(cloudResourceHelper).getNetworkResources(resources);
        verify(client).getVirtualMachines(RESOURCE_GROUP);
        verify(azureStackView).getStorageAccounts();
        verify(azureUtils).getStackName(any(CloudContext.class));
        verify(azureUtils).getResourceGroupName(any(CloudContext.class), eq(stack));
        verify(stack).getParameters();
        verify(azureStorage).isEncrytionNeeded(any());
    }

    @Test
    public void testUpscaleThenThereAreNoNewInstancesRequired() {
        CloudContext cloudContext = createCloudContext();
        AuthenticatedContext ac = new AuthenticatedContext(cloudContext, null);
        CloudResource template = createCloudResource(TEMPLATE, ResourceType.ARM_TEMPLATE);
        List<CloudResource> resources = List.of(createCloudResource("volumes", ResourceType.AZURE_VOLUMESET), template);
        List<Group> scaledGroups = createScaledGroups();
        PagedList<VirtualMachine> virtualMachines = createVirtualMachinesWithTwoInstance();

        when(cloudResourceHelper.getScaledGroups(stack)).thenReturn(scaledGroups);
        when(client.getVirtualMachines(RESOURCE_GROUP)).thenReturn(virtualMachines);

        List<CloudResourceStatus> actual = underTest.upscale(ac, stack, resources, azureStackView, client);

        assertFalse(actual.isEmpty());
        assertEquals(template, actual.get(0).getCloudResource());
        assertEquals(ResourceStatus.IN_PROGRESS, actual.get(0).getStatus());

        verify(azureUtils).getStackName(any(CloudContext.class));
        verify(azureUtils).getResourceGroupName(any(CloudContext.class), eq(stack));
        verify(cloudResourceHelper).getScaledGroups(stack);
        verifyZeroInteractions(azureStackView);
        verifyZeroInteractions(azureStorage);
        verifyZeroInteractions(azureTemplateDeploymentService);
        verifyZeroInteractions(templateDeployment);
        verifyZeroInteractions(azureComputeResourceService);
    }

    private List<Group> createScaledGroups() {
        Group group = mock(Group.class);
        when(group.getInstances()).thenReturn(List.of(createCloudInstance()));
        when(group.getInstancesSize()).thenReturn(1);
        return Collections.singletonList(group);
    }

    private CloudInstance createCloudInstance() {
        return new CloudInstance(INSTANCE_ID, null, null);
    }

    private CloudContext createCloudContext() {
        Location location = Location.location(Region.region("us-west-1"), AvailabilityZone.availabilityZone("us-west-1"));
        return new CloudContext(null, STACK_NAME, null, null, location, null, null, "");
    }

    private PagedList<VirtualMachine> createVirtualMachinesWithOneInstance() {
        VirtualMachine virtualMachine = mock(VirtualMachine.class);
        when(virtualMachine.name()).thenReturn(INSTANCE_ID);
        PagedList pagedList = new PagedList() {
            @Override
            public Page nextPage(String nextPageLink) throws RestException {
                return null;
            }
        };
        pagedList.add(virtualMachine);
        return pagedList;
    }

    private PagedList<VirtualMachine> createVirtualMachinesWithTwoInstance() {
        VirtualMachine virtualMachine1 = mock(VirtualMachine.class);
        VirtualMachine virtualMachine2 = mock(VirtualMachine.class);
        when(virtualMachine1.name()).thenReturn(INSTANCE_ID);
        when(virtualMachine2.name()).thenReturn("instanceId1");
        PagedList pagedList = new PagedList() {
            @Override
            public Page nextPage(String nextPageLink) throws RestException {
                return null;
            }
        };
        pagedList.add(virtualMachine1);
        pagedList.add(virtualMachine2);
        return pagedList;
    }

    private CloudResource createCloudResource(String name, ResourceType resourceType) {
        return new CloudResource.Builder()
                .name(name)
                .status(CommonStatus.CREATED)
                .type(resourceType)
                .params(PARAMETERS)
                .build();
    }

}