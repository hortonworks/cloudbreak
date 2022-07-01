package com.sequenceiq.cloudbreak.cm.polling.task;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.sequenceiq.cloudbreak.cluster.service.ClusterEventService;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.polling.AbsolutTimeBasedTimeoutChecker;

public class ClouderaManagerDecommissionWarningListenerTask extends ClouderaManagerDefaultListenerTask {

    private static final long POLL_FOR_90_MINUTES = TimeUnit.MINUTES.toSeconds(90);

    private AbsolutTimeBasedTimeoutChecker warningTimeoutChecker = new AbsolutTimeBasedTimeoutChecker(POLL_FOR_90_MINUTES);

    private boolean warningSent;

    public ClouderaManagerDecommissionWarningListenerTask(
            ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory, ClusterEventService clusterEventService, String commandName) {
        super(clouderaManagerApiPojoFactory, clusterEventService, commandName);
    }

    @Override
    public void sendWarningTimeoutEventIfNecessary(ClouderaManagerCommandPollerObject pollerObject) {
        if (pollerObject.getId() != null && warningTimeoutChecker.checkTimeout() && !warningSent) {
            getClusterEventService().fireClusterManagerEvent(pollerObject.getStack(),
                    ResourceEvent.CLUSTER_CM_COMMAND_TIMEOUT_WARNING, getCommandName(), Optional.of(pollerObject.getId()));
            warningSent = true;
        }
    }
}
