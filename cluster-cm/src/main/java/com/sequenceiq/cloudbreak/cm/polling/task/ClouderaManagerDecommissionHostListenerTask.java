package com.sequenceiq.cloudbreak.cm.polling.task;

import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;

public class ClouderaManagerDecommissionHostListenerTask extends AbstractClouderaManagerCommandCheckerTask<ClouderaManagerCommandPollerObject> {

    @Override
    public void handleTimeout(ClouderaManagerCommandPollerObject toolsResourceApi) {
        throw new ClouderaManagerOperationFailedException("Operation timed out. Failed to decommission host.");
    }

    @Override
    public String successMessage(ClouderaManagerCommandPollerObject toolsResourceApi) {
        return "Cloudera Manager host decommission finished with success result.";
    }

    @Override
    protected String getCommandName() {
        return "Decommission host";
    }
}
