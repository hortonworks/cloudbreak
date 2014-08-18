package com.sequenceiq.periscope.rest.converter;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.model.Alarm;
import com.sequenceiq.periscope.model.ScalingPolicy;
import com.sequenceiq.periscope.registry.Cluster;
import com.sequenceiq.periscope.rest.json.ScalingPolicyJson;
import com.sequenceiq.periscope.service.ClusterNotFoundException;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.ScalingService;

@Component
public class ScalingPolicyConverter extends AbstractConverter<ScalingPolicyJson, ScalingPolicy> {

    @Autowired
    private ClusterService clusterService;
    @Autowired
    private ScalingService scalingService;

    public List<ScalingPolicy> convertAllFromJson(List<ScalingPolicyJson> jsonList, String clusterId) {
        List<ScalingPolicy> policies = new ArrayList<>();
        try {
            Cluster cluster = clusterService.get(clusterId);
            for (ScalingPolicyJson policyJson : jsonList) {
                policies.add(convert(policyJson, cluster));
            }
        } catch (ClusterNotFoundException e) {
        }
        return policies;
    }

    public ScalingPolicy convert(ScalingPolicyJson source, Cluster cluster) {
        ScalingPolicy policy = new ScalingPolicy();
        policy.setId(source.getId());
        policy.setAdjustmentType(source.getAdjustmentType());
        Alarm alarm = scalingService.getAlarm(cluster, source.getAlarmId());
        if (alarm != null) {
            alarm.setScalingPolicy(policy);
        }
        policy.setAlarm(alarm);
        policy.setName(source.getName());
        policy.setScalingAdjustment(source.getScalingAdjustment());
        return policy;
    }

    @Override
    public ScalingPolicyJson convert(ScalingPolicy source) {
        ScalingPolicyJson json = new ScalingPolicyJson();
        json.setId(source.getId());
        json.setAdjustmentType(source.getAdjustmentType());
        Alarm alarm = source.getAlarm();
        json.setAlarmId(alarm == null ? null : alarm.getId());
        json.setName(source.getName());
        json.setScalingAdjustment(source.getScalingAdjustment());
        return json;
    }
}
