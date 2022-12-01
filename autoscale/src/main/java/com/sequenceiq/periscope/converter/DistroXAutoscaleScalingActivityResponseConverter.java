package com.sequenceiq.periscope.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.model.DistroXAutoscaleScalingActivityResponse;
import com.sequenceiq.periscope.domain.ScalingTrigger;

@Component
public class DistroXAutoscaleScalingActivityResponseConverter extends AbstractConverter<DistroXAutoscaleScalingActivityResponse, ScalingTrigger> {

    @Override
    public DistroXAutoscaleScalingActivityResponse convert(ScalingTrigger scalingTrigger) {
        DistroXAutoscaleScalingActivityResponse json = new DistroXAutoscaleScalingActivityResponse();
        json.setFlowId(scalingTrigger.getFlowId());
        json.setStartTime(scalingTrigger.getStartTime());
        json.setEndTime(scalingTrigger.getEndTime());
        json.setTriggerCrn(scalingTrigger.getTriggerCrn());
        json.setTriggerStatus(scalingTrigger.getTriggerStatus());
        return json;
    }
}