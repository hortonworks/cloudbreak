package com.sequenceiq.cloudbreak.cm.polling.task;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;

@Service
public class ClouderaManagerServiceStartListenerTask extends AbstractClouderaManagerCommandCheckerTask<ClouderaManagerCommandPollerObject> {

    @Override
    public void handleTimeout(ClouderaManagerCommandPollerObject toolsResourceApi) {
        throw new ClouderaManagerOperationFailedException("Operation timed out. Failed to start Cloudera Manager services.");
    }

    @Override
    public String successMessage(ClouderaManagerCommandPollerObject toolsResourceApi) {
        return "Cloudera Manager all service start finished with success result.";
    }

    @Override
    protected String getCommandName() {
        return "Service start";
    }
}
