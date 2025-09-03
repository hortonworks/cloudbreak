package com.sequenceiq.periscope.service;

import static com.google.common.collect.Lists.newArrayList;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType.WORKLOAD;
import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;
import static com.sequenceiq.periscope.api.model.ActivityStatus.DOWNSCALE_TRIGGER_SUCCESS;
import static com.sequenceiq.periscope.api.model.ActivityStatus.SCALING_FLOW_IN_PROGRESS;
import static com.sequenceiq.periscope.api.model.ActivityStatus.UPSCALE_TRIGGER_SUCCESS;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.MINUTES;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.AuthorizationEnvironmentCrnProvider;
import com.sequenceiq.authorization.service.AuthorizationResourceCrnProvider;
import com.sequenceiq.cloudbreak.api.model.StatusKind;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.periscope.api.model.ActivityStatus;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ScalingActivity;
import com.sequenceiq.periscope.model.NameOrCrn;
import com.sequenceiq.periscope.monitor.handler.CloudbreakCommunicator;
import com.sequenceiq.periscope.repository.ScalingActivityRepository;

@Service
public class ScalingActivityService implements AuthorizationResourceCrnProvider, AuthorizationEnvironmentCrnProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScalingActivityService.class);

    @Inject
    private ScalingActivityRepository scalingActivityRepository;

    @Inject
    private AutoscaleRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private CloudbreakCommunicator cloudbreakCommunicator;

    @Override
    public AuthorizationResourceType getSupportedAuthorizationResourceType() {
        return AuthorizationResourceType.DATAHUB;
    }

    @Override
    @Retryable(value = NotFoundException.class, maxAttempts = 3, backoff = @Backoff(delay = 5000))
    public String getResourceCrnByResourceName(String clusterName) {
        return clusterService.findOneByStackNameAndTenant(clusterName, restRequestThreadLocalService.getCloudbreakTenant())
                .orElseGet(() -> fetchClusterFromCBUsingName(clusterName)).getStackCrn();
    }

    @Override
    public Optional<String> getEnvironmentCrnByResourceCrn(String resourceCrn) {
        return clusterService.findOneByStackCrnAndTenant(resourceCrn, restRequestThreadLocalService.getCloudbreakTenant()).map(Cluster::getEnvironmentCrn);
    }

    protected Cluster fetchClusterFromCBUsingName(String stackName) {
        String accountId = restRequestThreadLocalService.getCloudbreakTenant();
        return Optional.ofNullable(cloudbreakCommunicator.getAutoscaleClusterByName(stackName, accountId))
                .filter(stack -> WORKLOAD.equals(stack.getStackType()) && stack.getClusterStatus().getStatusKind().equals(StatusKind.FINAL))
                .map(stack -> clusterService.create(stack))
                .orElseThrow(NotFoundException.notFound("cluster", stackName));
    }

    public ScalingActivity create(Cluster cluster, ActivityStatus activityStatus, String reason, long creationTimestamp, long yarnRecommendationTime,
            String yarnRecommendation) {
        ScalingActivity scalingActivity = new ScalingActivity();
        scalingActivity.setCluster(cluster);
        scalingActivity.setOperationId(UUID.randomUUID().toString());
        scalingActivity.setActivityStatus(activityStatus);
        scalingActivity.setScalingActivityReason(reason);
        scalingActivity.setYarnRecommendation(yarnRecommendation);
        scalingActivity.setYarnRecommendationTime(new Date(yarnRecommendationTime));
        scalingActivity.setStartTime(new Date(creationTimestamp));
        LOGGER.info("Creating ScalingActivity with creation timestamp: {} for cluster: {}", creationTimestamp, cluster.getStackCrn());
        return save(scalingActivity);
    }

    public ScalingActivity create(Cluster cluster, ActivityStatus activityStatus, String reason, long creationTimestamp) {
        ScalingActivity scalingActivity = new ScalingActivity();
        scalingActivity.setCluster(cluster);
        scalingActivity.setOperationId(UUID.randomUUID().toString());
        scalingActivity.setActivityStatus(activityStatus);
        scalingActivity.setScalingActivityReason(reason);
        scalingActivity.setStartTime(new Date(creationTimestamp));
        LOGGER.info("Creating ScalingActivity with creation timestamp: {} for cluster: {}", creationTimestamp, cluster.getStackCrn());
        return save(scalingActivity);
    }

    public void update(Long activityId, FlowIdentifier flowIdentifier, ActivityStatus activityStatus, String reason) {
        ScalingActivity scalingActivity = scalingActivityRepository.findById(activityId)
                .orElseThrow(notFound("ScalingActivity", activityId));
        scalingActivity.setFlowId(flowIdentifier.getPollableId());
        scalingActivity.setActivityStatus(activityStatus);
        scalingActivity.setScalingActivityReason(reason);
        LOGGER.info("Updating ScalingActivity: {} with FlowInformation and ActivityStatus: {}", activityId, activityStatus);
        save(scalingActivity);
    }

    public ScalingActivity findByOperationIdAndClusterCrn(String operationId, String clusterCrn) {
        LOGGER.info("Retrieving ScalingActivity by Id: {}", operationId);
        return scalingActivityRepository.findByOperationIdAndClusterCrn(operationId, clusterCrn).orElseThrow(notFound("ScalingActivity", operationId));
    }

    public ScalingActivity findByOperationIdAndClusterName(String operationId, String clusterName) {
        LOGGER.info("Retrieving ScalingActivity by Id: {}", operationId);
        return scalingActivityRepository.findByOperationIdAndClusterName(operationId, clusterName).orElseThrow(notFound("ScalingActivity", operationId));
    }

    public void setEndTime(Long scalingActivityId, long endTimeMillis) {
        scalingActivityRepository.setEndTimeForScalingActivity(scalingActivityId, new Date(endTimeMillis));
    }

    public void setActivityStatusForIds(Collection<Long> ids, ActivityStatus status) {
        scalingActivityRepository.setActivityStatusesInIds(ids, status);
    }

    public void setActivityStatusAndReasonForIds(Collection<Long> ids, ActivityStatus status, String message) {
        scalingActivityRepository.setActivityStatusAndReasonInIds(ids, status, message);
    }

    public void setEndTimes(Map<Long, Long> idsWithCompletedFlows) {
        List<ScalingActivity> activities = newArrayList(scalingActivityRepository.findAllById(idsWithCompletedFlows.keySet()));
        activities.forEach(sc -> sc.setEndTime(new Date(idsWithCompletedFlows.get(sc.getId()))));
        scalingActivityRepository.saveAll(activities);
    }

    public Page<ScalingActivity> findAllInGivenDurationForCluster(NameOrCrn clusterNameOrCrn, long durationInMinutes, Pageable pageable) {
        Date startTimeAfter = Date.from(now().minus(durationInMinutes, MINUTES));
        return clusterNameOrCrn.hasName() ?
                scalingActivityRepository.findAllByClusterNameWithStartTimeAfter(clusterNameOrCrn.getName(), startTimeAfter, pageable) :
                scalingActivityRepository.findAllByClusterCrnWithStartTimeAfter(clusterNameOrCrn.getCrn(), startTimeAfter, pageable);
    }

    public Page<ScalingActivity> findAllByFailedStatusesInGivenDuration(NameOrCrn clusterNameOrCrn, long durationInMinutes, Pageable pageable) {
        Date startTimeAfter = Date.from(now().minus(durationInMinutes, MINUTES));
        return clusterNameOrCrn.hasName() ?
                scalingActivityRepository.findAllByClusterNameAndInStatusesWithTimeAfter(clusterNameOrCrn.getName(),
                        newArrayList(ActivityStatus.DOWNSCALE_TRIGGER_FAILED, ActivityStatus.UPSCALE_TRIGGER_FAILED, ActivityStatus.METRICS_COLLECTION_FAILED,
                                ActivityStatus.SCALING_FLOW_FAILED), startTimeAfter, pageable) :
                scalingActivityRepository.findAllByClusterCrnAndInStatusesWithTimeAfter(clusterNameOrCrn.getCrn(),
                        newArrayList(ActivityStatus.DOWNSCALE_TRIGGER_FAILED, ActivityStatus.UPSCALE_TRIGGER_FAILED, ActivityStatus.METRICS_COLLECTION_FAILED,
                                ActivityStatus.SCALING_FLOW_FAILED), startTimeAfter, pageable);
    }

    public Page<ScalingActivity> findAllByFailedStatusesInTimeRangeForCluster(NameOrCrn clusterNameOrCrn,
            long timestampFrom, long timestampUntil, Pageable pageable) {
        Date startTimeUntil = Date.from(Instant.ofEpochMilli(timestampUntil));
        Date startTimeFrom = Date.from(Instant.ofEpochMilli(timestampFrom));
        return clusterNameOrCrn.hasName() ?
                scalingActivityRepository.findAllByClusterNameAndInStatusesBetweenInterval(clusterNameOrCrn.getName(),
                        newArrayList(ActivityStatus.DOWNSCALE_TRIGGER_FAILED, ActivityStatus.UPSCALE_TRIGGER_FAILED, ActivityStatus.METRICS_COLLECTION_FAILED,
                                ActivityStatus.SCALING_FLOW_FAILED), startTimeFrom, startTimeUntil, pageable) :
                scalingActivityRepository.findAllByClusterCrnAndInStatusesBetweenInterval(clusterNameOrCrn.getCrn(),
                        newArrayList(ActivityStatus.DOWNSCALE_TRIGGER_FAILED, ActivityStatus.UPSCALE_TRIGGER_FAILED, ActivityStatus.METRICS_COLLECTION_FAILED,
                                ActivityStatus.SCALING_FLOW_FAILED), startTimeFrom, startTimeUntil, pageable);
    }

    public Page<ScalingActivity> findAllInTimeRangeForCluster(NameOrCrn clusterNameOrCrn, long timestampFrom, long timestampUntil, Pageable pageable) {
        Date startTimeUntil = Date.from(Instant.ofEpochMilli(timestampUntil));
        Date startTimeFrom = Date.from(Instant.ofEpochMilli(timestampFrom));
        return clusterNameOrCrn.hasName() ?
                scalingActivityRepository.findAllByClusterNameBetweenInterval(clusterNameOrCrn.getName(), startTimeFrom, startTimeUntil, pageable) :
                scalingActivityRepository.findAllByClusterCrnBetweenInterval(clusterNameOrCrn.getCrn(), startTimeFrom, startTimeUntil, pageable);
    }

    public List<ScalingActivity> findAllByIds(Collection<Long> ids) {
        return newArrayList(scalingActivityRepository.findAllById(ids));
    }

    public Long countAllActivitiesInStatuses(Collection<ActivityStatus> statuses) {
        return scalingActivityRepository.countAllInActivityStatuses(statuses);
    }

    public Long countAllActivities() {
        return scalingActivityRepository.count();
    }

    public Set<Long> findAllIdsOfSuccessfulAndInProgressScalingActivity() {
        return Sets.newConcurrentHashSet(scalingActivityRepository.findAllIdsInActivityStatuses(newArrayList(UPSCALE_TRIGGER_SUCCESS,
                DOWNSCALE_TRIGGER_SUCCESS, SCALING_FLOW_IN_PROGRESS), Sort.by("startTime")));
    }

    public Set<Long> findAllInStatusesThatStartedBefore(Collection<ActivityStatus> statuses, long durationInHours) {
        Date startTimeFrom = new Date(now().minus(durationInHours, ChronoUnit.HOURS).toEpochMilli());
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

    public ScalingActivity save(ScalingActivity scalingActivity) {
        return scalingActivityRepository.save(scalingActivity);
    }
}
