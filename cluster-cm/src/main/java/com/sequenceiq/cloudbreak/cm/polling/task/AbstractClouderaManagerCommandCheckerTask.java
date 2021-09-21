package com.sequenceiq.cloudbreak.cm.polling.task;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.exception.CommandDetails;
import com.sequenceiq.cloudbreak.cm.exception.CommandDetailsFormatter;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;
import com.sequenceiq.cloudbreak.cm.util.ClouderaManagerCommandUtil;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

public abstract class AbstractClouderaManagerCommandCheckerTask<T extends ClouderaManagerCommandPollerObject> extends AbstractClouderaManagerApiCheckerTask<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractClouderaManagerCommandCheckerTask.class);

    protected AbstractClouderaManagerCommandCheckerTask(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            CloudbreakEventService cloudbreakEventService) {
        super(clouderaManagerApiPojoFactory, cloudbreakEventService);
    }

    protected boolean doStatusCheck(T pollerObject, CommandsResourceApi commandsResourceApi) throws ApiException {
        ApiCommand apiCommand = commandsResourceApi.readCommand(pollerObject.getId());
        if (apiCommand.getActive()) {
            LOGGER.debug("Command [{}] with id [{}] is active, so it hasn't finished yet", getCommandName(), pollerObject.getId());
            return false;
        } else if (apiCommand.getSuccess()) {
            return true;
        } else {
            List<CommandDetails> commandDetails = ClouderaManagerCommandUtil.getFailedOrActiveCommands(apiCommand, commandsResourceApi);
            String message = CommandDetailsFormatter.formatFailedCommands(commandDetails);
            LOGGER.debug("Top level command {}. Failed or active commands: {}", CommandDetails.fromApiCommand(apiCommand), commandDetails);
            throw new ClouderaManagerOperationFailedException(message);
        }
    }

    protected String getOperationIdentifier(T pollerObject) {
        return String.valueOf(pollerObject.getId());
    }

    protected abstract String getCommandName();
}
