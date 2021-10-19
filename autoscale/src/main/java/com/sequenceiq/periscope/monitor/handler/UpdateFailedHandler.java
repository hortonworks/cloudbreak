package com.sequenceiq.periscope.monitor.handler;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.api.model.ScalingStatus;
import com.sequenceiq.periscope.common.MessageCode;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.monitor.event.UpdateFailedEvent;
import com.sequenceiq.periscope.service.AltusMachineUserService;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.HistoryService;
import com.sequenceiq.periscope.utils.LoggingUtils;

@Component
public class UpdateFailedHandler implements ApplicationListener<UpdateFailedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateFailedHandler.class);

    private static final int RETRY_THRESHOLD = 5;

    @Inject
    private ClusterService clusterService;

    @Inject
    private HistoryService historyService;

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private AltusMachineUserService altusMachineUserService;

    private final Map<Long, Integer> updateFailures = new ConcurrentHashMap<>();

    private final Map<Long, Integer> forbiddenFailures = new ConcurrentHashMap<>();

    @Override
    public void onApplicationEvent(UpdateFailedEvent event) {
        long autoscaleClusterId = event.getClusterId();
        Cluster cluster = clusterService.findById(autoscaleClusterId);
        LoggingUtils.buildMdcContext(cluster);

        Integer failed = Optional.ofNullable(updateFailures.get(autoscaleClusterId))
                .map(failedCount -> failedCount + 1)
                .orElse(1);
        if (failed < RETRY_THRESHOLD) {
            updateFailures.put(autoscaleClusterId, failed);
            LOGGER.debug("Increased failed count '{}' for cluster '{}'", failed, cluster.getStackCrn());

            if (event.getCausedBy() != null && (event.getCausedBy() instanceof ForbiddenException)) {
                Integer forbiddencount = getForbiddenFailureCount(autoscaleClusterId) + 1;
                forbiddenFailures.put(autoscaleClusterId, forbiddencount);
                LOGGER.debug("Increased forbidden count '{}' for cluster '{}'", forbiddencount, cluster.getStackCrn());
            }
        } else {
            if (getForbiddenFailureCount(autoscaleClusterId) >= (RETRY_THRESHOLD - 1)) {
                LOGGER.info("Forbidden(403) failures exceeds max threshold '{}', reinitializaing polling machine user " +
                        " for cluster '{}'.", RETRY_THRESHOLD, cluster.getStackCrn());
                altusMachineUserService.initializeMachineUserForEnvironment(cluster);
            }
            suspendCluster(cluster);
            updateFailures.remove(autoscaleClusterId);
            forbiddenFailures.remove(autoscaleClusterId);
            historyService.createEntry(ScalingStatus.TRIGGER_FAILED, messagesService.getMessage(MessageCode.AUTOSCALING_TRIGGER_FAILURE), cluster);
            LOGGER.debug("Suspended cluster monitoring for cluster '{}' due to failing update attempts", cluster.getStackCrn());
        }
    }

    private Integer getForbiddenFailureCount(long autoscaleClusterId) {
        return Optional.ofNullable(forbiddenFailures.get(autoscaleClusterId)).orElse(0);
    }

    private void suspendCluster(Cluster cluster) {
        if (!cluster.getState().equals(ClusterState.SUSPENDED)) {
            clusterService.setState(cluster.getId(), ClusterState.SUSPENDED);
        }
    }
}