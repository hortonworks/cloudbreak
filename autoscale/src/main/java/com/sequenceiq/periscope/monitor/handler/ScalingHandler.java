package com.sequenceiq.periscope.monitor.handler;

import static java.lang.Math.ceil;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.periscope.aspects.AmbariRequestLogging;
import com.sequenceiq.periscope.domain.BaseAlert;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.monitor.event.ScalingEvent;
import com.sequenceiq.periscope.monitor.executor.LoggedExecutorService;
import com.sequenceiq.periscope.service.AmbariClientProvider;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.RejectedThreadService;
import com.sequenceiq.periscope.utils.ClusterUtils;
import com.sequenceiq.periscope.utils.TimeUtil;

@Component
public class ScalingHandler implements ApplicationListener<ScalingEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScalingHandler.class);

    @Inject
    private LoggedExecutorService loggedExecutorService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private AmbariClientProvider ambariClientProvider;

    @Inject
    private RejectedThreadService rejectedThreadService;

    @Inject
    private AmbariRequestLogging ambariRequestLogging;

    @Override
    public void onApplicationEvent(ScalingEvent event) {
        BaseAlert alert = event.getAlert();
        Cluster cluster = clusterService.findById(alert.getCluster().getId());
        MDCBuilder.buildMdcContext(cluster);
        scale(cluster, alert.getScalingPolicy());
    }

    private void scale(Cluster cluster, ScalingPolicy policy) {
        long remainingTime = getRemainingCooldownTime(cluster);
        if (remainingTime <= 0) {
            AmbariClient ambariClient = ambariClientProvider.createAmbariClient(cluster);
            int totalNodes = ambariRequestLogging.logging(ambariClient::getClusterHosts, "clusterHosts").size();
            int desiredNodeCount = getDesiredNodeCount(cluster, policy, totalNodes);
            if (totalNodes != desiredNodeCount) {
                Runnable scalingRequest = (Runnable) applicationContext.getBean("ScalingRequest", cluster, policy, totalNodes, desiredNodeCount);
                loggedExecutorService.submit("ScalingHandler", scalingRequest);
                rejectedThreadService.remove(cluster.getId());
                cluster.setLastScalingActivityCurrent();
                clusterService.save(cluster);
            } else {
                LOGGER.info("No scaling activity required");
            }
        } else {
            LOGGER.info("Cluster cannot be scaled for {} min(s)",
                    ClusterUtils.TIME_FORMAT.format((double) remainingTime / TimeUtil.MIN_IN_MS));
        }
    }

    private long getRemainingCooldownTime(Cluster cluster) {
        long coolDown = cluster.getCoolDown();
        long lastScalingActivity = cluster.getLastScalingActivity();
        return lastScalingActivity == 0L ? 0L : (coolDown * TimeUtil.MIN_IN_MS) - (System.currentTimeMillis() - lastScalingActivity);
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