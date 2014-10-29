package com.sequenceiq.periscope.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.MetricAlarm;
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

    public List<MetricAlarm> setAlarms(long clusterId, List<MetricAlarm> metricAlarms) throws ClusterNotFoundException {
        return addAlarms(clusterId, metricAlarms, true);
    }

    public List<MetricAlarm> addAlarm(long clusterId, MetricAlarm metricAlarm) throws ClusterNotFoundException {
        return addAlarms(clusterId, Arrays.asList(metricAlarm), false);
    }

    public List<TimeAlarm> setTimeAlarms(long clusterId, List<TimeAlarm> alarms) throws ClusterNotFoundException {
        return addTimeAlarms(clusterId, alarms, true);
    }

    public List<TimeAlarm> addTimeAlarm(long clusterId, TimeAlarm alarm) throws ClusterNotFoundException {
        return addTimeAlarms(clusterId, Arrays.asList(alarm), false);
    }

    public List<MetricAlarm> getAlarms(long clusterId) throws ClusterNotFoundException {
        return clusterService.get(clusterId).getMetricAlarms();
    }

    public List<TimeAlarm> getTimeAlarms(long clusterId) throws ClusterNotFoundException {
        return clusterService.get(clusterId).getTimeAlarms();
    }

    public List<MetricAlarm> deleteAlarm(long clusterId, long alarmId) throws ClusterNotFoundException {
        Cluster runningCluster = clusterService.get(clusterId);
        Cluster savedCluster = clusterRepository.findOne(clusterId);
        MetricAlarm metricAlarm = metricAlarmRepository.findOne(alarmId);
        if (metricAlarm == null) {
            throw new AlarmNotFoundException(alarmId);
        }
        List<MetricAlarm> metricAlarms = savedCluster.getMetricAlarms();
        metricAlarms.remove(metricAlarm);
        clusterRepository.save(savedCluster);
        runningCluster.setMetricAlarms(metricAlarms);
        return metricAlarms;
    }

    public List<TimeAlarm> deleteTimeAlarm(long clusterId, long alarmId) throws ClusterNotFoundException {
        Cluster runningCluster = clusterService.get(clusterId);
        Cluster savedCluster = clusterRepository.findOne(clusterId);
        TimeAlarm alarm = timeAlarmRepository.findOne(alarmId);
        if (alarm == null) {
            throw new AlarmNotFoundException(alarmId);
        }
        List<TimeAlarm> alarms = savedCluster.getTimeAlarms();
        alarms.remove(alarm);
        clusterRepository.save(savedCluster);
        runningCluster.setTimeAlarms(alarms);
        return alarms;
    }

    private List<MetricAlarm> addAlarms(long clusterId, List<MetricAlarm> metricAlarms, boolean override) throws ClusterNotFoundException {
        Cluster runningCluster = clusterService.get(clusterId);
        Cluster savedCluster = clusterRepository.findOne(clusterId);
        List<MetricAlarm> metricAlarmList = savedCluster.getMetricAlarms();
        if (override) {
            metricAlarmList.clear();
        }
        metricAlarmList.addAll(metricAlarms);
        metricAlarmRepository.save(metricAlarmList);
        clusterRepository.save(savedCluster);
        runningCluster.setMetricAlarms(metricAlarmList);
        return metricAlarmList;
    }

    private List<TimeAlarm> addTimeAlarms(long clusterId, List<TimeAlarm> alarms, boolean override) throws ClusterNotFoundException {
        Cluster runningCluster = clusterService.get(clusterId);
        Cluster savedCluster = clusterRepository.findOne(clusterId);
        List<TimeAlarm> alarmList = savedCluster.getTimeAlarms();
        if (override) {
            alarmList.clear();
        }
        alarmList.addAll(alarms);
        timeAlarmRepository.save(alarmList);
        clusterRepository.save(savedCluster);
        runningCluster.setTimeAlarms(alarmList);
        return alarmList;
    }

}
