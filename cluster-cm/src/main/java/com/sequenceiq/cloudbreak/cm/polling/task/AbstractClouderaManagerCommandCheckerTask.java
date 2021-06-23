package com.sequenceiq.cloudbreak.cm.polling.task;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
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
            String message = "Command [" + getCommandName() + "] failed: " + getResultMessageWithDetailedErrorsPostFix(apiCommand, commandsResourceApi);
            LOGGER.info(message);
            getCloudbreakEventService().fireClusterManagerEvent(pollerObject.getStack().getId(), pollerObject.getStack().getStatus().name(),
                    ResourceEvent.CLUSTER_CM_COMMAND_FAILED, Optional.of(pollerObject.getId()));
            throw new ClouderaManagerOperationFailedException(message);
        }
    }

    protected String getOperationIdentifier(T pollerObject) {
        return String.valueOf(pollerObject.getId());
    }

    protected abstract String getCommandName();
}
