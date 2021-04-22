package com.sequenceiq.cloudbreak.cm.polling.task;

import java.util.ArrayList;
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
import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

public class ClouderaManagerCollectDiagnosticsListenerTask extends AbstractClouderaManagerCommandCheckerTask<ClouderaManagerCommandPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerCollectDiagnosticsListenerTask.class);

    public ClouderaManagerCollectDiagnosticsListenerTask(ClouderaManagerApiPojoFactory apiPojoFactory, CloudbreakEventService eventService) {
        super(apiPojoFactory, eventService);
    }

    @Override
    protected boolean doStatusCheck(ClouderaManagerCommandPollerObject pollerObject, CommandsResourceApi commandsResourceApi) throws ApiException {
        commandsResourceApi = clouderaManagerApiPojoFactory.getCommandsResourceApi(pollerObject.getApiClient());
        ApiCommand apiCommand = commandsResourceApi.readCommand(pollerObject.getId());
        if (apiCommand.getActive()) {
            LOGGER.debug("Command [" + getCommandName() + "] with id [" + pollerObject.getId() + "] is active, so it hasn't finished yet");
            return false;
        } else if (apiCommand.getSuccess()) {
            return true;
        } else {
            List<String> errorReasons = new ArrayList<>();
            digForFailureCause(apiCommand, errorReasons, commandsResourceApi);
            String msg = "Collect diagnostics failed: " + errorReasons;
            LOGGER.info(msg);
            throw new ClouderaManagerOperationFailedException(msg);
        }
    }

    @Override
    public void handleTimeout(ClouderaManagerCommandPollerObject clouderaManagerCommandPollerObject) {
        throw new ClouderaManagerOperationFailedException("Operation timed out. Failed to collect diagnostics.");
    }

    @Override
    public String successMessage(ClouderaManagerCommandPollerObject clouderaManagerCommandPollerObject) {
        return "Cloudera Manager diagnostics collection finished with success result.";
    }

    @Override
    protected String getCommandName() {
        return "Collect diagnostics";
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
}
