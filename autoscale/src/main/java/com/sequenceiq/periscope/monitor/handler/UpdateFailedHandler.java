package com.sequenceiq.periscope.monitor.handler;

import static com.sequenceiq.periscope.api.model.ActivityStatus.METRICS_COLLECTION_FAILED;
import static com.sequenceiq.periscope.common.MessageCode.AUTOSCALE_YARN_RECOMMENDATION_FAILED;
import static java.time.Instant.now;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.api.model.ScalingStatus;
import com.sequenceiq.periscope.common.MessageCode;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ScalingActivity;
import com.sequenceiq.periscope.domain.UpdateFailedDetails;
import com.sequenceiq.periscope.monitor.event.UpdateFailedEvent;
import com.sequenceiq.periscope.service.AltusMachineUserService;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.HistoryService;
import com.sequenceiq.periscope.service.ScalingActivityService;
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

    @Inject
    private ScalingActivityService scalingActivityService;

    private final Map<Long, Integer> updateFailures = new ConcurrentHashMap<>();

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
            if (event.getCausedBy() instanceof ForbiddenException && event.isWithMachineUser()) {
                Integer forbiddencount = getForbiddenFailureCount(cluster) + 1;
                UpdateFailedDetails updateFailedDetails = new UpdateFailedDetails(event.getLastExceptionTimestamp(), forbiddencount.longValue(),
                        event.isWithMachineUser());
                clusterService.setUpdateFailedDetails(cluster.getId(), updateFailedDetails);
                LOGGER.debug("Increased forbidden count '{}' for cluster '{}' with failure details: {}, and machine user: {}", forbiddencount,
                        cluster.getStackCrn(), updateFailedDetails, cluster.getMachineUserCrn());
            }
        } else {
            if (getForbiddenFailureCount(cluster) > 0) {
                LOGGER.info("Forbidden(403) failure(s) are present for cluster: {}, re-initialising polling machine user", cluster.getStackCrn());
                altusMachineUserService.initializeMachineUserForEnvironment(cluster);
                clusterService.setUpdateFailedDetails(cluster.getId(), null);
            }
            suspendCluster(cluster);
            updateFailures.remove(autoscaleClusterId);
            ScalingActivity activity = scalingActivityService.create(cluster, METRICS_COLLECTION_FAILED,
                    messagesService.getMessageWithArgs(AUTOSCALE_YARN_RECOMMENDATION_FAILED, event.getCausedBy()), now().toEpochMilli());
            scalingActivityService.setEndTime(activity.getId(), now().toEpochMilli());
            historyService.createEntry(ScalingStatus.TRIGGER_FAILED, messagesService.getMessageWithArgs(MessageCode.AUTOSCALING_TRIGGER_FAILURE,
                    event.getPollingUserCrn()), cluster);
            LOGGER.debug("Suspended cluster monitoring for cluster '{}' due to failing update attempts", cluster.getStackCrn());
        }
    }

    private Integer getForbiddenFailureCount(Cluster cluster) {
        return cluster.getUpdateFailedDetails() != null ? cluster.getUpdateFailedDetails().getExceptionCount().intValue() : 0;
    }

    private void suspendCluster(Cluster cluster) {
        if (!cluster.getState().equals(ClusterState.SUSPENDED)) {
            clusterService.setState(cluster.getId(), ClusterState.SUSPENDED);
        }
    }
}
