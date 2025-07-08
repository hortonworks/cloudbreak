package com.sequenceiq.periscope.converter;

import static java.util.Map.entry;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.model.ActivityStatus;
import com.sequenceiq.periscope.api.model.ApiActivityStatus;
import com.sequenceiq.periscope.api.model.DistroXAutoscaleScalingActivityResponse;
import com.sequenceiq.periscope.domain.ScalingActivity;

@Component
public class DistroXAutoscaleScalingActivityResponseConverter extends AbstractConverter<DistroXAutoscaleScalingActivityResponse, ScalingActivity> {

    private static final Map<ActivityStatus, ApiActivityStatus> ACTIVITY_STATUS_MAP = Map.ofEntries(
            entry(ActivityStatus.ACTIVITY_PENDING, ApiActivityStatus.ACTIVITY_PENDING),
            entry(ActivityStatus.DOWNSCALE_TRIGGER_FAILED, ApiActivityStatus.DOWNSCALE_TRIGGER_FAILED),
            entry(ActivityStatus.DOWNSCALE_TRIGGER_SUCCESS, ApiActivityStatus.DOWNSCALE_TRIGGER_SUCCESS),
            entry(ActivityStatus.METRICS_COLLECTION_FAILED, ApiActivityStatus.METRICS_COLLECTION_FAILED),
            entry(ActivityStatus.METRICS_COLLECTION_SUCCESS, ApiActivityStatus.METRICS_COLLECTION_SUCCESS),
            entry(ActivityStatus.SCALING_FLOW_FAILED, ApiActivityStatus.SCALING_FLOW_FAILED),
            entry(ActivityStatus.SCALING_FLOW_IN_PROGRESS, ApiActivityStatus.SCALING_FLOW_IN_PROGRESS),
            entry(ActivityStatus.SCALING_FLOW_SUCCESS, ApiActivityStatus.SCALING_FLOW_SUCCESS),
            entry(ActivityStatus.UPSCALE_TRIGGER_SUCCESS, ApiActivityStatus.UPSCALE_TRIGGER_SUCCESS),
            entry(ActivityStatus.UPSCALE_TRIGGER_FAILED, ApiActivityStatus.UPSCALE_TRIGGER_FAILED),
            entry(ActivityStatus.UNKNOWN, ApiActivityStatus.UNKNOWN),
            entry(ActivityStatus.MANDATORY_DOWNSCALE, ApiActivityStatus.POLICY_ADJUSTMENT),
            entry(ActivityStatus.MANDATORY_UPSCALE, ApiActivityStatus.POLICY_ADJUSTMENT),
            entry(ActivityStatus.SCHEDULE_BASED_UPSCALE, ApiActivityStatus.SCHEDULE_BASED_UPSCALE),
            entry(ActivityStatus.SCHEDULE_BASED_DOWNSCALE, ApiActivityStatus.SCHEDULE_BASED_DOWNSCALE)
    );

    @Override
    public DistroXAutoscaleScalingActivityResponse convert(ScalingActivity scalingActivity) {
        DistroXAutoscaleScalingActivityResponse json = new DistroXAutoscaleScalingActivityResponse();
        json.setStartTime(scalingActivity.getStartTime());
        json.setEndTime(scalingActivity.getEndTime());
        json.setYarnRecommendationTime(scalingActivity.getYarnRecommendationTime());
        json.setOperationId(scalingActivity.getOperationId());
        json.setActivityStatus(convert(scalingActivity.getActivityStatus()));
        json.setScalingActivityReason(scalingActivity.getScalingActivityReason());
        json.setYarnRecommendation(scalingActivity.getYarnRecommendation());
        return json;
    }

    private ApiActivityStatus convert(ActivityStatus activityStatus) {
        return ACTIVITY_STATUS_MAP.get(activityStatus);
    }
}