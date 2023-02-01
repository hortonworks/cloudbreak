package com.sequenceiq.periscope.controller;

import static java.time.temporal.ChronoUnit.MINUTES;

import java.time.Instant;
import java.util.List;

import javax.inject.Inject;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareParam;
import com.sequenceiq.periscope.api.endpoint.v1.DistroXAutoScaleScalingActivityV1Endpoint;
import com.sequenceiq.periscope.api.model.DistroXAutoscaleScalingActivityResponse;
import com.sequenceiq.periscope.converter.DistroXAutoscaleScalingActivityResponseConverter;
import com.sequenceiq.periscope.model.NameOrCrn;
import com.sequenceiq.periscope.service.ScalingActivityService;

@Controller
public class DistroXAutoScaleScalingActivityV1Controller implements DistroXAutoScaleScalingActivityV1Endpoint {

    public static final Integer MINUTES_TO_SUBTRACT = 60;

    @Inject
    private DistroXAutoscaleScalingActivityResponseConverter distroXAutoscaleScalingActivityResponseConverter;

    @Inject
    private ScalingActivityService scalingActivityService;

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_DATAHUB)
    public DistroXAutoscaleScalingActivityResponse getScalingActivityUsingOperationId(@ResourceCrn @TenantAwareParam String clusterCrn, String operationId) {
        return distroXAutoscaleScalingActivityResponseConverter.convert(scalingActivityService.findByOperationIdAndClusterCrn(operationId, clusterCrn));
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_DATAHUB)
    public List<DistroXAutoscaleScalingActivityResponse> getScalingActivitiesInDurationByClusterName(@ResourceName String clusterName,
            long durationInMinutes, Integer pageNumber, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("startTime").descending());
        return distroXAutoscaleScalingActivityResponseConverter
                .convertAllToJson(scalingActivityService.findAllInGivenDurationForCluster(NameOrCrn.ofName(clusterName), durationInMinutes, pageable));
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_DATAHUB)
    public List<DistroXAutoscaleScalingActivityResponse> getScalingActivitiesInDurationByClusterCrn(@ResourceCrn String clusterCrn,
            long durationInMinutes, Integer pageNumber, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("startTime").descending());
        return distroXAutoscaleScalingActivityResponseConverter
                .convertAllToJson(scalingActivityService.findAllInGivenDurationForCluster(NameOrCrn.ofCrn(clusterCrn), durationInMinutes, pageable));
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_DATAHUB)
    public List<DistroXAutoscaleScalingActivityResponse> getFailedScalingActivitiesInGivenDurationByClusterName(@ResourceName String clusterName,
            long durationInMinutes, Integer pageNumber, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("startTime").descending());
        return distroXAutoscaleScalingActivityResponseConverter
                .convertAllToJson(scalingActivityService.findAllByFailedStatusesInGivenDuration(NameOrCrn.ofName(clusterName), durationInMinutes, pageable));
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_DATAHUB)
    public List<DistroXAutoscaleScalingActivityResponse> getFailedScalingActivitiesInGivenDurationByClusterCrn(@ResourceCrn String clusterCrn,
            long durationInMinutes, Integer pageNumber, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("startTime").descending());
        return distroXAutoscaleScalingActivityResponseConverter
                .convertAllToJson(scalingActivityService.findAllByFailedStatusesInGivenDuration(NameOrCrn.ofCrn(clusterCrn), durationInMinutes, pageable));
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_DATAHUB)
    public List<DistroXAutoscaleScalingActivityResponse> getScalingActivitiesBetweenIntervalByClusterName(@ResourceName String clusterName,
            long startTimeFromInEpochMilliSec, long startTimeUntilInEpochMilliSec, Integer pageNumber, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("startTime").descending());
        if (startTimeFromInEpochMilliSec == 0) {
            startTimeFromInEpochMilliSec = Instant.now().minus(MINUTES_TO_SUBTRACT, MINUTES).toEpochMilli();
        }
        if (startTimeUntilInEpochMilliSec == 0) {
            startTimeUntilInEpochMilliSec = Instant.now().toEpochMilli();
        }
        return distroXAutoscaleScalingActivityResponseConverter
                .convertAllToJson(scalingActivityService.findAllInTimeRangeForCluster(NameOrCrn.ofName(clusterName),
                        startTimeFromInEpochMilliSec, startTimeUntilInEpochMilliSec, pageable));
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_DATAHUB)
    public List<DistroXAutoscaleScalingActivityResponse> getScalingActivitiesBetweenIntervalByClusterCrn(@ResourceCrn String clusterCrn,
            long startTimeFromInEpochMilliSec, long startTimeUntilInEpochMilliSec, Integer pageNumber, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("startTime").descending());
        if (startTimeFromInEpochMilliSec == 0) {
            startTimeFromInEpochMilliSec = Instant.now().minus(MINUTES_TO_SUBTRACT, MINUTES).toEpochMilli();
        }
        if (startTimeUntilInEpochMilliSec == 0) {
            startTimeUntilInEpochMilliSec = Instant.now().toEpochMilli();
        }
        return distroXAutoscaleScalingActivityResponseConverter
                .convertAllToJson(scalingActivityService.findAllInTimeRangeForCluster(NameOrCrn.ofCrn(clusterCrn),
                        startTimeFromInEpochMilliSec, startTimeUntilInEpochMilliSec, pageable));
    }
}