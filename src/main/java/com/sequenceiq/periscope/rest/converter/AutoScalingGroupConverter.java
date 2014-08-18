package com.sequenceiq.periscope.rest.converter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.model.AutoScalingGroup;
import com.sequenceiq.periscope.rest.json.AutoScalingGroupJson;

@Component
public class AutoScalingGroupConverter extends AbstractConverter<AutoScalingGroupJson, AutoScalingGroup> {

    @Autowired
    private ScalingPolicyConverter scalingPolicyConverter;

    public AutoScalingGroup convert(AutoScalingGroupJson source, String clusterId) {
        AutoScalingGroup group = new AutoScalingGroup();
        group.setCoolDown(source.getCoolDown());
        group.setMaxSize(source.getMaxSize());
        group.setMinSize(source.getMinSize());
        group.setScalingPolicies(scalingPolicyConverter.convertAllFromJson(source.getScalingPolicies(), clusterId));
        return group;
    }

    @Override
    public AutoScalingGroupJson convert(AutoScalingGroup source) {
        AutoScalingGroupJson group = new AutoScalingGroupJson();
        group.setCoolDown(source.getCoolDown());
        group.setMaxSize(source.getMaxSize());
        group.setMinSize(source.getMinSize());
        group.setScalingPolicies(scalingPolicyConverter.convertAllToJson(source.getScalingPolicies()));
        return group;
    }
}
