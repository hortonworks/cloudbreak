package com.sequenceiq.cloudbreak.cloud.azure.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.azure.management.resources.Deployment;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.task.database.PollingStarter;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;

@ExtendWith(MockitoExtension.class)
public class AzureTemplateCreatorServiceTest {

    private static final String RESOURCE_GROUP_NAME = "resourceGroupName";

    private static final String TEMPLATE_NAME = "templateName";

    private static final String TEMPLATE_CONTENT = "templateContent";

    private static final String TEMPLATE_PARAMETERS = "templateParameters";

    private final AzureTemplateCreatorService underTest = new AzureTemplateCreatorService();

    private final AzureTemplateDeploymentParameters azureTemplateDeploymentParameters =
            new AzureTemplateDeploymentParameters(RESOURCE_GROUP_NAME, TEMPLATE_NAME, TEMPLATE_CONTENT, TEMPLATE_PARAMETERS);

    @Mock
    private AzureClient azureClient;

    @Mock
    private PollingStarter deploymentPoller;

    @Test
    void testCreateOrPollTemplateDeploymentWhenDeploymentDoesNotExists() throws Exception {
        Deployment deployment = mock(Deployment.class);
        when(azureClient.templateDeploymentExists(RESOURCE_GROUP_NAME, TEMPLATE_NAME)).thenReturn(false);
        when(azureClient.createTemplateDeployment(azureTemplateDeploymentParameters)).thenReturn(deployment);

        Deployment returnedDeployment = underTest.createOrPollTemplateDeployment(azureClient, azureTemplateDeploymentParameters, deploymentPoller);

        assertEquals(deployment, returnedDeployment);
        verify(azureClient).createTemplateDeployment(azureTemplateDeploymentParameters);
        verify(deploymentPoller, never()).startPolling();
        verify(azureClient, never()).getTemplateDeployment(RESOURCE_GROUP_NAME, TEMPLATE_NAME);
    }

    @ParameterizedTest
    @EnumSource(value = ResourceStatus.class, names = {"IN_PROGRESS"}, mode = EnumSource.Mode.EXCLUDE)
    void testCreateOrPollTemplateDeploymentWhenDeploymentNotInProgress(ResourceStatus resourceStatus) throws Exception {
        Deployment deployment = mock(Deployment.class);
        when(azureClient.templateDeploymentExists(RESOURCE_GROUP_NAME, TEMPLATE_NAME)).thenReturn(true);
        when(azureClient.getTemplateDeploymentStatus(RESOURCE_GROUP_NAME, TEMPLATE_NAME)).thenReturn(resourceStatus);
        when(azureClient.createTemplateDeployment(azureTemplateDeploymentParameters)).thenReturn(deployment);

        Deployment returnedDeployment = underTest.createOrPollTemplateDeployment(azureClient, azureTemplateDeploymentParameters, deploymentPoller);

        assertEquals(deployment, returnedDeployment);
        verify(azureClient).createTemplateDeployment(azureTemplateDeploymentParameters);
        verify(deploymentPoller, never()).startPolling();
        verify(azureClient, never()).getTemplateDeployment(RESOURCE_GROUP_NAME, TEMPLATE_NAME);
    }

    @Test
    void testCreateOrPollTemplateDeploymentWhenDeploymentIsInProgress() throws Exception {
        Deployment deployment = mock(Deployment.class);
        when(azureClient.templateDeploymentExists(RESOURCE_GROUP_NAME, TEMPLATE_NAME)).thenReturn(true);
        when(azureClient.getTemplateDeploymentStatus(RESOURCE_GROUP_NAME, TEMPLATE_NAME)).thenReturn(ResourceStatus.IN_PROGRESS);
        when(azureClient.getTemplateDeployment(RESOURCE_GROUP_NAME, TEMPLATE_NAME)).thenReturn(deployment);

        Deployment returnedDeployment = underTest.createOrPollTemplateDeployment(azureClient, azureTemplateDeploymentParameters, deploymentPoller);

        assertEquals(deployment, returnedDeployment);
        verify(azureClient, never()).createTemplateDeployment(azureTemplateDeploymentParameters);
        verify(deploymentPoller).startPolling();
        verify(azureClient).getTemplateDeployment(RESOURCE_GROUP_NAME, TEMPLATE_NAME);
    }

}
