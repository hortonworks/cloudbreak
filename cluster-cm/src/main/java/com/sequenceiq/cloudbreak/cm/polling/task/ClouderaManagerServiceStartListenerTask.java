package com.sequenceiq.cloudbreak.cm.polling.task;

import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollerObject;

public class ClouderaManagerServiceStartListenerTask extends AbstractClouderaManagerCommandCheckerTask<ClouderaManagerPollerObject> {

    public ClouderaManagerServiceStartListenerTask(ClouderaManagerClientFactory clouderaManagerClientFactory) {
        super(clouderaManagerClientFactory);
    }

    @Override
    public void handleTimeout(ClouderaManagerPollerObject toolsResourceApi) {
        throw new ClouderaManagerOperationFailedException("Operation timed out. Failed to start Cloudera Manager services.");
    }

    @Override
    public String successMessage(ClouderaManagerPollerObject toolsResourceApi) {
        return "Cloudera Manager all service start finished with success result.";
    }

    @Override
    protected String getCommandName() {
        return "Service start";
    }
}
