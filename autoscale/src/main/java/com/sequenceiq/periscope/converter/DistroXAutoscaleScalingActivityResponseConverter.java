package com.sequenceiq.periscope.converter;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.model.ActivityStatus;
import com.sequenceiq.periscope.api.model.ApiActivityStatus;
import com.sequenceiq.periscope.api.model.DistroXAutoscaleScalingActivityResponse;
import com.sequenceiq.periscope.domain.ScalingActivity;

@Component
public class DistroXAutoscaleScalingActivityResponseConverter extends AbstractConverter<DistroXAutoscaleScalingActivityResponse, ScalingActivity> {

    private static final Map<ActivityStatus, ApiActivityStatus> ACTIVITY_STATUS_MAP = Map.ofEntries(
            Map.entry(ActivityStatus.ACTIVITY_PENDING, ApiActivityStatus.ACTIVITY_PENDING),
            Map.entry(ActivityStatus.DOWNSCALE_TRIGGER_FAILED, ApiActivityStatus.DOWNSCALE_TRIGGER_FAILED),
            Map.entry(ActivityStatus.DOWNSCALE_TRIGGER_SUCCESS, ApiActivityStatus.DOWNSCALE_TRIGGER_SUCCESS),
            Map.entry(ActivityStatus.METRICS_COLLECTION_FAILED, ApiActivityStatus.METRICS_COLLECTION_FAILED),
            Map.entry(ActivityStatus.METRICS_COLLECTION_SUCCESS, ApiActivityStatus.METRICS_COLLECTION_SUCCESS),
            Map.entry(ActivityStatus.SCALING_FLOW_FAILED, ApiActivityStatus.SCALING_FLOW_FAILED),
            Map.entry(ActivityStatus.SCALING_FLOW_IN_PROGRESS, ApiActivityStatus.SCALING_FLOW_IN_PROGRESS),
            Map.entry(ActivityStatus.SCALING_FLOW_SUCCESS, ApiActivityStatus.SCALING_FLOW_SUCCESS),
            Map.entry(ActivityStatus.UPSCALE_TRIGGER_SUCCESS, ApiActivityStatus.UPSCALE_TRIGGER_SUCCESS),
            Map.entry(ActivityStatus.UPSCALE_TRIGGER_FAILED, ApiActivityStatus.UPSCALE_TRIGGER_FAILED)
    );

    @Override
    public DistroXAutoscaleScalingActivityResponse convert(ScalingActivity scalingActivity) {
        DistroXAutoscaleScalingActivityResponse json = new DistroXAutoscaleScalingActivityResponse();
        json.setStartTime(scalingActivity.getStartTime());
        json.setEndTime(scalingActivity.getEndTime());
        json.setOperationId(scalingActivity.getOperationId());
        json.setActivityStatus(convert(scalingActivity.getActivityStatus()));
        json.setScalingActivityReason(scalingActivity.getScalingActivityReason());
        return json;
    }

    private ApiActivityStatus convert(ActivityStatus activityStatus) {
        return ACTIVITY_STATUS_MAP.get(activityStatus);
    }
}