package com.sequenceiq.periscope.service;

import java.util.Arrays;
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

    public List<MetricAlarm> setMetricAlarms(PeriscopeUser user, long clusterId, List<MetricAlarm> metricAlarms)
            throws ClusterNotFoundException {
        return addMetricAlarms(user, clusterId, metricAlarms, true);
    }

    public List<MetricAlarm> addMetricAlarm(PeriscopeUser user, long clusterId, MetricAlarm metricAlarm)
            throws ClusterNotFoundException {
        return addMetricAlarms(user, clusterId, Arrays.asList(metricAlarm), false);
    }

    public List<TimeAlarm> setTimeAlarms(PeriscopeUser user, long clusterId, List<TimeAlarm> alarms)
            throws ClusterNotFoundException {
        return addTimeAlarms(user, clusterId, alarms, true);
    }

    public List<TimeAlarm> addTimeAlarm(PeriscopeUser user, long clusterId, TimeAlarm alarm)
            throws ClusterNotFoundException {
        return addTimeAlarms(user, clusterId, Arrays.asList(alarm), false);
    }

    public List<MetricAlarm> getMetricAlarms(PeriscopeUser user, long clusterId) throws ClusterNotFoundException {
        return clusterService.get(user, clusterId).getMetricAlarms();
    }

    public List<TimeAlarm> getTimeAlarms(PeriscopeUser user, long clusterId) throws ClusterNotFoundException {
        return clusterService.get(user, clusterId).getTimeAlarms();
    }

    public List<MetricAlarm> deleteMetricAlarm(PeriscopeUser user, long clusterId, long alarmId) throws ClusterNotFoundException {
        Cluster cluster = clusterService.get(user, clusterId);
        MetricAlarm metricAlarm = metricAlarmRepository.findOne(alarmId);
        if (metricAlarm == null) {
            throw new AlarmNotFoundException(alarmId);
        }
        List<MetricAlarm> alarms = cluster.getMetricAlarms();
        alarms.remove(metricAlarm);
        metricAlarmRepository.delete(metricAlarm);
        clusterRepository.save(cluster);
        cluster.setMetricAlarms(alarms);
        return alarms;
    }

    public List<TimeAlarm> deleteTimeAlarm(PeriscopeUser user, long clusterId, long alarmId) throws ClusterNotFoundException {
        Cluster cluster = clusterService.get(user, clusterId);
        TimeAlarm alarm = timeAlarmRepository.findOne(alarmId);
        if (alarm == null) {
            throw new AlarmNotFoundException(alarmId);
        }
        List<TimeAlarm> alarms = cluster.getTimeAlarms();
        alarms.remove(alarm);
        timeAlarmRepository.delete(alarm);
        clusterRepository.save(cluster);
        cluster.setTimeAlarms(alarms);
        return alarms;
    }

    private List<MetricAlarm> addMetricAlarms(PeriscopeUser user, long clusterId, List<MetricAlarm> alarms, boolean override)
            throws ClusterNotFoundException {
        Cluster cluster = clusterService.get(user, clusterId);
        List<MetricAlarm> alarmList = cluster.getMetricAlarms();
        if (override) {
            alarmList.clear();
        }
        metricAlarmRepository.save(alarms);
        alarmList.addAll(alarms);
        clusterRepository.save(cluster);
        cluster.setMetricAlarms(alarmList);
        return alarmList;
    }

    private List<TimeAlarm> addTimeAlarms(PeriscopeUser user, long clusterId, List<TimeAlarm> alarms, boolean override)
            throws ClusterNotFoundException {
        Cluster cluster = clusterService.get(user, clusterId);
        List<TimeAlarm> alarmList = cluster.getTimeAlarms();
        if (override) {
            alarmList.clear();
        }
        timeAlarmRepository.save(alarms);
        alarmList.addAll(alarms);
        clusterRepository.save(cluster);
        cluster.setTimeAlarms(alarmList);
        return alarmList;
    }

}
