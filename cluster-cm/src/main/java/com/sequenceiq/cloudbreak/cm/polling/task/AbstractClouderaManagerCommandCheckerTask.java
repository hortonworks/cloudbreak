package com.sequenceiq.cloudbreak.cm.polling.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.sequenceiq.cloudbreak.cluster.service.ClusterBasedStatusCheckerTask;
import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollerObject;

public abstract class AbstractClouderaManagerCommandCheckerTask<T extends ClouderaManagerPollerObject> extends ClusterBasedStatusCheckerTask<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractClouderaManagerCommandCheckerTask.class);

    private static final int BAD_GATEWAY = 502;

    private static final int INTERNAL_SERVER_ERROR = 500;

    private static final int INTERNAL_SERVER_ERROR_LIMIT = 5;

    //CHECKSTYLE:OFF
    protected final ClouderaManagerClientFactory clouderaManagerClientFactory;

    private int internalServerErrorCounter = 0;
    //CHECKSTYLE:ON

    protected AbstractClouderaManagerCommandCheckerTask(ClouderaManagerClientFactory clouderaManagerClientFactory) {
        this.clouderaManagerClientFactory = clouderaManagerClientFactory;
    }

    @Override
    public final boolean checkStatus(T pollerObject) {
        ApiClient apiClient = pollerObject.getApiClient();
        CommandsResourceApi commandsResourceApi = clouderaManagerClientFactory.getCommandsResourceApi(apiClient);
        try {
            return doStatusCheck(pollerObject, commandsResourceApi);
        } catch (ApiException e) {
            if (e.getCode() == BAD_GATEWAY) {
                LOGGER.debug("Cloudera Manager is not (yet) available.", e);
                return false;
            } else if (e.getCode() == INTERNAL_SERVER_ERROR) {
                if (internalServerErrorCounter < INTERNAL_SERVER_ERROR_LIMIT) {
                    internalServerErrorCounter++;
                    LOGGER.warn("Command [{}] with id [{}] returned with internal server error for the {}. time(s). Tolerating till {} occasions.",
                            getCommandName(), pollerObject.getId(), internalServerErrorCounter, INTERNAL_SERVER_ERROR_LIMIT);
                    return false;
                } else {
                    throw new ClouderaManagerOperationFailedException(
                            String.format("Command [%s] with id [%s] returned with internal server error for %s times. Operation is considered failed.",
                            getCommandName(), pollerObject.getId(), INTERNAL_SERVER_ERROR_LIMIT));
                }
            } else {
                throw new ClouderaManagerOperationFailedException(String.format("Cloudera Manager [%s] operation  failed.", getCommandName()), e);
            }
        }
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
