package com.sequenceiq.periscope.rest.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.model.ScalingPolicyJson;
import com.sequenceiq.periscope.domain.BaseAlert;
import com.sequenceiq.periscope.domain.ScalingPolicy;

@Component
public class ScalingPolicyConverter extends AbstractConverter<ScalingPolicyJson, ScalingPolicy> {

    @Override
    public ScalingPolicy convert(ScalingPolicyJson source) {
        ScalingPolicy policy = new ScalingPolicy();
        policy.setAdjustmentType(source.getAdjustmentType());
        policy.setName(source.getName());
        policy.setScalingAdjustment(source.getScalingAdjustment());
        policy.setHostGroup(source.getHostGroup());
        return policy;
    }

    @Override
    public ScalingPolicyJson convert(ScalingPolicy source) {
        ScalingPolicyJson json = new ScalingPolicyJson();
        json.setId(source.getId());
        json.setAdjustmentType(source.getAdjustmentType());
        BaseAlert alert = source.getAlert();
        json.setAlertId(alert == null ? null : alert.getId());
        json.setName(source.getName());
        json.setScalingAdjustment(source.getScalingAdjustment());
        json.setHostGroup(source.getHostGroup());
        return json;
    }
}
