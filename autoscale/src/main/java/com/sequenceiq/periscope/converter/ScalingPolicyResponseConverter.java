package com.sequenceiq.periscope.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.model.ScalingPolicyResponse;
import com.sequenceiq.periscope.domain.ScalingPolicy;

@Component
public class ScalingPolicyResponseConverter extends AbstractConverter<ScalingPolicyResponse, ScalingPolicy> {

    @Override
    public ScalingPolicy convert(ScalingPolicyResponse source) {
        ScalingPolicy policy = new ScalingPolicy();
        policy.setAdjustmentType(source.getAdjustmentType());
        policy.setName(source.getName());
        policy.setScalingAdjustment(source.getScalingAdjustment());
        policy.setHostGroup(source.getHostGroup());
        return policy;
    }

    @Override
    public ScalingPolicyResponse convert(ScalingPolicy source) {
        ScalingPolicyResponse json = new ScalingPolicyResponse();
        json.setAdjustmentType(source.getAdjustmentType());
        json.setName(source.getName());
        json.setScalingAdjustment(source.getScalingAdjustment());
        json.setHostGroup(source.getHostGroup());
        return json;
    }
}
