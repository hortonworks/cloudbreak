package com.sequenceiq.periscope.monitor;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.registry.ClusterRegistry;
import com.sequenceiq.periscope.repository.ClusterRepository;

@Component
public class MonitorInitializer implements InitializingBean {

    @Autowired
    private ClusterRepository clusterRepository;
    @Autowired
    private ClusterRegistry clusterRegistry;

    @Override
    public void afterPropertiesSet() throws Exception {
        for (Cluster cluster : clusterRepository.findAll()) {
            clusterRegistry.add(cluster.getUser(), cluster);
            cluster.start();
        }
    }
}
