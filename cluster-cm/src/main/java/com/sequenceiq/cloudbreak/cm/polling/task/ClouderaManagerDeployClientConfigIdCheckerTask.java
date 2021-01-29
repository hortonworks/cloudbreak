package com.sequenceiq.cloudbreak.cm.polling.task;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.sequenceiq.cloudbreak.cluster.service.ClusterBasedStatusCheckerTask;
import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.commands.DeployClientConfigCommandRetriever;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

public class ClouderaManagerDeployClientConfigIdCheckerTask
        extends ClusterBasedStatusCheckerTask<ClouderaManagerCommandPollerObject> {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ClouderaManagerDeployClientConfigIdCheckerTask.class);

    private final ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory;

    private final DeployClientConfigCommandRetriever deployClientConfigCommandRetriever;

    public ClouderaManagerDeployClientConfigIdCheckerTask(
            ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            DeployClientConfigCommandRetriever deployClientConfigCommandRetriever) {
        this.clouderaManagerApiPojoFactory = clouderaManagerApiPojoFactory;
        this.deployClientConfigCommandRetriever = deployClientConfigCommandRetriever;
    }

    @Override
    public boolean checkStatus(ClouderaManagerCommandPollerObject pollerObject) {
        ApiClient apiClient = pollerObject.getApiClient();
        ClustersResourceApi api = clouderaManagerApiPojoFactory.getClustersResourceApi(apiClient);
        boolean result = false;
        try {
            BigDecimal commandId = deployClientConfigCommandRetriever.getCommandId(api, pollerObject.getStack());
            if (commandId != null && !commandId.equals(pollerObject.getId())) {
                LOGGER.debug("Found a new latest command ID for deployClusterClientConfig command: {}", commandId);
                result = true;
            }
            if (pollerObject.getId() != null && pollerObject.getId().equals(commandId)) {
                LOGGER.debug("There is a valid command ID found: {}, but that is not the "
                        + "newest command id of deployClusterClientConfig", pollerObject.getId());
            }
        } catch (ApiException | CloudbreakException e) {
            LOGGER.debug("Exception during polling the newest deployClusterClientConfig command.", e);
            result = false;
        }
        return result;
    }

    @Override
    public void handleTimeout(ClouderaManagerCommandPollerObject pollerObject) {
        throw new ClouderaManagerOperationFailedException("Operation timed out. "
                + "Failed to get newest successful deployClusterClientConfig command ID.");
    }

    @Override
    public String successMessage(ClouderaManagerCommandPollerObject pollerObject) {
        return "Cloudera Manager get deploy cluster client config finished with success result.";
    }
}
