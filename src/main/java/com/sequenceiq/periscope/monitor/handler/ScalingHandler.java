package com.sequenceiq.periscope.monitor.handler;

import static java.lang.Math.ceil;

import java.util.concurrent.ExecutorService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.domain.BaseAlert;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.log.Logger;
import com.sequenceiq.periscope.log.PeriscopeLoggerFactory;
import com.sequenceiq.periscope.monitor.event.ScalingEvent;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.utils.ClusterUtils;

@Component
public class ScalingHandler implements ApplicationListener<ScalingEvent> {

    private static final Logger LOGGER = PeriscopeLoggerFactory.getLogger(ScalingHandler.class);

    @Autowired
    private ExecutorService executorService;
    @Autowired
    private ClusterService clusterService;
    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public void onApplicationEvent(ScalingEvent event) {
        BaseAlert alert = event.getAlert();
        Cluster cluster = clusterService.find(alert.getCluster().getId());
        scale(cluster, alert.getScalingPolicy());
    }

    private void scale(Cluster cluster, ScalingPolicy policy) {
        long clusterId = cluster.getId();
        long remainingTime = getRemainingCooldownTime(cluster);
        if (remainingTime <= 0) {
            int totalNodes = ClusterUtils.getTotalNodes(cluster);
            int desiredNodeCount = getDesiredNodeCount(cluster, policy, totalNodes);
            if (totalNodes != desiredNodeCount) {
                ScalingRequest scalingRequest = (ScalingRequest)
                        applicationContext.getBean("ScalingRequest", cluster, policy, totalNodes, desiredNodeCount);
                executorService.execute(scalingRequest);
                cluster.setLastScalingActivityCurrent();
                clusterService.save(cluster);
            } else {
                LOGGER.info(clusterId, "No scaling activity required");
            }
        } else {
            LOGGER.info(clusterId, "Cluster cannot be scaled for {} min(s)",
                    ClusterUtils.TIME_FORMAT.format((double) remainingTime / ClusterUtils.MIN_IN_MS));
        }
    }

    private long getRemainingCooldownTime(Cluster cluster) {
        int coolDown = cluster.getCoolDown();
        long lastScalingActivity = cluster.getLastScalingActivity();
        return lastScalingActivity == 0 ? 0 : (coolDown * ClusterUtils.MIN_IN_MS) - (System.currentTimeMillis() - lastScalingActivity);
    }

    private int getDesiredNodeCount(Cluster cluster, ScalingPolicy policy, int totalNodes) {
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
        return desiredNodeCount < minSize ? minSize : desiredNodeCount > maxSize ? maxSize : desiredNodeCount;
    }

}