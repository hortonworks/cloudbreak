package com.sequenceiq.cloudbreak.cm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.sequenceiq.cloudbreak.cluster.service.ClusterBasedStatusCheckerTask;

public abstract class AbstractClouderaManagerCommandCheckerTask<T extends ClouderaManagerCommandPollerObject> extends ClusterBasedStatusCheckerTask<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractClouderaManagerCommandCheckerTask.class);

    @Override
    public boolean checkStatus(T pollerObject) {
        ApiClient apiClient = pollerObject.getApiClient();
        CommandsResourceApi commandsResourceApi = new CommandsResourceApi(apiClient);
        try {
            ApiCommand apiCommand = commandsResourceApi.readCommand(pollerObject.getId());

            if (apiCommand.getActive()) {
                LOGGER.debug("Command [" + getCommandName() + "] id " + pollerObject.getId() + " is active, so it hasn't finished yet");
                return false;
            } else if (apiCommand.getSuccess()) {
                return true;
            } else {
                String resultMessage = apiCommand.getResultMessage();
                LOGGER.info("Command [" + getCommandName() + "] failed: " + resultMessage);
                throw new ClouderaManagerOperationFailedException("Command [" + getCommandName() + "] failed: " + resultMessage);
            }
        } catch (ApiException e) {
            LOGGER.debug("cloudera manager is not running", e);
            return false;
        }
    }

    abstract String getCommandName();
}
