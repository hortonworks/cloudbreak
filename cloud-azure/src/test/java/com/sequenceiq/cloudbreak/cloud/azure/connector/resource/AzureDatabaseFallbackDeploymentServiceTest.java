package com.sequenceiq.cloudbreak.cloud.azure.connector.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

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

    private final ListAppender<ILoggingEvent> logAppender = new ListAppender<>();

    private Level originalLogLevel;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "retryService", retryService);
        Logger logger = (Logger) LoggerFactory.getLogger(AzureDatabaseFallbackDeploymentService.class);
        originalLogLevel = logger.getLevel();
        logger.setLevel(Level.INFO);
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        Logger logger = (Logger) LoggerFactory.getLogger(AzureDatabaseFallbackDeploymentService.class);
        logger.detachAppender(logAppender);
        logger.setLevel(originalLogLevel);
        logAppender.stop();
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
    void deployWithFallbackTransientProvisioningFailureRedeploysOnce() {
        DatabaseStack databaseStack = createDatabaseStack(PRIMARY_SKU, List.of());
        ManagementException transientException = createManagementException("DeploymentFailed");

        when(azureDatabaseTemplateBuilder.build(cloudContext, databaseStack)).thenReturn(TEMPLATE);
        when(azureExceptionHandler.isTransientProvisioningFailure(transientException)).thenReturn(true);
        mockRetryThrowsThenSucceeds(transientException);

        String result = underTest.deployWithFallback(STACK_NAME, RESOURCE_GROUP, client, cloudContext, databaseStack);

        assertNull(result);
        verify(client, times(2)).createTemplateDeployment(eq(RESOURCE_GROUP), eq(STACK_NAME), eq(TEMPLATE), anyString());
    }

    @Test
    void deployWithFallbackTransientProvisioningFailureWithFallbackSkusKeepsPrimarySkuOnRedeploy() {
        DatabaseStack databaseStack = createDatabaseStack(PRIMARY_SKU, List.of(FALLBACK_SKU_1));
        ManagementException transientException = createManagementException("DeploymentFailed");

        when(azureDatabaseTemplateBuilder.build(eq(cloudContext), any(DatabaseStack.class))).thenReturn(TEMPLATE);
        when(azureExceptionHandler.isTransientProvisioningFailure(transientException)).thenReturn(true);
        mockRetryThrowsThenSucceeds(transientException);

        String result = underTest.deployWithFallback(STACK_NAME, RESOURCE_GROUP, client, cloudContext, databaseStack);

        assertNull(result);
        verify(azureDatabaseTemplateBuilder, times(1)).build(eq(cloudContext), any(DatabaseStack.class));
        verify(client, times(2)).createTemplateDeployment(eq(RESOURCE_GROUP), eq(STACK_NAME), eq(TEMPLATE), anyString());
        assertThat(logAppender.list)
                .anySatisfy(event -> {
                    assertThat(event.getLevel()).isEqualTo(Level.INFO);
                    assertThat(event.getFormattedMessage()).isEqualTo("DB ARM deployment test-rg/test-stack succeeded on redeploy "
                            + "after a transient Azure provisioning error.");
                });
    }

    @Test
    void deployWithFallbackTransientFailureThenCapacityErrorFallsBackToNextSku() {
        DatabaseStack databaseStack = createDatabaseStack(PRIMARY_SKU, List.of(FALLBACK_SKU_1, FALLBACK_SKU_2));
        ManagementException transientException = createManagementException("DeploymentFailed");
        ManagementException capacityException = createManagementException("SkuNotAvailable");
        String fallbackTemplate = "{\"template\":\"fallback\"}";

        when(azureDatabaseTemplateBuilder.build(eq(cloudContext), any(DatabaseStack.class))).thenReturn(TEMPLATE, fallbackTemplate);
        when(azureExceptionHandler.isTransientProvisioningFailure(transientException)).thenReturn(true);
        when(azureExceptionHandler.isCapacityError(capacityException)).thenReturn(true);
        mockRetryRunsAction();
        when(client.createTemplateDeployment(eq(RESOURCE_GROUP), eq(STACK_NAME), eq(TEMPLATE), anyString()))
                .thenThrow(transientException, capacityException);

        String result = underTest.deployWithFallback(STACK_NAME, RESOURCE_GROUP, client, cloudContext, databaseStack);

        assertEquals(FALLBACK_SKU_1, result);
        assertThat(capacityException.getSuppressed()).containsExactly(transientException);
        ArgumentCaptor<DatabaseStack> stackCaptor = ArgumentCaptor.forClass(DatabaseStack.class);
        verify(azureDatabaseTemplateBuilder, times(2)).build(eq(cloudContext), stackCaptor.capture());
        assertThat(stackCaptor.getAllValues()).extracting(stack -> stack.getDatabaseServer().getFlavor())
                .containsExactly(PRIMARY_SKU, FALLBACK_SKU_1);
        verify(client, times(2)).createTemplateDeployment(eq(RESOURCE_GROUP), eq(STACK_NAME), eq(TEMPLATE), anyString());
        verify(client).createTemplateDeployment(eq(RESOURCE_GROUP), eq(STACK_NAME), eq(fallbackTemplate), anyString());
    }

    @Test
    void deployWithFallbackTransientProvisioningFailureTwiceThrowsSecondErrorAfterSingleRedeploy() {
        DatabaseStack databaseStack = createDatabaseStack(PRIMARY_SKU, List.of());
        ManagementException firstException = createManagementException("DeploymentFailed");
        ManagementException secondException = createManagementException("DeploymentFailed");

        when(azureDatabaseTemplateBuilder.build(cloudContext, databaseStack)).thenReturn(TEMPLATE);
        when(azureExceptionHandler.isTransientProvisioningFailure(firstException)).thenReturn(true);
        mockRetryRunsAction();
        when(client.createTemplateDeployment(eq(RESOURCE_GROUP), eq(STACK_NAME), eq(TEMPLATE), anyString()))
                .thenThrow(firstException, secondException);

        ManagementException thrown = assertThrows(ManagementException.class,
                () -> underTest.deployWithFallback(STACK_NAME, RESOURCE_GROUP, client, cloudContext, databaseStack));

        assertSame(secondException, thrown);
        assertThat(thrown.getSuppressed()).containsExactly(firstException);
        verify(client, times(2)).createTemplateDeployment(eq(RESOURCE_GROUP), eq(STACK_NAME), eq(TEMPLATE), anyString());
        assertThat(logAppender.list).noneMatch(event -> event.getFormattedMessage().contains("succeeded on redeploy"));
    }

    @Test
    void deployWithFallbackSameTransientExceptionTwiceDoesNotSuppressItself() {
        DatabaseStack databaseStack = createDatabaseStack(PRIMARY_SKU, List.of());
        ManagementException transientException = createManagementException("DeploymentFailed");

        when(azureDatabaseTemplateBuilder.build(cloudContext, databaseStack)).thenReturn(TEMPLATE);
        when(azureExceptionHandler.isTransientProvisioningFailure(transientException)).thenReturn(true);
        mockRetryAlwaysThrows(transientException);

        ManagementException thrown = assertThrows(ManagementException.class,
                () -> underTest.deployWithFallback(STACK_NAME, RESOURCE_GROUP, client, cloudContext, databaseStack));

        assertSame(transientException, thrown);
        assertThat(thrown.getSuppressed()).isEmpty();
        verify(client, times(2)).createTemplateDeployment(eq(RESOURCE_GROUP), eq(STACK_NAME), eq(TEMPLATE), anyString());
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

    @Test
    void deployWithFallbackSimulatedCapacityFailureSkipsPrimaryAndDeploysFallbackSku() {
        DatabaseStack databaseStack = createDatabaseStack(PRIMARY_SKU, List.of(FALLBACK_SKU_1, FALLBACK_SKU_2), 1);
        when(azureDatabaseTemplateBuilder.build(eq(cloudContext), any(DatabaseStack.class))).thenReturn(TEMPLATE);
        mockRetryRunsAction();

        String result = underTest.deployWithFallback(STACK_NAME, RESOURCE_GROUP, client, cloudContext, databaseStack);

        assertEquals(FALLBACK_SKU_1, result);
        ArgumentCaptor<DatabaseStack> stackCaptor = ArgumentCaptor.forClass(DatabaseStack.class);
        verify(azureDatabaseTemplateBuilder, times(1)).build(eq(cloudContext), stackCaptor.capture());
        assertEquals(FALLBACK_SKU_1, stackCaptor.getValue().getDatabaseServer().getFlavor());
        verify(client, times(1)).createTemplateDeployment(eq(RESOURCE_GROUP), eq(STACK_NAME), eq(TEMPLATE), anyString());
    }

    @Test
    void deployWithFallbackAllAttemptsSimulatedCapacityFailuresThrowsWithoutSubmittingDeployment() {
        DatabaseStack databaseStack = createDatabaseStack(PRIMARY_SKU, List.of(FALLBACK_SKU_1, FALLBACK_SKU_2), Integer.MAX_VALUE);

        InsufficientCapacityException thrown = assertThrows(InsufficientCapacityException.class,
                () -> underTest.deployWithFallback(STACK_NAME, RESOURCE_GROUP, client, cloudContext, databaseStack));

        assertTrue(thrown.getMessage().contains(String.format("all instance types [%s, %s, %s] exhausted due to capacity shortage",
                PRIMARY_SKU, FALLBACK_SKU_1, FALLBACK_SKU_2)), thrown.getMessage());
        assertTrue(thrown.getMessage().contains(String.format("Simulated capacity failure for SKU '%s'", FALLBACK_SKU_2)), thrown.getMessage());
        assertNull(thrown.getCause());
        verifyNoInteractions(azureDatabaseTemplateBuilder, retryService, client);
    }

    @Test
    void deployWithFallbackSimulatedCapacityFailureWithoutFallbackTypesThrowsWithoutSubmittingDeployment() {
        DatabaseStack databaseStack = createDatabaseStack(PRIMARY_SKU, List.of(), 1);

        InsufficientCapacityException thrown = assertThrows(InsufficientCapacityException.class,
                () -> underTest.deployWithFallback(STACK_NAME, RESOURCE_GROUP, client, cloudContext, databaseStack));

        assertTrue(thrown.getMessage().contains(String.format("all instance types [%s] exhausted due to capacity shortage", PRIMARY_SKU)),
                thrown.getMessage());
        assertTrue(thrown.getMessage().contains(String.format("Simulated capacity failure for SKU '%s'", PRIMARY_SKU)), thrown.getMessage());
        verifyNoInteractions(azureDatabaseTemplateBuilder, retryService, client);
    }

    @Test
    void deployWithFallbackSimulatedCapacityFailureThenRealCapacityErrorOnLastSkuThrowsExhausted() {
        DatabaseStack databaseStack = createDatabaseStack(PRIMARY_SKU, List.of(FALLBACK_SKU_1), 1);
        ManagementException capacityException = createManagementException("SkuNotAvailable");

        when(azureDatabaseTemplateBuilder.build(eq(cloudContext), any(DatabaseStack.class))).thenReturn(TEMPLATE);
        when(azureExceptionHandler.isCapacityError(capacityException)).thenReturn(true);
        mockRetryAlwaysThrows(capacityException);

        InsufficientCapacityException thrown = assertThrows(InsufficientCapacityException.class,
                () -> underTest.deployWithFallback(STACK_NAME, RESOURCE_GROUP, client, cloudContext, databaseStack));

        assertTrue(thrown.getMessage().contains(String.format("all instance types [%s, %s] exhausted due to capacity shortage",
                PRIMARY_SKU, FALLBACK_SKU_1)), thrown.getMessage());
        assertEquals(capacityException, thrown.getCause());
        ArgumentCaptor<DatabaseStack> stackCaptor = ArgumentCaptor.forClass(DatabaseStack.class);
        verify(azureDatabaseTemplateBuilder, times(1)).build(eq(cloudContext), stackCaptor.capture());
        assertEquals(FALLBACK_SKU_1, stackCaptor.getValue().getDatabaseServer().getFlavor());
        verify(client, times(1)).createTemplateDeployment(eq(RESOURCE_GROUP), eq(STACK_NAME), eq(TEMPLATE), anyString());
    }

    private DatabaseStack createDatabaseStack(String flavor, List<String> fallbackTypes, int simulatedCapacityFailures) {
        DatabaseStack databaseStack = createDatabaseStack(flavor, fallbackTypes);
        databaseStack.getDatabaseServer().putParameter(DatabaseServer.SIMULATED_CAPACITY_FAILURES, simulatedCapacityFailures);
        return databaseStack;
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
        return new ManagementException("Deployment failed: " + code, null, new ManagementError(code, "Deployment failed."));
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
