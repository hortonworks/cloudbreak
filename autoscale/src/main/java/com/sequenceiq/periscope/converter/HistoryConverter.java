package com.sequenceiq.periscope.converter;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.model.AutoscaleClusterHistoryActivity;
import com.sequenceiq.periscope.api.model.AutoscaleClusterHistoryResponse;
import com.sequenceiq.periscope.api.model.ScalingStatus;
import com.sequenceiq.periscope.domain.History;

@Component
public class HistoryConverter extends AbstractConverter<AutoscaleClusterHistoryResponse, History> {

    private Set<ScalingStatus> scalingActivityStatus = Set.of(ScalingStatus.SUCCESS, ScalingStatus.FAILED);

    @Override
    public AutoscaleClusterHistoryResponse convert(History source) {
        AutoscaleClusterHistoryResponse json = new AutoscaleClusterHistoryResponse();
        json.setStackCrn(source.getStackCrn());
        json.setScalingStatus(source.getScalingStatus());
        json.setTimestamp(source.getTimestamp());
        json.setStatusReason(source.getStatusReason());

        if (scalingActivityStatus.contains(source.getScalingStatus())) {
            AutoscaleClusterHistoryActivity scalingActivity = new AutoscaleClusterHistoryActivity();
            scalingActivity.setHostGroup(source.getHostGroup());
            scalingActivity.setOriginalNodeCount(source.getOriginalNodeCount());
            scalingActivity.setProperties(source.getProperties());
            scalingActivity.setAdjustment(source.getAdjustment());
            scalingActivity.setAdjustmentType(source.getAdjustmentType());
            scalingActivity.setAlertType(source.getAlertType());
            json.setAutoscaleClusterHistoryActivity(scalingActivity);
        }
        return json;
    }
}
