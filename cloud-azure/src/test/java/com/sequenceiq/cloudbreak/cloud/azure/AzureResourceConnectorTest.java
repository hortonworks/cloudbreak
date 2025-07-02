package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.ACCEPTANCE_POLICY_PARAMETER;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.resourcemanager.resources.models.Deployment;
import com.azure.resourcemanager.resources.models.DeploymentExportResult;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.connector.resource.AzureComputeResourceService;
import com.sequenceiq.cloudbreak.cloud.azure.connector.resource.AzureDatabaseResourceService;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureImageTermsSignerService;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureMarketplaceImage;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureMarketplaceImageProviderService;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureImageFormatValidator;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureStackView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.exception.CloudImageException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.provider.ProviderResourceSyncer;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
public class AzureResourceConnectorTest {

    private static final AdjustmentType ADJUSTMENT_TYPE = AdjustmentType.EXACT;

    private static final long THRESHOLD = 1;

    private static final String STACK_NAME = "someStackNameValue";

    private static final String RESOURCE_GROUP_NAME = "resourceGroupName";

    private static final String IMAGE_NAME = "image-name";

    @Mock
    private AuthenticatedContext ac;

    @Mock
    private CloudStack stack;

    @Mock
    private PersistenceNotifier notifier;

    @Mock
    private AzureClient client;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private Deployment deployment;

    @Mock
    private AzureStackViewProvider azureStackViewProvider;

    @InjectMocks
    private AzureResourceConnector underTest;

    @Mock
    private AzureTemplateBuilder azureTemplateBuilder;

    @Mock
    private AzureUtils azureUtils;

    @Mock
    private AzureStorage azureStorage;

    @Mock
    private Retry retryService;

    @Mock
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @Mock
    private AzureComputeResourceService azureComputeResourceService;

    @Mock
    private AzureCloudResourceService azureCloudResourceService;

    @Mock
    private AzureMarketplaceImageProviderService azureMarketplaceImageProviderService;

    @Mock
    private AzureImageFormatValidator azureImageFormatValidator;

    @Mock
    private AzureTerminationHelperService azureTerminationHelperService;

    @Mock
    private AzureDatabaseResourceService azureDatabaseResourceService;

    @Mock
    private AzureImageTermsSignerService azureImageTermsSignerService;

    @Mock
    private List<ProviderResourceSyncer> providerResourceSyncers;

    private List<CloudResource> instances;

    private List<Group> groups;

    private Network network;

    private Image imageModel;

    @BeforeEach
    public void setUp() {
        DeploymentExportResult deploymentExportResult = mock(DeploymentExportResult.class);
        Group group = mock(Group.class);
        AzureStackView azureStackView = mock(AzureStackView.class);
        groups = List.of(group);
        CloudResource cloudResource = mock(CloudResource.class);
        instances = List.of(cloudResource);
        network = new Network(new Subnet("0.0.0.0/16"));
        AzureImage image = new AzureImage("id", "name", true);
        imageModel = new Image(IMAGE_NAME, new HashMap<>(), "centos7", "redhat7", "", "", "default", "default-id", new HashMap<>(), "2019-10-24", 1571884856L);

        lenient().when(stack.getGroups()).thenReturn(groups);
        lenient().when(stack.getNetwork()).thenReturn(network);
        lenient().when(stack.getImage()).thenReturn(imageModel);
        lenient().when(stack.getParameters()).thenReturn(Map.of(ACCEPTANCE_POLICY_PARAMETER, Boolean.TRUE.toString()));
        lenient().when(ac.getCloudContext()).thenReturn(cloudContext);
        lenient().when(ac.getParameter(AzureClient.class)).thenReturn(client);
        lenient().when(ac.getCloudCredential()).thenReturn(new CloudCredential("aCredentialId", "aCredentialName", "account"));
        lenient().when(azureUtils.getStackName(cloudContext)).thenReturn(STACK_NAME);
        lenient().when(azureStorage.getCustomImage(any(), any(), any())).thenReturn(image);
        lenient().when(deployment.exportTemplate()).thenReturn(deploymentExportResult);
        lenient().when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, stack)).thenReturn(RESOURCE_GROUP_NAME);
        lenient().when(azureCloudResourceService.getDeploymentCloudResources(deployment)).thenReturn(instances);
        lenient().when(azureCloudResourceService.getInstanceCloudResources(STACK_NAME, instances, groups, RESOURCE_GROUP_NAME)).thenReturn(instances);
        lenient().when(azureStackViewProvider.getAzureStack(any(), eq(stack), eq(client), eq(ac))).thenReturn(azureStackView);
    }

    @Test
    public void testWhenTemplateDeploymentDoesNotExistThenComputeResourceServiceBuildsTheResources() {
        when(client.templateDeploymentExists(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(false);
        when(client.createTemplateDeployment(any(), any(), any(), any())).thenReturn(deployment);
        when(azureImageFormatValidator.isMarketplaceImageFormat(any(Image.class))).thenReturn(false);

        AdjustmentTypeWithThreshold adjustmentTypeWithThreshold = new AdjustmentTypeWithThreshold(ADJUSTMENT_TYPE, THRESHOLD);
        underTest.launch(ac, stack, notifier, adjustmentTypeWithThreshold);

        verify(azureComputeResourceService, times(1)).buildComputeResourcesForLaunch(eq(ac), eq(stack), eq(adjustmentTypeWithThreshold),
                eq(instances), any());
        verify(azureCloudResourceService, times(1)).getInstanceCloudResources(STACK_NAME, instances, groups, RESOURCE_GROUP_NAME);
        verify(azureUtils, times(1)).getCustomSubnetIds(network);
        verify(azureMarketplaceImageProviderService, never()).getSourceImage(eq(imageModel));
        verify(azureMarketplaceImageProviderService, times(0)).get(imageModel);
    }

    @Test
    public void testWhenTemplateDeploymentExistsAndInProgressThenComputeResourceServiceBuildsTheResources() {
        when(client.templateDeploymentExists(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(true);
        when(client.getTemplateDeployment(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(deployment);
        when(client.getTemplateDeploymentStatus(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(ResourceStatus.IN_PROGRESS);
        when(azureImageFormatValidator.isMarketplaceImageFormat(imageModel)).thenReturn(false);

        AdjustmentTypeWithThreshold adjustmentTypeWithThreshold = new AdjustmentTypeWithThreshold(ADJUSTMENT_TYPE, THRESHOLD);
        underTest.launch(ac, stack, notifier, adjustmentTypeWithThreshold);

        verify(azureComputeResourceService, times(1)).buildComputeResourcesForLaunch(any(AuthenticatedContext.class),
                any(CloudStack.class), eq(adjustmentTypeWithThreshold), any(), any());
        verify(azureCloudResourceService, times(1)).getInstanceCloudResources(STACK_NAME, instances, groups, RESOURCE_GROUP_NAME);
        verify(client, never()).createTemplateDeployment(any(), any(), any(), any());
        verify(client, times(2)).getTemplateDeployment(any(), any());
        verify(azureMarketplaceImageProviderService, never()).getSourceImage(eq(imageModel));
        verify(azureMarketplaceImageProviderService, times(0)).get(imageModel);
    }

    @Test
    public void testWhenTemplateDeploymentExistsAndFinishedThenComputeResourceServiceBuildsTheResources() {
        when(client.templateDeploymentExists(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(true);
        when(client.createTemplateDeployment(any(), any(), any(), any())).thenReturn(deployment);
        when(client.getTemplateDeploymentStatus(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(ResourceStatus.CREATED);
        when(azureImageFormatValidator.isMarketplaceImageFormat(imageModel)).thenReturn(false);

        AdjustmentTypeWithThreshold adjustmentTypeWithThreshold = new AdjustmentTypeWithThreshold(ADJUSTMENT_TYPE, THRESHOLD);
        underTest.launch(ac, stack, notifier, adjustmentTypeWithThreshold);

        verify(azureComputeResourceService, times(1)).buildComputeResourcesForLaunch(any(AuthenticatedContext.class),
                any(CloudStack.class), eq(adjustmentTypeWithThreshold), any(), any());
        verify(azureCloudResourceService, times(1)).getInstanceCloudResources(STACK_NAME, instances, groups, RESOURCE_GROUP_NAME);
        verify(client, times(1)).createTemplateDeployment(any(), any(), any(), any());
        verify(client, times(1)).getTemplateDeployment(any(), any());
        verify(azureMarketplaceImageProviderService, never()).getSourceImage(eq(imageModel));
        verify(azureMarketplaceImageProviderService, times(0)).get(imageModel);
    }

    @Test
    public void testWhenMarketplaceImageThenTemplateBuilderUsesMarketplaceImage() {
        when(client.templateDeploymentExists(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(false);
        when(client.createTemplateDeployment(any(), any(), any(), any())).thenReturn(deployment);
        when(azureImageFormatValidator.isMarketplaceImageFormat(any(Image.class))).thenReturn(true);

        AdjustmentTypeWithThreshold adjustmentTypeWithThreshold = new AdjustmentTypeWithThreshold(ADJUSTMENT_TYPE, THRESHOLD);
        underTest.launch(ac, stack, notifier, adjustmentTypeWithThreshold);

        verify(azureMarketplaceImageProviderService, times(1)).get(imageModel);
        verify(azureMarketplaceImageProviderService, never()).getSourceImage(eq(imageModel));
    }

    @Test
    public void testWhenMarketplaceImageThenTemplateBuilderUsesMarketplaceImageGlobalSettingOff() {
        when(client.templateDeploymentExists(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(false);
        when(client.createTemplateDeployment(any(), any(), any(), any())).thenReturn(deployment);
        when(azureImageFormatValidator.isMarketplaceImageFormat(any(Image.class))).thenReturn(true);
        when(stack.getParameters()).thenReturn(Map.of(ACCEPTANCE_POLICY_PARAMETER, Boolean.FALSE.toString()));

        AdjustmentTypeWithThreshold adjustmentTypeWithThreshold = new AdjustmentTypeWithThreshold(ADJUSTMENT_TYPE, THRESHOLD);
        underTest.launch(ac, stack, notifier, adjustmentTypeWithThreshold);

        verify(azureMarketplaceImageProviderService, times(1)).get(imageModel);
        verify(azureMarketplaceImageProviderService, never()).getSourceImage(eq(imageModel));
        verify(azureImageTermsSignerService, never()).sign(eq(null), eq(null), any());
    }

    @Test
    public void testWhenSourceImageExistsThenComputeResourceServiceBuildsTheResources() {

        AzureMarketplaceImage azureMarketplaceImage = new AzureMarketplaceImage("cloudera", "my-offer", "my-plan", "my-version", true);

        when(client.templateDeploymentExists(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(false);
        when(client.createTemplateDeployment(any(), any(), any(), any())).thenReturn(deployment);
        when(azureImageFormatValidator.isMarketplaceImageFormat(any(Image.class))).thenReturn(false);
        when(azureImageFormatValidator.hasSourceImagePlan(any(Image.class))).thenReturn(true);
        when(azureMarketplaceImageProviderService.getSourceImage(imageModel)).thenReturn(azureMarketplaceImage);

        AdjustmentTypeWithThreshold adjustmentTypeWithThreshold = new AdjustmentTypeWithThreshold(ADJUSTMENT_TYPE, THRESHOLD);
        underTest.launch(ac, stack, notifier, adjustmentTypeWithThreshold);

        verify(azureComputeResourceService, times(1)).buildComputeResourcesForLaunch(eq(ac), eq(stack), eq(adjustmentTypeWithThreshold),
                eq(instances), any());
        verify(azureCloudResourceService, times(1)).getInstanceCloudResources(STACK_NAME, instances, groups, RESOURCE_GROUP_NAME);
        verify(azureUtils, times(1)).getCustomSubnetIds(network);
        verify(azureMarketplaceImageProviderService, times(0)).get(imageModel);
        verify(azureMarketplaceImageProviderService, times(2)).getSourceImage(eq(imageModel));
        verify(azureTemplateBuilder).build(eq(STACK_NAME), any(), any(), any(), any(), eq(stack), eq(AzureInstanceTemplateOperation.PROVISION),
                eq(azureMarketplaceImage));
    }

    @Test
    public void testWhenSourceImageExistsThenSignFails() {
        AzureMarketplaceImage azureMarketplaceImage = new AzureMarketplaceImage("cloudera", "my-offer", "my-plan", "my-version", true);

        when(azureImageFormatValidator.isMarketplaceImageFormat(any(Image.class))).thenReturn(false);
        when(azureImageFormatValidator.hasSourceImagePlan(any(Image.class))).thenReturn(true);
        when(azureMarketplaceImageProviderService.getSourceImage(imageModel)).thenReturn(azureMarketplaceImage);
        doThrow(new CloudImageException("")).when(azureImageTermsSignerService).sign(any(), any(), any());

        AdjustmentTypeWithThreshold adjustmentTypeWithThreshold = new AdjustmentTypeWithThreshold(ADJUSTMENT_TYPE, THRESHOLD);
        CloudConnectorException exception =
                assertThrows(CloudConnectorException.class, () -> underTest.launch(ac, stack, notifier, adjustmentTypeWithThreshold));
        assertFalse(exception instanceof CloudImageException, "Should not fallback in case of source image signing");

        verify(azureComputeResourceService, never()).buildComputeResourcesForLaunch(eq(ac), eq(stack), eq(adjustmentTypeWithThreshold),
                eq(instances), any());
        verify(azureCloudResourceService, never()).getInstanceCloudResources(STACK_NAME, instances, groups, RESOURCE_GROUP_NAME);
        verify(azureMarketplaceImageProviderService, times(0)).get(imageModel);
        verify(azureMarketplaceImageProviderService, times(1)).getSourceImage(eq(imageModel));
        verify(azureTemplateBuilder, never()).build(eq(STACK_NAME), any(), any(), any(), any(), eq(stack), eq(AzureInstanceTemplateOperation.PROVISION),
                eq(azureMarketplaceImage));
    }

    @Test
    public void testLaunchLoadBalancerHandlesGracefully() throws Exception {
        when(azureTemplateBuilder.buildLoadBalancer(eq(STACK_NAME), any(AzureCredentialView.class), any(AzureStackView.class), eq(cloudContext), eq(stack),
                eq(AzureInstanceTemplateOperation.PROVISION))).thenReturn("{}");
        when(client.templateDeploymentExists(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(false);
        when(client.createTemplateDeployment(any(), any(), any(), any())).thenReturn(deployment);

        List<CloudResourceStatus> cloudResourceStatuses = underTest.launchLoadBalancers(ac, stack, notifier);
        Assertions.assertEquals(1, cloudResourceStatuses.size());

        verify(azureCloudResourceService, times(1)).getInstanceCloudResources(STACK_NAME, instances, groups, RESOURCE_GROUP_NAME);
        verify(azureMarketplaceImageProviderService, never()).getSourceImage(eq(imageModel));
        verify(azureMarketplaceImageProviderService, times(0)).get(imageModel);
    }

    @Test
    public void testWhenLaunchLoadBalancerTemplateDeploymentExistsAndInProgressThenComputeResourceServiceBuildsTheResources() {
        when(azureTemplateBuilder.buildLoadBalancer(eq(STACK_NAME), any(AzureCredentialView.class), any(AzureStackView.class), eq(cloudContext), eq(stack),
                eq(AzureInstanceTemplateOperation.PROVISION))).thenReturn("{}");
        when(client.templateDeploymentExists(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(true);
        when(client.getTemplateDeployment(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(deployment);
        when(client.getTemplateDeploymentStatus(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(ResourceStatus.IN_PROGRESS);

        underTest.launchLoadBalancers(ac, stack, notifier);

        verify(azureCloudResourceService, times(1)).getInstanceCloudResources(STACK_NAME, instances, groups, RESOURCE_GROUP_NAME);
        verify(client, never()).createTemplateDeployment(any(), any(), any(), any());
        verify(client, times(2)).getTemplateDeployment(any(), any());
        verify(azureMarketplaceImageProviderService, never()).getSourceImage(eq(imageModel));
        verify(azureMarketplaceImageProviderService, times(0)).get(imageModel);
    }

    @Test
    public void testTerminate() {
        when(azureTerminationHelperService.handleTransientDeployment(any(), any(), any())).thenReturn(List.of());
        when(azureTerminationHelperService.terminate(any(), any(), any()))
                .thenReturn(List.of(new CloudResourceStatus(instances.get(0), ResourceStatus.DELETED)));
        List<CloudResourceStatus> statuses = underTest.terminate(ac, stack, new ArrayList<>(instances));
        for (CloudResourceStatus status : statuses) {
            Assertions.assertEquals(ResourceStatus.DELETED, status.getStatus());
        }
    }

    @Test
    public void testUpgradeDatabaseServer() {
        DatabaseStack databaseStack = mock(DatabaseStack.class);
        PersistenceNotifier persistenceNotifier = mock(PersistenceNotifier.class);
        CloudResource resource1 = CloudResource.builder().withType(ResourceType.AZURE_DATABASE).withName("resource1").build();
        CloudResource resource2 = CloudResource.builder().withType(ResourceType.AZURE_PRIVATE_ENDPOINT).withName("resource2").build();

        underTest.upgradeDatabaseServer(ac, databaseStack, databaseStack, persistenceNotifier, TargetMajorVersion.VERSION_11, List.of(resource1, resource2));
        verify(azureDatabaseResourceService, times(1))
                .upgradeDatabaseServer(eq(ac), eq(databaseStack), eq(databaseStack), eq(persistenceNotifier),
                        eq(TargetMajorVersion.VERSION_11), eq(List.of(resource1, resource2)));
    }
}