package com.sequenceiq.periscope.controller;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.periscope.api.endpoint.v1.DistroXAutoScaleScalingActivityV1Endpoint;
import com.sequenceiq.periscope.api.model.DistroXAutoscaleScalingActivityResponse;
import com.sequenceiq.periscope.converter.DistroXAutoscaleScalingActivityResponseConverter;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.model.NameOrCrn;
import com.sequenceiq.periscope.service.ScalingTriggerService;

@Controller
public class DistroXAutoScaleScalingActivityV1Controller implements DistroXAutoScaleScalingActivityV1Endpoint {

    @Inject
    private DistroXAutoscaleScalingActivityResponseConverter distroXAutoscaleScalingActivityResponseConverter;

    @Inject
    private ScalingTriggerService scalingTriggerService;

    @Inject
    private AutoScaleClusterCommonService asClusterCommonService;

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.SCALE_DATAHUB)
    public List<DistroXAutoscaleScalingActivityResponse> getAllScalingTriggersByClusterName(@ResourceName String clusterName) {
        Cluster cluster = asClusterCommonService.getClusterByCrnOrName(NameOrCrn.ofName(clusterName));
        return distroXAutoscaleScalingActivityResponseConverter.convertAllToJson(scalingTriggerService.findAllForCluster(cluster.getId()));
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.SCALE_DATAHUB)
    public List<DistroXAutoscaleScalingActivityResponse> getAllScalingTriggersByClusterCrn(@ResourceCrn String clusterCrn) {
        Cluster cluster = asClusterCommonService.getClusterByCrnOrName(NameOrCrn.ofCrn(clusterCrn));
        return distroXAutoscaleScalingActivityResponseConverter.convertAllToJson(scalingTriggerService.findAllForCluster(cluster.getId()));
    }

    @Override
    @DisableCheckPermissions
    public DistroXAutoscaleScalingActivityResponse getParticularScalingTrigger(String triggerCrn) {
        return distroXAutoscaleScalingActivityResponseConverter.convert(scalingTriggerService.findByCrn(triggerCrn));
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.SCALE_DATAHUB)
    public List<DistroXAutoscaleScalingActivityResponse> getScalingTriggersInDurationByClusterName(@ResourceName String clusterName, long durationInMinutes) {
        Cluster cluster = asClusterCommonService.getClusterByCrnOrName(NameOrCrn.ofName(clusterName));
        return distroXAutoscaleScalingActivityResponseConverter.convertAllToJson(scalingTriggerService.findAllInGivenIntervalForCluster(cluster, durationInMinutes));
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.SCALE_DATAHUB)
    public List<DistroXAutoscaleScalingActivityResponse> getScalingTriggersInDurationByClusterCrn(@ResourceCrn String clusterCrn, long durationInMinutes) {
        Cluster cluster = asClusterCommonService.getClusterByCrnOrName(NameOrCrn.ofCrn(clusterCrn));
        return distroXAutoscaleScalingActivityResponseConverter.convertAllToJson(scalingTriggerService.findAllInGivenIntervalForCluster(cluster, durationInMinutes));
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.SCALE_DATAHUB)
    public List<DistroXAutoscaleScalingActivityResponse> getFailedScalingTriggersInGivenDurationByClusterName(@ResourceName String clusterName, long durationInMinutes) {
        Cluster cluster = asClusterCommonService.getClusterByCrnOrName(NameOrCrn.ofName(clusterName));
        return distroXAutoscaleScalingActivityResponseConverter.convertAllToJson(scalingTriggerService.findAllWithTriggerFailed(cluster, durationInMinutes));
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.SCALE_DATAHUB)
    public List<DistroXAutoscaleScalingActivityResponse> getFailedScalingTriggersInGivenDurationByClusterCrn(@ResourceCrn String clusterCrn, long durationInMinutes) {
        Cluster cluster = asClusterCommonService.getClusterByCrnOrName(NameOrCrn.ofCrn(clusterCrn));
        return distroXAutoscaleScalingActivityResponseConverter.convertAllToJson(scalingTriggerService.findAllWithTriggerFailed(cluster, durationInMinutes));
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.SCALE_DATAHUB)
    public List<DistroXAutoscaleScalingActivityResponse> getScalingTriggersInTimeRangeByClusterName(@ResourceName String clusterName, long startTime, long endTime) {
        Cluster cluster = asClusterCommonService.getClusterByCrnOrName(NameOrCrn.ofName(clusterName));
        return distroXAutoscaleScalingActivityResponseConverter.convertAllToJson(scalingTriggerService.findAllInTimeRangeForCluster(cluster, startTime, endTime));
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.SCALE_DATAHUB)
    public List<DistroXAutoscaleScalingActivityResponse> getScalingTriggersInTimeRangeByClusterCrn(@ResourceCrn String clusterCrn, long startTime, long endTime) {
        Cluster cluster = asClusterCommonService.getClusterByCrnOrName(NameOrCrn.ofCrn(clusterCrn));
        return distroXAutoscaleScalingActivityResponseConverter.convertAllToJson(scalingTriggerService.findAllInTimeRangeForCluster(cluster, startTime, endTime));
    }
}