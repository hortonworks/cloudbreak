package com.sequenceiq.periscope.monitor.handler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.StackResponse;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.log.MDCBuilder;
import com.sequenceiq.periscope.monitor.event.UpdateFailedEvent;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.configuration.CloudbreakClientConfiguration;

@Component
public class UpdateFailedHandler implements ApplicationListener<UpdateFailedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateFailedHandler.class);

    private static final String DELETE_STATUSES_PREFIX = "DELETE_";

    private static final int RETRY_THRESHOLD = 5;

    @Inject
    private ClusterService clusterService;

    @Inject
    private CloudbreakClientConfiguration cloudbreakClientConfiguration;

    private final Map<Long, Integer> updateFailures = new ConcurrentHashMap<>();

    @Override
    public void onApplicationEvent(UpdateFailedEvent event) {
        long id = event.getClusterId();
        Cluster cluster = clusterService.find(id);
        MDCBuilder.buildMdcContext(cluster);
        Integer failed = updateFailures.get(id);
        if (failed == null) {
            updateFailures.put(id, 1);
        } else if (RETRY_THRESHOLD - 1 == failed) {
            try {
                CloudbreakClient cloudbreakClient = cloudbreakClientConfiguration.cloudbreakClient();
                StackResponse stackResponse = cloudbreakClient.stackEndpoint().get(cluster.getStackId());
                String stackStatus = stackResponse.getStatus().name();
                if (stackStatus.startsWith(DELETE_STATUSES_PREFIX)) {
                    clusterService.removeById(id);
                    LOGGER.info("Delete cluster due to failing update attempts and Cloudbreak stack status");
                } else {
                    suspendCluster(cluster);
                }
            } catch (Exception ex) {
                LOGGER.warn("Cluster status could not be verified by Cloudbreak for remove.", ex);
                suspendCluster(cluster);
            }
            updateFailures.remove(id);
        } else {
            updateFailures.put(id, failed + 1);
        }
    }

    private void suspendCluster(Cluster cluster) {
        cluster.setState(ClusterState.SUSPENDED);
        clusterService.save(cluster);
        LOGGER.info("Suspend cluster monitoring due to failing update attempts");
    }
}
