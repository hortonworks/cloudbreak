package com.sequenceiq.cloudbreak.cm.polling.task;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiHostRef;
import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandListPollerObject;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

public abstract class AbstractClouderaManagerCommandListCheckerTask<T extends ClouderaManagerCommandListPollerObject>
        extends AbstractClouderaManagerApiCheckerTask<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractClouderaManagerCommandListCheckerTask.class);

    protected AbstractClouderaManagerCommandListCheckerTask(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            CloudbreakEventService cloudbreakEventService) {
        super(clouderaManagerApiPojoFactory, cloudbreakEventService);
    }

    protected boolean doStatusCheck(T pollerObject, CommandsResourceApi commandsResourceApi) throws ApiException {
        List<ApiCommand> apiCommands = collectApiCommands(pollerObject, commandsResourceApi);
        boolean allCommandsFinished = apiCommands.stream().noneMatch(ApiCommand::getActive);
        if (allCommandsFinished) {
            validateApiCommandResults(apiCommands);
            return true;
        } else {
            return false;
        }
    }

    private List<ApiCommand> collectApiCommands(T pollerObject, CommandsResourceApi commandsResourceApi)  throws ApiException {
        List<ApiCommand> apiCommands = new ArrayList<>();
        for (Integer commandId : pollerObject.getIdList()) {
            ApiCommand apiCommand = commandsResourceApi.readCommand(commandId);
            apiCommands.add(apiCommand);
            if (apiCommand.getActive()) {
                LOGGER.debug("Command [" + getCommandName() + "] with id [" + commandId + "] is active, so it hasn't finished yet");
                break;
            }
        }
        return apiCommands;
    }

    private void validateApiCommandResults(List<ApiCommand> apiCommands) {
        List<ApiCommand> failedCommands = apiCommands.stream().filter(cmd -> !cmd.getSuccess()).collect(Collectors.toList());
        if (!failedCommands.isEmpty()) {
            String message = failedCommands.stream().map(cmd -> createFailedCommandResultString(cmd)).collect(Collectors.joining(","));
            LOGGER.info(message);
            throw new ClouderaManagerOperationFailedException(message);
        }
    }

    private String createFailedCommandResultString(ApiCommand cmd) {
        String resultMessage = cmd.getResultMessage();
        List<String> detailedMessages = ApiCommandUtil.getFailedCommandMessages(cmd.getChildren());
        ApiHostRef hostRef = cmd.getHostRef();
        String hostRefStr = hostRef == null ? "Unknown" : hostRef.getHostname();
        return "Command [" + cmd.getName() + ":" + cmd.getId() + "] failed on host [" + hostRefStr + "]: " + resultMessage +
                ". Detailed messages: " + String.join("\n", detailedMessages);
    }

    protected String getOperationIdentifier(T pollerObject) {
        return StringUtils.join(pollerObject.getIdList(), ",");
    }
}
