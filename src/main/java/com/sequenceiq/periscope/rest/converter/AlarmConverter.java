package com.sequenceiq.periscope.rest.converter;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.model.Alarm;
import com.sequenceiq.periscope.model.ScalingPolicy;
import com.sequenceiq.periscope.registry.Cluster;
import com.sequenceiq.periscope.rest.json.AlarmJson;
import com.sequenceiq.periscope.service.ClusterNotFoundException;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.ScalingService;

@Component
public class AlarmConverter extends AbstractConverter<AlarmJson, Alarm> {

    @Autowired
    private ClusterService clusterService;
    @Autowired
    private ScalingService scalingService;

    public List<Alarm> convertAllFromJson(List<AlarmJson> jsonList, String clusterId) {
        List<Alarm> alarms = new ArrayList<>();
        try {
            Cluster cluster = clusterService.get(clusterId);
            for (AlarmJson alarmJson : jsonList) {
                alarms.add(convert(alarmJson, cluster));
            }
        } catch (ClusterNotFoundException e) {
        }
        return alarms;
    }

    public Alarm convert(AlarmJson source, Cluster cluster) {
        Alarm alarm = new Alarm();
        alarm.setId(source.getId());
        ScalingPolicy scalingPolicy = scalingService.getScalingPolicy(cluster, source.getScalingPolicyId());
        if (scalingPolicy != null) {
            scalingPolicy.setAlarm(alarm);
        }
        alarm.setScalingPolicy(scalingPolicy);
        alarm.setAlarmName(source.getAlarmName());
        alarm.setComparisonOperator(source.getComparisonOperator());
        alarm.setDescription(source.getDescription());
        alarm.setMetric(source.getMetric());
        alarm.setPeriod(source.getPeriod());
        alarm.setThreshold(source.getThreshold());
        return alarm;
    }

    @Override
    public AlarmJson convert(Alarm source) {
        AlarmJson alarmJson = new AlarmJson();
        alarmJson.setId(source.getId());
        ScalingPolicy scalingPolicy = source.getScalingPolicy();
        alarmJson.setScalingPolicyId(scalingPolicy == null ? null : scalingPolicy.getId());
        alarmJson.setAlarmName(source.getAlarmName());
        alarmJson.setComparisonOperator(source.getComparisonOperator());
        alarmJson.setDescription(source.getDescription());
        alarmJson.setMetric(source.getMetric());
        alarmJson.setPeriod(source.getPeriod());
        alarmJson.setThreshold(source.getThreshold());
        return alarmJson;
    }

}
