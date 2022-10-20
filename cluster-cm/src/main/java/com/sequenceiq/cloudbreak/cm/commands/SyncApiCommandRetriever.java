package com.sequenceiq.cloudbreak.cm.commands;

import static com.sequenceiq.cloudbreak.cm.commands.AbstractCommandTableResource.ERROR_CODES_FROM;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.client.ApiResponse;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiCommandList;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cm.model.CommandResource;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.view.StackView;

@Service
public class SyncApiCommandRetriever {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(SyncApiCommandRetriever.class);

    private final ActiveCommandTableResource activeCommandTableResource;

    private final RecentCommandTableResource recentCommandTableResource;

    public SyncApiCommandRetriever(ActiveCommandTableResource activeCommandTableResource,
            RecentCommandTableResource recentCommandTableResource) {
        this.activeCommandTableResource = activeCommandTableResource;
        this.recentCommandTableResource = recentCommandTableResource;
    }

    /**
     * Obtain a sync API command ID with different strategies:
     * - use listActiveCommands against CM API and find the command ID from that response.
     * - if listActiveCommands is empty (in case of the command after the timeout) use /cmf/commands/activeCommandTable call,
     * - if that won't work either, use /cmf/commands/commandTable call (both with the response cookie of listActiveCommands call)
     */
    public Optional<BigDecimal> getCommandId(String commandName, ClustersResourceApi api, StackView stack)
            throws CloudbreakException, ApiException {
        return getCommandId(commandName, api, stack, false);
    }

    /**
     * Obtain a sync API command ID:
     * - use listActiveCommands against CB API, but use only the cookies from the response
     * - with the cookie of listActiveCommands response, try to gather the last finished command ID with /cmf/commands/commandTable call
     */
    public Optional<BigDecimal> getLastFinishedCommandId(String commandName, ClustersResourceApi api, StackView stack)
            throws CloudbreakException, ApiException {
        Optional<BigDecimal> lastSyncApiCommandId = getCommandId(commandName, api, stack, true);
        lastSyncApiCommandId.ifPresent(commandId -> {
            LOGGER.debug("Found already existing {} command with id: {}", commandName, commandId);
        });
        return lastSyncApiCommandId;
    }

    private Optional<BigDecimal> getCommandId(String commandName, ClustersResourceApi api, StackView stack, boolean skipActiveRunningCommands)
            throws CloudbreakException, ApiException {
        ApiResponse<ApiCommandList> commandListResponse =
                api.listActiveCommandsWithHttpInfo(stack.getName(), null, null);
        Optional<BigDecimal> foundCommandId =
                Optional.ofNullable(getCommandIdFromActiveCommands(commandName, commandListResponse, skipActiveRunningCommands));
        if (foundCommandId.isEmpty() && !skipActiveRunningCommands) {
            foundCommandId = fetchCommandIdFromCommandTable(commandName, activeCommandTableResource, api, commandListResponse.getHeaders());
        }
        if (foundCommandId.isEmpty()) {
            foundCommandId = fetchCommandIdFromCommandTable(commandName, recentCommandTableResource, api, commandListResponse.getHeaders());
        }
        return foundCommandId;
    }

    @VisibleForTesting
    BigDecimal getCommandIdFromActiveCommands(String commandName, ApiResponse<ApiCommandList> response, boolean skipActiveRunningCommand) {
        LOGGER.debug("Response status code from listActiveCommands: {}", response.getStatusCode());
        if (response.getStatusCode() >= ERROR_CODES_FROM) {
            return null;
        } else {
            ApiCommandList commandList = response.getData();
            List<CommandResource> commands = commandList.getItems().stream()
                    .filter(c -> c.getParent() == null)
                    .map(this::convertApiCommandToCommandResource)
                    .collect(Collectors.toList());
            return getLatestCommandId(commandName, commands, "listActiveCommands", skipActiveRunningCommand);
        }
    }

    private BigDecimal getLatestCommandId(String commandName, List<CommandResource> commandList, String path, boolean skipActiveRunningCommands) {
        if (skipActiveRunningCommands) {
            LOGGER.debug("Skipping active running commands from command ID check [{}]", commandName);
            return null;
        } else {
            return commandList.stream()
                    .filter(c -> commandName.equals(c.getName()))
                    .filter(c -> c.getStart() != null)
                    .peek(c -> LOGGER.debug("Found latest {} command with [command_id: {}, start: {}] by {} call", commandName, c.getId(), c.getStart(), path))
                    .max(Comparator.comparingLong(CommandResource::getStart))
                    .map(c -> new BigDecimal(c.getId()))
                    .orElse(null);
        }
    }

    private BigDecimal getLatestCommandId(String commandName, List<CommandResource> commandList, String path) {
        return getLatestCommandId(commandName, commandList, path, false);
    }

    private Optional<BigDecimal> fetchCommandIdFromCommandTable(String commandName, AbstractCommandTableResource commandTable,
            ClustersResourceApi api, Map<String, List<String>> headers) throws ApiException, CloudbreakException {
        String uriPath = commandTable.getUriPath();
        LOGGER.debug("The last {} command could not be found  by listing commands... Trying {} call", commandName, uriPath);
        List<CommandResource> commandsFromRunningCommandsTable = commandTable.getCommands(
                commandName, api, headers);
        LOGGER.debug("Processing of {} call has been completed with commands {}", uriPath, commandsFromRunningCommandsTable);
        return Optional.ofNullable(getLatestCommandId(commandName, commandsFromRunningCommandsTable, commandTable.getUriPath()));
    }

    private CommandResource convertApiCommandToCommandResource(ApiCommand apiCommand) {
        CommandResource command = new CommandResource();
        command.setStart(new DateTime(apiCommand.getStartTime()).toDate().toInstant().toEpochMilli());
        command.setName(apiCommand.getName());
        command.setId(apiCommand.getId().longValue());
        command.setSuccess(apiCommand.getSuccess());
        return command;
    }
}