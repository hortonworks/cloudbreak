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
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.periscope.api.model.ActivityStatus;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ScalingActivity;
import com.sequenceiq.periscope.repository.ScalingActivityRepository;

@Service
public class ScalingActivityService {

    @Inject
    private ScalingActivityRepository scalingActivityRepository;

    @Inject
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    public ScalingActivity create(Cluster cluster, ActivityStatus activityStatus, String reason, long creationTimestamp) {
        ScalingActivity scalingActivity = new ScalingActivity();
        scalingActivity.setCluster(cluster);
        scalingActivity.setActivityCrn(createScalingActivityCrn(Crn.safeFromString(cluster.getClusterPertain().getUserCrn()).getAccountId()));
        scalingActivity.setActivityStatus(activityStatus);
        scalingActivity.setScalingActivityReason(reason);
        scalingActivity.setStartTime(new Date(creationTimestamp));
        return scalingActivityRepository.save(scalingActivity);
    }

    public void updateWithFlowIdAndTriggerStatus(FlowIdentifier flowIdentifier, String activityCrn, ActivityStatus activityStatus) {
        ScalingActivity scalingActivity = findByCrn(activityCrn);
        scalingActivity.setFlowId(flowIdentifier.getPollableId());
        scalingActivity.setActivityStatus(activityStatus);
        save(scalingActivity);
    }

    public ScalingActivity findByCrn(String activityCrn) {
        return scalingActivityRepository.findByActivityCrn(activityCrn).orElseThrow(notFound("ScalingActivity", activityCrn));
    }

    public List<ScalingActivity> findAllForCluster(Long clusterId) {
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

    public void deleteScalingActivityForCluster(Long clusterId) {
        scalingActivityRepository.deleteAllByCluster(clusterId);
    }

    public void deleteScalingActivity(Set<Long> activityIds) {
        scalingActivityRepository.deleteAllById(activityIds);
    }

    public void save(ScalingActivity scalingActivity) {
        scalingActivityRepository.save(scalingActivity);
    }

    private String createScalingActivityCrn(String accountId) {
        return regionAwareCrnGenerator.generateCrnStringWithUuid(CrnResourceDescriptor.SCALING_ACTIVITY, accountId);
    }
}
