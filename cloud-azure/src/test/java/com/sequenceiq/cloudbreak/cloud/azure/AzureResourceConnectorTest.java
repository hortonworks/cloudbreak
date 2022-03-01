package com.sequenceiq.cloudbreak.cloud.azure;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.microsoft.azure.management.resources.Deployment;
import com.microsoft.azure.management.resources.DeploymentExportResult;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.connector.resource.AzureComputeResourceService;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureMarketplaceImageProviderService;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureImageFormatValidator;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureStackView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.AdjustmentType;

@RunWith(MockitoJUnitRunner.class)
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

    private List<CloudResource> instances;

    private List<Group> groups;

    private Network network;

    private Image imageModel;

    @Before
    public void setUp() {
        DeploymentExportResult deploymentExportResult = mock(DeploymentExportResult.class);
        Group group = mock(Group.class);
        AzureStackView azureStackView = mock(AzureStackView.class);
        groups = List.of(group);
        CloudResource cloudResource = mock(CloudResource.class);
        instances = List.of(cloudResource);
        network = new Network(new Subnet("0.0.0.0/16"));
        AzureImage image = new AzureImage("id", "name", true);
        imageModel = new Image(IMAGE_NAME, new HashMap<>(), "centos7", "redhat7", "", "default", "default-id", new HashMap<>());

        when(stack.getGroups()).thenReturn(groups);
        when(stack.getNetwork()).thenReturn(network);
        when(stack.getImage()).thenReturn(imageModel);
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(ac.getParameter(AzureClient.class)).thenReturn(client);
        when(ac.getCloudCredential()).thenReturn(new CloudCredential("aCredentialId", "aCredentialName"));
        when(azureUtils.getStackName(cloudContext)).thenReturn(STACK_NAME);
        when(azureStorage.getCustomImage(any(), any(), any())).thenReturn(image);
        when(deployment.exportTemplate()).thenReturn(deploymentExportResult);
        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, stack)).thenReturn(RESOURCE_GROUP_NAME);
        when(azureCloudResourceService.getDeploymentCloudResources(deployment)).thenReturn(instances);
        when(azureCloudResourceService.getInstanceCloudResources(STACK_NAME, instances, groups, RESOURCE_GROUP_NAME)).thenReturn(instances);
        when(azureStackViewProvider.getAzureStack(any(), eq(stack), eq(client), eq(ac))).thenReturn(azureStackView);
    }

    @Test
    public void testWhenTemplateDeploymentDoesNotExistThenComputeResourceServiceBuildsTheResources() {
        when(client.templateDeploymentExists(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(false);
        when(client.createTemplateDeployment(any(), any(), any(), any())).thenReturn(deployment);
        when(azureImageFormatValidator.isMarketplaceImageFormat(any())).thenReturn(false);

        AdjustmentTypeWithThreshold adjustmentTypeWithThreshold = new AdjustmentTypeWithThreshold(ADJUSTMENT_TYPE, THRESHOLD);
        underTest.launch(ac, stack, notifier, adjustmentTypeWithThreshold);

        verify(azureComputeResourceService, times(1)).buildComputeResourcesForLaunch(eq(ac), eq(stack), eq(adjustmentTypeWithThreshold),
                eq(instances), any());
        verify(azureCloudResourceService, times(1)).getInstanceCloudResources(STACK_NAME, instances, groups, RESOURCE_GROUP_NAME);
        verify(azureUtils, times(1)).getCustomNetworkId(network);
        verify(azureUtils, times(1)).getCustomSubnetIds(network);
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
        verify(azureUtils, times(1)).getCustomNetworkId(network);
        verify(client, never()).createTemplateDeployment(any(), any(), any(), any());
        verify(client, times(2)).getTemplateDeployment(any(), any());
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
        verify(azureUtils, times(1)).getCustomNetworkId(network);
        verify(client, times(1)).createTemplateDeployment(any(), any(), any(), any());
        verify(client, times(1)).getTemplateDeployment(any(), any());
        verify(azureMarketplaceImageProviderService, times(0)).get(imageModel);
    }

    @Test
    public void testWhenMarketplaceImageThenTemplateBuilderUsesMarketplaceImage() {
        when(client.templateDeploymentExists(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(false);
        when(client.createTemplateDeployment(any(), any(), any(), any())).thenReturn(deployment);
        when(azureImageFormatValidator.isMarketplaceImageFormat(any())).thenReturn(true);

        AdjustmentTypeWithThreshold adjustmentTypeWithThreshold = new AdjustmentTypeWithThreshold(ADJUSTMENT_TYPE, THRESHOLD);
        underTest.launch(ac, stack, notifier, adjustmentTypeWithThreshold);

        verify(azureMarketplaceImageProviderService, times(1)).get(imageModel);
    }

    @Test
    public void testLaunchLoadBalancerHandlesGracefully() throws Exception {
        List<CloudResourceStatus> cloudResourceStatuses = underTest.launchLoadBalancers(ac, stack, notifier);
        Assert.assertEquals(0, cloudResourceStatuses.size());
    }

    @Test
    public void testTermiate() {
        when(azureTerminationHelperService.handleTransientDeployment(any(), any(), any())).thenReturn(List.of());
        when(azureTerminationHelperService.terminate(any(), any(), any()))
                .thenReturn(List.of(new CloudResourceStatus(instances.get(0), ResourceStatus.DELETED)));
        List<CloudResourceStatus> statuses = underTest.terminate(ac, stack, new ArrayList<>(instances));
        for (CloudResourceStatus status : statuses) {
            Assert.assertEquals(ResourceStatus.DELETED, status.getStatus());
        }
    }
}