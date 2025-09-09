package com.sequenceiq.cloudbreak.cm.polling.task;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.sequenceiq.cloudbreak.cluster.service.ClusterEventService;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.exception.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.exception.CommandDetails;
import com.sequenceiq.cloudbreak.cm.exception.CommandDetailsFormatter;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;
import com.sequenceiq.cloudbreak.cm.util.ClouderaManagerCommandUtil;
import com.sequenceiq.cloudbreak.event.ResourceEvent;

public abstract class AbstractClouderaManagerCommandCheckerTask<T extends ClouderaManagerCommandPollerObject> extends AbstractClouderaManagerApiCheckerTask<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractClouderaManagerCommandCheckerTask.class);

    protected AbstractClouderaManagerCommandCheckerTask(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            ClusterEventService clusterEventService) {
        super(clouderaManagerApiPojoFactory, clusterEventService);
    }

    protected boolean doStatusCheck(T pollerObject) throws ApiException {
        CommandsResourceApi commandsResourceApi = clouderaManagerApiPojoFactory.getCommandsResourceApi(pollerObject.getApiClient());
        ApiCommand apiCommand = commandsResourceApi.readCommand(pollerObject.getId());
        if (apiCommand.isActive()) {
            LOGGER.debug("Command [{}] with id [{}] is active, so it hasn't finished yet", getCommandName(), pollerObject.getId());
            return false;
        } else if (apiCommand.isSuccess()) {
            return true;
        } else {
            List<CommandDetails> commandDetails = ClouderaManagerCommandUtil.getFailedOrActiveCommands(apiCommand, commandsResourceApi);
            String message = CommandDetailsFormatter.formatFailedCommands(commandDetails);
            LOGGER.debug("Top level command {}. Failed or active commands: {}", CommandDetails.fromApiCommand(apiCommand), commandDetails);
            throw new ClouderaManagerOperationFailedException(message);
        }
    }

    @Override
    public String successMessage(T pollerObject) {
        return String.format("Cloudera Manager application of command '%s' was a success", getCommandName().toLowerCase(Locale.ROOT));
    }

    @Override
    public void handleTimeout(ClouderaManagerCommandPollerObject pollerObject) {
        throw new ClouderaManagerOperationFailedException(String.format("Operation timed out. Failed to execute command %s.",
                getCommandName().toLowerCase(Locale.ROOT)));
    }

    protected String getOperationIdentifier(T pollerObject) {
        return String.valueOf(pollerObject.getId());
    }

    @Override
    public void sendFailureEvent(T pollerObject) {
        if (pollerObject.getId() != null) {
            getClusterEventService().fireClusterManagerEvent(pollerObject.getStack(),
                    ResourceEvent.CLUSTER_CM_COMMAND_FAILED, getCommandName(), Optional.of(pollerObject.getId()));
        }
    }

    @Override
    public void sendTimeoutEvent(T pollerObject) {
        if (pollerObject.getId() != null) {
            getClusterEventService().fireClusterManagerEvent(pollerObject.getStack(),
                    ResourceEvent.CLUSTER_CM_COMMAND_TIMEOUT, getCommandName(), Optional.of(pollerObject.getId()));
        }
    }

    protected abstract String getCommandName();

    @Override
    protected String getPollingName() {
        return getCommandName();
    }

    @Override
    protected String getToleratedErrorMessage(T pollerObject, ApiException e) {
        return String.format("Command [%s] with id [%s] failed with a tolerated error '%s' for the %s. time(s). ",
                getCommandName(), getOperationIdentifier(pollerObject), e.getMessage(), toleratedErrorCounter);
    }

    @Override
    protected String getErrorMessage(T pollerObject, ApiException e) {
        return String.format("Command [%s] with id [%s] failed with a %s.",
                getCommandName(), getOperationIdentifier(pollerObject), e.getClass().getSimpleName());
    }
}
