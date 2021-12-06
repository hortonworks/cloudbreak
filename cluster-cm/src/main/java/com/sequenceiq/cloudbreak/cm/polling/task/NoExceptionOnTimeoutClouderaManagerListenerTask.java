package com.sequenceiq.cloudbreak.cm.polling.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cluster.service.ClusterEventService;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;

public class NoExceptionOnTimeoutClouderaManagerListenerTask extends ClouderaManagerDefaultListenerTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(NoExceptionOnTimeoutClouderaManagerListenerTask.class);

    public NoExceptionOnTimeoutClouderaManagerListenerTask(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            ClusterEventService clusterEventService, String commandDescription) {
        super(clouderaManagerApiPojoFactory, clusterEventService, commandDescription);
    }

    @Override
    public void handleTimeout(ClouderaManagerCommandPollerObject pollerObject) {
        LOGGER.info("Timeout ignored for polling of command {}, commandDescription={}.", pollerObject.getId(), getCommandName());
    }
}
