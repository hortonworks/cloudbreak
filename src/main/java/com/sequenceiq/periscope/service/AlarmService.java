package com.sequenceiq.periscope.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.model.Alarm;
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

    public List<Alarm> setAlarms(String clusterId, List<Alarm> alarms) throws ClusterNotFoundException {
        ClusterDetails clusterDetails = clusterService.get(clusterId).getClusterDetails();
        clusterDetails.setAlarms(alarms);
        alarmRepository.save(alarms);
        clusterDetailsRepository.save(clusterDetails);
        return alarms;
    }

    public List<Alarm> getAlarms(String clusterId) throws ClusterNotFoundException {
        return clusterService.get(clusterId).getAlarms();
    }

}
