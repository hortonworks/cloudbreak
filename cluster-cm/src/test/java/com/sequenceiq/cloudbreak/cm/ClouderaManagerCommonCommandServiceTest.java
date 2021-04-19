package com.sequenceiq.cloudbreak.cm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiCommandList;
import com.sequenceiq.cloudbreak.cm.commands.SyncApiCommandPollerConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.util.CheckedFunction;

@ExtendWith(MockitoExtension.class)
public class ClouderaManagerCommonCommandServiceTest {

    private static final String COMMAND_NAME = "commandName";

    private static final String CLUSTER_NAME = "clusterName";

    private static final String STACK_CRN = "crn:cdp:datalake:us-west-1:acc1:datalake:cluster";

    @Mock
    private SyncApiCommandPollerConfig syncApiCommandPollerConfig;

    @Mock
    private ClouderaManagerSyncApiCommandIdProvider clouderaManagerSyncApiCommandIdProvider;

    @Mock
    private CheckedFunction<String, ApiCommand, ApiException> checkedFunction;

    @Mock
    private ClustersResourceApi clustersResourceApi;

    private ClouderaManagerCommonCommandService underTest;

    private Stack stack;

    private List<ApiCommand> commands;

    @BeforeEach
    public void setUp() {
        underTest = new ClouderaManagerCommonCommandService(syncApiCommandPollerConfig, clouderaManagerSyncApiCommandIdProvider);
        stack = new Stack();
        stack.setName(CLUSTER_NAME);
        stack.setResourceCrn(STACK_CRN);
        commands = new ArrayList<>();
    }

    @Test
    public void testGetDeployClientConfigCommandId() throws CloudbreakException, ApiException {
        // GIVEN
        given(syncApiCommandPollerConfig.isSyncApiCommandPollingEnaabled(STACK_CRN)).willReturn(false);
        given(clustersResourceApi.deployClientConfig(CLUSTER_NAME)).willReturn(new ApiCommand().name(COMMAND_NAME).id(1));
        // WHEN
        Integer result = underTest.getDeployClientConfigCommandId(stack, clustersResourceApi, commands);
        // THEN
        assertEquals(1, result);
    }

    @Test
    public void testGetDeployClientConfigCommandIdWithSyncApiPolling() throws CloudbreakException, ApiException {
        // GIVEN
        given(syncApiCommandPollerConfig.isSyncApiCommandPollingEnaabled(STACK_CRN)).willReturn(true);
        given(syncApiCommandPollerConfig.getDeployClusterClientConfigCommandName()).willReturn(COMMAND_NAME);
        given(clouderaManagerSyncApiCommandIdProvider.executeSyncApiCommandAndGetCommandId(anyString(), any(), any(), any(), any())).willReturn(1);
        // WHEN
        Integer result = underTest.getDeployClientConfigCommandId(stack, clustersResourceApi, commands);
        // THEN
        assertEquals(1, result);
        verify(clouderaManagerSyncApiCommandIdProvider, times(1)).executeSyncApiCommandAndGetCommandId(anyString(), any(), any(), any(), any());
    }

    @Test
    public void testGetApiCommand() throws ApiException {
        // GIVEN
        given(checkedFunction.apply(CLUSTER_NAME)).willReturn(new ApiCommand().name(COMMAND_NAME).id(1));
        // WHEN
        underTest.getApiCommand(new ArrayList<>(), COMMAND_NAME, CLUSTER_NAME, checkedFunction);
        // THEN
        verify(checkedFunction, times(1)).apply(CLUSTER_NAME);
    }

    @Test
    public void testGetApiCommandAlreadyRunning() throws ApiException {
        // GIVEN
        List<ApiCommand> commands = new ApiCommandList().addItemsItem(new ApiCommand().id(1).name(COMMAND_NAME)).getItems();
        // WHEN
        ApiCommand result = underTest.getApiCommand(commands, COMMAND_NAME, CLUSTER_NAME, checkedFunction);
        // THEN
        assertEquals(COMMAND_NAME, result.getName());
    }
}
