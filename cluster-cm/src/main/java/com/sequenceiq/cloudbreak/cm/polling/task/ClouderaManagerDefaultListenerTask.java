package com.sequenceiq.cloudbreak.cm.polling.task;

import com.sequenceiq.cloudbreak.cluster.service.ClusterEventService;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;

public class ClouderaManagerDefaultListenerTask extends AbstractClouderaManagerCommandCheckerTask<ClouderaManagerCommandPollerObject> {

    private String commandName;

    public ClouderaManagerDefaultListenerTask(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            ClusterEventService clusterEventService, String commandName) {
        super(clouderaManagerApiPojoFactory, clusterEventService);
        this.commandName = commandName;
    }

    @Override
    protected String getCommandName() {
        return commandName;
    }
}
