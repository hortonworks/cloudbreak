package com.sequenceiq.cloudbreak.cloud.azure.template;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.microsoft.azure.management.resources.Deployment;
import com.sequenceiq.cloudbreak.cloud.azure.AzureCloudResourceService;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@RunWith(MockitoJUnitRunner.class)
public class AzureTransientDeploymentServiceTest {

    private static final String RESOURCE_GROUP = "resource_group";

    private static final String DEPLOYMENT_NAME = "deployment_name";

    private static final String INSTANCE_1 = "instance-1";

    private static final String STORAGE_1 = "storage_1";

    private static final String IP_1 = "ip_1";

    @Mock
    private AzureClient client;

    @Mock
    private Deployment deployment;

    @Mock
    private AzureCloudResourceService azureCloudResourceService;

    @InjectMocks
    private AzureTransientDeploymentService underTest;

    @Test
    public void testEmptyTransientDeploymentCancelled() {

        when(client.getTemplateDeploymentStatus(RESOURCE_GROUP, DEPLOYMENT_NAME)).thenReturn(ResourceStatus.IN_PROGRESS);
        when(client.getTemplateDeployment(RESOURCE_GROUP, DEPLOYMENT_NAME)).thenReturn(deployment);
        when(azureCloudResourceService.getDeploymentCloudResources(deployment)).thenReturn(Collections.emptyList());

        List<CloudResource> deployedResources = underTest.handleTransientDeployment(client, RESOURCE_GROUP, DEPLOYMENT_NAME);

        verify(client).getTemplateDeploymentStatus(RESOURCE_GROUP, DEPLOYMENT_NAME);
        verify(client).getTemplateDeployment(RESOURCE_GROUP, DEPLOYMENT_NAME);
        verify(deployment).cancel();
        assertEquals(0, deployedResources.size());
    }

    @Test
    public void testNonEmptyTransientDeploymentCancelled() {
        CloudResource vm1 = createCloudResource(INSTANCE_1, ResourceType.AZURE_INSTANCE);
        CloudResource storage1 = createCloudResource(STORAGE_1, ResourceType.AZURE_STORAGE);
        CloudResource ip1 = createCloudResource(IP_1, ResourceType.AZURE_PUBLIC_IP);

        when(client.getTemplateDeploymentStatus(RESOURCE_GROUP, DEPLOYMENT_NAME)).thenReturn(ResourceStatus.IN_PROGRESS);
        when(client.getTemplateDeployment(RESOURCE_GROUP, DEPLOYMENT_NAME)).thenReturn(deployment);
        when(azureCloudResourceService.getDeploymentCloudResources(deployment)).thenReturn(List.of(vm1, storage1, ip1));

        List<CloudResource> deployedResources = underTest.handleTransientDeployment(client, RESOURCE_GROUP, DEPLOYMENT_NAME);

        verify(client).getTemplateDeploymentStatus(RESOURCE_GROUP, DEPLOYMENT_NAME);
        verify(client).getTemplateDeployment(RESOURCE_GROUP, DEPLOYMENT_NAME);
        verify(deployment).cancel();
        assertEquals(3, deployedResources.size());
    }

    @Test
    public void testNonTransientDeploymentNotCancelled() {
        when(client.getTemplateDeploymentStatus(RESOURCE_GROUP, DEPLOYMENT_NAME)).thenReturn(ResourceStatus.CREATED);

        List<CloudResource> deployedResources = underTest.handleTransientDeployment(client, RESOURCE_GROUP, DEPLOYMENT_NAME);

        verify(client).getTemplateDeploymentStatus(RESOURCE_GROUP, DEPLOYMENT_NAME);
        verify(client,  times(0)).getTemplateDeployment(RESOURCE_GROUP, DEPLOYMENT_NAME);
        verify(deployment, times(0)).cancel();
        assertEquals(0, deployedResources.size());
    }

    private CloudResource createCloudResource(String name, ResourceType resourceType) {
        return new CloudResource.Builder()
                .name(name)
                .status(CommonStatus.CREATED)
                .type(resourceType)
                .instanceId("instanceId")
                .params(Collections.emptyMap())
                .build();
    }
}