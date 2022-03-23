package com.sequenceiq.cloudbreak.cm.polling.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cluster.service.ClusterEventService;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollerObject;

public class ClouderaManagerStatusListenerTask extends ClouderaManagerStartupListenerTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerStatusListenerTask.class);

    public ClouderaManagerStatusListenerTask(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory, ClusterEventService clusterEventService) {
        super(clouderaManagerApiPojoFactory, clusterEventService);
    }

    @Override
    public void handleTimeout(ClouderaManagerPollerObject clouderaManagerPollerObject) {
        LOGGER.info("Timeout ignored for polling of CM status of stack id {}, command={}.", clouderaManagerPollerObject.getStack().getId(), getPollingName());
    }
}
