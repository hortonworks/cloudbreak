package com.sequenceiq.cloudbreak.cm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.client.ApiResponse;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiCommandList;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Protocol;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

@ExtendWith(MockitoExtension.class)
public class ClouderaManagerDeployClientConfigProviderTest {

    private static final int INTERRUPT_TIMEOUT_SECONDS = 120;

    private static final int SUCCESS_STATUS_CODE = 200;

    private static final int NOT_FOUND_STATUS_CODE = 404;

    private static final int RANDOM_PORT = 9999;

    private static final String ACTIVE_COMMAND_TABLE = "/cmf/commands/activeCommandTable";

    private static final String RECENT_COMMAND_TABLE = "/cmf/commands/commandTable";

    @Mock
    private ClustersResourceApi clustersResourceApi;

    @Mock
    private ExecutorService executorService;

    @Mock
    private Future<BigDecimal> future;

    @Mock
    private ApiClient apiClient;

    @Mock
    private OkHttpClient okHttpClient;

    @Mock
    private Call requestCall;

    private ClouderaManagerDeployClientConfigProvider underTest;

    private Stack stack;

    private final Map<String, List<String>> emptyHeaders = new HashMap<>();

    @BeforeEach
    public void setUp() {
        underTest = Mockito.spy(new ClouderaManagerDeployClientConfigProvider(INTERRUPT_TIMEOUT_SECONDS));
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
        assertEquals(7L, result.longValue());
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
            throws ApiException, CloudbreakException, InterruptedException,
            ExecutionException, TimeoutException {
        // GIVEN
        ApiResponse<ApiCommandList> response = createApiCommandListResponse();
        doReturn(executorService).when(underTest).createExecutor();
        given(executorService.submit(any(Callable.class))).willReturn(future);
        given(future.get(INTERRUPT_TIMEOUT_SECONDS, TimeUnit.SECONDS)).willThrow(new TimeoutException());
        given(clustersResourceApi.listActiveCommandsWithHttpInfo(anyString(), isNull()))
                .willReturn(response);
        doReturn(null).when(underTest).getCommandIdFromActiveCommands(response);
        doReturn(null).when(underTest).getCommandIdFromCommandsTable(clustersResourceApi, ACTIVE_COMMAND_TABLE, emptyHeaders);
        doReturn(null).when(underTest).getCommandIdFromCommandsTable(clustersResourceApi, RECENT_COMMAND_TABLE, emptyHeaders);
        given(underTest.getCommandIdFromActiveCommands(response)).willReturn(null);
        // WHEN
        CloudbreakException exception = assertThrows(CloudbreakException.class,
                () -> underTest.deployClientConfigAndGetCommandId(clustersResourceApi, stack));
        // THEN
        assertTrue(exception.getMessage().contains("Obtaining Cloudera Manager Deploy config command ID was not possible"));
        verify(underTest, times(1)).getCommandIdFromActiveCommands(response);
        verify(underTest, times(1)).getCommandIdFromCommandsTable(clustersResourceApi, ACTIVE_COMMAND_TABLE, emptyHeaders);
        verify(underTest, times(1)).getCommandIdFromCommandsTable(clustersResourceApi, RECENT_COMMAND_TABLE, emptyHeaders);
        verify(executorService, times(1)).shutdown();
    }

    @Test
    public void testGetDeployClientConfigCommandIdByListCommands()
            throws ApiException, CloudbreakException, InterruptedException, ExecutionException, TimeoutException {
        // GIVEN
        doReturn(executorService).when(underTest).createExecutor();
        given(executorService.submit(any(Callable.class))).willReturn(future);
        given(future.get(INTERRUPT_TIMEOUT_SECONDS, TimeUnit.SECONDS)).willThrow(new TimeoutException());
        given(clustersResourceApi.listActiveCommandsWithHttpInfo(anyString(), isNull()))
                .willReturn(createApiCommandListResponse());
        // WHEN
        BigDecimal result = underTest.deployClientConfigAndGetCommandId(clustersResourceApi, stack);
        // THEN
        assertEquals(5L, result.longValue());
        verify(executorService, times(1)).shutdown();
    }

    @Test
    public void testGetDeployClientConfigCommandIdByListCommandsApiException()
            throws ApiException, InterruptedException, ExecutionException, TimeoutException {
        // GIVEN
        doReturn(executorService).when(underTest).createExecutor();
        given(executorService.submit(any(Callable.class))).willReturn(future);
        given(future.get(INTERRUPT_TIMEOUT_SECONDS, TimeUnit.SECONDS)).willThrow(new TimeoutException());
        given(clustersResourceApi.listActiveCommandsWithHttpInfo(anyString(), isNull()))
                .willThrow(new ApiException("my exc"));
        // WHEN
        ApiException apiException = assertThrows(ApiException.class,
                () -> underTest.deployClientConfigAndGetCommandId(clustersResourceApi, stack)
        );
        // THEN
        assertTrue(apiException.getMessage().contains("my exc"));
        verify(executorService, times(1)).shutdown();
    }

    @Test
    public void testGetDeployClientConfigCommandIdByCommandTable()
            throws ApiException, CloudbreakException, InterruptedException,
            ExecutionException, TimeoutException, IOException {
        // GIVEN
        Map<String, List<String>> headers = new HashMap<>();
        List<String> headerList = new ArrayList<>();
        headerList.add("mycookie");
        headers.put("Set-Cookie", headerList);
        Request request = createDefaultRequest();
        doReturn(executorService).when(underTest).createExecutor();
        given(executorService.submit(any(Callable.class))).willReturn(future);
        given(future.get(INTERRUPT_TIMEOUT_SECONDS, TimeUnit.SECONDS)).willThrow(new TimeoutException());
        given(clustersResourceApi.listActiveCommandsWithHttpInfo(anyString(), isNull()))
                .willReturn(new ApiResponse<>(SUCCESS_STATUS_CODE, headers,
                        new ApiCommandList().items(new ArrayList<>())));
        given(clustersResourceApi.getApiClient()).willReturn(apiClient);
        given(apiClient.getHttpClient()).willReturn(okHttpClient);
        given(apiClient.buildRequest(any(), any(), any(), isNull(),
                any(), any(), any(), isNull())).willReturn(request);
        given(okHttpClient.newCall(any())).willReturn(requestCall);
        given(requestCall.execute()).willReturn(
                createResponse(SUCCESS_STATUS_CODE, createResponseDefaultString(), request));
        // WHEN
        BigDecimal result = underTest.deployClientConfigAndGetCommandId(clustersResourceApi, stack);
        // THEN
        assertEquals(3L, result.longValue());
        verify(executorService, times(1)).shutdown();
    }

    @Test
    public void testGetDeployClientConfigCommandIdByCommandTableWithListCommands()
            throws ApiException, CloudbreakException, InterruptedException,
            ExecutionException, TimeoutException, IOException {
        // GIVEN
        Request request = createDefaultRequest();
        doReturn(executorService).when(underTest).createExecutor();
        given(executorService.submit(any(Callable.class))).willReturn(future);
        given(future.get(INTERRUPT_TIMEOUT_SECONDS, TimeUnit.SECONDS)).willThrow(new TimeoutException());
        given(clustersResourceApi.listActiveCommandsWithHttpInfo(anyString(), isNull()))
                .willReturn(createApiCommandListResponse(NOT_FOUND_STATUS_CODE));
        given(clustersResourceApi.getApiClient()).willReturn(apiClient);
        given(apiClient.getHttpClient()).willReturn(okHttpClient);
        given(apiClient.buildRequest(any(), any(), any(), isNull(),
                any(), any(), any(), isNull())).willReturn(request);
        given(okHttpClient.newCall(any())).willReturn(requestCall);
        given(requestCall.execute()).willReturn(
                createResponse(SUCCESS_STATUS_CODE, createResponseDefaultString(), request));
        // WHEN
        BigDecimal result = underTest.deployClientConfigAndGetCommandId(clustersResourceApi, stack);
        // THEN
        assertEquals(3L, result.longValue());
        verify(executorService, times(1)).shutdown();
    }

    @Test
    public void testGetDeployClientConfigCommandIdByCommandTableNotFound()
            throws ApiException, InterruptedException, ExecutionException,
            TimeoutException, IOException {
        // GIVEN
        Request request = createDefaultRequest();
        doReturn(executorService).when(underTest).createExecutor();
        given(executorService.submit(any(Callable.class))).willReturn(future);
        given(future.get(INTERRUPT_TIMEOUT_SECONDS, TimeUnit.SECONDS)).willThrow(new TimeoutException());
        given(clustersResourceApi.listActiveCommandsWithHttpInfo(anyString(), isNull()))
                .willReturn(createApiCommandListResponse(NOT_FOUND_STATUS_CODE));
        given(clustersResourceApi.getApiClient()).willReturn(apiClient);
        given(apiClient.getHttpClient()).willReturn(okHttpClient);
        given(apiClient.buildRequest(any(), any(), any(), isNull(),
                any(), any(), any(), isNull())).willReturn(request);
        given(okHttpClient.newCall(any())).willReturn(requestCall);
        given(requestCall.execute()).willReturn(
                createResponse(NOT_FOUND_STATUS_CODE, createResponseDefaultString(), request));
        // WHEN
        CloudbreakException exception = assertThrows(CloudbreakException.class,
                () -> underTest.deployClientConfigAndGetCommandId(clustersResourceApi, stack));
        // THEN
        assertTrue(exception.getMessage().contains("returned with status code: 404"));
        verify(executorService, times(1)).shutdown();
    }

    @Test
    public void testGetDeployClientConfigCommandIdByCommandTableInvalidResponse()
            throws ApiException, InterruptedException, ExecutionException,
            TimeoutException, IOException {
        // GIVEN
        Request request = createDefaultRequest();
        doReturn(executorService).when(underTest).createExecutor();
        given(executorService.submit(any(Callable.class))).willReturn(future);
        given(future.get(INTERRUPT_TIMEOUT_SECONDS, TimeUnit.SECONDS)).willThrow(new TimeoutException());
        given(clustersResourceApi.listActiveCommandsWithHttpInfo(anyString(), isNull()))
                .willReturn(createApiCommandListResponse(NOT_FOUND_STATUS_CODE));
        given(clustersResourceApi.getApiClient()).willReturn(apiClient);
        given(apiClient.getHttpClient()).willReturn(okHttpClient);
        given(apiClient.buildRequest(any(), any(), any(), isNull(),
                any(), any(), any(), isNull())).willReturn(request);
        given(okHttpClient.newCall(any())).willReturn(requestCall);
        given(requestCall.execute()).willReturn(
                createResponse(SUCCESS_STATUS_CODE, "invalid", request));
        // WHEN
        CloudbreakException exception = assertThrows(CloudbreakException.class,
                () -> underTest.deployClientConfigAndGetCommandId(clustersResourceApi, stack));
        // THEN
        assertTrue(exception.getMessage().contains("error during processing"));
        verify(executorService, times(1)).shutdown();
    }

    @Test
    public void testGetDeployClientConfigCommandIdByRecentCommandTable()
            throws ApiException, CloudbreakException, InterruptedException,
            ExecutionException, TimeoutException, IOException {
        // GIVEN
        Map<String, List<String>> headers = new HashMap<>();
        List<String> headerList = new ArrayList<>();
        headerList.add("mycookie");
        headers.put("Set-Cookie", headerList);
        Request request = createDefaultRequest();
        doReturn(executorService).when(underTest).createExecutor();
        given(executorService.submit(any(Callable.class))).willReturn(future);
        given(future.get(INTERRUPT_TIMEOUT_SECONDS, TimeUnit.SECONDS)).willThrow(new TimeoutException());
        given(clustersResourceApi.listActiveCommandsWithHttpInfo(anyString(), isNull()))
                .willReturn(new ApiResponse<>(SUCCESS_STATUS_CODE, headers,
                        new ApiCommandList().items(new ArrayList<>())));
        given(clustersResourceApi.getApiClient()).willReturn(apiClient);
        given(apiClient.getHttpClient()).willReturn(okHttpClient);
        given(apiClient.buildRequest(any(), any(), any(), isNull(),
                any(), any(), any(), isNull())).willReturn(request);
        given(okHttpClient.newCall(any())).willReturn(requestCall);
        given(requestCall.execute()).willReturn(
                createResponse(SUCCESS_STATUS_CODE, "[]", request))
                .willReturn(
                createResponse(SUCCESS_STATUS_CODE, createResponseDefaultString(), request));
        // WHEN
        BigDecimal result = underTest.deployClientConfigAndGetCommandId(clustersResourceApi, stack);
        // THEN
        assertEquals(3L, result.longValue());
        verify(executorService, times(1)).shutdown();
        verify(requestCall, times(2)).execute();
    }

    @Test
    public void testCreateNewPathSegments() {
        // GIVEN
        HttpUrl defaultHttpUrl = new HttpUrl.Builder()
                .host("localhost")
                .scheme("https")
                .port(RANDOM_PORT)
                .addPathSegment("cluster-proxy")
                .addPathSegment("proxy")
                .addPathSegment("crn:mycrn")
                .addPathSegment("cb-internal")
                .addPathSegment("api")
                .addPathSegment("v31")
                .addPathSegment("cmf")
                .addPathSegment("commands")
                .addPathSegment("commandTable")
                .build();
        HttpUrl.Builder httpUrlBuilder = new HttpUrl.Builder()
                .host(defaultHttpUrl.host())
                .scheme(defaultHttpUrl.scheme())
                .port(defaultHttpUrl.port());
        // WHEN
        underTest.addPathSegmentsFromDefaultUrl(defaultHttpUrl, httpUrlBuilder);
        List<String> result = httpUrlBuilder.build().pathSegments();
        // THEN
        assertFalse(result.contains("api"));
        assertFalse(result.contains("v31"));
        assertTrue(result.contains("cluster-proxy"));
        assertTrue(result.contains("commandTable"));
    }

    private ApiCommand createCommand() {
        ApiCommand command = new ApiCommand();
        command.setId(new BigDecimal(7L));
        return command;
    }

    private ApiResponse<ApiCommandList> createApiCommandListResponse() {
        return createApiCommandListResponse(SUCCESS_STATUS_CODE);
    }

    private ApiResponse<ApiCommandList> createApiCommandListResponse(int statusCode) {
        ApiCommandList commandList = new ApiCommandList();
        ApiCommand command1 = new ApiCommand();
        command1.setId(new BigDecimal(4L));
        command1.setName("DeployClusterClientConfig");
        command1.setStartTime(new DateTime(2000, 1, 1, 1, 1, 1).toString());
        ApiCommand command2 = new ApiCommand();
        command2.setId(new BigDecimal(5L));
        command2.setName("DeployClusterClientConfig");
        command2.setStartTime(new DateTime(2000, 1, 1, 1, 1, 2).toString());
        ApiCommand command3 = new ApiCommand();
        command3.setId(new BigDecimal(6L));
        command3.setName("RestartServices");
        command3.setStartTime(new DateTime(2000, 1, 1, 1, 1, 3).toString());
        commandList.addItemsItem(command1);
        commandList.addItemsItem(command2);
        commandList.addItemsItem(command3);
        return new ApiResponse<>(statusCode, emptyHeaders, commandList);
    }

    private Request createDefaultRequest() {
        return new Request.Builder()
                .url("https://localhost:9443/api/v1/cmf/commands/commandTable")
                .get()
                .build();
    }

    private Response createResponse(int statusCode, String body, Request request) {
        return new Response.Builder()
                .code(statusCode)
                .request(request)
                .body(createResponseBody(body))
                .protocol(Protocol.HTTP_2)
                .build();
    }

    private ResponseBody createResponseBody(String bodyString) {
        return ResponseBody
                .create(MediaType.parse("application/json"), bodyString);
    }

    private String createResponseDefaultString() {
        return "[{\"id\": 1,\"name\": \"DeployClusterClientConfig\",\"start\": 1611513499011}, "
                + "{\"id\": 2,\"name\": \"RestartServices\",\"start\": 1611513499013}, "
                + "{\"id\": 3,\"name\": \"DeployClusterClientConfig\",\"start\": 1611513499021}]";
    }

}
