package com.sequenceiq.periscope.init;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.monitor.handler.CloudbreakCommunicator;
import com.sequenceiq.periscope.service.AltusMachineUserService;
import com.sequenceiq.periscope.service.ClusterService;

@Component
public class InitializeMachineUserService {

    @Inject
    private ClusterService clusterService;

    @Inject
    private AltusMachineUserService altusMachineUserService;

    @Inject
    private CloudbreakCommunicator cloudbreakCommunicator;

    @PostConstruct
    public void init() {
        List<Cluster> clusters = clusterService.findByEnvironmentCrnOrMachineUserCrn(null, null);
        clusters.forEach(cluster ->  {
            if (Strings.isNullOrEmpty(cluster.getEnvironmentCrn())) {
                String envCrn = cloudbreakCommunicator.getAutoscaleClusterByCrn(cluster.getStackCrn()).getEnvironmentCrn();
                clusterService.setEnvironmentCrn(cluster.getId(), envCrn);
                cluster = clusterService.findById(cluster.getId());
            }

            if (Strings.isNullOrEmpty(cluster.getMachineUserCrn())) {
                altusMachineUserService.initializeMachineUserForEnvironment(cluster);
            }
        });
    }
}
