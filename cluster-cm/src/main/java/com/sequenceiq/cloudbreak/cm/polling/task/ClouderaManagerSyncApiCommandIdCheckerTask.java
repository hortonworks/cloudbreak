package com.sequenceiq.cloudbreak.cm.polling.task;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.sequenceiq.cloudbreak.cluster.service.ClusterBasedStatusCheckerTask;
import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.commands.SyncApiCommandRetriever;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerSyncCommandPollerObject;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

public class ClouderaManagerSyncApiCommandIdCheckerTask
        extends ClusterBasedStatusCheckerTask<ClouderaManagerCommandPollerObject> {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ClouderaManagerSyncApiCommandIdCheckerTask.class);

    private final ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory;

    private final SyncApiCommandRetriever syncApiCommandRetriever;

    public ClouderaManagerSyncApiCommandIdCheckerTask(
            ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            SyncApiCommandRetriever syncApiCommandRetriever) {
        this.clouderaManagerApiPojoFactory = clouderaManagerApiPojoFactory;
        this.syncApiCommandRetriever = syncApiCommandRetriever;
    }

    @Override
    public boolean checkStatus(ClouderaManagerCommandPollerObject pollerObject) {
        ApiClient apiClient = pollerObject.getApiClient();
        ClustersResourceApi api = clouderaManagerApiPojoFactory.getClustersResourceApi(apiClient);
        boolean result = false;
        ClouderaManagerSyncCommandPollerObject castedObj = cast(pollerObject);
        try {
            Optional<Integer> commandId = syncApiCommandRetriever.getCommandId(
                    castedObj.getCommandName(), api, castedObj.getStack());
            if (commandId.isPresent() && !commandId.get().equals(pollerObject.getId())) {
                LOGGER.debug("Found a new latest command ID for {} command: {}", castedObj.getCommandName(), commandId);
                result = true;
            } else if (castedObj.getId() != null && castedObj.getId().equals(commandId.orElse(null))) {
                LOGGER.debug("There is a valid command ID found: {}, but that is not the "
                        + "newest command id of {}", castedObj.getId(), castedObj.getCommandName());
            } else {
                LOGGER.debug("Not found any new commandId yet - [recent_id: {}]", castedObj.getId());
            }
        } catch (ApiException | CloudbreakException e) {
            LOGGER.debug(String.format("Exception during polling the newest %s command.", castedObj.getCommandName()), e);
            result = false;
        }
        return result;
    }

    @Override
    public void handleTimeout(ClouderaManagerCommandPollerObject pollerObject) {
        throw new ClouderaManagerOperationFailedException(String.format("Operation timed out. "
                + "Failed to get newest successful %s command ID.", cast(pollerObject).getCommandName()));
    }

    @Override
    public String successMessage(ClouderaManagerCommandPollerObject pollerObject) {
        return String.format("Cloudera Manager %s command finished with success result.", cast(pollerObject).getCommandName());
    }

    private ClouderaManagerSyncCommandPollerObject cast(ClouderaManagerCommandPollerObject pollerObject) {
        return (ClouderaManagerSyncCommandPollerObject) pollerObject;
    }
}