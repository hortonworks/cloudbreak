package com.sequenceiq.cloudbreak.cm.commands;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.client.ApiResponse;
import com.cloudera.api.swagger.model.ApiCommandList;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cm.model.CommandResource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@Service
public class DeployClientConfigCommandRetriever {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DeployClientConfigCommandRetriever.class);

    private static final String DEPLOY_CLIENT_CONFIG_COMMAND_NAME = "DeployClusterClientConfig";

    private static final int ERROR_CODES_FROM = 400;

    private final ActiveCommandTableResource activeCommandTableResource;

    private final RecentCommandTableResource recentCommandTableResource;

    public DeployClientConfigCommandRetriever(ActiveCommandTableResource activeCommandTableResource,
            RecentCommandTableResource recentCommandTableResource) {
        this.activeCommandTableResource = activeCommandTableResource;
        this.recentCommandTableResource = recentCommandTableResource;
    }

    /**
     * Obtain deploy cluster client config command ID with different strategies:
     * - use listActiveCommands against
     *   CM API and find the command ID from that response.
     * - if listActiveCommands is empty (in case of the command finishes during the
     *   deployClusterClientConfig timeout), use /cmf/commands/activeCommandTable call, if that won't work either,
     *   use /cmf/commands/commandTable call (both with the response cookie of listActiveCommands call)
     */
    public BigDecimal getCommandId(ClustersResourceApi api, Stack stack)
            throws CloudbreakException, ApiException {
        ApiResponse<ApiCommandList> commandListResponse =
                api.listActiveCommandsWithHttpInfo(stack.getName(), null);
        BigDecimal foundCommandId = getCommandIdFromActiveCommands(commandListResponse);
        if (foundCommandId == null) {
            LOGGER.debug("The last deploy client config command could not be found  "
                    + "by listing active commands. Trying {} call", activeCommandTableResource.getUriPath());
            List<CommandResource> commandsFromRunningCommandsTable = activeCommandTableResource.getCommands(
                    api, commandListResponse.getHeaders());
            LOGGER.debug("Processing activeCommandTable were successful.");
            foundCommandId = getLatestDeployClientConfigCommandId(
                    commandsFromRunningCommandsTable, activeCommandTableResource.getUriPath());
        }
        if (foundCommandId == null) {
            LOGGER.debug("The last deploy client config command could not be found  "
                    + "by listing active commandTable. Trying {} call", recentCommandTableResource.getUriPath());
            List<CommandResource> commandsFromRecentCommandsTable = recentCommandTableResource.getCommands(
                    api, commandListResponse.getHeaders());
            foundCommandId = getLatestDeployClientConfigCommandId(
                    commandsFromRecentCommandsTable, recentCommandTableResource.getUriPath());
        }
        return foundCommandId;
    }

    @VisibleForTesting
    BigDecimal getCommandIdFromActiveCommands(ApiResponse<ApiCommandList> response) {
        if (response.getStatusCode() >= ERROR_CODES_FROM) {
            return null;
        }
        ApiCommandList commandList = response.getData();
        List<CommandResource> commands = commandList.getItems().stream()
                .filter(c -> c.getParent() == null)
                .map(c -> {
                    CommandResource command = new CommandResource();
                    command.setStart(new DateTime(c.getStartTime())
                            .toDate().toInstant().toEpochMilli());
                    command.setName(c.getName());
                    command.setId(c.getId().longValue());
                    command.setSuccess(c.getSuccess());
                    return command;
                }).collect(Collectors.toList());
        return getLatestDeployClientConfigCommandId(commands, "listActiveCommands");
    }

    private BigDecimal getLatestDeployClientConfigCommandId(
            List<CommandResource> commandList, String path) {
        return commandList.stream()
                .filter(c -> DEPLOY_CLIENT_CONFIG_COMMAND_NAME.equals(c.getName()))
                .filter(c -> c.getSuccess() != null && c.getSuccess())
                .peek(c -> {
                    LOGGER.debug("Found latest DeployClusterClientConfig command "
                                    + "with [command_id: {}, start: {}] by commandTable call",
                            c.getId(), c.getStart());
                })
                .max(Comparator.comparingLong(CommandResource::getStart))
                .map(c -> new BigDecimal(c.getStart()))
                .orElse(null);
    }
}
