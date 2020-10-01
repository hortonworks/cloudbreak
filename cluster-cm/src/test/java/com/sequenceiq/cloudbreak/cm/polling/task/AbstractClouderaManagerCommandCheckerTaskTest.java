package com.sequenceiq.cloudbreak.cm.polling.task;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;

import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

@RunWith(MockitoJUnitRunner.class)
public class AbstractClouderaManagerCommandCheckerTaskTest {

    private static final int ID = 1;

    private static final int FIVE = 5;

    private static final int SIX = 6;

    private static final int TEN = 10;

    @Rule
    public final ExpectedException expectedEx = ExpectedException.none();

    private final ApiClient apiClient = Mockito.mock(ApiClient.class);

    private final ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory = Mockito.mock(ClouderaManagerApiPojoFactory.class);

    private final CommandsResourceApi commandsResourceApi = Mockito.mock(CommandsResourceApi.class);

    private final CloudbreakEventService cloudbreakEventService = Mockito.mock(CloudbreakEventService.class);

    private final AbstractClouderaManagerCommandCheckerTask<ClouderaManagerCommandPollerObject> underTest
            = new ClouderaManagerDecommissionHostListenerTask(clouderaManagerApiPojoFactory, cloudbreakEventService);

    @Before
    public void setup() {
        when(clouderaManagerApiPojoFactory.getCommandsResourceApi(apiClient)).thenReturn(commandsResourceApi);
    }

    @Test
    public void testPollingWithFiveInternalServerErrors() throws ApiException {
        Stack stack = new Stack();
        BigDecimal id = new BigDecimal(ID);
        ClouderaManagerCommandPollerObject pollerObject = new ClouderaManagerCommandPollerObject(stack, apiClient, id);
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
        ClouderaManagerCommandPollerObject pollerObject = new ClouderaManagerCommandPollerObject(stack, apiClient, id);
        when(commandsResourceApi.readCommand(id)).thenAnswer(new Http500Answer(SIX));

        expectedEx.expect(ClouderaManagerOperationFailedException.class);
        expectedEx.expectMessage("Operation is considered failed.");

        for (int i = 0; i < SIX; i++) {
            boolean inProgress = underTest.checkStatus(pollerObject);
            assertFalse(inProgress);
        }
        underTest.checkStatus(pollerObject);
    }

    @Test
    public void testPollingWithFiveSocketExceptions() throws ApiException {
        Stack stack = new Stack();
        BigDecimal id = new BigDecimal(ID);
        ClouderaManagerCommandPollerObject pollerObject = new ClouderaManagerCommandPollerObject(stack, apiClient, id);
        SocketTimeoutException socketTimeoutException = new SocketTimeoutException("timeout");
        ApiException apiException0 = new ApiException(socketTimeoutException);
        SocketException socketException = new SocketException("Network is unreachable (connect failed)");
        ApiException apiException1 = new ApiException(socketException);
        when(commandsResourceApi.readCommand(id))
                .thenAnswer(new ExceptionThrowingApiCommandAnswer(apiException0, apiException1, apiException1, apiException1, apiException1));

        for (int i = 0; i < FIVE; i++) {
            boolean inProgress = underTest.checkStatus(pollerObject);
            assertFalse(inProgress);
        }
        boolean result = underTest.checkStatus(pollerObject);

        assertTrue(result);
    }

    @Test
    public void testPollingWithThreeInternalServerErrorAndThreeSocketExceptions() throws ApiException {
        Stack stack = new Stack();
        BigDecimal id = new BigDecimal(1);
        ClouderaManagerCommandPollerObject pollerObject = new ClouderaManagerCommandPollerObject(stack, apiClient, id);
        SocketException socketException = new SocketException("Network is unreachable (connect failed)");
        ApiException socketApiException = new ApiException(socketException);
        ApiException internalServerError = new ApiException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "error");
        when(commandsResourceApi.readCommand(id)).thenAnswer(new ExceptionThrowingApiCommandAnswer(internalServerError, internalServerError,
                internalServerError, socketApiException, socketApiException, socketApiException));

        expectedEx.expect(ClouderaManagerOperationFailedException.class);
        expectedEx.expectMessage("Operation is considered failed.");

        for (int i = 0; i < SIX; i++) {
            boolean inProgress = underTest.checkStatus(pollerObject);
            assertFalse(inProgress);
        }
        underTest.checkStatus(pollerObject);
    }

    @Test
    public void testPollingWithConnectException() throws ApiException {
        Stack stack = new Stack();
        stack.setId(1L);
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(Status.UPDATE_IN_PROGRESS);
        stack.setStackStatus(stackStatus);
        BigDecimal id = new BigDecimal(1);
        ClouderaManagerCommandPollerObject pollerObject = new ClouderaManagerCommandPollerObject(stack, apiClient, id);
        ConnectException connectException = new ConnectException("Connect failed.");
        ApiException apiException = new ApiException(connectException);
        ApiException[] exceptions = new ApiException[TEN];
        for (int i = 0; i < TEN; i++) {
            exceptions[i] = apiException;
        }
        when(commandsResourceApi.readCommand(id)).thenAnswer(new ExceptionThrowingApiCommandAnswer(exceptions));

        for (int i = 0; i < TEN; i++) {
            boolean inProgress = underTest.checkStatus(pollerObject);
            assertFalse(inProgress);
        }
        underTest.checkStatus(pollerObject);

        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(anyLong(), anyString(), any(), anyList());
    }
}
