package com.sequenceiq.cloudbreak.cm.polling.task;

import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;

public class ClouderaManagerStopManagementServiceListenerTask extends AbstractClouderaManagerCommandCheckerTask<ClouderaManagerCommandPollerObject> {

    @Override
    public void handleTimeout(ClouderaManagerCommandPollerObject toolsResourceApi) {
        throw new ClouderaManagerOperationFailedException("Operation timed out. Failed to stop management service.");
    }

    @Override
    public String successMessage(ClouderaManagerCommandPollerObject toolsResourceApi) {
        return "Successfully stopped management service.";
    }

    @Override
    protected String getCommandName() {
        return "Stop Cloudera Manager management service";
    }
}
