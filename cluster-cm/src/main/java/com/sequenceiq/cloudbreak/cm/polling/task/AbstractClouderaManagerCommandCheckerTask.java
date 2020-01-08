package com.sequenceiq.cloudbreak.cm.polling.task;

import java.net.SocketException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.sequenceiq.cloudbreak.cluster.service.ClusterBasedStatusCheckerTask;
import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollerObject;

public abstract class AbstractClouderaManagerCommandCheckerTask<T extends ClouderaManagerPollerObject> extends ClusterBasedStatusCheckerTask<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractClouderaManagerCommandCheckerTask.class);

    private static final int BAD_GATEWAY = 502;

    private static final int INTERNAL_SERVER_ERROR = 500;

    private static final int TOLERATED_ERROR_LIMIT = 5;

    //CHECKSTYLE:OFF
    protected final ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory;

    private int toleratedErrorCounter = 0;
    //CHECKSTYLE:ON

    protected AbstractClouderaManagerCommandCheckerTask(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory) {
        this.clouderaManagerApiPojoFactory = clouderaManagerApiPojoFactory;
    }

    @Override
    public final boolean checkStatus(T pollerObject) {
        ApiClient apiClient = pollerObject.getApiClient();
        CommandsResourceApi commandsResourceApi = clouderaManagerApiPojoFactory.getCommandsResourceApi(apiClient);
        try {
            return doStatusCheck(pollerObject, commandsResourceApi);
        } catch (ApiException e) {
            if (e.getCode() == BAD_GATEWAY) {
                LOGGER.debug("Cloudera Manager is not (yet) available.", e);
                return false;
            } else if (isToleratedError(e)) {
                if (toleratedErrorCounter < TOLERATED_ERROR_LIMIT) {
                    toleratedErrorCounter++;
                    LOGGER.warn("Command [{}] with id [{}] failed with a tolerated error '{}' for the {}. time(s). Tolerating till {} occasions.",
                            getCommandName(), pollerObject.getId(), e.getMessage(), toleratedErrorCounter, TOLERATED_ERROR_LIMIT);
                    return false;
                } else {
                    throw new ClouderaManagerOperationFailedException(
                            String.format("Command [%s] with id [%s] failed with a tolerated error '%s' for %s times. Operation is considered failed.",
                            getCommandName(), pollerObject.getId(), e.getMessage(), TOLERATED_ERROR_LIMIT));
                }
            } else {
                throw new ClouderaManagerOperationFailedException(String.format("Cloudera Manager [%s] operation  failed.", getCommandName()), e);
            }
        }
    }

    private boolean isToleratedError(ApiException e) {
        return e.getCode() == INTERNAL_SERVER_ERROR || (e.getCause() != null && e.getCause() instanceof SocketException);
    }

    protected boolean doStatusCheck(T pollerObject, CommandsResourceApi commandsResourceApi) throws ApiException {
        ApiCommand apiCommand = commandsResourceApi.readCommand(pollerObject.getId());
        if (apiCommand.getActive()) {
            LOGGER.debug("Command [" + getCommandName() + "] with id [" + pollerObject.getId() + "] is active, so it hasn't finished yet");
            return false;
        } else if (apiCommand.getSuccess()) {
            return true;
        } else {
            String resultMessage = apiCommand.getResultMessage();
            LOGGER.info("Command [" + getCommandName() + "] failed: " + resultMessage);
            throw new ClouderaManagerOperationFailedException("Command [" + getCommandName() + "] failed: " + resultMessage);
        }
    }

    protected abstract String getCommandName();
}
