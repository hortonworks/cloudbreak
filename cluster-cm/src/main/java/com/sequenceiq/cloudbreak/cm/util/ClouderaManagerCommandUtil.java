package com.sequenceiq.cloudbreak.cm.util;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiCommandList;
import com.sequenceiq.cloudbreak.cm.exception.CommandDetails;

public class ClouderaManagerCommandUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerCommandUtil.class);

    private ClouderaManagerCommandUtil() {

    }

    private static Predicate<ApiCommand> commandFailed() {
        return cmd -> {
            if (cmd != null) {
                if (cmd.getActive() != null && cmd.getActive()) {
                    return true;
                }
                return cmd.getSuccess() != null && !cmd.getSuccess();
            }
            return false;
        };
    }

    private static Function<ApiCommand, ApiCommand> readCommand(CommandsResourceApi commandsResourceApi) {
        return cmd -> {
            try {
                return commandsResourceApi.readCommand(cmd.getId());
            } catch (ApiException e) {
                LOGGER.debug("Failed to read command. id [{}]", cmd.getId(), e);
                return new ApiCommand();
            }
        };
    }

    public static List<CommandDetails> getFailedOrActiveCommands(ApiCommand apiCommand, CommandsResourceApi commandsResourceApi) {
        List<CommandDetails> failedCommands = new LinkedList<>();
        try {
            Optional.ofNullable(apiCommand.getChildren()).map(ApiCommandList::getItems).orElse(List.of()).stream()
                    .filter(commandFailed())
                    .map(readCommand(commandsResourceApi))
                    .forEach(cmd -> failedCommands.addAll(getFailedOrActiveCommands(cmd, commandsResourceApi)));
            if (failedCommands.isEmpty() && StringUtils.isNotEmpty(apiCommand.getResultMessage())) {
                failedCommands.add(CommandDetails.fromApiCommand(apiCommand));
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to get all command failure from Cloudera Manager. Proceeding silently.", e);
        }
        return failedCommands;
    }
}
