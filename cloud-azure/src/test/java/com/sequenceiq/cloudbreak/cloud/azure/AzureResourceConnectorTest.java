package com.sequenceiq.cloudbreak.cloud.azure;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.microsoft.azure.management.resources.Deployment;
import com.microsoft.azure.management.resources.DeploymentExportResult;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.AdjustmentType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.connector.resource.AzureComputeResourceService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.transform.CloudResourceHelper;
import com.sequenceiq.cloudbreak.service.Retry;

@RunWith(MockitoJUnitRunner.class)
public class AzureResourceConnectorTest {

    private static final AdjustmentType ADJUSTMENT_TYPE = AdjustmentType.EXACT;

    private static final long THRESHOLD = 1L;

    private static final String STACK_NAME = "someStackNameValue";

    private static final String RESOURCE_GROUP_NAME = "resourceGroupName";

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

    @InjectMocks
    private AzureResourceConnector underTest;

    @Mock
    private AzureTemplateBuilder azureTemplateBuilder;

    @Mock
    private AzureUtils azureUtils;

    @Mock
    private AzureStorage azureStorage;

    @Mock
    private CloudResourceHelper cloudResourceHelper;

    @Mock
    private Retry retryService;

    @Mock
    private AzureComputeResourceService azureComputeResourceService;

    private List<CloudResource> instances;

    private List<Group> groups;

    private Network network;

    @Before
    public void setUp() {
        Location location = mock(Location.class);
        Region region = mock(Region.class);
        DeploymentExportResult deploymentExportResult = mock(DeploymentExportResult.class);
        Group group = mock(Group.class);
        groups = List.of(group);
        CloudResource cloudResource = mock(CloudResource.class);
        CloudInstance cloudInstance = mock(CloudInstance.class);
        instances = List.of(cloudResource);
        network = new Network(new Subnet("0.0.0.0/16"));
        when(stack.getGroups()).thenReturn(groups);
        when(stack.getNetwork()).thenReturn(network);
        when(location.getRegion()).thenReturn(region);
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location);
        when(group.getType()).thenReturn(InstanceGroupType.CORE);
        when(group.getName()).thenReturn("someInstanceGroupName");
        when(ac.getParameter(AzureClient.class)).thenReturn(client);
        when(azureUtils.getStackName(cloudContext)).thenReturn(STACK_NAME);
        when(deployment.exportTemplate()).thenReturn(deploymentExportResult);
        when(group.getReferenceInstanceConfiguration()).thenReturn(cloudInstance);
        when(cloudInstance.getTemplate()).thenReturn(mock(InstanceTemplate.class));
        when(client.createTemplateDeployment(any(), any(), any(), any())).thenReturn(deployment);
        when(azureUtils.getResourceGroupName(cloudContext, stack)).thenReturn(RESOURCE_GROUP_NAME);
        when(azureUtils.getInstanceCloudResources(cloudContext, deployment, groups)).thenReturn(instances);
    }

    @Test
    public void testWhenTemplateDeploymentDoesNotExistsThenComputeResourceServiceBuildsTheTheResources() {
        when(client.templateDeploymentExists(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(false);

        underTest.launch(ac, stack, notifier, ADJUSTMENT_TYPE, THRESHOLD);

        verify(azureComputeResourceService, times(1)).buildComputeResourcesForLaunch(eq(ac), eq(stack), eq(ADJUSTMENT_TYPE),
                eq(THRESHOLD), eq(instances), any());
        verify(azureUtils, times(1)).getInstanceCloudResources(cloudContext, deployment, groups);
        verify(azureUtils, times(1)).getCustomNetworkId(network);
        verify(azureUtils, times(3)).getCustomSubnetIds(network);
    }

    @Test
    public void testWhenTemplateDeploymentExistsThenComputeResourceServiceBuildsTheTheResources() {
        when(client.templateDeploymentExists(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(true);

        underTest.launch(ac, stack, notifier, ADJUSTMENT_TYPE, THRESHOLD);

        verify(azureComputeResourceService, times(0)).buildComputeResourcesForLaunch(any(AuthenticatedContext.class),
                any(CloudStack.class), any(AdjustmentType.class), anyLong(), any(), any());
        verify(azureUtils, times(0)).getInstanceCloudResources(cloudContext, deployment, groups);
        verify(azureUtils, times(0)).getCustomNetworkId(network);
        verify(azureUtils, times(2)).getCustomSubnetIds(network);
    }

}