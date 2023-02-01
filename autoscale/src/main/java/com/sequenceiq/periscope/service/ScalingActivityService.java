package com.sequenceiq.periscope.service;

import static com.google.common.collect.Lists.newArrayList;
import static com.sequenceiq.periscope.api.model.ActivityStatus.DOWNSCALE_TRIGGER_SUCCESS;
import static com.sequenceiq.periscope.api.model.ActivityStatus.UPSCALE_TRIGGER_SUCCESS;
import static com.sequenceiq.periscope.service.NotFoundException.notFound;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.periscope.api.model.ActivityStatus;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ScalingActivity;
import com.sequenceiq.periscope.repository.ScalingActivityRepository;

@Service
public class ScalingActivityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScalingActivityService.class);

    @Inject
    private ScalingActivityRepository scalingActivityRepository;

    public ScalingActivity create(Cluster cluster, ActivityStatus activityStatus, String reason, long creationTimestamp) {
        ScalingActivity scalingActivity = new ScalingActivity();
        scalingActivity.setCluster(cluster);
        scalingActivity.setOperationId(UUID.randomUUID().toString());
        scalingActivity.setActivityStatus(activityStatus);
        scalingActivity.setScalingActivityReason(reason);
        scalingActivity.setStartTime(new Date(creationTimestamp));
        LOGGER.info("Creating ScalingActivity with creation timestamp: {} for cluster: {}", creationTimestamp, cluster.getStackCrn());
        return scalingActivityRepository.save(scalingActivity);
    }

    public void updateWithFlowIdAndTriggerStatus(FlowIdentifier flowIdentifier, String operationId, ActivityStatus activityStatus) {
        ScalingActivity scalingActivity = findByOperationId(operationId);
        scalingActivity.setFlowId(flowIdentifier.getPollableId());
        scalingActivity.setActivityStatus(activityStatus);
        LOGGER.info("Updating ScalingActivity: {} with FlowInformation and ActivityStatus: {}", operationId, activityStatus);
        save(scalingActivity);
    }

    public ScalingActivity findByOperationId(String operationId) {
        LOGGER.info("Retrieving ScalingActivity by Id: {}", operationId);
        return scalingActivityRepository.findByOperationId(operationId).orElseThrow(notFound("ScalingActivity", operationId));
    }

    public List<ScalingActivity> findAllForCluster(Long clusterId) {
        LOGGER.info("Retrieving all ScalingActivity for clusterId: {}", clusterId);
        return scalingActivityRepository.findAllByCluster(clusterId);
    }

    public List<Long> findAllIdsOlderThanDurationForCluster(Cluster cluster, long durationInMinutes) {
        Date startTimeBefore = new Date(Instant.now().minus(durationInMinutes, ChronoUnit.MINUTES).toEpochMilli());
        return scalingActivityRepository.findAllByClusterWithStartTimeBefore(cluster.getId(), startTimeBefore)
                .stream().map(ScalingActivity::getId).collect(Collectors.toList());
    }

    public List<ScalingActivity> findAllScalingActivityWithStatusesForCluster(Cluster cluster, Collection<ActivityStatus> statuses) {
        return scalingActivityRepository.findAllByClusterAndInStatuses(cluster.getId(), statuses);
    }

    public List<Long> findAllIdsOfSuccessfulScalingActivityForCluster(Cluster cluster) {
        return findAllScalingActivityWithStatusesForCluster(cluster, newArrayList(UPSCALE_TRIGGER_SUCCESS, DOWNSCALE_TRIGGER_SUCCESS))
                .stream().map(ScalingActivity::getId).collect(Collectors.toList());
    }

    public List<ScalingActivity> findAllByStatusBetweenIntervalForCluster(Cluster cluster, ActivityStatus status, long timestampFrom,
            long timestampUntil) {
        Date startTimeFrom = new Date(timestampFrom);
        Date startTimeUntil = new Date(timestampUntil);
        return scalingActivityRepository.findAllByClusterAndActivityStatusBetweenInterval(cluster.getId(), status, startTimeFrom, startTimeUntil);
    }

    public Set<Long> findAllInStatusesThatStartedBefore(Collection<ActivityStatus> statuses, long durationInHours) {
        Date startTimeFrom = new Date(Instant.now().minus(durationInHours, ChronoUnit.HOURS).toEpochMilli());
        return Sets.newConcurrentHashSet(scalingActivityRepository.findAllIdsInActivityStatusesWithStartTimeBefore(statuses, startTimeFrom));
    }

    public void deleteScalingActivityByIds(Set<Long> activityIds) {
        LOGGER.info("Deleting {} scalingActivities by their Ids", activityIds.size());
        scalingActivityRepository.deleteAllById(activityIds);
    }

    public void deleteScalingActivityForCluster(Long clusterId) {
        LOGGER.info("Deleting all scaling activity for clusterId: {}", clusterId);
        scalingActivityRepository.deleteAllByCluster(clusterId);
    }

    public void save(ScalingActivity scalingActivity) {
        scalingActivityRepository.save(scalingActivity);
    }
}
