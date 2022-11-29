package com.sequenceiq.periscope.service;

import static com.google.common.collect.Lists.newArrayList;
import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;
import static com.sequenceiq.periscope.api.model.TriggerStatus.DOWNSCALE_TRIGGER_SUCCESS;
import static com.sequenceiq.periscope.api.model.TriggerStatus.UPSCALE_TRIGGER_SUCCESS;

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
import com.sequenceiq.periscope.api.model.TriggerStatus;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ScalingTrigger;
import com.sequenceiq.periscope.repository.ScalingTriggerRepository;

@Service
public class ScalingTriggerService {

    @Inject
    private ScalingTriggerRepository scalingTriggerRepository;

    @Inject
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    public ScalingTrigger create(Cluster cluster, TriggerStatus triggerStatus, String yarnMetricsResponse, long creationTimestamp) {
        ScalingTrigger scalingTrigger = new ScalingTrigger();
        scalingTrigger.setCluster(cluster);
        scalingTrigger.setTriggerCrn(createTriggerCrn(Crn.safeFromString(cluster.getClusterPertain().getUserCrn()).getAccountId()));
        scalingTrigger.setTriggerStatus(triggerStatus);
        scalingTrigger.setTriggerReason(yarnMetricsResponse);
        scalingTrigger.setStartTime(new Date(creationTimestamp));
        return scalingTriggerRepository.save(scalingTrigger);
    }

    public void updateWithFlowIdAndTriggerStatus(FlowIdentifier flowIdentifier, String triggerCrn, TriggerStatus triggerStatus) {
        ScalingTrigger scalingTrigger = findByCrn(triggerCrn);
        scalingTrigger.setFlowId(flowIdentifier.getPollableId());
        scalingTrigger.setTriggerStatus(triggerStatus);
        save(scalingTrigger);
    }

    public ScalingTrigger findByCrn(String triggerCrn) {
        return scalingTriggerRepository.findByTriggerCrn(triggerCrn).orElseThrow(notFound("ScalingTrigger", triggerCrn));
    }

    public List<ScalingTrigger> findAllForCluster(Long clusterId) {
        return scalingTriggerRepository.findAllByCluster(clusterId);
    }

    public List<Long> findAllIdsOlderThanDurationForCluster(Cluster cluster, long durationInMinutes) {
        Date startTimeBefore = new Date(Instant.now().minus(durationInMinutes, ChronoUnit.MINUTES).toEpochMilli());
        return scalingTriggerRepository.findAllByClusterWithStartTimeBefore(cluster.getId(), startTimeBefore)
                .stream().map(ScalingTrigger::getId).collect(Collectors.toList());
    }

    public List<ScalingTrigger> findAllScalingTriggersWithStatusesForCluster(Cluster cluster, Collection<TriggerStatus> statuses) {
        return scalingTriggerRepository.findAllByClusterAndInTriggerStatuses(cluster.getId(), statuses);
    }

    public List<Long> findAllIdsOfSuccessfulScalingTriggersForCluster(Cluster cluster) {
        return findAllScalingTriggersWithStatusesForCluster(cluster, newArrayList(UPSCALE_TRIGGER_SUCCESS, DOWNSCALE_TRIGGER_SUCCESS))
                .stream().map(ScalingTrigger::getId).collect(Collectors.toList());
    }

    public List<ScalingTrigger> findAllByTriggerStatusBetweenIntervalForCluster(Cluster cluster, TriggerStatus status, long timestampFrom,
            long timestampUntil) {
        Date startTimeFrom = new Date(timestampFrom);
        Date startTimeUntil = new Date(timestampUntil);
        return scalingTriggerRepository.findAllByClusterAndTriggerStatusBetweenInterval(cluster.getId(), status, startTimeFrom, startTimeUntil);
    }

    public void deleteScalingTriggers(Set<Long> triggerIds) {
        scalingTriggerRepository.deleteAllById(triggerIds);
    }

    public void save(ScalingTrigger scalingTrigger) {
        scalingTriggerRepository.save(scalingTrigger);
    }

    private String createTriggerCrn(String accountId) {
        return regionAwareCrnGenerator.generateCrnStringWithUuid(CrnResourceDescriptor.SCALING_TRIGGER, accountId);
    }
}
