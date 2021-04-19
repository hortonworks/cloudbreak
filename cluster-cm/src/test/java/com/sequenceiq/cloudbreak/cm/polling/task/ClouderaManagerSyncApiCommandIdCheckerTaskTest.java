package com.sequenceiq.cloudbreak.cm.polling.task;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.commands.SyncApiCommandRetriever;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerSyncCommandPollerObject;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@ExtendWith(MockitoExtension.class)
public class ClouderaManagerSyncApiCommandIdCheckerTaskTest {

    private static final String COMMAND_NAME = "DeployClusterClientConfig";

    private ClouderaManagerSyncApiCommandIdCheckerTask underTest;

    @Mock
    private SyncApiCommandRetriever commandRetriever;

    @Mock
    private ApiClient apiClient;

    @Mock
    private Stack stack;

    @BeforeEach
    public void setUp() {
        underTest = new ClouderaManagerSyncApiCommandIdCheckerTask(
                new ClouderaManagerApiPojoFactory(), commandRetriever);
    }

    @Test
    public void testCheckStatus() throws ApiException, CloudbreakException {
        // GIVEN
        ClouderaManagerSyncCommandPollerObject pollerObject =
                new ClouderaManagerSyncCommandPollerObject(stack, apiClient, null, COMMAND_NAME);
        given(commandRetriever.getCommandId(anyString(), any(), any())).willReturn(Optional.of(1));
        // WHEN
        boolean result = underTest.checkStatus(pollerObject);
        // THEN
        assertTrue(result);
    }

    @Test
    public void testCheckStatusWithRecentCommandId() throws ApiException, CloudbreakException {
        // GIVEN
        ClouderaManagerSyncCommandPollerObject pollerObject =
                new ClouderaManagerSyncCommandPollerObject(stack, apiClient, 1, COMMAND_NAME);
        given(commandRetriever.getCommandId(anyString(), any(), any())).willReturn(Optional.of(2));
        // WHEN
        boolean result = underTest.checkStatus(pollerObject);
        // THEN
        assertTrue(result);
    }

    @Test
    public void testCheckStatusWithSameRecentCommandId() throws ApiException, CloudbreakException {
        // GIVEN
        ClouderaManagerSyncCommandPollerObject pollerObject =
                new ClouderaManagerSyncCommandPollerObject(stack, apiClient, 1, COMMAND_NAME);
        given(commandRetriever.getCommandId(anyString(), any(), any())).willReturn(Optional.of(1));
        // WHEN
        boolean result = underTest.checkStatus(pollerObject);
        // THEN
        assertFalse(result);
    }

}