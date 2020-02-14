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
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
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
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiCommandList;
import com.cloudera.api.swagger.model.ApiServiceRef;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollerObject;
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

    private CloudbreakEventService cloudbreakEventService = Mockito.mock(CloudbreakEventService.class);

    private final AbstractClouderaManagerCommandCheckerTask underTest
            = new ClouderaManagerDecommissionHostListenerTask(clouderaManagerApiPojoFactory, cloudbreakEventService);

    @Before
    public void setup() {
        when(clouderaManagerApiPojoFactory.getCommandsResourceApi(apiClient)).thenReturn(commandsResourceApi);
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
        ClouderaManagerPollerObject pollerObject = new ClouderaManagerPollerObject(stack, apiClient, id);
        SocketException socketException = new SocketException("Network is unreachable (connect failed)");
        ApiException apiException = new ApiException(socketException);
        when(commandsResourceApi.readCommand(id))
                .thenAnswer(new ExceptionThrowingApiCommandAnswer(apiException, apiException, apiException, apiException, apiException));

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
        ClouderaManagerPollerObject pollerObject = new ClouderaManagerPollerObject(stack, apiClient, id);
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
        ClouderaManagerPollerObject pollerObject = new ClouderaManagerPollerObject(stack, apiClient, id);
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

    @Test
    public void testParseResultMessageFromChildrenFirstLevel() {
        ApiCommandList commandList = getApiCommandList("name0", "message0", "serviceName0");
        List<String> actual = underTest.parseResultMessageFromChildren(commandList);

        Assert.assertEquals(actual.size(), 1);
        Assert.assertEquals(actual.get(0), "name0(serviceName0): message0");
    }

    @Test
    public void testParseResultMessageFromChildrenRecursively() {
        ApiCommandList level1 = getApiCommandList("name0", "message0", "serviceName0");
        ApiCommandList level2 = getApiCommandList("name1", "message1", "serviceName1");
        level1.getItems().get(0).setChildren(level2);
        List<String> actual = underTest.parseResultMessageFromChildren(level1);

        Assert.assertEquals(actual.size(), 2);
        Assert.assertEquals(actual.get(0), "name0(serviceName0): message0");
        Assert.assertEquals(actual.get(1), "name1(serviceName1): message1");
    }

    @Test
    public void testParseResultMessageFromChildrenFiveLevel() {
        ApiCommandList level1 = getApiCommandList("name0", "message0", "serviceName0");
        ApiCommandList level2 = getApiCommandList("name1", "message1", "serviceName1");
        ApiCommandList level3 = getApiCommandList("name2", "message2", "serviceName2");
        ApiCommandList level4 = getApiCommandList("name3", "message3", "serviceName3");
        ApiCommandList level5 = getApiCommandList("name4", "message4", "serviceName4");
        level1.getItems().get(0).setChildren(level2);
        level2.getItems().get(0).setChildren(level3);
        level3.getItems().get(0).setChildren(level4);
        level4.getItems().get(0).setChildren(level5);
        List<String> actual = underTest.parseResultMessageFromChildren(level1);

        Assert.assertEquals(actual.size(), 5);
        Assert.assertEquals(actual.get(0), "name0(serviceName0): message0");
        Assert.assertEquals(actual.get(1), "name1(serviceName1): message1");
        Assert.assertEquals(actual.get(2), "name2(serviceName2): message2");
        Assert.assertEquals(actual.get(3), "name3(serviceName3): message3");
        Assert.assertEquals(actual.get(4), "name4(serviceName4): message4");
    }

    @Test
    public void testParseResultMessageFromChildrenTwoChildren() {
        ApiCommandList level1 = getApiCommandList("name0", "message0", "serviceName0");
        ApiCommand level1Ch1 = getApiCommand("name1", "message1", "serviceName1");
        ApiCommand level1Ch2 = getApiCommand("name2", "message2", "serviceName2");
        level1.addItemsItem(level1Ch1);
        level1.addItemsItem(level1Ch2);
        List<String> actual = underTest.parseResultMessageFromChildren(level1);

        Assert.assertEquals(actual.size(), 3);
        Assert.assertEquals(actual.get(0), "name0(serviceName0): message0");
        Assert.assertEquals(actual.get(1), "name1(serviceName1): message1");
        Assert.assertEquals(actual.get(2), "name2(serviceName2): message2");
    }

    @Test
    public void testParseResultMessageFromChildrenWhenItemsNull() {
        ApiCommandList commandList = new ApiCommandList();
        List<String> actual = underTest.parseResultMessageFromChildren(commandList);

        Assert.assertEquals(actual.size(), 0);
    }

    @Test
    public void testParseResultMessageFromChildrenWhenItemsEmpty() {
        ApiCommandList commandList = new ApiCommandList();
        commandList.setItems(Collections.emptyList());
        List<String> actual = underTest.parseResultMessageFromChildren(commandList);

        Assert.assertEquals(actual.size(), 0);
    }

    private ApiCommandList getApiCommandList(String name, String message, String serviceName) {
        ApiCommandList commandList = new ApiCommandList();
        ApiCommand apiCommand = getApiCommand(name, message, serviceName);
        commandList.addItemsItem(apiCommand);
        return commandList;
    }

    private ApiCommand getApiCommand(String name, String message, String serviceName) {
        return new ApiCommand()
                .name(name)
                .resultMessage(message)
                .serviceRef(new ApiServiceRef().serviceName(serviceName));
    }
}