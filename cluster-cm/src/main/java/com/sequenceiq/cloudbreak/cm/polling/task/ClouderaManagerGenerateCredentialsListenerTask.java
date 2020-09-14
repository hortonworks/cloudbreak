package com.sequenceiq.cloudbreak.cm.polling.task;

import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

public class ClouderaManagerGenerateCredentialsListenerTask extends AbstractClouderaManagerCommandCheckerTask<ClouderaManagerCommandPollerObject> {

    public ClouderaManagerGenerateCredentialsListenerTask(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            CloudbreakEventService cloudbreakEventService) {
        super(clouderaManagerApiPojoFactory, cloudbreakEventService);
    }

    @Override
    public void handleTimeout(ClouderaManagerCommandPollerObject toolsResourceApi) {
        throw new ClouderaManagerOperationFailedException("Operation timed out. Failed to generate credentials.");
    }

    @Override
    public String successMessage(ClouderaManagerCommandPollerObject toolsResourceApi) {
        return "Successfully generated credentials.";
    }

    @Override
    protected String getCommandName() {
        return "Generate Credentials";
    }
}
