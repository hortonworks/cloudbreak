package com.sequenceiq.periscope.monitor.handler;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.FailureReport;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceMetaDataJson;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceStatus;
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

    private static final String AVAILABLE = "AVAILABLE";

    private static final int RETRY_THRESHOLD = 5;

    @Inject
    private ClusterService clusterService;

    @Inject
    private CloudbreakClientConfiguration cloudbreakClientConfiguration;

    private final Map<Long, Integer> updateFailures = new ConcurrentHashMap<>();

    @Override
    public void onApplicationEvent(UpdateFailedEvent event) {
        long id = event.getClusterId();
        LOGGER.info("Cluster {} failed", id);
        Cluster cluster = clusterService.findById(id);
        MDCBuilder.buildMdcContext(cluster);
        Integer failed = updateFailures.get(id);
        if (failed == null) {
            updateFailures.put(id, 1);
        } else if (RETRY_THRESHOLD - 1 == failed) {
            try {
                CloudbreakClient cloudbreakClient = cloudbreakClientConfiguration.cloudbreakClient();
                StackResponse stackResponse = cloudbreakClient.stackV1Endpoint().get(cluster.getStackId(), new HashSet<>());
                String stackStatus = stackResponse.getStatus().name();
                String clusterStatus = stackResponse.getCluster().getStatus().name();
                if (stackStatus.startsWith(DELETE_STATUSES_PREFIX)) {
                    clusterService.removeById(id);
                    LOGGER.info("Delete cluster {} due to failing update attempts and Cloudbreak stack status", id);
                } else if (stackStatus.equals(AVAILABLE) && clusterStatus.equals(AVAILABLE)) {
                    // Ambari server is unreacheable but the stack and cluster statuses are "AVAILABLE"
                    reportAmbariServerFailure(cluster, stackResponse, cloudbreakClient);
                    suspendCluster(cluster);
                    LOGGER.info("Suspend cluster monitoring for cluster {} due to failing update attempts and Cloudbreak stack status {}", id, stackStatus);
                } else {
                    suspendCluster(cluster);
                    LOGGER.info("Suspend cluster monitoring for cluster {}", id);
                }
            } catch (Exception ex) {
                LOGGER.warn("Cluster status could not be verified by Cloudbreak for remove. Suspending cluster. Original message: {}",
                        ex.getMessage());
                suspendCluster(cluster);
            }
            updateFailures.remove(id);
        } else {
            updateFailures.put(id, failed + 1);
        }
    }

    private void suspendCluster(Cluster cluster) {
        clusterService.setState(cluster, ClusterState.SUSPENDED);
        LOGGER.info("Suspend cluster monitoring due to failing update attempts");
    }

    private void reportAmbariServerFailure(Cluster cluster, StackResponse stackResponse, CloudbreakClient cbClient) {
        Optional<InstanceMetaDataJson> pgw = stackResponse.getInstanceGroups().stream().flatMap(ig -> ig.getMetadata().stream()).filter(
                im -> im.getInstanceType() == InstanceMetadataType.GATEWAY_PRIMARY && im.getInstanceStatus() != InstanceStatus.TERMINATED).findFirst();
        if (pgw.isPresent()) {
            FailureReport failureReport = new FailureReport();
            failureReport.setFailedNodes(Collections.singletonList(pgw.get().getDiscoveryFQDN()));
            try {
                cbClient.clusterEndpoint().failureReport(cluster.getStackId(), failureReport);
            } catch (Exception e) {
                LOGGER.warn("Exception during failure report. Original message: {}", e.getMessage());
            }
        }
    }
}
