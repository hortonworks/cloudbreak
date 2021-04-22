package com.sequenceiq.cloudbreak.cm.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.client.ApiResponse;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiCommandList;
import com.sequenceiq.cloudbreak.cm.model.CommandResource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@ExtendWith(MockitoExtension.class)
public class SyncApiCommandRetrieverTest {

    private static final String COMMAND_NAME = "DeployClusterClientConfig";

    private static final int NOT_FOUND_STATUS_CODE = 404;

    private static final int SUCCESS_STATUS_CODE = 200;

    private SyncApiCommandRetriever underTest;

    @Mock
    private ActiveCommandTableResource activeCommandTableResource;

    @Mock
    private RecentCommandTableResource recentCommandTableResource;

    @Mock
    private ClustersResourceApi clustersResourceApi;

    private Stack stack;

    private final Map<String, List<String>> emptyHeaders = new HashMap<>();

    @BeforeEach
    public void setUp() {
        underTest = new SyncApiCommandRetriever(
                activeCommandTableResource, recentCommandTableResource);
        stack = new Stack();
        stack.setName("mycluster");
    }

    @Test
    public void testGetCommandId() throws ApiException, CloudbreakException {
        // GIVEN
        given(clustersResourceApi.listActiveCommandsWithHttpInfo(anyString(), isNull()))
                .willReturn(createApiCommandListResponse(NOT_FOUND_STATUS_CODE));
        given(activeCommandTableResource.getCommands(COMMAND_NAME, clustersResourceApi, emptyHeaders)).willReturn(new ArrayList<>());
        given(recentCommandTableResource.getCommands(COMMAND_NAME, clustersResourceApi, emptyHeaders)).willReturn(createSampleCommands());
        // WHEN
        Optional<BigDecimal> result = underTest.getCommandId(COMMAND_NAME, clustersResourceApi, stack);
        // THEN
        assertEquals(2L, result.get().longValue());
        verify(clustersResourceApi, times(1)).listActiveCommandsWithHttpInfo(anyString(), isNull());
        verify(activeCommandTableResource, times(1)).getCommands(COMMAND_NAME, clustersResourceApi, emptyHeaders);
        verify(recentCommandTableResource, times(1)).getCommands(COMMAND_NAME, clustersResourceApi, emptyHeaders);
    }

    @Test
    public void testGetCommandIdByListCommands()
            throws ApiException, CloudbreakException {
        // GIVEN
        given(clustersResourceApi.listActiveCommandsWithHttpInfo(anyString(), isNull()))
                .willReturn(createApiCommandListResponse());
        // WHEN
        Optional<BigDecimal> result = underTest.getCommandId(COMMAND_NAME, clustersResourceApi, stack);
        // THEN
        assertEquals(5L, result.get().longValue());
    }

    @Test
    public void testGetCommandIdByListCommandsWithSkipRunningCommands()
            throws ApiException, CloudbreakException {
        // GIVEN
        given(clustersResourceApi.listActiveCommandsWithHttpInfo(anyString(), isNull()))
                .willReturn(createApiCommandListResponse());
        given(recentCommandTableResource.getCommands(COMMAND_NAME, clustersResourceApi, emptyHeaders)).willReturn(createSampleCommands());
        // WHEN
        Optional<BigDecimal> result = underTest.getLastFinishedCommandId(
                COMMAND_NAME, clustersResourceApi, stack);
        // THEN
        assertEquals(2L, result.get().longValue());
        verify(activeCommandTableResource, times(0)).getCommands(COMMAND_NAME, clustersResourceApi, emptyHeaders);
        verify(recentCommandTableResource, times(1)).getCommands(COMMAND_NAME, clustersResourceApi, emptyHeaders);
    }

    @Test
    public void testGetCommandIdByListCommandsNotFound()
            throws ApiException, CloudbreakException {
        // GIVEN
        given(clustersResourceApi.listActiveCommandsWithHttpInfo(anyString(), isNull()))
                .willReturn(createApiCommandListResponse(NOT_FOUND_STATUS_CODE));
        given(activeCommandTableResource.getCommands(COMMAND_NAME, clustersResourceApi, emptyHeaders)).willReturn(new ArrayList<>());
        given(recentCommandTableResource.getCommands(COMMAND_NAME, clustersResourceApi, emptyHeaders)).willReturn(new ArrayList<>());
        // WHEN
        Optional<BigDecimal> result = underTest.getCommandId(COMMAND_NAME, clustersResourceApi, stack);
        // THEN
        assertTrue(result.isEmpty());
        verify(activeCommandTableResource, times(1)).getCommands(COMMAND_NAME, clustersResourceApi, emptyHeaders);
        verify(recentCommandTableResource, times(1)).getCommands(COMMAND_NAME, clustersResourceApi, emptyHeaders);
    }

    private ApiResponse<ApiCommandList> createApiCommandListResponse() {
        return createApiCommandListResponse(SUCCESS_STATUS_CODE);
    }

    private ApiResponse<ApiCommandList> createApiCommandListResponse(int statusCode) {
        ApiCommandList commandList = new ApiCommandList();
        ApiCommand command1 = new ApiCommand();
        command1.setId(new BigDecimal(4L));
        command1.setName("DeployClusterClientConfig");
        command1.setSuccess(true);
        command1.setStartTime(new DateTime(2000, 1, 1, 1, 1, 1).toString());
        ApiCommand command2 = new ApiCommand();
        command2.setId(new BigDecimal(5L));
        command2.setName("DeployClusterClientConfig");
        command2.setSuccess(true);
        command2.setStartTime(new DateTime(2000, 1, 1, 1, 1, 2).toString());
        ApiCommand command3 = new ApiCommand();
        command3.setId(new BigDecimal(6L));
        command3.setName("RestartServices");
        command3.setSuccess(true);
        command3.setStartTime(new DateTime(2000, 1, 1, 1, 1, 3).toString());
        commandList.addItemsItem(command1);
        commandList.addItemsItem(command2);
        commandList.addItemsItem(command3);
        return new ApiResponse<>(statusCode, emptyHeaders, commandList);
    }

    private List<CommandResource> createSampleCommands() {
        List<CommandResource> commands = new ArrayList<>();
        CommandResource commandResource1 = new CommandResource();
        commandResource1.setSuccess(false);
        commandResource1.setId(1L);
        commandResource1.setName("DeployClusterClientConfig");
        CommandResource commandResource2 = new CommandResource();
        commandResource2.setId(2L);
        commandResource2.setSuccess(true);
        commandResource2.setName("DeployClusterClientConfig");
        commandResource2.setStart(new DateTime(2000, 1, 1, 1, 1, 5).getMillis());
        commands.add(commandResource1);
        commands.add(commandResource2);
        return commands;
    }
}