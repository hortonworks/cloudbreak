package com.sequenceiq.cloudbreak.cm.polling.task;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.commands.DeployClientConfigCommandRetriever;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@ExtendWith(MockitoExtension.class)
public class ClouderaManagerDeployClientConfigIdCheckerTaskTest {

    private ClouderaManagerDeployClientConfigIdCheckerTask underTest;

    @Mock
    private DeployClientConfigCommandRetriever commandRetriever;

    @Mock
    private ApiClient apiClient;

    @Mock
    private Stack stack;

    @BeforeEach
    public void setUp() {
        underTest = new ClouderaManagerDeployClientConfigIdCheckerTask(
                new ClouderaManagerApiPojoFactory(), commandRetriever);
    }

    @Test
    public void testCheckStatus() throws ApiException, CloudbreakException {
        // GIVEN
        ClouderaManagerCommandPollerObject pollerObject =
                new ClouderaManagerCommandPollerObject(stack, apiClient, null);
        given(commandRetriever.getCommandId(any(), any())).willReturn(new BigDecimal(1L));
        // WHEN
        boolean result = underTest.checkStatus(pollerObject);
        // THEN
        assertTrue(result);
    }

    @Test
    public void testCheckStatusWithRecentCommandId() throws ApiException, CloudbreakException {
        // GIVEN
        ClouderaManagerCommandPollerObject pollerObject =
                new ClouderaManagerCommandPollerObject(stack, apiClient, new BigDecimal(1L));
        given(commandRetriever.getCommandId(any(), any())).willReturn(new BigDecimal(2L));
        // WHEN
        boolean result = underTest.checkStatus(pollerObject);
        // THEN
        assertTrue(result);
    }

    @Test
    public void testCheckStatusWithSameRecentCommandId() throws ApiException, CloudbreakException {
        // GIVEN
        ClouderaManagerCommandPollerObject pollerObject =
                new ClouderaManagerCommandPollerObject(stack, apiClient, new BigDecimal(1L));
        given(commandRetriever.getCommandId(any(), any())).willReturn(new BigDecimal(1L));
        // WHEN
        boolean result = underTest.checkStatus(pollerObject);
        // THEN
        assertFalse(result);
    }

}
