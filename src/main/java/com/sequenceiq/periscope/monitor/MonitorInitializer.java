package com.sequenceiq.periscope.monitor;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.log.Logger;
import com.sequenceiq.periscope.log.PeriscopeLoggerFactory;
import com.sequenceiq.periscope.model.HostResolution;
import com.sequenceiq.periscope.registry.ClusterRegistry;
import com.sequenceiq.periscope.registry.ClusterState;
import com.sequenceiq.periscope.registry.ConnectionException;
import com.sequenceiq.periscope.repository.ClusterRepository;

@Component
public class MonitorInitializer implements InitializingBean {

    private static final Logger LOGGER = PeriscopeLoggerFactory.getLogger(MonitorInitializer.class);

    @Autowired
    private ClusterRepository clusterRepository;
    @Autowired
    private ClusterRegistry clusterRegistry;

    @Value("${periscope.hostname.resolution:public}")
    private String hostNameResolution;

    @Override
    public void afterPropertiesSet() {
        for (Cluster cluster : clusterRepository.findAll()) {
            cluster.setResolution(HostResolution.valueOf(hostNameResolution.toUpperCase()));
            clusterRegistry.add(cluster.getUser(), cluster);
            try {
                if (cluster.isRunning()) {
                    cluster.start();
                }
            } catch (ConnectionException e) {
                cluster.setState(ClusterState.SUSPENDED);
                clusterRepository.save(cluster);
                LOGGER.info(cluster.getId(), "Monitoring cannot be started, suspending monitoring", e);
            }
        }
    }
}
