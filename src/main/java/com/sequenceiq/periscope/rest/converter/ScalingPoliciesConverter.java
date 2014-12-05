package com.sequenceiq.periscope.rest.converter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.model.ScalingPolicies;
import com.sequenceiq.periscope.rest.json.ScalingPoliciesJson;

@Component
public class ScalingPoliciesConverter extends AbstractConverter<ScalingPoliciesJson, ScalingPolicies> {

    @Autowired
    private ScalingPolicyConverter scalingPolicyConverter;

    @Override
    public ScalingPoliciesJson convert(ScalingPolicies source) {
        ScalingPoliciesJson group = new ScalingPoliciesJson();
        group.setCoolDown(source.getCoolDown());
        group.setMaxSize(source.getMaxSize());
        group.setMinSize(source.getMinSize());
        group.setScalingPolicies(scalingPolicyConverter.convertAllToJson(source.getScalingPolicies()));
        return group;
    }
}
