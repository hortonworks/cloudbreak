package com.sequenceiq.cloudbreak.cloud.azure.connector.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.azure.core.management.exception.ManagementError;
import com.azure.core.management.exception.ManagementException;
import com.sequenceiq.cloudbreak.cloud.azure.AzureDatabaseTemplateBuilder;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureExceptionHandler;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.InsufficientCapacityException;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.service.retry.Retry;
import com.sequenceiq.common.model.AzureDatabaseType;

@ExtendWith(MockitoExtension.class)
class AzureDatabaseFallbackDeploymentServiceTest {

    private static final String STACK_NAME = "test-stack";

    private static final String RESOURCE_GROUP = "test-rg";

    private static final String PRIMARY_SKU = "Standard_D4s_v3";

    private static final String FALLBACK_SKU_1 = "Standard_D2s_v3";

    private static final String FALLBACK_SKU_2 = "Standard_B2s";

    private static final String TEMPLATE = "{\"template\":\"content\"}";

    @Mock
    private AzureDatabaseTemplateBuilder azureDatabaseTemplateBuilder;

    @Mock
    private AzureExceptionHandler azureExceptionHandler;

    @Mock
    private Retry retryService;

    @Mock
    private AzureClient client;

    @Mock
    private CloudContext cloudContext;

    @InjectMocks
    private AzureDatabaseFallbackDeploymentService underTest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "retryService", retryService);
    }

    @Test
    void deployWithFallbackFirstAttemptSucceedsReturnsNull() {
        DatabaseStack databaseStack = createDatabaseStack(PRIMARY_SKU, List.of(FALLBACK_SKU_1, FALLBACK_SKU_2));
        when(azureDatabaseTemplateBuilder.build(eq(cloudContext), any(DatabaseStack.class))).thenReturn(TEMPLATE);
        mockRetryRunsAction();

        String result = underTest.deployWithFallback(STACK_NAME, RESOURCE_GROUP, client, cloudContext, databaseStack);

        assertNull(result);
        verify(client, times(1)).createTemplateDeployment(eq(RESOURCE_GROUP), eq(STACK_NAME), eq(TEMPLATE), anyString());
    }

    @Test
    void deployWithFallbackFallbackDisabledEmptyListDeploysWithoutFallback() {
        DatabaseStack databaseStack = createDatabaseStack(PRIMARY_SKU, List.of());
        when(azureDatabaseTemplateBuilder.build(cloudContext, databaseStack)).thenReturn(TEMPLATE);
        mockRetryRunsAction();

        String result = underTest.deployWithFallback(STACK_NAME, RESOURCE_GROUP, client, cloudContext, databaseStack);

        assertNull(result);
        verify(client, times(1)).createTemplateDeployment(eq(RESOURCE_GROUP), eq(STACK_NAME), eq(TEMPLATE), anyString());
    }

    @Test
    void deployWithFallbackCapacityErrorFallsBackToNextSku() {
        DatabaseStack databaseStack = createDatabaseStack(PRIMARY_SKU, List.of(FALLBACK_SKU_1, FALLBACK_SKU_2));
        ManagementException capacityException = createManagementException("SkuNotAvailable");

        when(azureDatabaseTemplateBuilder.build(eq(cloudContext), any(DatabaseStack.class))).thenReturn(TEMPLATE);
        when(azureExceptionHandler.isCapacityError(capacityException)).thenReturn(true);
        mockRetryThrowsThenSucceeds(capacityException);

        String result = underTest.deployWithFallback(STACK_NAME, RESOURCE_GROUP, client, cloudContext, databaseStack);

        assertEquals(FALLBACK_SKU_1, result);
        verify(client, times(2)).createTemplateDeployment(eq(RESOURCE_GROUP), eq(STACK_NAME), eq(TEMPLATE), anyString());
    }

    @Test
    void deployWithFallbackAllSkusExhaustedThrowsInsufficientCapacityException() {
        DatabaseStack databaseStack = createDatabaseStack(PRIMARY_SKU, List.of(FALLBACK_SKU_1));
        ManagementException capacityException = createManagementException("AllocationFailed");

        when(azureDatabaseTemplateBuilder.build(eq(cloudContext), any(DatabaseStack.class))).thenReturn(TEMPLATE);
        when(azureExceptionHandler.isCapacityError(capacityException)).thenReturn(true);
        mockRetryAlwaysThrows(capacityException);

        InsufficientCapacityException thrown = assertThrows(InsufficientCapacityException.class,
                () -> underTest.deployWithFallback(STACK_NAME, RESOURCE_GROUP, client, cloudContext, databaseStack));

        assertEquals(capacityException, thrown.getCause());
        verify(client, times(2)).createTemplateDeployment(eq(RESOURCE_GROUP), eq(STACK_NAME), eq(TEMPLATE), anyString());
    }

    @Test
    void deployWithFallbackNonCapacityErrorThrowsImmediately() {
        DatabaseStack databaseStack = createDatabaseStack(PRIMARY_SKU, List.of(FALLBACK_SKU_1, FALLBACK_SKU_2));
        ManagementException nonCapacityException = createManagementException("InvalidParameter");

        when(azureDatabaseTemplateBuilder.build(eq(cloudContext), any(DatabaseStack.class))).thenReturn(TEMPLATE);
        when(azureExceptionHandler.isCapacityError(nonCapacityException)).thenReturn(false);
        mockRetryAlwaysThrows(nonCapacityException);

        ManagementException thrown = assertThrows(ManagementException.class,
                () -> underTest.deployWithFallback(STACK_NAME, RESOURCE_GROUP, client, cloudContext, databaseStack));

        assertEquals(nonCapacityException, thrown);
        verify(client, times(1)).createTemplateDeployment(eq(RESOURCE_GROUP), eq(STACK_NAME), eq(TEMPLATE), anyString());
    }

    @Test
    void deployWithFallbackConflictRetrySucceedsAfterRetry() {
        DatabaseStack databaseStack = createDatabaseStack(PRIMARY_SKU, List.of(FALLBACK_SKU_1));
        ManagementException conflictException = createManagementException("Conflict");

        when(azureDatabaseTemplateBuilder.build(eq(cloudContext), any(DatabaseStack.class))).thenReturn(TEMPLATE);
        when(azureExceptionHandler.isExceptionCodeConflict(conflictException)).thenReturn(true);

        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(0);
            try {
                action.run();
            } catch (Retry.ActionFailedException e) {
                action.run();
            }
            return null;
        }).when(retryService).testWith2SecDelayMax5Times(any(Runnable.class));

        when(client.createTemplateDeployment(eq(RESOURCE_GROUP), eq(STACK_NAME), eq(TEMPLATE), anyString()))
                .thenThrow(conflictException)
                .thenReturn(null);

        String result = underTest.deployWithFallback(STACK_NAME, RESOURCE_GROUP, client, cloudContext, databaseStack);

        assertNull(result);
        verify(client, times(2)).createTemplateDeployment(eq(RESOURCE_GROUP), eq(STACK_NAME), eq(TEMPLATE), anyString());
    }

    private DatabaseStack createDatabaseStack(String flavor, List<String> fallbackTypes) {
        DatabaseServer databaseServer = DatabaseServer.builder()
                .withFlavor(flavor)
                .withFallbackInstanceTypes(fallbackTypes)
                .withParams(Map.of(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, AzureDatabaseType.FLEXIBLE_SERVER.name()))
                .build();
        return new DatabaseStack(null, databaseServer, Map.of(), null);
    }

    private ManagementException createManagementException(String code) {
        ManagementError error = mock(ManagementError.class);
        lenient().when(error.getCode()).thenReturn(code);
        ManagementException exception = mock(ManagementException.class);
        lenient().when(exception.getValue()).thenReturn(error);
        return exception;
    }

    private void mockRetryRunsAction() {
        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(0);
            action.run();
            return null;
        }).when(retryService).testWith2SecDelayMax5Times(any(Runnable.class));
    }

    private void mockRetryThrowsThenSucceeds(ManagementException exceptionOnFirstCall) {
        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(0);
            action.run();
            return null;
        }).when(retryService).testWith2SecDelayMax5Times(any(Runnable.class));

        when(client.createTemplateDeployment(eq(RESOURCE_GROUP), eq(STACK_NAME), eq(TEMPLATE), anyString()))
                .thenThrow(exceptionOnFirstCall)
                .thenReturn(null);
    }

    private void mockRetryAlwaysThrows(ManagementException exception) {
        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(0);
            action.run();
            return null;
        }).when(retryService).testWith2SecDelayMax5Times(any(Runnable.class));

        when(client.createTemplateDeployment(eq(RESOURCE_GROUP), eq(STACK_NAME), eq(TEMPLATE), anyString()))
                .thenThrow(exception);
    }
}
