package com.sequenceiq.periscope.monitor;

import static java.lang.Math.ceil;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.service.Clock;
import com.sequenceiq.periscope.domain.BaseAlert;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.monitor.executor.LoggedExecutorService;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.RejectedThreadService;
import com.sequenceiq.periscope.utils.ClusterUtils;
import com.sequenceiq.periscope.utils.TimeUtil;

@Component
public class ScalingHandlerUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScalingHandlerUtil.class);

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private RejectedThreadService rejectedThreadService;

    @Inject
    private CloudbreakClient cloudbreakClient;

    @Inject
    private ClusterService clusterService;

    @Inject
    private LoggedExecutorService loggedExecutorService;

    @Inject
    private Clock clock;

    public void scaleIfNeed(Cluster cluster, BaseAlert alert) {
        ScalingPolicy policy = alert.getScalingPolicy();
        int totalNodes = Math.toIntExact(cloudbreakClient.autoscaleEndpoint().getHostMetadataCountForAutoscale(cluster.getStackId(), policy.getHostGroup()));
        int desiredNodeCount = getDesiredNodeCount(cluster, policy, totalNodes);
        if (totalNodes != desiredNodeCount) {
            LOGGER.info("{} cluster id will be scaled up with {} policy", cluster.getId(), policy.getName());
            cluster.setLastScalingActivityCurrent();
            clusterService.updateLastScalingActivity(cluster);
            scale(cluster, policy);
        } else {
            LOGGER.info("No scaling activity required for '{}' policy", policy.getName());
        }
    }

    private void scale(Cluster cluster, ScalingPolicy policy) {
        int totalNodes = Math.toIntExact(cloudbreakClient.autoscaleEndpoint().getHostMetadataCountForAutoscale(cluster.getStackId(), policy.getHostGroup()));
        int desiredNodeCount = getDesiredNodeCount(cluster, policy, totalNodes);
        Runnable scalingRequest = (Runnable) applicationContext.getBean("ScalingRequest", cluster, policy, totalNodes, desiredNodeCount);
        loggedExecutorService.submit("ScalingHandler", scalingRequest);
        rejectedThreadService.remove(cluster.getId());
    }

    public synchronized boolean isCooldownElapsed(Cluster cluster) {
        long remainingTime = getRemainingCooldownTime(cluster);
        if (remainingTime <= 0) {
            LOGGER.info("Cooldown elapsed for cluster: {}", cluster.getId());
            return true;
        }
        LOGGER.info("Cluster cannot be scaled for {} min(s)",
                ClusterUtils.TIME_FORMAT.format((double) remainingTime / TimeUtil.MIN_IN_MS));
        return false;
    }

    private long getRemainingCooldownTime(Cluster cluster) {
        long coolDown = cluster.getCoolDown();
        long lastScalingActivity = cluster.getLastScalingActivity();
        return lastScalingActivity == 0L ? 0L : (coolDown * TimeUtil.MIN_IN_MS) - (clock.getCurrentTime() - lastScalingActivity);
    }

    @VisibleForTesting
    protected int getDesiredNodeCount(Cluster cluster, ScalingPolicy policy, int totalNodes) {
        int scalingAdjustment = policy.getScalingAdjustment();
        int desiredNodeCount;
        switch (policy.getAdjustmentType()) {
            case NODE_COUNT:
                desiredNodeCount = totalNodes + scalingAdjustment;
                break;
            case PERCENTAGE:
                desiredNodeCount = totalNodes
                        + (int) (ceil(totalNodes * ((double) scalingAdjustment / ClusterUtils.MAX_CAPACITY)));
                break;
            case EXACT:
                desiredNodeCount = policy.getScalingAdjustment();
                break;
            default:
                desiredNodeCount = totalNodes;
        }
        int minSize = cluster.getMinSize();
        int maxSize = cluster.getMaxSize();
        LOGGER.info("Desired node count calculated by minSize: {} maxSixe: {}, desiredCount {}, total nodes: {} for cluster: {}",
                minSize, maxSize, desiredNodeCount, totalNodes, cluster.getId());
        return desiredNodeCount < minSize ? minSize : Math.min(desiredNodeCount, maxSize);
    }
}
