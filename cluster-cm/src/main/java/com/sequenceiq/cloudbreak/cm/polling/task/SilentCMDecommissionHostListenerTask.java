package com.sequenceiq.cloudbreak.cm.polling.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cluster.service.ClusterEventService;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;

public class SilentCMDecommissionHostListenerTask extends ClouderaManagerDefaultListenerTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(SilentCMDecommissionHostListenerTask.class);

    public SilentCMDecommissionHostListenerTask(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            ClusterEventService clusterEventService) {
        super(clouderaManagerApiPojoFactory, clusterEventService, "Decommission host");
    }

    @Override
    public void handleTimeout(ClouderaManagerCommandPollerObject toolsResourceApi) {
        LOGGER.info("Timeout ignored for polling of command {}.", toolsResourceApi.getId());
    }
}
