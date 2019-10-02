package com.sequenceiq.cloudbreak.cm.polling.task;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollerObject;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@RunWith(MockitoJUnitRunner.class)
public class AbstractClouderaManagerCommandCheckerTaskTest {

    private static final int ID = 1;

    private static final int FIVE = 5;

    private static final int SIX = 6;

    @Rule
    public final ExpectedException expectedEx = ExpectedException.none();

    private final ApiClient apiClient = Mockito.mock(ApiClient.class);

    private final ClouderaManagerClientFactory clouderaManagerClientFactory = Mockito.mock(ClouderaManagerClientFactory.class);

    private final CommandsResourceApi commandsResourceApi = Mockito.mock(CommandsResourceApi.class);

    private final AbstractClouderaManagerCommandCheckerTask underTest = new ClouderaManagerDecommissionHostListenerTask(clouderaManagerClientFactory);

    @Before
    public void setup() {
        when(clouderaManagerClientFactory.getCommandsResourceApi(apiClient)).thenReturn(commandsResourceApi);
    }

    @Test
    public void testPollingWithFiveInternalServerErrors() throws ApiException {
        Stack stack = new Stack();
        BigDecimal id = new BigDecimal(ID);
        ClouderaManagerPollerObject pollerObject = new ClouderaManagerPollerObject(stack, apiClient, id);
        when(commandsResourceApi.readCommand(id)).thenAnswer(new Http500Answer(FIVE));

        for (int i = 0; i < FIVE; i++) {
            boolean inProgress = underTest.checkStatus(pollerObject);
            assertFalse(inProgress);
        }
        boolean result = underTest.checkStatus(pollerObject);

        assertTrue(result);
    }

    @Test
    public void testPollingWithSixInternalServerErrors() throws ApiException {
        Stack stack = new Stack();
        BigDecimal id = new BigDecimal(1);
        ClouderaManagerPollerObject pollerObject = new ClouderaManagerPollerObject(stack, apiClient, id);
        when(commandsResourceApi.readCommand(id)).thenAnswer(new Http500Answer(SIX));

        expectedEx.expect(ClouderaManagerOperationFailedException.class);
        expectedEx.expectMessage("internal server error");

        for (int i = 0; i < SIX; i++) {
            boolean inProgress = underTest.checkStatus(pollerObject);
            assertFalse(inProgress);
        }
        underTest.checkStatus(pollerObject);
    }

}