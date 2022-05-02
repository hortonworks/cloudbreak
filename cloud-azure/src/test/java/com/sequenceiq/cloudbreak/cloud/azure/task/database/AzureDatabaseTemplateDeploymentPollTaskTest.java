package com.sequenceiq.cloudbreak.cloud.azure.task.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.template.AzureTemplateDeploymentParameters;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;

@ExtendWith(MockitoExtension.class)
public class AzureDatabaseTemplateDeploymentPollTaskTest {

    private static final String RESOURCE_GROUP_NAME = "resourceGroupName";

    private static final String TEMPLATE_NAME = "templateName";

    private static final String TEMPLATE_CONTENT = "templateContent";

    private static final String TEMPLATE_PARAMETERS = "templateParameters";

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private AzureDatabaseTemplateDeploymentContext azureDatabaseTemplateDeploymentContext;

    @InjectMocks
    private AzureDatabaseTemplateDeploymentPollTask underTest;

    @Mock
    private AzureClient azureClient;

    @BeforeEach
    void setup() {
        when(azureDatabaseTemplateDeploymentContext.getAzureClient()).thenReturn(azureClient);
        AzureTemplateDeploymentParameters azureTemplateDeploymentParameters =
                new AzureTemplateDeploymentParameters(RESOURCE_GROUP_NAME, TEMPLATE_NAME, TEMPLATE_CONTENT, TEMPLATE_PARAMETERS);
        when(azureDatabaseTemplateDeploymentContext.getAzureTemplateDeploymentParameters()).thenReturn(azureTemplateDeploymentParameters);
    }

    @Test
    void testDoCallWhenTemplateSucceeded() {
        when(azureClient.getTemplateDeploymentStatus(RESOURCE_GROUP_NAME, TEMPLATE_NAME)).thenReturn(ResourceStatus.CREATED);

        Boolean result = underTest.doCall();

        assertTrue(result);
    }

    @Test
    void testDoCallWhenTemplateDeleted() {
        when(azureClient.getTemplateDeploymentStatus(RESOURCE_GROUP_NAME, TEMPLATE_NAME)).thenReturn(ResourceStatus.DELETED);

        CloudConnectorException e = assertThrows(CloudConnectorException.class, () ->
                underTest.doCall()
        );

        String exceptionTest = String.format("Deployment %s in resource group %s is either deleted or does not exist", TEMPLATE_NAME, RESOURCE_GROUP_NAME);
        assertEquals(exceptionTest, e.getMessage());
    }

    @Test
    void testDoCallWhenTemplateFailed() {
        when(azureClient.getTemplateDeploymentStatus(RESOURCE_GROUP_NAME, TEMPLATE_NAME)).thenReturn(ResourceStatus.FAILED);

        CloudConnectorException e = assertThrows(CloudConnectorException.class, () ->
                underTest.doCall()
        );

        String exceptionTest = String.format("Deployment %s in resource group %s was either cancelled or failed.", TEMPLATE_NAME, RESOURCE_GROUP_NAME);
        assertEquals(exceptionTest, e.getMessage());
    }

    @Test
    void testDoCallWhenTemplateInProgress() {
        when(azureClient.getTemplateDeploymentStatus(RESOURCE_GROUP_NAME, TEMPLATE_NAME)).thenReturn(ResourceStatus.IN_PROGRESS);

        Boolean result = underTest.doCall();

        assertFalse(result);
    }

}
