package com.sequenceiq.periscope.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.domain.Alarm;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.repository.AlarmRepository;
import com.sequenceiq.periscope.repository.ClusterRepository;

@Service
public class AlarmService {

    @Autowired
    private ClusterRepository clusterRepository;
    @Autowired
    private AlarmRepository alarmRepository;
    @Autowired
    private ClusterService clusterService;

    public List<Alarm> setAlarms(long clusterId, List<Alarm> alarms) throws ClusterNotFoundException {
        return addAlarms(clusterId, alarms, true);
    }

    public List<Alarm> addAlarm(long clusterId, Alarm alarm) throws ClusterNotFoundException {
        return addAlarms(clusterId, Arrays.asList(alarm), false);
    }

    public List<Alarm> getAlarms(long clusterId) throws ClusterNotFoundException {
        return clusterService.get(clusterId).getAlarms();
    }

    public List<Alarm> deleteAlarm(long clusterId, long alarmId) throws ClusterNotFoundException {
        Cluster runningCluster = clusterService.get(clusterId);
        Cluster savedCluster = clusterRepository.findOne(clusterId);
        Alarm alarm = alarmRepository.findOne(alarmId);
        if (alarm == null) {
            throw new AlarmNotFoundException(alarmId);
        }
        List<Alarm> alarms = savedCluster.getAlarms();
        alarms.remove(alarm);
        clusterRepository.save(savedCluster);
        runningCluster.setAlarms(alarms);
        return alarms;
    }

    private List<Alarm> addAlarms(long clusterId, List<Alarm> alarms, boolean override) throws ClusterNotFoundException {
        Cluster runningCluster = clusterService.get(clusterId);
        Cluster savedCluster = clusterRepository.findOne(clusterId);
        List<Alarm> alarmList = savedCluster.getAlarms();
        if (override) {
            alarmList.clear();
        }
        alarmList.addAll(alarms);
        alarmRepository.save(alarmList);
        clusterRepository.save(savedCluster);
        runningCluster.setAlarms(alarmList);
        return alarmList;
    }

}
