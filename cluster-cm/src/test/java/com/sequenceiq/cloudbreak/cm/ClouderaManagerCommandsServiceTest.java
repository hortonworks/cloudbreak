package com.sequenceiq.cloudbreak.cm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.apache.http.HttpStatus;
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

        assertEquals(actualApiCommand, apiCommand);
    }

    @Test
    public void testRetryApiCommand() throws ApiException {
        when(clouderaManagerApiFactory.getCommandsResourceApi(apiClient)).thenReturn(commandsResourceApi);
        when(commandsResourceApi.retry(BigDecimal.ONE)).thenReturn(apiCommand);

        ApiCommand actualApiCommand = underTest.retryApiCommand(apiClient, COMMAND_ID);

        assertEquals(actualApiCommand, apiCommand);
    }

    @Test
    public void getApiCommandIfExist() throws ApiException {
        when(clouderaManagerApiFactory.getCommandsResourceApi(apiClient)).thenReturn(commandsResourceApi);
        when(commandsResourceApi.readCommand(BigDecimal.ONE)).thenReturn(apiCommand);

        Optional<ApiCommand> actualApiCommand = underTest.getApiCommandIfExist(apiClient, COMMAND_ID);

        assertEquals(actualApiCommand.get(), apiCommand);
    }

    @Test
    public void getApiCommandIfExistWhenNull() throws ApiException {
        when(clouderaManagerApiFactory.getCommandsResourceApi(apiClient)).thenReturn(commandsResourceApi);

        Optional<ApiCommand> actualApiCommand = underTest.getApiCommandIfExist(apiClient, COMMAND_ID);

        assertTrue(actualApiCommand.isEmpty());
    }

    @Test
    public void getApiCommandIfExistWhenApiException() throws ApiException {
        when(clouderaManagerApiFactory.getCommandsResourceApi(apiClient)).thenReturn(commandsResourceApi);
        when(commandsResourceApi.readCommand(BigDecimal.ONE)).thenThrow(new ApiException("msg"));

        assertThrows(ApiException.class, () -> underTest.getApiCommandIfExist(apiClient, COMMAND_ID));
    }

    @Test
    public void getApiCommandIfExistWhenNotFoundException() throws ApiException {
        when(clouderaManagerApiFactory.getCommandsResourceApi(apiClient)).thenReturn(commandsResourceApi);
        ApiException apiException = new ApiException(null, null, HttpStatus.SC_NOT_FOUND, null, null);
        when(commandsResourceApi.readCommand(BigDecimal.ONE)).thenThrow(apiException);

        Optional<ApiCommand> actualApiCommand = underTest.getApiCommandIfExist(apiClient, COMMAND_ID);

        assertTrue(actualApiCommand.isEmpty());
    }
}
