package com.sequenceiq.periscope.monitor.handler;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.flow.api.model.RetryableFlowResponse;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.monitor.event.ClusterScaleStatusEvent;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.utils.LoggingUtils;

@Component
public class ClusterScaleStatusHandler implements ApplicationListener<ClusterScaleStatusEvent> {

    public static final int MS_IN_A_DAY = 24 * 60 * 60 * 1000;

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterScaleStatusHandler.class);

    @Inject
    private ClusterService clusterService;

    @Inject
    private CloudbreakCommunicator cloudbreakCommunicator;

    @Override
    public void onApplicationEvent(ClusterScaleStatusEvent event) {
        long autoscaleClusterId = event.getClusterId();
        Cluster cluster = clusterService.findById(autoscaleClusterId);
        if (cluster == null) {
            return;
        }
        LoggingUtils.buildMdcContext(cluster);
        StackStatusV4Response statusResponse = cloudbreakCommunicator.getStackStatusByCrn(cluster.getStackCrn());
        LOGGER.debug("Analysing CB Cluster Status '{}' for Cluster '{}'", statusResponse, cluster.getStackCrn());
        boolean clusterAvailable = Optional.ofNullable(statusResponse.getStatus()).map(Status::isAvailable).orElse(false)
                && Optional.ofNullable(statusResponse.getClusterStatus()).map(Status::isAvailable).orElse(false);
        LOGGER.debug("CB Cluster availability is '{}' for Cluster object '{}'", clusterAvailable, cluster);
        if (!clusterAvailable) {
            LOGGER.debug("Checking retriable flows for Cluster '{}'", cluster.getStackCrn());
            List<RetryableFlowResponse> retryableFlowResponses =
                    cloudbreakCommunicator.listRetryableFlows(cluster.getStackName(), cluster.getClusterPertain().getTenant());
            LOGGER.debug("CB Cluster '{}' has retryable flows '{}'", cluster, retryableFlowResponses);
            if (retryableFlowResponses.isEmpty()) {
                LOGGER.debug("No retriable operations in cluster '{}'",
                        cluster.getStackCrn());
                return;
            }
            // Retry the failed scale operation only once a day, unless there are successive scaling activities
            if (retryableFlowResponses.get(0).getName().contains("scale") &&
                    cluster.getLastScalingActivity() > cluster.getLastRetried() &&
                    (System.currentTimeMillis() - cluster.getLastRetried()) > MS_IN_A_DAY) {
                LOGGER.debug("Retrying failed scale operation in cluster '{}'",
                        cluster.getStackCrn());
                cloudbreakCommunicator.retryLastFailedScale(cluster.getStackName(), cluster.getClusterPertain().getTenant());
                LOGGER.debug("Retried failed scale operation in cluster '{}'",
                        cluster.getStackCrn());
                cluster.setLastRetried(System.currentTimeMillis());
                clusterService.save(cluster);
            }
        }
    }
}
