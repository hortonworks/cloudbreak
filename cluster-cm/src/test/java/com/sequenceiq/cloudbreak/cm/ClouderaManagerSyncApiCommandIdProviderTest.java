package com.sequenceiq.cloudbreak.cm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.sequenceiq.cloudbreak.cm.commands.SyncApiCommandPollerConfig;
import com.sequenceiq.cloudbreak.cm.commands.SyncApiCommandRetriever;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@ExtendWith(MockitoExtension.class)
public class ClouderaManagerSyncApiCommandIdProviderTest {

    private static final int INTERRUPT_TIMEOUT_SECONDS = 120;

    private static final String COMMAND_NAME = "DeployClusterClientConfig";

    @Mock
    private ClustersResourceApi clustersResourceApi;

    @Mock
    private ExecutorService executorService;

    @Mock
    private Future<ApiCommand> future;

    @Mock
    private SyncApiCommandRetriever syncApiCommandRetriever;

    @Mock
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    @Mock
    private SyncApiCommandPollerConfig syncApiCommandPollerConfig;

    @Mock
    private Callable<ApiCommand> apiCommandCallable;

    private ClouderaManagerSyncApiCommandIdProvider underTest;

    private Stack stack;

    @BeforeEach
    public void setUp() {
        underTest = spy(new ClouderaManagerSyncApiCommandIdProvider(
                syncApiCommandRetriever, clouderaManagerPollingServiceProvider, syncApiCommandPollerConfig, executorService));
        stack = new Stack();
        stack.setName("mycluster");
    }

    @Test
    public void testGetDeployClientConfigCommandId() throws Exception {
        // GIVEN
        given(syncApiCommandPollerConfig.getInterruptTimeoutSeconds()).willReturn(INTERRUPT_TIMEOUT_SECONDS);
        given(executorService.submit(any(Callable.class))).willReturn(future);
        given(future.get(INTERRUPT_TIMEOUT_SECONDS, TimeUnit.SECONDS)).willReturn(createCommand(1L));
        // WHEN
        BigDecimal result = underTest.executeSyncApiCommandAndGetCommandId(
                COMMAND_NAME, clustersResourceApi, stack, null, apiCommandCallable);
        // THEN
        assertEquals(1L, result.longValue());
    }

    @Test
    public void testGetDeployClientConfigCommandIdWithRunningActiveCommand()
            throws ApiException, CloudbreakException {
        // GIVEN
        List<ApiCommand> activeCommands = new ArrayList<>();
        ApiCommand command = new ApiCommand();
        command.setId(new BigDecimal(10L));
        command.setName(COMMAND_NAME);
        activeCommands.add(command);
        // WHEN
        BigDecimal result = underTest.executeSyncApiCommandAndGetCommandId(
                COMMAND_NAME, clustersResourceApi, stack, activeCommands, apiCommandCallable);
        // THEN
        assertEquals(10L, result.longValue());
    }

    @Test
    public void testGetDeployClientConfigCommandIdApiException()
            throws InterruptedException, ExecutionException, TimeoutException {
        // GIVEN
        given(syncApiCommandPollerConfig.getInterruptTimeoutSeconds()).willReturn(INTERRUPT_TIMEOUT_SECONDS);
        given(executorService.submit(any(Callable.class))).willReturn(future);
        given(future.get(INTERRUPT_TIMEOUT_SECONDS, TimeUnit.SECONDS)).willThrow(new ExecutionException(new ApiException("my exc")));
        // WHEN
        ApiException executionException = assertThrows(ApiException.class,
                () -> underTest.executeSyncApiCommandAndGetCommandId(
                        COMMAND_NAME, clustersResourceApi, stack, null, apiCommandCallable)
        );
        // THEN
        assertTrue(executionException.getMessage().contains("my exc"));
    }

    @Test
    public void testGetDeployClientConfigCommandIdNullAnswers()
            throws InterruptedException, ExecutionException, TimeoutException {
        // GIVEN
        given(syncApiCommandPollerConfig.getInterruptTimeoutSeconds()).willReturn(INTERRUPT_TIMEOUT_SECONDS);
        given(executorService.submit(any(Callable.class))).willReturn(future);
        given(future.get(INTERRUPT_TIMEOUT_SECONDS, TimeUnit.SECONDS)).willThrow(new TimeoutException());
        given(future.cancel(true)).willReturn(true);
        // WHEN
        CloudbreakException exception = assertThrows(CloudbreakException.class,
                () -> underTest.executeSyncApiCommandAndGetCommandId(
                        COMMAND_NAME, clustersResourceApi, stack, null, apiCommandCallable));
        // THEN
        assertTrue(exception.getMessage().contains(String.format("Obtaining Cloudera Manager %s command ID was not possible", COMMAND_NAME)));
        verify(future, times(1)).cancel(true);
    }

    @Test
    public void testGetDeployClientConfigCommandIdByListCommandsApiException()
            throws ApiException, InterruptedException, ExecutionException,
            TimeoutException, CloudbreakException {
        // GIVEN
        given(syncApiCommandPollerConfig.getInterruptTimeoutSeconds()).willReturn(INTERRUPT_TIMEOUT_SECONDS);
        given(executorService.submit(any(Callable.class))).willReturn(future);
        given(future.get(INTERRUPT_TIMEOUT_SECONDS, TimeUnit.SECONDS)).willThrow(new TimeoutException());
        given(future.cancel(true)).willReturn(true);
        given(syncApiCommandRetriever.getLastFinishedCommandId(COMMAND_NAME, clustersResourceApi, stack))
                .willReturn(Optional.empty());
        given(syncApiCommandRetriever.getCommandId(COMMAND_NAME, clustersResourceApi, stack))
                .willThrow(new ApiException("my exc"));
        // WHEN
        ApiException apiException = assertThrows(ApiException.class,
                () -> underTest.executeSyncApiCommandAndGetCommandId(
                        COMMAND_NAME, clustersResourceApi, stack, null, apiCommandCallable)
        );
        // THEN
        assertTrue(apiException.getMessage().contains("my exc"));
        verify(future, times(1)).cancel(true);
    }

    private ApiCommand createCommand(long id) {
        ApiCommand command = new ApiCommand();
        command.setId(new BigDecimal(id));
        return command;
    }

}