package com.sequenceiq.cloudbreak.cloud.azure.upscale;

import static com.sequenceiq.cloudbreak.cloud.model.CloudResource.PRIVATE_ID;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_INSTANCE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.core.management.exception.ManagementError;
import com.azure.resourcemanager.compute.models.ApiError;
import com.azure.resourcemanager.compute.models.ApiErrorException;
import com.azure.resourcemanager.resources.models.Deployment;
import com.azure.resourcemanager.resources.models.DeploymentExportResult;
import com.sequenceiq.cloudbreak.cloud.azure.AzureCloudResourceService;
import com.sequenceiq.cloudbreak.cloud.azure.AzureInstanceTemplateOperation;
import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceGroupMetadataProvider;
import com.sequenceiq.cloudbreak.cloud.azure.AzureTestUtils;
import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.connector.resource.AzureComputeResourceService;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureImageTermsSignerService;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureMarketplaceImage;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureMarketplaceImageProviderService;
import com.sequenceiq.cloudbreak.cloud.azure.template.AzureTemplateDeploymentService;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureImageFormatValidator;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureStackView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.exception.QuotaExceededException;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.cloud.notification.ResourceNotifier;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.cloudbreak.cloud.transform.CloudResourceHelper;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
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
    private ResourceRetriever resourceRetriever;

    @Mock
    private AzureCloudResourceService azureCloudResourceService;

    @Mock
    private AzureScaleUtilService azureScaleUtilService;

    @Mock
    private AzureImageFormatValidator azureImageFormatValidator;

    @Mock
    private AzureMarketplaceImageProviderService azureMarketplaceImageProviderService;

    @Mock
    private AzureImageTermsSignerService azureImageTermsSignerService;

    @BeforeEach
    public void before() {
        when(azureUtils.getStackName(any(CloudContext.class))).thenReturn(STACK_NAME);
        when(azureUtils.getFullInstanceId(nullable(String.class), nullable(String.class), nullable(String.class), nullable(String.class)))
                .thenReturn("instanceid");
        when(azureResourceGroupMetadataProvider.getResourceGroupName(any(CloudContext.class), eq(stack))).thenReturn(RESOURCE_GROUP);
    }

    private void vhdImageStubbing() {
        Image image = new Image("azureImage.vhd", null, null, null, null, null, null, null, null, null);
        when(stack.getImage()).thenReturn(image);
        AzureMarketplaceImage azureMpImage = new AzureMarketplaceImage("", "", "", "");
        when(azureMarketplaceImageProviderService.get(eq(image))).thenReturn(azureMpImage);
        when(azureImageFormatValidator.isMarketplaceImageFormat(eq(image))).thenReturn(true);
    }

    @Test
    public void testUpscaleWhenSourceImageSignAttempted() throws QuotaExceededException {
        CloudContext cloudContext = createCloudContext();
        CloudCredential cloudCredential = mock(CloudCredential.class);
        when(cloudCredential.getParameters()).thenReturn(Map.of("subscriptionId", "subscriptionId"));
        AuthenticatedContext ac = new AuthenticatedContext(cloudContext, cloudCredential);
        CloudResource template = createCloudResource(TEMPLATE, ResourceType.ARM_TEMPLATE);
        List<CloudResource> resources = List.of(createCloudResource("volumes", ResourceType.AZURE_VOLUMESET), template);
        InstanceTemplate instanceTemplate = mock(InstanceTemplate.class);
        when(instanceTemplate.getPrivateId()).thenReturn(1L);
        when(instanceTemplate.getGroupName()).thenReturn("worker");
        when(instanceTemplate.getStatus()).thenReturn(InstanceStatus.CREATE_REQUESTED);
        List<Group> scaledGroups = createScaledGroups(new CloudInstance("instanceid", instanceTemplate, null, null, null));

        Map<String, String> packageVersions = Map.of(ImagePackageVersion.SOURCE_IMAGE.getKey(), "cloudera:cdp-7_2_17:runtime-7_2_17:200.46967063.1701370975");
        Image image = new Image("azureImage.vhd", null, null, null, null, null, null, packageVersions, null, null);
        when(stack.getImage()).thenReturn(image);
        AzureMarketplaceImage azureMpImage = new AzureMarketplaceImage("cloudera", "7_2_17", "runtime-7_2_17", "200.46967063.1701370975");
        when(azureMarketplaceImageProviderService.getSourceImage(eq(image))).thenReturn(azureMpImage);
        when(azureImageFormatValidator.isMarketplaceImageFormat(eq(image))).thenReturn(false);
        when(azureImageFormatValidator.hasSourceImagePlan(eq(image))).thenReturn(true);

        when(cloudResourceHelper.getScaledGroups(stack)).thenReturn(scaledGroups);
        when(azureTemplateDeploymentService.getTemplateDeployment(client, stack, ac, azureStackView, AzureInstanceTemplateOperation.UPSCALE))
                .thenReturn(templateDeployment);
        when(templateDeployment.exportTemplate()).thenReturn(mock(DeploymentExportResult.class));
        CloudResource newInstance = CloudResource.builder().withInstanceId("instanceid").withType(ResourceType.AZURE_INSTANCE).withStatus(CommonStatus.CREATED)
                .withName("instance").withParameters(Map.of()).build();
        List<CloudResource> newInstances = List.of(newInstance);
        when(azureCloudResourceService.getDeploymentCloudResources(templateDeployment)).thenReturn(newInstances);
        when(azureCloudResourceService.getInstanceCloudResources(STACK_NAME, newInstances, scaledGroups, RESOURCE_GROUP)).thenReturn(newInstances);
        when(azureCloudResourceService.getNetworkResources(resources)).thenReturn(NETWORK_RESOURCES);
        when(azureScaleUtilService.getArmTemplate(anyList(), anyString())).thenReturn(template);
        when(resourceRetriever.findAllByStatusAndTypeAndStack(CommonStatus.CREATED, AZURE_INSTANCE, cloudContext.getId())).thenReturn(new ArrayList<>());
        doNothing().when(cloudResourceHelper).updateDeleteOnTerminationFlag(anyList(), anyBoolean(), any());

        AdjustmentTypeWithThreshold adjustmentTypeWithThreshold = new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, 0L);
        List<CloudResourceStatus> actual = underTest.upscale(ac, stack, resources, azureStackView, client,
                adjustmentTypeWithThreshold);

        assertFalse(actual.isEmpty());
        assertEquals(template, actual.get(0).getCloudResource());
        assertEquals(ResourceStatus.IN_PROGRESS, actual.get(0).getStatus());

        verify(azureImageTermsSignerService).signImageTermsIfAllowed(eq(stack), eq(client), eq(azureMpImage), isNull());
        verify(cloudResourceHelper).getScaledGroups(stack);
        verify(azureTemplateDeploymentService).getTemplateDeployment(client, stack, ac, azureStackView, AzureInstanceTemplateOperation.UPSCALE);
        verify(templateDeployment).exportTemplate();
        verify(azureCloudResourceService).getInstanceCloudResources(STACK_NAME, newInstances, scaledGroups, RESOURCE_GROUP);
        verify(azureCloudResourceService).getNetworkResources(resources);
        verify(azureUtils).getStackName(any(CloudContext.class));
        verify(azureResourceGroupMetadataProvider).getResourceGroupName(any(CloudContext.class), eq(stack));
        ArgumentCaptor<List<CloudResource>> newInstancesArgumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(azureComputeResourceService).buildComputeResourcesForUpscale(eq(ac), eq(stack), eq(scaledGroups), newInstancesArgumentCaptor.capture(),
                eq(List.of()), eq(NETWORK_RESOURCES), eq(adjustmentTypeWithThreshold));
        List<CloudResource> cloudResourceList = newInstancesArgumentCaptor.getValue();
        assertThat(cloudResourceList).extracting(CloudResource::getType).containsExactly(AZURE_INSTANCE);
        assertThat(cloudResourceList).extracting(CloudResource::getStatus).containsExactly(CommonStatus.CREATED);
        assertThat(cloudResourceList).extracting(CloudResource::getName).containsExactly("instance");
        assertThat(cloudResourceList).extracting(CloudResource::isPersistent).containsExactly(true);
        assertThat(cloudResourceList).extracting(CloudResource::isStackAware).containsExactly(true);
        assertThat(cloudResourceList).extracting(CloudResource::getInstanceId).containsExactly("instanceid");
        assertThat(cloudResourceList).extracting(CloudResource::getParameters).extracting(map -> map.get(PRIVATE_ID)).containsExactly(1L);
        verify(cloudResourceHelper, times(1)).updateDeleteOnTerminationFlag(any(), eq(false), any());
        verify(cloudResourceHelper, times(1)).updateDeleteOnTerminationFlag(any(), eq(true), any());
    }

    @Test
    public void testUpscaleThenThereAreNewInstancesRequired() throws QuotaExceededException {
        vhdImageStubbing();

        CloudContext cloudContext = createCloudContext();
        CloudCredential cloudCredential = mock(CloudCredential.class);
        when(cloudCredential.getParameters()).thenReturn(Map.of("subscriptionId", "subscriptionId"));
        AuthenticatedContext ac = new AuthenticatedContext(cloudContext, cloudCredential);
        CloudResource template = createCloudResource(TEMPLATE, ResourceType.ARM_TEMPLATE);
        List<CloudResource> resources = List.of(createCloudResource("volumes", ResourceType.AZURE_VOLUMESET), template);
        InstanceTemplate instanceTemplate = mock(InstanceTemplate.class);
        when(instanceTemplate.getPrivateId()).thenReturn(1L);
        when(instanceTemplate.getGroupName()).thenReturn("worker");
        when(instanceTemplate.getStatus()).thenReturn(InstanceStatus.CREATE_REQUESTED);
        List<Group> scaledGroups = createScaledGroups(new CloudInstance("instanceid", instanceTemplate, null, null, null));

        when(cloudResourceHelper.getScaledGroups(stack)).thenReturn(scaledGroups);
        when(azureTemplateDeploymentService.getTemplateDeployment(client, stack, ac, azureStackView, AzureInstanceTemplateOperation.UPSCALE))
                .thenReturn(templateDeployment);
        when(templateDeployment.exportTemplate()).thenReturn(mock(DeploymentExportResult.class));
        CloudResource newInstance = CloudResource.builder().withInstanceId("instanceid").withType(ResourceType.AZURE_INSTANCE).withStatus(CommonStatus.CREATED)
                .withName("instance").withParameters(Map.of()).build();
        List<CloudResource> newInstances = List.of(newInstance);
        when(azureCloudResourceService.getDeploymentCloudResources(templateDeployment)).thenReturn(newInstances);
        when(azureCloudResourceService.getInstanceCloudResources(STACK_NAME, newInstances, scaledGroups, RESOURCE_GROUP)).thenReturn(newInstances);
        when(azureCloudResourceService.getNetworkResources(resources)).thenReturn(NETWORK_RESOURCES);
        when(azureScaleUtilService.getArmTemplate(anyList(), anyString())).thenReturn(template);
        when(resourceRetriever.findAllByStatusAndTypeAndStack(CommonStatus.CREATED, AZURE_INSTANCE, cloudContext.getId())).thenReturn(new ArrayList<>());
        doNothing().when(cloudResourceHelper).updateDeleteOnTerminationFlag(anyList(), anyBoolean(), any());

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
        ArgumentCaptor<List<CloudResource>> newInstancesArgumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(azureComputeResourceService).buildComputeResourcesForUpscale(eq(ac), eq(stack), eq(scaledGroups), newInstancesArgumentCaptor.capture(),
                eq(List.of()), eq(NETWORK_RESOURCES), eq(adjustmentTypeWithThreshold));
        List<CloudResource> cloudResourceList = newInstancesArgumentCaptor.getValue();
        assertThat(cloudResourceList).extracting(CloudResource::getType).containsExactly(AZURE_INSTANCE);
        assertThat(cloudResourceList).extracting(CloudResource::getStatus).containsExactly(CommonStatus.CREATED);
        assertThat(cloudResourceList).extracting(CloudResource::getName).containsExactly("instance");
        assertThat(cloudResourceList).extracting(CloudResource::isPersistent).containsExactly(true);
        assertThat(cloudResourceList).extracting(CloudResource::isStackAware).containsExactly(true);
        assertThat(cloudResourceList).extracting(CloudResource::getInstanceId).containsExactly("instanceid");
        assertThat(cloudResourceList).extracting(CloudResource::getParameters).extracting(map -> map.get(PRIVATE_ID)).containsExactly(1L);
        verify(cloudResourceHelper, times(1)).updateDeleteOnTerminationFlag(any(), eq(false), any());
        verify(cloudResourceHelper, times(1)).updateDeleteOnTerminationFlag(any(), eq(true), any());
    }

    @Test
    public void testUpscaleButQuotaIssueHappen() throws QuotaExceededException {
        vhdImageStubbing();

        CloudContext cloudContext = createCloudContext();
        CloudCredential cloudCredential = mock(CloudCredential.class);
        when(cloudCredential.getParameters()).thenReturn(Map.of("subscriptionId", "subscriptionId"));
        AuthenticatedContext ac = new AuthenticatedContext(cloudContext, cloudCredential);
        CloudResource template = createCloudResource(TEMPLATE, ResourceType.ARM_TEMPLATE);
        List<CloudResource> resources = List.of(createCloudResource("volumes", ResourceType.AZURE_VOLUMESET), template);
        InstanceTemplate instanceTemplate = mock(InstanceTemplate.class);
        when(instanceTemplate.getPrivateId()).thenReturn(1L);
        when(instanceTemplate.getGroupName()).thenReturn("worker");
        when(instanceTemplate.getStatus()).thenReturn(InstanceStatus.CREATE_REQUESTED);
        List<Group> scaledGroups = createScaledGroups(new CloudInstance("instanceid", instanceTemplate, null, null, null));

        when(cloudResourceHelper.getScaledGroups(stack)).thenReturn(scaledGroups);
        ApiError cloudError = new ApiError();
        List<ManagementError> details = new ArrayList<>();
        AzureTestUtils.setDetails(cloudError, details);
        ManagementError quotaError = new ManagementError(
                "QuotaExceeded",
                "Operation could not be completed as it results in exceeding approved standardNCPromoFamily Cores quota. " +
                        "Additional details - Deployment Model: Resource Manager, Location: westus2, Current Limit: 200, Current Usage: 24, " +
                        "Additional Required: 600," +
                        " (Minimum) New Limit Required: 624. Submit a request for Quota increase at https://aka" +
                        ".ms/ProdportalCRP/#blade/Microsoft_Azure_Capacity/UsageAndQuota.ReactView/Parameters/%7B%22subscriptionId%22:" +
                        "%223ddda1c7-d1f5-4e7b-ac81-0523f483b3b3%22,%22command%22:%22openQuotaApprovalBlade%22,%22quotas%22:" +
                        "[%7B%22location%22:%22westus2%22,%22" +
                        "providerId%22:%22Microsoft.Compute%22,%22resourceName%22:%22standardNCPromoFamily%22,%22quotaRequest%22:%7B%22" +
                        "properties%22:%7B%22limit%22:" +
                        "624,%22unit%22:%22Count%22,%22name%22:%7B%22value%22:%22standardNCPromoFamily%22%7D%7D%7D%7D]%7D " +
                        "by specifying parameters listed in the " +
                        "'Details' section for deployment to succeed. Please read more about quota limits at https://docs.microsoft.com/en-us/azure/" +
                        "azure-supportability/per-vm-quota-requests");
        details.add(quotaError);
        ApiErrorException cloudException = new ApiErrorException("", null, cloudError);
        when(azureScaleUtilService.getArmTemplate(anyList(), anyString())).thenThrow(cloudException);
        doThrow(new QuotaExceededException(200, 24, 600, "QuotaExceeded", new BadRequestException("")))
                .when(azureScaleUtilService).checkIfQuotaLimitIssued(cloudException);

        AdjustmentTypeWithThreshold adjustmentTypeWithThreshold = new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, 0L);
        QuotaExceededException quotaExceededException = assertThrows(QuotaExceededException.class, () -> {
            underTest.upscale(ac, stack, resources, azureStackView, client,
                    adjustmentTypeWithThreshold);
        });
        assertEquals(200, quotaExceededException.getCurrentLimit());
        assertEquals(24, quotaExceededException.getCurrentUsage());
        assertEquals(600, quotaExceededException.getAdditionalRequired());
    }

    @Test
    public void testUpscaleWhenThereAreReattachableVolumeSets() throws QuotaExceededException {
        vhdImageStubbing();

        CloudContext cloudContext = createCloudContext();
        CloudCredential cloudCredential = mock(CloudCredential.class);
        when(cloudCredential.getParameters()).thenReturn(Map.of("subscriptionId", "subscriptionId"));
        AuthenticatedContext ac = new AuthenticatedContext(cloudContext, cloudCredential);
        CloudResource template = createCloudResource(TEMPLATE, ResourceType.ARM_TEMPLATE);
        CloudResource notReattachableVolumeSet = createCloudResource("volumes", ResourceType.AZURE_VOLUMESET);
        CloudResource detachedVolumeSet = createCloudResource("detachedvolumes", ResourceType.AZURE_VOLUMESET, CommonStatus.DETACHED, null);
        CloudResource alreadyCreatedVolumeSet = createCloudResource("alreadycreatedvolumes", ResourceType.AZURE_VOLUMESET, CommonStatus.CREATED,
                "instanceid");
        List<CloudResource> resources = List.of(detachedVolumeSet, alreadyCreatedVolumeSet, notReattachableVolumeSet, template);
        InstanceTemplate instanceTemplate = mock(InstanceTemplate.class);
        when(instanceTemplate.getPrivateId()).thenReturn(1L);
        when(instanceTemplate.getGroupName()).thenReturn("worker");
        when(instanceTemplate.getStatus()).thenReturn(InstanceStatus.CREATE_REQUESTED);
        List<Group> scaledGroups = createScaledGroups(new CloudInstance("instanceid", instanceTemplate, null, null, null));

        when(cloudResourceHelper.getScaledGroups(stack)).thenReturn(scaledGroups);
        when(azureTemplateDeploymentService.getTemplateDeployment(client, stack, ac, azureStackView, AzureInstanceTemplateOperation.UPSCALE))
                .thenReturn(templateDeployment);
        when(templateDeployment.exportTemplate()).thenReturn(mock(DeploymentExportResult.class));
        CloudResource newInstance = CloudResource.builder().withInstanceId("instanceid").withType(ResourceType.AZURE_INSTANCE).withStatus(CommonStatus.CREATED)
                .withName("instance").withParameters(Map.of()).build();
        List<CloudResource> newInstances = List.of(newInstance);
        when(azureCloudResourceService.getDeploymentCloudResources(templateDeployment)).thenReturn(newInstances);
        when(azureCloudResourceService.getInstanceCloudResources(STACK_NAME, newInstances, scaledGroups, RESOURCE_GROUP)).thenReturn(newInstances);
        when(azureCloudResourceService.getNetworkResources(resources)).thenReturn(NETWORK_RESOURCES);
        doNothing().when(cloudResourceHelper).updateDeleteOnTerminationFlag(anyList(), anyBoolean(), any());

        AdjustmentTypeWithThreshold adjustmentTypeWithThreshold = new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, 0L);
        underTest.upscale(ac, stack, resources, azureStackView, client, adjustmentTypeWithThreshold);

        ArgumentCaptor<List<CloudResource>> newInstancesArgumentCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<CloudResource>> reattachCaptor = ArgumentCaptor.forClass(List.class);
        verify(azureComputeResourceService).buildComputeResourcesForUpscale(eq(ac), eq(stack), eq(scaledGroups), newInstancesArgumentCaptor.capture(),
                reattachCaptor.capture(), eq(NETWORK_RESOURCES), eq(adjustmentTypeWithThreshold));
        List<CloudResource> cloudResourceList = newInstancesArgumentCaptor.getValue();
        assertThat(cloudResourceList).extracting(CloudResource::getType).containsExactly(AZURE_INSTANCE);
        assertThat(cloudResourceList).extracting(CloudResource::getStatus).containsExactly(CommonStatus.CREATED);
        assertThat(cloudResourceList).extracting(CloudResource::getName).containsExactly("instance");
        assertThat(cloudResourceList).extracting(CloudResource::isPersistent).containsExactly(true);
        assertThat(cloudResourceList).extracting(CloudResource::isStackAware).containsExactly(true);
        assertThat(cloudResourceList).extracting(CloudResource::getInstanceId).containsExactly("instanceid");
        assertThat(cloudResourceList).extracting(CloudResource::getParameters).extracting(map -> map.get(PRIVATE_ID)).containsExactly(1L);
        List<CloudResource> reattachableVolumeSets = reattachCaptor.getValue();
        assertEquals(2, reattachableVolumeSets.size());
        assertEquals(detachedVolumeSet, reattachableVolumeSets.get(0));
        assertEquals(alreadyCreatedVolumeSet, reattachableVolumeSets.get(1));
        verify(cloudResourceHelper, times(1)).updateDeleteOnTerminationFlag(any(), eq(false), any());
        verify(cloudResourceHelper, times(1)).updateDeleteOnTerminationFlag(any(), eq(true), any());
    }

    @Test
    public void testUpscaleWhenVmsNotStartedInTime() {
        vhdImageStubbing();

        CloudContext cloudContext = createCloudContext();
        CloudCredential cloudCredential = mock(CloudCredential.class);
        when(cloudCredential.getParameters()).thenReturn(Map.of("subscriptionId", "subscriptionId"));
        AuthenticatedContext ac = new AuthenticatedContext(cloudContext, cloudCredential);
        CloudResource template = createCloudResource(TEMPLATE, ResourceType.ARM_TEMPLATE);
        List<CloudResource> resources = List.of(createCloudResource("volumes", ResourceType.AZURE_VOLUMESET), template);
        InstanceTemplate instanceTemplate = mock(InstanceTemplate.class);
        when(instanceTemplate.getPrivateId()).thenReturn(1L);
        when(instanceTemplate.getGroupName()).thenReturn("worker");
        when(instanceTemplate.getStatus()).thenReturn(InstanceStatus.CREATE_REQUESTED);
        List<Group> scaledGroups = createScaledGroups(new CloudInstance("instanceid", instanceTemplate, null, null, null));

        when(cloudResourceHelper.getScaledGroups(stack)).thenReturn(scaledGroups);
        ApiError cloudError = AzureTestUtils.apiError("code", "Error happened");
        List<ManagementError> details = new ArrayList<>();
        AzureTestUtils.setDetails(cloudError, details);
        ManagementError managementError = AzureTestUtils.managementError("code", "Please check the power state later");
        details.add(managementError);
        when(azureTemplateDeploymentService.getTemplateDeployment(client, stack, ac, azureStackView, AzureInstanceTemplateOperation.UPSCALE))
                .thenThrow(new Retry.ActionFailedException("VMs not started in time.", new ApiErrorException("Error", null, cloudError)));
        when(azureUtils.convertToCloudConnectorException(any(ApiErrorException.class), anyString())).thenCallRealMethod();
        when(azureUtils.convertToCloudConnectorException(any(Throwable.class), anyString())).thenCallRealMethod();
        CloudConnectorException cloudConnectorException = assertThrows(CloudConnectorException.class, () ->
                underTest.upscale(ac, stack, resources, azureStackView, client, new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, 0L))
        );

        assertThat(cloudConnectorException.getMessage())
                .contains("Stack upscale failed, status code code, error message: Error happened, details: Please check the power state later");
    }

    private List<Group> createScaledGroups(CloudInstance cloudInstance) {
        Group group = mock(Group.class);
        when(group.getInstances()).thenReturn(List.of(cloudInstance));
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
        return CloudResource.builder()
                .withName(name)
                .withStatus(status)
                .withType(resourceType)
                .withInstanceId(instanceId)
                .withParameters(PARAMETERS)
                .build();
    }
}
