package com.sequenceiq.periscope.monitor.handler;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.FailureReportV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.FailedNode;
import com.sequenceiq.periscope.monitor.event.UpdateFailedEvent;
import com.sequenceiq.periscope.repository.FailedNodeRepository;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.utils.StackResponseUtils;

@Component
public class UpdateFailedHandler implements ApplicationListener<UpdateFailedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateFailedHandler.class);

    private static final String DELETE_STATUSES_PREFIX = "DELETE_";

    private static final String AVAILABLE = "AVAILABLE";

    private static final int RETRY_THRESHOLD = 5;

    @Inject
    private ClusterService clusterService;

    @Inject
    private StackResponseUtils stackResponseUtils;

    @Inject
    private CloudbreakCommunicator cloudbreakCommunicator;

    @Inject
    private FailedNodeRepository failedNodeRepository;

    private final Map<Long, Integer> updateFailures = new ConcurrentHashMap<>();

    @Override
    public void onApplicationEvent(UpdateFailedEvent event) {
        long autoscaleClusterId = event.getClusterId();
        LOGGER.debug("Cluster {} failed", autoscaleClusterId);
        Cluster cluster = clusterService.findById(autoscaleClusterId);
        if (cluster == null) {
            return;
        }
        MDCBuilder.buildMdcContext(cluster);
        StackV4Response stackResponse = getStackById(cluster.getStackCrn());
        if (stackResponse == null) {
            LOGGER.debug("Suspending cluster {}", autoscaleClusterId);
            suspendCluster(cluster);
            return;
        }
        String stackStatus = getStackStatus(stackResponse);
        if (stackStatus.startsWith(DELETE_STATUSES_PREFIX)) {
            clusterService.removeById(autoscaleClusterId);
            long deletedFailedNodeRecords = failedNodeRepository.deleteByClusterId(autoscaleClusterId);
            LOGGER.debug("Delete cluster {} due to failing update attempts and Cloudbreak stack status. {} deleted failed node records.",
                    autoscaleClusterId,
                    deletedFailedNodeRecords);
            return;
        }
        Integer failed = updateFailures.get(autoscaleClusterId);
        if (failed == null) {
            LOGGER.debug("New failed cluster id: [{}]", autoscaleClusterId);
            updateFailures.put(autoscaleClusterId, 1);
        } else if (RETRY_THRESHOLD - 1 == failed) {
            Status clusterStatus = stackResponse.getCluster().getStatus();
            if (stackStatus.equals(AVAILABLE) && clusterStatus != null && clusterStatus.name().equals(AVAILABLE)) {
                // Cluster manager server is unreacheable but the stack and cluster statuses are "AVAILABLE"
                reportClusterManagerServerFailure(cluster, stackResponse);
                LOGGER.debug("Suspend cluster monitoring for cluster {} due to failing update attempts and Cloudbreak stack status {}",
                        autoscaleClusterId, stackStatus);
            } else {
                LOGGER.debug("Suspend cluster monitoring for cluster {}", autoscaleClusterId);
            }
            suspendCluster(cluster);
            updateFailures.remove(autoscaleClusterId);
        } else {
            int value = failed + 1;
            LOGGER.debug("Increase failed count[{}] for cluster id: [{}]", value, autoscaleClusterId);
            updateFailures.put(autoscaleClusterId, value);
        }
    }

    private String getStackStatus(StackV4Response stackResponse) {
        return stackResponse.getStatus() != null ? stackResponse.getStatus().name() : "";
    }

    private StackV4Response getStackById(String stackCrn) {
        try {
            return cloudbreakCommunicator.getByCrn(stackCrn);
        } catch (Exception e) {
            LOGGER.warn("Cluster status could not be verified by Cloudbreak. Original message: {}",
                    e.getMessage());
            return null;
        }
    }

    private void suspendCluster(Cluster cluster) {
        clusterService.setState(cluster, ClusterState.SUSPENDED);
    }

    private void reportClusterManagerServerFailure(Cluster cluster, StackV4Response stackResponse) {
        Optional<InstanceMetaDataV4Response> pgw = stackResponseUtils.getNotTerminatedPrimaryGateways(stackResponse);
        if (pgw.isPresent()) {
            FailureReportV4Request failureReport = new FailureReportV4Request();
            failureReport.setFailedNodes(Collections.singletonList(pgw.get().getDiscoveryFQDN()));
            try {
                cloudbreakCommunicator.failureReport(cluster.getStackCrn(), failureReport);
                FailedNode failedNode = new FailedNode();
                failedNode.setClusterId(cluster.getId());
                failedNode.setName(pgw.get().getDiscoveryFQDN());
                failedNodeRepository.save(failedNode);
            } catch (Exception e) {
                LOGGER.warn("Exception during failure report. Original message: {}", e.getMessage());
                throw new RuntimeException(e);
            }
        }
    }
}
