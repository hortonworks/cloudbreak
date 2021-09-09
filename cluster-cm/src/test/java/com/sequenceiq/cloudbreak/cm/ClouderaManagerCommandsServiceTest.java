package com.sequenceiq.cloudbreak.cm;

import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerCommandsServiceTest {

    private static final BigDecimal COMMAND_ID = BigDecimal.ONE;

    @Mock
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Mock
    private ApiClient apiClient;

    @Mock
    private CommandsResourceApi commandsResourceApi;

    @Mock
    private ApiCommand apiCommand;

    @InjectMocks
    private ClouderaManagerCommandsService underTest;

    @Test
    public void testGetApiCommand() throws ApiException {
        when(clouderaManagerApiFactory.getCommandsResourceApi(apiClient)).thenReturn(commandsResourceApi);
        when(commandsResourceApi.readCommand(BigDecimal.ONE)).thenReturn(apiCommand);

        ApiCommand actualApiCommand = underTest.getApiCommand(apiClient, COMMAND_ID);

        Assertions.assertEquals(actualApiCommand, apiCommand);
    }

    @Test
    public void testRetryApiCommand() throws ApiException {
        when(clouderaManagerApiFactory.getCommandsResourceApi(apiClient)).thenReturn(commandsResourceApi);
        when(commandsResourceApi.retry(BigDecimal.ONE)).thenReturn(apiCommand);

        ApiCommand actualApiCommand = underTest.retryApiCommand(apiClient, COMMAND_ID);

        Assertions.assertEquals(actualApiCommand, apiCommand);
    }
}