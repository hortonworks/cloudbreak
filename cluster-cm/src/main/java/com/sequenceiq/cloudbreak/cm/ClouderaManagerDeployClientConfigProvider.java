package com.sequenceiq.cloudbreak.cm;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.client.ApiResponse;
import com.cloudera.api.swagger.client.Pair;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiCommandList;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cm.model.CommandResource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

@Service
public class ClouderaManagerDeployClientConfigProvider {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ClouderaManagerDeployClientConfigProvider.class);

    private static final String CMF_COMMANDS_ACTIVE_COMMAND_TABLE_URI_PATH = "/cmf/commands/activeCommandTable";

    private static final String CMF_COMMANDS_RECENT_COMMAND_TABLE_URI_PATH = "/cmf/commands/commandTable";

    private static final String DEPLOY_CLIENT_CONFIG_COMMAND_NAME = "DeployClusterClientConfig";

    private static final int INTERVAL_MINUTES = 30;

    private static final int ERROR_CODES_FROM = 400;

    private static final int INVALID_INDEX = -1;

    private final Integer interruptTimeoutSeconds;

    public ClouderaManagerDeployClientConfigProvider(
            @Value("${cb.cm.client.commands.deployClientConfig.interrupt.timeout.seconds:}") Integer interruptTimeoutSeconds) {
        this.interruptTimeoutSeconds = interruptTimeoutSeconds;
    }

    /**
     * Obtain deploy cluster client config command ID with different strategies:
     * - first run deployClusterClientConfig command on CM, if it returns successfully,
     *   get the command ID of the command.
     * - if deployClusterClientConfig CM command times out, use listActiveCommands against
     *   CM API and find the command ID from that response.
     * - if listActiveCommands is empty (in case of the command finishes during the
     *   deployClusterClientConfig timeout), use /cmf/commands/activeCommandTable call, if that won't work either,
     *   use /cmf/commands/commandTable call (both with the response cookie of listActiveCommands call)
     */
    public BigDecimal deployClientConfigAndGetCommandId(
            ClustersResourceApi api, Stack stack) throws CloudbreakException, ApiException {
        ExecutorService executor = createExecutor();
        Future<BigDecimal> future = executor.submit(() -> {
            ApiCommand deployCommand = api.deployClientConfig(stack.getName());
            return deployCommand.getId();
        });
        try {
            return future.get(interruptTimeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException timeoutException) {
            LOGGER.debug("Deploy client config command took too much time. Start command ID "
                    + "query by listing active commands");
            ApiResponse<ApiCommandList> commandListResponse =
                    api.listActiveCommandsWithHttpInfo(stack.getName(), null);
            BigDecimal commandIdFromActiveCommands = getCommandIdFromActiveCommands(commandListResponse);
            if (commandIdFromActiveCommands != null) {
                return commandIdFromActiveCommands;
            }
            LOGGER.debug("The last deploy client config command could not be found  "
                    + "by listing active commands. Trying {} call", CMF_COMMANDS_ACTIVE_COMMAND_TABLE_URI_PATH);
            BigDecimal commandIdFromRunningCommandsTable = getCommandIdFromCommandsTable(api,
                    CMF_COMMANDS_ACTIVE_COMMAND_TABLE_URI_PATH, commandListResponse.getHeaders());
            if (commandIdFromRunningCommandsTable != null) {
                return commandIdFromRunningCommandsTable;
            }
            LOGGER.debug("The last deploy client config command could not be found  "
                    + "by listing recent commandTable. Trying {} call", CMF_COMMANDS_RECENT_COMMAND_TABLE_URI_PATH);
            BigDecimal commandIdFromRecentCommandsTable = getCommandIdFromCommandsTable(api,
                    CMF_COMMANDS_RECENT_COMMAND_TABLE_URI_PATH, commandListResponse.getHeaders());
            if (commandIdFromRecentCommandsTable != null) {
                return commandIdFromRecentCommandsTable;
            } else {
                throw new CloudbreakException(
                        String.format("Obtaining Cloudera Manager Deploy config command ID was not possible neither by"
                                + " listing Cloudera Manager commands nor using commandTable [stack: %s]", stack.getName()));
            }
        } catch (ExecutionException ee) {
            throw new ApiException(ee.getCause());
        } catch (InterruptedException e) {
            throw new CloudbreakException(
                    "Obtaining Cloudera Manager Deploy config command ID interrupted", e);
        } finally {
            executor.shutdown();
        }
    }

    @VisibleForTesting
    BigDecimal getCommandIdFromActiveCommands(ApiResponse<ApiCommandList> response) {
        BigDecimal result = null;
        if (response.getStatusCode() >= ERROR_CODES_FROM) {
            return result;
        }
        ApiCommandList commandList = response.getData();
        List<ApiCommand> commands = commandList.getItems();
        long currentStartTime = Long.MIN_VALUE;
        for (ApiCommand command : commands) {
            if (command.getParent() == null && command.getStartTime() != null
                    && DEPLOY_CLIENT_CONFIG_COMMAND_NAME.equals(command.getName())) {
                long startTime = new DateTime(command.getStartTime())
                        .toDate().toInstant().toEpochMilli();
                if (startTime > currentStartTime) {
                    LOGGER.debug("Found latest DeployClusterClientConfig command " +
                            "with [command_id: {}, starTime: {}] by active commands]",
                            command.getId(), command.getStartTime());
                    currentStartTime = startTime;
                    result = command.getId();
                }
            }
        }
        return result;
    }

    @VisibleForTesting
    BigDecimal getCommandIdFromCommandsTable(ClustersResourceApi api, String path,
            Map<String, List<String>> headers) throws CloudbreakException, ApiException {
        try {
            OkHttpClient httpClient = api.getApiClient().getHttpClient();
            Request request = createRequest(api.getApiClient(), path, headers);
            Response response = httpClient
                    .newCall(request)
                    .execute();
            if (response.code() >= ERROR_CODES_FROM) {
                LOGGER.debug("{} request against Cloudera Manager API returned with status code: {}, response: {}",
                        path, response.toString(), response.code());
                throw new CloudbreakException(
                        String.format("%s request against CM API returned with status code: %d",
                                path, response.code()));
            }
            return getCommandIdFromCMResponse(response, path);
        } catch (IOException e) {
            throw new CloudbreakException(
                    String.format("DeployClusterClientConfig - error during processing %s CM request",
                            path), e);
        }
    }

    private BigDecimal getCommandIdFromCMResponse(Response response, String path) throws IOException {
        BigDecimal result = null;
        try (ResponseBody responseBody = response.body()) {
            String responseBodyStr = responseBody.string();
            LOGGER.debug("Start processing commandTable response: {}", responseBodyStr);
            List<CommandResource> commandList =
                    new ObjectMapper().readValue(responseBodyStr, new TypeReference<>() {
                    });
            LOGGER.debug("Processing commandTable were successful.");
            long currentStartTime = Long.MIN_VALUE;
            for (CommandResource command : commandList) {
                if (DEPLOY_CLIENT_CONFIG_COMMAND_NAME.equals(command.getName())) {
                    LOGGER.debug("Found DeployClusterClientConfig command based on "
                                    + "{} response [command_id: {}]", path,
                            command.getId());
                    long startTime = command.getStart();
                    if (startTime > currentStartTime) {
                        LOGGER.debug("Found latest DeployClusterClientConfig command " +
                                "with [command_id: {}, start: {}] by commandTable call",
                                command.getId(), command.getStart());
                        currentStartTime = startTime;
                        result = new BigDecimal(command.getId());
                    }
                }
            }
        }
        return result;
    }

    // similar as apiClient#Call creator methods - so keep this in one place
    private Request createRequest(ApiClient apiClient, String path, Map<String, List<String>> headers)
            throws ApiException {
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
        long startTime = new DateTime()
                .minusMinutes(INTERVAL_MINUTES)
                .toDate().toInstant().toEpochMilli();
        long endTime = new DateTime()
                .plusMinutes(INTERVAL_MINUTES)
                .toDate().toInstant().toEpochMilli();
        queryParams.add(new Pair("startTime", Long.toString(startTime)));
        queryParams.add(new Pair("endTime", Long.toString(endTime)));
        if (headers.containsKey("Set-Cookie")) {
            LOGGER.debug("Copying Set-Cookie header from listActiveCommands "
                    + "to commandTable request (as Cookie header)");
            headerParams.put("Cookie", headers.get("Set-Cookie").get(0));
        }
    }

    @VisibleForTesting
    ExecutorService createExecutor() {
        return Executors.newSingleThreadExecutor();
    }

    @VisibleForTesting
    void addPathSegmentsFromDefaultUrl(
            HttpUrl defaultHttpUrl, HttpUrl.Builder httpUrlBuilder) {
        List<String> defaultPathSegments = defaultHttpUrl.pathSegments();
        int apiIndex = defaultPathSegments.indexOf("api");
        int versionNumberIndex =  apiIndex == INVALID_INDEX ? INVALID_INDEX : apiIndex + 1;
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
