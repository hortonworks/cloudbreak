package com.sequenceiq.cloudbreak.cm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.sequenceiq.cloudbreak.cm.commands.DeployClientConfigCommandRetriever;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@ExtendWith(MockitoExtension.class)
public class ClouderaManagerDeployClientConfigProviderTest {

    private static final int INTERRUPT_TIMEOUT_SECONDS = 120;

    @Mock
    private ClustersResourceApi clustersResourceApi;

    @Mock
    private ExecutorService executorService;

    @Mock
    private Future<BigDecimal> future;

    @Mock
    private DeployClientConfigCommandRetriever deployClientConfigCommandRetriever;

    @Mock
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    private ClouderaManagerDeployClientConfigProvider underTest;

    private Stack stack;

    @BeforeEach
    public void setUp() {
        underTest = Mockito.spy(new ClouderaManagerDeployClientConfigProvider(
                deployClientConfigCommandRetriever, clouderaManagerPollingServiceProvider, INTERRUPT_TIMEOUT_SECONDS));
        stack = new Stack();
        stack.setName("mycluster");
    }

    @Test
    public void testGetDeployClientConfigCommandId() throws ApiException, CloudbreakException {
        // GIVEN
        given(clustersResourceApi.deployClientConfig(stack.getName())).willReturn(createCommand());
        // WHEN
        BigDecimal result = underTest.deployClientConfigAndGetCommandId(clustersResourceApi, stack);
        // THEN
        assertEquals(1L, result.longValue());
    }

    @Test
    public void testGetDeployClientConfigCommandIdApiException() throws ApiException, CloudbreakException {
        // GIVEN
        given(clustersResourceApi.deployClientConfig(stack.getName())).willThrow(new ApiException("my exc"));
        // WHEN
        ApiException executionException = assertThrows(ApiException.class,
                () -> underTest.deployClientConfigAndGetCommandId(clustersResourceApi, stack)
        );
        // THEN
        assertTrue(executionException.getMessage().contains("my exc"));
    }

    @Test
    public void testGetDeployClientConfigCommandIdNullAnswers()
            throws InterruptedException,
            ExecutionException, TimeoutException {
        // GIVEN
        doReturn(executorService).when(underTest).createExecutor();
        given(executorService.submit(any(Callable.class))).willReturn(future);
        given(future.get(INTERRUPT_TIMEOUT_SECONDS, TimeUnit.SECONDS)).willThrow(new TimeoutException());
        // WHEN
        CloudbreakException exception = assertThrows(CloudbreakException.class,
                () -> underTest.deployClientConfigAndGetCommandId(clustersResourceApi, stack));
        // THEN
        assertTrue(exception.getMessage().contains("Obtaining Cloudera Manager Deploy config command ID was not possible"));
        verify(executorService, times(1)).shutdown();
    }

    @Test
    public void testGetDeployClientConfigCommandIdByListCommandsApiException()
            throws ApiException, InterruptedException, ExecutionException,
            TimeoutException, CloudbreakException {
        // GIVEN
        doReturn(executorService).when(underTest).createExecutor();
        given(executorService.submit(any(Callable.class))).willReturn(future);
        given(future.get(INTERRUPT_TIMEOUT_SECONDS, TimeUnit.SECONDS)).willThrow(new TimeoutException());
        given(deployClientConfigCommandRetriever.getCommandId(clustersResourceApi, stack))
                .willReturn(null)
                .willThrow(new ApiException("my exc"));
        // WHEN
        ApiException apiException = assertThrows(ApiException.class,
                () -> underTest.deployClientConfigAndGetCommandId(clustersResourceApi, stack)
        );
        // THEN
        assertTrue(apiException.getMessage().contains("my exc"));
        verify(executorService, times(1)).shutdown();
    }

    private ApiCommand createCommand() {
        ApiCommand command = new ApiCommand();
        command.setId(new BigDecimal(1L));
        return command;
    }

}
