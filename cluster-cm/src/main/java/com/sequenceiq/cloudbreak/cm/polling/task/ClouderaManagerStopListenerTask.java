package com.sequenceiq.cloudbreak.cm.polling.task;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;

@Service
public class ClouderaManagerStopListenerTask extends AbstractClouderaManagerCommandCheckerTask<ClouderaManagerCommandPollerObject> {

    @Override
    public void handleTimeout(ClouderaManagerCommandPollerObject toolsResourceApi) {
        throw new ClouderaManagerOperationFailedException("Operation timed out. Failed to stop Cloudera Manager services.");
    }

    @Override
    public String successMessage(ClouderaManagerCommandPollerObject toolsResourceApi) {
        return "Cloudera Manager all service stop finished with success result.";
    }

    @Override
    protected String getCommandName() {
        return "Stop";
    }
}
