package com.sequenceiq.periscope.rest.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.policies.scaling.ScalingPolicy;
import com.sequenceiq.periscope.rest.json.ScalingPolicyJson;

@Component
public class ScalingPolicyConverter extends AbstractConverter<ScalingPolicyJson, ScalingPolicy> {

    @Override
    public ScalingPolicy convert(ScalingPolicyJson source) {
        return new ScalingPolicy(source.getCoolDown(), source.getScaleUpRules(), source.getScaleDownRules());
    }

    @Override
    public ScalingPolicyJson convert(ScalingPolicy source) {
        return new ScalingPolicyJson(source.getCoolDown(), source.getScaleUpConfig(), source.getScaleDownConfig());
    }
}
