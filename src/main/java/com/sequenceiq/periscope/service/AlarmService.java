package com.sequenceiq.periscope.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.domain.Alarm;
import com.sequenceiq.periscope.domain.ClusterDetails;
import com.sequenceiq.periscope.model.Cluster;
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
        ClusterDetails clusterDetails = clusterDetailsRepository.findOne(clusterId);
        clusterDetails.addAlarms(alarms);
        alarmRepository.save(alarms);
        clusterDetailsRepository.save(clusterDetails);
        cluster.setClusterDetails(clusterDetails);
        return clusterDetails.getAlarms();
    }

    public List<Alarm> getAlarms(long clusterId) throws ClusterNotFoundException {
        return clusterService.get(clusterId).getAlarms();
    }

    public List<Alarm> deleteAlarm(long clusterId, long alarmId) throws ClusterNotFoundException {
        Cluster cluster = clusterService.get(clusterId);
        ClusterDetails clusterDetails = clusterDetailsRepository.findOne(clusterId);
        Alarm alarm = alarmRepository.findOne(alarmId);
        if (alarm == null) {
            throw new AlarmNotFoundException(alarmId);
        }
        List<Alarm> alarms = clusterDetails.getAlarms();
        alarms.remove(alarm);
        cluster.setClusterDetails(clusterDetails);
        clusterDetailsRepository.save(clusterDetails);
        return alarms;
    }

}
