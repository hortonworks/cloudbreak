package com.sequenceiq.periscope.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.MetricAlarm;
import com.sequenceiq.periscope.domain.PeriscopeUser;
import com.sequenceiq.periscope.domain.TimeAlarm;
import com.sequenceiq.periscope.repository.ClusterRepository;
import com.sequenceiq.periscope.repository.MetricAlarmRepository;
import com.sequenceiq.periscope.repository.TimeAlarmRepository;

@Service
public class AlarmService {

    @Autowired
    private ClusterRepository clusterRepository;
    @Autowired
    private MetricAlarmRepository metricAlarmRepository;
    @Autowired
    private TimeAlarmRepository timeAlarmRepository;
    @Autowired
    private ClusterService clusterService;

    public MetricAlarm addMetricAlarm(PeriscopeUser user, long clusterId, MetricAlarm metricAlarm) throws ClusterNotFoundException {
        Cluster cluster = clusterService.get(user, clusterId);
        metricAlarmRepository.save(metricAlarm);
        cluster.addAlarm(metricAlarm);
        clusterRepository.save(cluster);
        return metricAlarm;
    }

    public MetricAlarm setMetricAlarm(PeriscopeUser user, long clusterId, long alarmId, MetricAlarm metricAlarm) throws ClusterNotFoundException {
        Cluster cluster = clusterService.get(user, clusterId);
        List<MetricAlarm> metricAlarms = cluster.getMetricAlarms();
        MetricAlarm savedAlarm = null;
        for (MetricAlarm alarm : metricAlarms) {
            if (alarm.getId() == alarmId) {
                alarm.setComparisonOperator(metricAlarm.getComparisonOperator());
                alarm.setMetric(metricAlarm.getMetric());
                alarm.setDescription(metricAlarm.getDescription());
                alarm.setPeriod(metricAlarm.getPeriod());
                alarm.setNotifications(metricAlarm.getNotifications());
                alarm.setThreshold(metricAlarm.getThreshold());
                metricAlarmRepository.save(alarm);
                savedAlarm = alarm;
                break;
            }
        }
        if (savedAlarm == null) {
            throw new AlarmNotFoundException(alarmId);
        }
        return savedAlarm;
    }

    public TimeAlarm addTimeAlarm(PeriscopeUser user, long clusterId, TimeAlarm alarm) throws ClusterNotFoundException {
        Cluster cluster = clusterService.get(user, clusterId);
        timeAlarmRepository.save(alarm);
        cluster.addAlarm(alarm);
        clusterRepository.save(cluster);
        return alarm;
    }

    public TimeAlarm setTimeAlarm(PeriscopeUser user, long clusterId, long alarmId, TimeAlarm timeAlarm) throws ClusterNotFoundException {
        Cluster cluster = clusterService.get(user, clusterId);
        List<TimeAlarm> timeAlarms = cluster.getTimeAlarms();
        TimeAlarm savedAlarm = null;
        for (TimeAlarm alarm : timeAlarms) {
            if (alarm.getId() == alarmId) {
                alarm.setNotifications(timeAlarm.getNotifications());
                alarm.setDescription(timeAlarm.getDescription());
                alarm.setCron(timeAlarm.getCron());
                alarm.setTimeZone(timeAlarm.getTimeZone());
                alarm.setName(timeAlarm.getName());
                timeAlarmRepository.save(alarm);
                savedAlarm = alarm;
                break;
            }
        }
        if (savedAlarm == null) {
            throw new AlarmNotFoundException(alarmId);
        }
        return savedAlarm;
    }

    public List<MetricAlarm> getMetricAlarms(PeriscopeUser user, long clusterId) throws ClusterNotFoundException {
        return clusterService.get(user, clusterId).getMetricAlarms();
    }

    public List<TimeAlarm> getTimeAlarms(PeriscopeUser user, long clusterId) throws ClusterNotFoundException {
        return clusterService.get(user, clusterId).getTimeAlarms();
    }

    public void deleteMetricAlarm(PeriscopeUser user, long clusterId, long alarmId) throws ClusterNotFoundException {
        Cluster cluster = clusterService.get(user, clusterId);
        MetricAlarm metricAlarm = metricAlarmRepository.findOne(alarmId);
        if (metricAlarm == null) {
            throw new AlarmNotFoundException(alarmId);
        }
        List<MetricAlarm> alarms = cluster.getMetricAlarms();
        alarms.remove(metricAlarm);
        metricAlarmRepository.delete(metricAlarm);
        cluster.setMetricAlarms(alarms);
    }

    public void deleteTimeAlarm(PeriscopeUser user, long clusterId, long alarmId) throws ClusterNotFoundException {
        Cluster cluster = clusterService.get(user, clusterId);
        TimeAlarm alarm = timeAlarmRepository.findOne(alarmId);
        if (alarm == null) {
            throw new AlarmNotFoundException(alarmId);
        }
        List<TimeAlarm> alarms = cluster.getTimeAlarms();
        alarms.remove(alarm);
        timeAlarmRepository.delete(alarm);
        cluster.setTimeAlarms(alarms);
    }

}
