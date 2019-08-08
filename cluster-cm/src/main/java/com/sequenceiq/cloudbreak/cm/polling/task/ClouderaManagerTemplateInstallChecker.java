package com.sequenceiq.cloudbreak.cm.polling.task;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
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
                List<String> errorReasons = new ArrayList<>();
                digForFailureCause(apiCommand, errorReasons, commandsResourceApi);

                String msg = "Cluster template install failed: " + errorReasons;
                LOGGER.info(msg);
                throw new ClouderaManagerOperationFailedException(msg);
            }
        } catch (ApiException e) {
            LOGGER.debug("Cloudera Manager is not running", e);
            return false;
        }
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
        List<String> childErrors = new LinkedList<>();
        Optional.ofNullable(apiCommand.getChildren()).map(ApiCommandList::getItems).orElse(List.of()).stream()
                .filter(commandFailed())
                .map(readCommand(commandsResourceApi))
                .forEach(cmd -> digForFailureCause(cmd, childErrors, commandsResourceApi));

        if (childErrors.isEmpty() && StringUtils.isNotEmpty(apiCommand.getResultMessage())) {
            String reason = String.format("Command [%s], with id [%.0f] failed: %s", apiCommand.getName(), apiCommand.getId(), apiCommand.getResultMessage());
            errorReasons.add(reason);
        }

        errorReasons.addAll(childErrors);
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
