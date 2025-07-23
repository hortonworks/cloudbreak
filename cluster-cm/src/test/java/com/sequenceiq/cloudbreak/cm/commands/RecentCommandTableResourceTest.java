package com.sequenceiq.cloudbreak.cm.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.sequenceiq.cloudbreak.cm.model.CommandResource;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

@ExtendWith(MockitoExtension.class)
public class RecentCommandTableResourceTest {

    private static final String COMMAND_NAME = "DeployClusterClientConfig";

    private static final int RANDOM_PORT = 9999;

    private static final int SUCCESS_STATUS_CODE = 200;

    private static final int NOT_FOUND_STATUS_CODE = 404;

    private RecentCommandTableResource underTest;

    @Mock
    private ApiClient apiClient;

    @Mock
    private OkHttpClient okHttpClient;

    @Mock
    private Call requestCall;

    @Mock
    private ClustersResourceApi clustersResourceApi;

    private final Map<String, List<String>> emptyHeaders = new HashMap<>();

    @BeforeEach
    public void setUp() {
        underTest = new RecentCommandTableResource();
    }

    @Test
    public void testGetCommands() throws ApiException, CloudbreakException, IOException {
        // GIVEN
        Map<String, List<String>> headers = new HashMap<>();
        List<String> headerList = new ArrayList<>();
        headerList.add("mycookie");
        headers.put("Set-Cookie", headerList);
        Request request = createDefaultRequest();
        given(clustersResourceApi.getApiClient()).willReturn(apiClient);
        given(apiClient.getHttpClient()).willReturn(okHttpClient);
        given(apiClient.buildRequest(any(), any(), any(), isNull(), isNull(),
                any(), any(), any(), isNull())).willReturn(request);
        given(okHttpClient.newCall(any())).willReturn(requestCall);
        given(requestCall.execute()).willReturn(
                createResponse(SUCCESS_STATUS_CODE, createResponseDefaultString(), request));
        // WHEN
        List<CommandResource> result = underTest.getCommands(COMMAND_NAME, clustersResourceApi, headers);
        // THEN
        assertEquals(3, result.size());

    }

    @Test
    public void testGetCommandsNotFound() throws ApiException, CloudbreakException, IOException {
        // GIVEN
        Request request = createDefaultRequest();
        given(clustersResourceApi.getApiClient()).willReturn(apiClient);
        given(apiClient.getHttpClient()).willReturn(okHttpClient);
        given(apiClient.buildRequest(any(), any(), any(), isNull(), isNull(),
                any(), any(), any(), isNull())).willReturn(request);
        given(okHttpClient.newCall(any())).willReturn(requestCall);
        given(requestCall.execute()).willReturn(
                createResponse(NOT_FOUND_STATUS_CODE, createResponseDefaultString(), request));
        // WHEN
        CloudbreakException exception = assertThrows(CloudbreakException.class,
                () -> underTest.getCommands(COMMAND_NAME, clustersResourceApi, emptyHeaders));
        // THEN
        assertTrue(exception.getMessage().contains("/cmf/commands/commandTable request against CM API returned with status code: 404"));

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
                .message("message")
                .build();
    }

    private ResponseBody createResponseBody(String bodyString) {
        return ResponseBody
                .create(MediaType.parse("application/json"), bodyString);
    }

    private String createResponseDefaultString() {
        return "[{\"id\": 1,\"success\": true, \"name\": \"DeployClusterClientConfig\",\"start\": 1611513499011}, "
                + "{\"id\": 2,\"success\": true,\"name\": \"RestartServices\",\"start\": 1611513499013}, "
                + "{\"id\": 3,\"success\": true,\"name\": \"DeployClusterClientConfig\",\"start\": 1611513499021}]";
    }

}