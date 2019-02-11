package com.sequenceiq.cloudbreak.service.cluster.clouderamanager;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiCommandList;
import com.sequenceiq.cloudbreak.service.ClusterBasedStatusCheckerTask;

@Service
public class ClouderaManagerTemplateInstallChecker extends ClusterBasedStatusCheckerTask<ClouderaManagerCommandPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerTemplateInstallChecker.class);

    @Override
    public boolean checkStatus(ClouderaManagerCommandPollerObject pollerObject) {
        LOGGER.debug("Check if command with id " + pollerObject.getId() + " is still running for " + pollerObject.getStack().getAmbariIp());
        ApiClient apiClient = pollerObject.getApiClient();
        ClouderaManagerResourceApi clouderaManagerResourceApi = new ClouderaManagerResourceApi(apiClient);
        try {
            String viewType = "FULL";
            ApiCommandList commandList = clouderaManagerResourceApi.listActiveCommands(viewType);

            Optional<ApiCommand> runningCommandWithId = commandList.getItems().stream()
                    .filter(apiCommand -> pollerObject.getId().equals(apiCommand.getId()))
                    .findFirst();

            if (runningCommandWithId.isPresent()) {
                LOGGER.debug("The command id " + pollerObject.getId() + " has been found in active commands, so it hasn't finished yet");
                return false;
            } else {
                return true;
            }
        } catch (ApiException e) {
            LOGGER.info("Can not list active commands");
            return false;
        }
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
}
