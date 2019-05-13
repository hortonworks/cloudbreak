package com.sequenceiq.cloudbreak.cm.polling.task;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiCommandList;
import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;

@Service
public class ClouderaManagerTemplateInstallChecker extends AbstractClouderaManagerCommandCheckerTask<ClouderaManagerCommandPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerTemplateInstallChecker.class);

    @Inject
    private ClouderaManagerClientFactory clouderaManagerClientFactory;

    @Override
    public boolean checkStatus(ClouderaManagerCommandPollerObject pollerObject) {
        ApiClient apiClient = pollerObject.getApiClient();
        CommandsResourceApi commandsResourceApi = clouderaManagerClientFactory.getCommandsResourceApi(apiClient);
        try {
            ApiCommand apiCommand = commandsResourceApi.readCommand(pollerObject.getId());

            if (apiCommand.getActive()) {
                LOGGER.debug("Command [" + getCommandName() + "] with id [" + pollerObject.getId() + "] is active, so it hasn't finished yet");
                return false;
            } else if (apiCommand.getSuccess()) {
                return true;
            } else {
                List<String> errorReasons = Optional.ofNullable(apiCommand.getChildren()).stream()
                        .flatMap(extractErrorMessages(commandsResourceApi))
                        .collect(Collectors.toList());

                LOGGER.info("Command [" + getCommandName() + "] failed: " + errorReasons);
                throw new ClouderaManagerOperationFailedException("Cluster template install failed: " + errorReasons);
            }
        } catch (ApiException e) {
            LOGGER.debug("cloudera manager is not running", e);
            return false;
        }
    }

    private Function<ApiCommandList, Stream<String>> extractErrorMessages(CommandsResourceApi commandsResourceApi) {
        return apiCommandList -> {
            List<String> errorReasons = new ArrayList<>();
            apiCommandList.getItems().stream()
                    .filter(commandFailed())
                    .map(readCommand(commandsResourceApi))
                    .forEach(cmd -> digForFailureCause(cmd, errorReasons, commandsResourceApi));
            return errorReasons.stream();
        };
    }

    private Predicate<ApiCommand> commandFailed() {
        return cmd -> !cmd.getSuccess();
    }

    private Function<ApiCommand, ApiCommand> readCommand(CommandsResourceApi commandsResourceApi) {
        return cmd -> {
            try {
                return commandsResourceApi.readCommand(cmd.getId());
            } catch (ApiException e) {
                LOGGER.debug("Failed to read command. id [{}]", cmd.getId(), e);
                return new ApiCommand();
            }
        };
    }

    private void digForFailureCause(ApiCommand apiCommand, List<String> errorReasons, CommandsResourceApi commandsResourceApi) {
        List<ApiCommand> children = Optional.ofNullable(apiCommand.getChildren()).map(ApiCommandList::getItems).orElse(List.of());
        if (children.isEmpty()) {
            String reason = String.format("Command [%s], with id [%.0f] failed: %s", apiCommand.getName(), apiCommand.getId(), apiCommand.getResultMessage());
            errorReasons.add(reason);
        } else {
            children.stream()
                    .filter(commandFailed())
                    .map(readCommand(commandsResourceApi))
                    .forEach(cmd -> digForFailureCause(cmd, errorReasons, commandsResourceApi));
        }
    }

    @Override
    public void handleTimeout(ClouderaManagerCommandPollerObject clouderaManagerPollerObject) {
        throw new ClouderaManagerOperationFailedException("Operation timed out. Template install timed out with this command id: "
                + clouderaManagerPollerObject.getId());
    }

    @Override
    public String successMessage(ClouderaManagerCommandPollerObject clouderaManagerPollerObject) {
        return String.format("Template installation success for stack '%s'", clouderaManagerPollerObject.getStack().getId());
    }

    @Override
    protected String getCommandName() {
        return "Template install";
    }
}
