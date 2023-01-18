package com.sequenceiq.cloudbreak.cm.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.client.Pair;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cm.model.CommandResource;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

public abstract class AbstractCommandTableResource {

    public static final int ERROR_CODES_FROM = 400;

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AbstractCommandTableResource.class);

    private static final int INTERVAL_MINUTES = 30;

    private static final int INVALID_INDEX = -1;

    public abstract String getUriPath();

    public List<CommandResource> getCommands(String commandName, ClustersResourceApi api,
            Map<String, List<String>> headers) throws CloudbreakException, ApiException {
        try {
            OkHttpClient httpClient = api.getApiClient().getHttpClient();
            Request request = createRequest(api.getApiClient(), getUriPath(), headers);
            Response response = httpClient
                    .newCall(request)
                    .execute();
            if (response.code() >= ERROR_CODES_FROM) {
                LOGGER.debug("{} request against Cloudera Manager API returned with status code: {}, response: {}",
                        request.httpUrl().toString(), response, response.code());
                throw new CloudbreakException(
                        String.format("%s request against CM API returned with status code: %d",
                                getUriPath(), response.code()));
            }
            return getCommandFromCMResponse(response, getUriPath(), commandName);
        } catch (IOException e) {
            throw new CloudbreakException(
                    String.format("%s - error during processing %s CM request",
                            commandName, getUriPath()), e);
        }
    }

    private List<CommandResource> getCommandFromCMResponse(Response response, String path, String commandName) throws IOException {
        try (ResponseBody responseBody = response.body()) {
            String responseBodyStr = responseBody.string();
            LOGGER.debug("Start processing commandTable response: {}, path: {}, commandName: {}", responseBodyStr, path, commandName);
            return JsonUtil.jsonToType(responseBodyStr, new CommandResourceListTypeReference());
        }
    }

    // similar as apiClient#Call creator methods - so keep this in one place
    private Request createRequest(ApiClient apiClient, String path, Map<String, List<String>> headers) throws ApiException {
        List<Pair> queryParams = new ArrayList<>();
        Map<String, String> headerParams = new HashMap<>();
        String[] localVarAccepts = new String[]{"application/json"};
        String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            headerParams.put("Accept", localVarAccept);
        }
        String[] localVarContentTypes = new String[0];
        String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);
        headerParams.put("Content-Type", localVarContentType);
        String[] authNames = new String[]{"basic"};
        enhanceRequestParams(headers, queryParams, headerParams);
        Request defaultRequest = apiClient.buildRequest(path, "GET", queryParams, null,
                headerParams, new HashMap<>(), authNames, null);
        return createRequestFromDefaultRequest(defaultRequest);
    }

    private Request createRequestFromDefaultRequest(Request defaultRequest) {
        HttpUrl defaultHttpUrl = defaultRequest.httpUrl();
        HttpUrl.Builder httpUrlBuilder = new HttpUrl.Builder()
                .username(defaultHttpUrl.username())
                .password(defaultHttpUrl.password())
                .host(defaultHttpUrl.host())
                .scheme(defaultHttpUrl.scheme())
                .port(defaultHttpUrl.port());
        addPathSegmentsFromDefaultUrl(defaultHttpUrl, httpUrlBuilder);
        for (String queryParamName : defaultHttpUrl.queryParameterNames()) {
            httpUrlBuilder.addQueryParameter(queryParamName, defaultHttpUrl.queryParameter(queryParamName));
        }
        return new Request.Builder()
                .headers(defaultRequest.headers())
                .url(httpUrlBuilder.build())
                .cacheControl(defaultRequest.cacheControl())
                .tag(defaultRequest.tag())
                .get()
                .build();
    }

    /**
     * Extends request that is created based on the ApiClient endpoint implementations
     * with default query params + Cookie header from listActiveCommands call.
     * Example query: /cmf/commands/commandTable?limit=501&startTime=1234&endTime1235
     */
    private void enhanceRequestParams(Map<String, List<String>> headers, List<Pair> queryParams, Map<String, String> headerParams) {
        queryParams.add(new Pair("limit", "501"));
        if (headers.containsKey("Set-Cookie")) {
            LOGGER.debug("Copying Set-Cookie header from listActiveCommands to commandTable request (as Cookie header)");
            headerParams.put("Cookie", headers.get("Set-Cookie").get(0));
        }
    }

    @VisibleForTesting
    void addPathSegmentsFromDefaultUrl(HttpUrl defaultHttpUrl, HttpUrl.Builder httpUrlBuilder) {
        List<String> defaultPathSegments = defaultHttpUrl.pathSegments();
        int apiIndex = defaultPathSegments.indexOf("api");
        int versionNumberIndex = apiIndex == INVALID_INDEX ? INVALID_INDEX : apiIndex + 1;
        for (int i = 0; i < defaultPathSegments.size(); i++) {
            String pathSegment = defaultPathSegments.get(i);
            if (i != apiIndex && i != versionNumberIndex) {
                LOGGER.debug("Add path segment to commandTable request: {}", pathSegment);
                httpUrlBuilder.addPathSegment(pathSegment);
            } else {
                LOGGER.debug("Skipping path segment from commandTable request: {}", pathSegment);
            }
        }
    }
}
