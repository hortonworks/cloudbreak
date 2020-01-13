package com.sequenceiq.cloudbreak.cm.polling.task;

import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollerObject;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

public class ClouderaManagerApplyHostTemplateListenerTask extends AbstractClouderaManagerCommandCheckerTask<ClouderaManagerPollerObject> {

    public ClouderaManagerApplyHostTemplateListenerTask(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            CloudbreakEventService cloudbreakEventService) {
        super(clouderaManagerApiPojoFactory, cloudbreakEventService);
    }

    @Override
    public void handleTimeout(ClouderaManagerPollerObject toolsResourceApi) {
        throw new ClouderaManagerOperationFailedException("Operation timed out. Failed to apply host template.");
    }

    @Override
    public String successMessage(ClouderaManagerPollerObject toolsResourceApi) {
        return "Cloudera Manager applied host template finished with success result.";
    }

    @Override
    protected String getCommandName() {
        return "Apply host template";
    }
}
