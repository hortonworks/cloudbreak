package com.sequenceiq.cloudbreak.service.cluster.clouderamanager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.sequenceiq.cloudbreak.service.ClusterBasedStatusCheckerTask;

@Service
public class ClouderaManagerStopListenerTask extends ClusterBasedStatusCheckerTask<ClouderaManagerCommandPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerStopListenerTask.class);

    @Override
    public boolean checkStatus(ClouderaManagerCommandPollerObject pollerObject) {
        ApiClient apiClient = pollerObject.getApiClient();
        CommandsResourceApi commandsResourceApi = new CommandsResourceApi(apiClient);
        try {
            ApiCommand apiCommand = commandsResourceApi.readCommand(pollerObject.getId());

            if (apiCommand.getActive()) {
                LOGGER.debug("The command id " + pollerObject.getId() + " is active, so it hasn't finished yet");
                return false;
            } else if (apiCommand.getSuccess()) {
                return true;
            } else {
                String resultMessage = apiCommand.getResultMessage();
                LOGGER.info("Stop command failed: " + resultMessage);
                throw new ClouderaManagerOperationFailedException("Stop command failed: " + resultMessage);
            }
        } catch (ApiException e) {
            LOGGER.debug("cloudera manager is not running", e);
            return false;
        }
    }

    @Override
    public void handleTimeout(ClouderaManagerCommandPollerObject toolsResourceApi) {
        throw new ClouderaManagerOperationFailedException("Operation timed out. Failed to check Cloudera Manager startup.");
    }

    @Override
    public String successMessage(ClouderaManagerCommandPollerObject toolsResourceApi) {
        return "Cloudera Manager startup finished with success result.";
    }
}
