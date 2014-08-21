package com.sequenceiq.periscope.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.model.Alarm;
import com.sequenceiq.periscope.model.Cluster;
import com.sequenceiq.periscope.model.ClusterDetails;
import com.sequenceiq.periscope.repository.AlarmRepository;
import com.sequenceiq.periscope.repository.ClusterDetailsRepository;

@Service
public class AlarmService {

    @Autowired
    private ClusterDetailsRepository clusterDetailsRepository;
    @Autowired
    private AlarmRepository alarmRepository;
    @Autowired
    private ClusterService clusterService;

    public List<Alarm> addAlarms(long clusterId, List<Alarm> alarms) throws ClusterNotFoundException {
        Cluster cluster = clusterService.get(clusterId);
        ClusterDetails clusterDetails = cluster.getClusterDetails();
        clusterDetails.addAlarms(alarms);
        alarmRepository.save(alarms);
        clusterDetailsRepository.save(clusterDetails);
        return clusterDetails.getAlarms();
    }

    public List<Alarm> getAlarms(long clusterId) throws ClusterNotFoundException {
        return clusterService.get(clusterId).getAlarms();
    }

    public List<Alarm> deleteAlarm(long clusterId, long alarmId) throws ClusterNotFoundException {
        Cluster cluster = clusterService.get(clusterId);
        Alarm alarm = alarmRepository.findOne(alarmId);
        if (alarm == null) {
            throw new AlarmNotFoundException(alarmId);
        }
        List<Alarm> alarms = cluster.getAlarms();
        alarms.remove(alarm);
        clusterDetailsRepository.save(cluster.getClusterDetails());
        return alarms;
    }

}
