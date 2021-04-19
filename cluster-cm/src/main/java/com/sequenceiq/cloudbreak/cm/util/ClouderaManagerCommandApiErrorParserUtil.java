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

public class ClouderaManagerCommandApiErrorParserUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerCommandApiErrorParserUtil.class);

    private ClouderaManagerCommandApiErrorParserUtil() {

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

    public static List<String> getErrors(ApiCommand apiCommand, CommandsResourceApi commandsResourceApi) {
        List<String> errors = new LinkedList<>();
        try {
            Optional.ofNullable(apiCommand.getChildren()).map(ApiCommandList::getItems).orElse(List.of()).stream()
                    .filter(commandFailed())
                    .map(readCommand(commandsResourceApi))
                    .forEach(cmd -> errors.addAll(getErrors(cmd, commandsResourceApi)));

            if (errors.isEmpty() && StringUtils.isNotEmpty(apiCommand.getResultMessage())) {
                String reason = String.format("Command [%s], with id [%s] failed: %s",
                        apiCommand.getName(), apiCommand.getId(), apiCommand.getResultMessage());
                errors.add(reason);
            }
        } catch (RuntimeException e) {
            LOGGER.warn("We have tried to get the error reason an go through the command tree, but it has failed. It is still better to proceed silently", e);
        }

        return errors;
    }
}
