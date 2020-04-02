package com.sequenceiq.periscope.monitor.handler;

import static java.lang.Math.ceil;

import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.periscope.domain.BaseAlert;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.monitor.evaluator.ScalingConstants;
import com.sequenceiq.periscope.monitor.event.ScalingEvent;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.RejectedThreadService;
import com.sequenceiq.periscope.utils.ClusterUtils;
import com.sequenceiq.periscope.utils.StackResponseUtils;

@Component
public class ScalingHandler implements ApplicationListener<ScalingEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScalingHandler.class);

    @Inject
    @Qualifier("periscopeListeningScheduledExecutorService")
    private ExecutorService executorService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private RejectedThreadService rejectedThreadService;

    @Inject
    private StackResponseUtils stackResponseUtils;

    @Inject
    private CloudbreakCommunicator cloudbreakCommunicator;

    @Override
    public void onApplicationEvent(ScalingEvent event) {
        BaseAlert alert = event.getAlert();
        Cluster cluster = clusterService.findById(alert.getCluster().getId());
        MDCBuilder.buildMdcContext(cluster);
        ScalingPolicy policy = alert.getScalingPolicy();

        int hostGroupNodeCount = getHostGroupNodeCount(event, cluster, policy);
        int desiredNodeCount = getDesiredNodeCount(event, hostGroupNodeCount);
        if (hostGroupNodeCount != desiredNodeCount) {
            Runnable scalingRequest = (Runnable) applicationContext.getBean("ScalingRequest", cluster, policy,
                    hostGroupNodeCount, desiredNodeCount, event.getDecommissionNodeIds());

            executorService.submit(scalingRequest);
            rejectedThreadService.remove(cluster.getId());
            cluster.setLastScalingActivityCurrent();
            clusterService.save(cluster);
        } else {
            LOGGER.debug("No scaling activity required for cluster crn {}", cluster.getStackCrn());
        }
    }

    private int getDesiredNodeCount(ScalingEvent event, int hostGroupNodeCount) {
        ScalingPolicy policy = event.getAlert().getScalingPolicy();
        int scalingAdjustment = policy.getScalingAdjustment();
        int desiredHostGroupNodeCount;
        switch (policy.getAdjustmentType()) {
            case NODE_COUNT:
                desiredHostGroupNodeCount = hostGroupNodeCount + scalingAdjustment;
                break;
            case PERCENTAGE:
                desiredHostGroupNodeCount = hostGroupNodeCount
                        + (int) (ceil(hostGroupNodeCount * ((double) scalingAdjustment / ClusterUtils.MAX_CAPACITY)));
                break;
            case EXACT:
                desiredHostGroupNodeCount = policy.getScalingAdjustment();
                break;
            case LOAD_BASED:
                desiredHostGroupNodeCount = hostGroupNodeCount + event.getScaleUpNodeCount()
                        .orElseGet(() -> {
                            return -1 * event.getDecommissionNodeIds().size();
                        });
                break;
            default:
                desiredHostGroupNodeCount = hostGroupNodeCount;
        }
        int minSize = ScalingConstants.DEFAULT_HOSTGROUP_MIN_SIZE;
        int maxSize = ScalingConstants.DEFAULT_HOSTGROUP_MAX_SIZE;

        return desiredHostGroupNodeCount < minSize ? minSize : desiredHostGroupNodeCount > maxSize ? maxSize : desiredHostGroupNodeCount;
    }

    private Integer getHostGroupNodeCount(ScalingEvent event, Cluster cluster, ScalingPolicy policy) {
        return event.getHostGroupNodeCount().orElseGet(() -> {
            StackV4Response stackV4Response = cloudbreakCommunicator.getByCrn(cluster.getStackCrn());
            return stackResponseUtils.getNodeCountForHostGroup(stackV4Response, policy.getHostGroup());
        });
    }
}