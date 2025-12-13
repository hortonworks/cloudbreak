package com.sequenceiq.cloudbreak.cloud.azure.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.resources.models.Deployment;
import com.azure.resourcemanager.resources.models.DeploymentOperation;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureListResult;

@ExtendWith(MockitoExtension.class)
class AzureTemplateDeploymentFailureReasonProviderTest {

    private static final String RESOURCE_GROUP = "resource-group";

    private static final String DEPLOYMENT_NAME = "deployment-name";

    private static final String ERROR_MASSAGE = "Internal error";

    @InjectMocks
    private AzureTemplateDeploymentFailureReasonProvider underTest;

    @Mock
    private AzureClient client;

    @Mock
    private Deployment deployment;

    @Mock
    private AzureListResult<DeploymentOperation> deploymentOperations;

    @Test
    void testGetFailureMessageShouldReturnErrorReasonFromFailedDeploymentOperation() {
        when(client.getTemplateDeployment(RESOURCE_GROUP, DEPLOYMENT_NAME)).thenReturn(deployment);
        when(deployment.provisioningState()).thenReturn("Failed");
        when(client.getTemplateDeploymentOperations(RESOURCE_GROUP, DEPLOYMENT_NAME)).thenReturn(deploymentOperations);

        DeploymentOperation operation1 = createDeploymentOperation("Successful", "Successful operation");
        DeploymentOperation operation2 = createDeploymentOperation("Failed", ERROR_MASSAGE);
        when(deploymentOperations.getAll()).thenReturn(List.of(operation1, operation2));

        Optional<String> actual = underTest.getFailureMessage(RESOURCE_GROUP, DEPLOYMENT_NAME, client);

        assertTrue(actual.isPresent());
        assertEquals(ERROR_MASSAGE, actual.get());
    }

    @Test
    void testGetFailureMessageShouldReturnOptionalEmptyWhenTheDeploymentStatusIsNotFailed() {
        when(client.getTemplateDeployment(RESOURCE_GROUP, DEPLOYMENT_NAME)).thenReturn(deployment);
        when(deployment.provisioningState()).thenReturn("Succeeded");

        Optional<String> actual = underTest.getFailureMessage(RESOURCE_GROUP, DEPLOYMENT_NAME, client);

        assertTrue(actual.isEmpty());
        verify(client).getTemplateDeployment(RESOURCE_GROUP, DEPLOYMENT_NAME);
        verifyNoMoreInteractions(client);
    }

    @Test
    void testGetFailureMessageShouldReturnOptionalEmptyWhenThereIsNoFailedOperationFound() {
        when(client.getTemplateDeployment(RESOURCE_GROUP, DEPLOYMENT_NAME)).thenReturn(deployment);
        when(deployment.provisioningState()).thenReturn("Failed");
        when(client.getTemplateDeploymentOperations(RESOURCE_GROUP, DEPLOYMENT_NAME)).thenReturn(deploymentOperations);

        DeploymentOperation operation1 = createDeploymentOperation("Successful", "Successful operation 1");
        DeploymentOperation operation2 = createDeploymentOperation("Successful", "Successful operation 2");
        when(deploymentOperations.getAll()).thenReturn(List.of(operation1, operation2));

        Optional<String> actual = underTest.getFailureMessage(RESOURCE_GROUP, DEPLOYMENT_NAME, client);

        assertTrue(actual.isEmpty());
    }

    @Test
    void testGetFailureMessageShouldReturnOptionalEmptyWhenThereIsAFailedOperationWithoutErrorMessage() {
        when(client.getTemplateDeployment(RESOURCE_GROUP, DEPLOYMENT_NAME)).thenReturn(deployment);
        when(deployment.provisioningState()).thenReturn("Failed");
        when(client.getTemplateDeploymentOperations(RESOURCE_GROUP, DEPLOYMENT_NAME)).thenReturn(deploymentOperations);

        DeploymentOperation operation1 = createDeploymentOperation("Successful", "Successful operation");
        DeploymentOperation operation2 = createDeploymentOperation("Failed", null);
        when(deploymentOperations.getAll()).thenReturn(List.of(operation1, operation2));

        Optional<String> actual = underTest.getFailureMessage(RESOURCE_GROUP, DEPLOYMENT_NAME, client);

        assertTrue(actual.isEmpty());
    }

    @Test
    void testGetFailureMessageShouldReturnOptionalEmptyWhenTheClientThrowsAnException() {
        doThrow(ManagementException.class).when(client).getTemplateDeployment(RESOURCE_GROUP, DEPLOYMENT_NAME);

        Optional<String> actual = underTest.getFailureMessage(RESOURCE_GROUP, DEPLOYMENT_NAME, client);

        assertTrue(actual.isEmpty());
    }

    private DeploymentOperation createDeploymentOperation(String status, String message) {
        DeploymentOperation deploymentOperation = mock(DeploymentOperation.class);
        when(deploymentOperation.provisioningState()).thenReturn(status);
        lenient().when(deploymentOperation.statusMessage()).thenReturn(message);
        return deploymentOperation;
    }
}