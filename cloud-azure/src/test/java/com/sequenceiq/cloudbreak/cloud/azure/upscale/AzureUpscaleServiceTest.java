package com.sequenceiq.cloudbreak.cloud.azure.upscale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.microsoft.azure.CloudError;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.resources.Deployment;
import com.microsoft.azure.management.resources.DeploymentExportResult;
import com.sequenceiq.cloudbreak.cloud.azure.AzureCloudResourceService;
import com.sequenceiq.cloudbreak.cloud.azure.AzureInstanceTemplateOperation;
import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceGroupMetadataProvider;
import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.connector.resource.AzureComputeResourceService;
import com.sequenceiq.cloudbreak.cloud.azure.template.AzureTemplateDeploymentService;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureStackView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.notification.ResourceNotifier;
import com.sequenceiq.cloudbreak.cloud.transform.CloudResourceHelper;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@RunWith(MockitoJUnitRunner.class)
public class AzureUpscaleServiceTest {

    private static final Map<String, Object> PARAMETERS = Collections.emptyMap();

    private static final String STACK_NAME = "Test Cluster";

    private static final String RESOURCE_GROUP = "resource group";

    private static final List<CloudResource> NETWORK_RESOURCES = List.of(mock(CloudResource.class));

    private static final String TEMPLATE = "template";

    @InjectMocks
    private AzureUpscaleService underTest;

    @Mock
    private AzureTemplateDeploymentService azureTemplateDeploymentService;

    @Mock
    private AzureUtils azureUtils;

    @Mock
    private CloudResourceHelper cloudResourceHelper;

    @Mock
    private AzureComputeResourceService azureComputeResourceService;

    @Mock
    private AzureClient client;

    @Mock
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @Mock
    private CloudStack stack;

    @Mock
    private AzureStackView azureStackView;

    @Mock
    private Deployment templateDeployment;

    @Mock
    private ResourceNotifier resourceNotifier;

    @Mock
    private AzureCloudResourceService azureCloudResourceService;

    @Before
    public void before() {
        when(azureUtils.getStackName(any(CloudContext.class))).thenReturn(STACK_NAME);
        when(azureResourceGroupMetadataProvider.getResourceGroupName(any(CloudContext.class), eq(stack))).thenReturn(RESOURCE_GROUP);
    }

    @Test
    public void testUpscaleThenThereAreNewInstancesRequired() {
        CloudContext cloudContext = createCloudContext();
        AuthenticatedContext ac = new AuthenticatedContext(cloudContext, null);
        CloudResource template = createCloudResource(TEMPLATE, ResourceType.ARM_TEMPLATE);
        List<CloudResource> resources = List.of(createCloudResource("volumes", ResourceType.AZURE_VOLUMESET), template);
        List<Group> scaledGroups = createScaledGroups();

        when(cloudResourceHelper.getScaledGroups(stack)).thenReturn(scaledGroups);
        when(azureTemplateDeploymentService.getTemplateDeployment(client, stack, ac, azureStackView, AzureInstanceTemplateOperation.UPSCALE))
                .thenReturn(templateDeployment);
        when(templateDeployment.exportTemplate()).thenReturn(mock(DeploymentExportResult.class));
        CloudResource newInstance = CloudResource.builder().instanceId("instanceid").type(ResourceType.AZURE_INSTANCE).status(CommonStatus.CREATED)
                .name("instance").params(Map.of()).build();
        List<CloudResource> newInstances = List.of(newInstance);
        when(azureCloudResourceService.getDeploymentCloudResources(templateDeployment)).thenReturn(newInstances);
        when(azureCloudResourceService.getInstanceCloudResources(STACK_NAME, newInstances, scaledGroups, RESOURCE_GROUP)).thenReturn(newInstances);

        when(azureCloudResourceService.getNetworkResources(resources)).thenReturn(NETWORK_RESOURCES);

        AdjustmentTypeWithThreshold adjustmentTypeWithThreshold = new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, 0L);
        List<CloudResourceStatus> actual = underTest.upscale(ac, stack, resources, azureStackView, client,
                adjustmentTypeWithThreshold);

        assertFalse(actual.isEmpty());
        assertEquals(template, actual.get(0).getCloudResource());
        assertEquals(ResourceStatus.IN_PROGRESS, actual.get(0).getStatus());

        verify(cloudResourceHelper).getScaledGroups(stack);
        verify(azureTemplateDeploymentService).getTemplateDeployment(client, stack, ac, azureStackView, AzureInstanceTemplateOperation.UPSCALE);
        verify(templateDeployment).exportTemplate();
        verify(azureCloudResourceService).getInstanceCloudResources(STACK_NAME, newInstances, scaledGroups, RESOURCE_GROUP);
        verify(azureCloudResourceService).getNetworkResources(resources);
        verify(azureUtils).getStackName(any(CloudContext.class));
        verify(azureResourceGroupMetadataProvider).getResourceGroupName(any(CloudContext.class), eq(stack));
        verify(azureComputeResourceService).buildComputeResourcesForUpscale(ac, stack, scaledGroups, newInstances, List.of(), NETWORK_RESOURCES,
                adjustmentTypeWithThreshold);
    }

    @Test
    public void testUpscaleWhenThereAreReattachableVolumeSets() {
        CloudContext cloudContext = createCloudContext();
        AuthenticatedContext ac = new AuthenticatedContext(cloudContext, null);
        CloudResource template = createCloudResource(TEMPLATE, ResourceType.ARM_TEMPLATE);
        CloudResource notReattachableVolumeSet = createCloudResource("volumes", ResourceType.AZURE_VOLUMESET);
        CloudResource detachedVolumeSet = createCloudResource("detachedvolumes", ResourceType.AZURE_VOLUMESET, CommonStatus.DETACHED, null);
        CloudResource alreadyCreatedVolumeSet = createCloudResource("alreadycreatedvolumes", ResourceType.AZURE_VOLUMESET, CommonStatus.CREATED,
                "instanceid");
        List<CloudResource> resources = List.of(detachedVolumeSet, alreadyCreatedVolumeSet, notReattachableVolumeSet, template);
        List<Group> scaledGroups = createScaledGroups();

        when(cloudResourceHelper.getScaledGroups(stack)).thenReturn(scaledGroups);
        when(azureTemplateDeploymentService.getTemplateDeployment(client, stack, ac, azureStackView, AzureInstanceTemplateOperation.UPSCALE))
                .thenReturn(templateDeployment);
        when(templateDeployment.exportTemplate()).thenReturn(mock(DeploymentExportResult.class));
        CloudResource newInstance = CloudResource.builder().instanceId("instanceid").type(ResourceType.AZURE_INSTANCE).status(CommonStatus.CREATED)
                .name("instance").params(Map.of()).build();
        List<CloudResource> newInstances = List.of(newInstance);
        when(azureCloudResourceService.getDeploymentCloudResources(templateDeployment)).thenReturn(newInstances);
        when(azureCloudResourceService.getInstanceCloudResources(STACK_NAME, newInstances, scaledGroups, RESOURCE_GROUP)).thenReturn(newInstances);
        when(azureCloudResourceService.getNetworkResources(resources)).thenReturn(NETWORK_RESOURCES);

        AdjustmentTypeWithThreshold adjustmentTypeWithThreshold = new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, 0L);
        underTest.upscale(ac, stack, resources, azureStackView, client, adjustmentTypeWithThreshold);

        ArgumentCaptor<List<CloudResource>> reattachCaptor = ArgumentCaptor.forClass(List.class);
        verify(azureComputeResourceService).buildComputeResourcesForUpscale(eq(ac), eq(stack), eq(scaledGroups), eq(newInstances), reattachCaptor.capture(),
                eq(NETWORK_RESOURCES), eq(adjustmentTypeWithThreshold));
        List<CloudResource> reattachableVolumeSets = reattachCaptor.getValue();
        assertEquals(2, reattachableVolumeSets.size());
        assertEquals(detachedVolumeSet, reattachableVolumeSets.get(0));
        assertEquals(alreadyCreatedVolumeSet, reattachableVolumeSets.get(1));
    }

    @Test
    public void testUpscaleWhenVmsNotStartedInTime() {
        CloudContext cloudContext = createCloudContext();
        AuthenticatedContext ac = new AuthenticatedContext(cloudContext, null);
        CloudResource template = createCloudResource(TEMPLATE, ResourceType.ARM_TEMPLATE);
        List<CloudResource> resources = List.of(createCloudResource("volumes", ResourceType.AZURE_VOLUMESET), template);
        List<Group> scaledGroups = createScaledGroups();

        when(cloudResourceHelper.getScaledGroups(stack)).thenReturn(scaledGroups);
        CloudError cloudError = new CloudError().withMessage("Error happened");
        cloudError.details().add(new CloudError().withMessage("Please check the power state later"));
        when(azureTemplateDeploymentService.getTemplateDeployment(client, stack, ac, azureStackView, AzureInstanceTemplateOperation.UPSCALE))
                .thenThrow(new Retry.ActionFailedException("VMs not started in time.", new CloudException("Error", null, cloudError)));
        when(azureUtils.convertToCloudConnectorException(any(CloudException.class), anyString())).thenCallRealMethod();
        when(azureUtils.convertToCloudConnectorException(any(Throwable.class), anyString())).thenCallRealMethod();
        CloudConnectorException cloudConnectorException = assertThrows(CloudConnectorException.class, () ->
                underTest.upscale(ac, stack, resources, azureStackView, client, new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, 0L))
        );

        assertThat(cloudConnectorException.getMessage())
                .contains("Stack upscale failed, status code null, error message: Error happened, details: Please check the power state later");
    }

    private List<Group> createScaledGroups() {
        Group group = mock(Group.class);
        return Collections.singletonList(group);
    }

    private CloudContext createCloudContext() {
        Location location = Location.location(Region.region("us-west-1"), AvailabilityZone.availabilityZone("us-west-1"));
        return CloudContext.Builder.builder()
                .withName(STACK_NAME)
                .withLocation(location)
                .build();
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
                .params(PARAMETERS)
                .build();
    }
}
