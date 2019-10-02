package com.sequenceiq.cloudbreak.cm.polling.task;

import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollerObject;

public class ClouderaManagerStartManagementServiceListenerTask extends AbstractClouderaManagerCommandCheckerTask<ClouderaManagerPollerObject> {

    public ClouderaManagerStartManagementServiceListenerTask(ClouderaManagerClientFactory clouderaManagerClientFactory) {
        super(clouderaManagerClientFactory);
    }

    @Override
    public void handleTimeout(ClouderaManagerPollerObject toolsResourceApi) {
        throw new ClouderaManagerOperationFailedException("Operation timed out. Failed to start management service.");
    }

    @Override
    public String successMessage(ClouderaManagerPollerObject toolsResourceApi) {
        return "Successfully started management service.";
    }

    @Override
    protected String getCommandName() {
        return "Start Cloudera Manager management service";
    }
}
