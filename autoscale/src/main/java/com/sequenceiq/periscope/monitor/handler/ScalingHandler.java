package com.sequenceiq.periscope.monitor.handler;

import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.periscope.domain.BaseAlert;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.monitor.evaluator.ScalingPolicyTargetCalculator;
import com.sequenceiq.periscope.monitor.event.ScalingEvent;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.RejectedThreadService;

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
    private ScalingPolicyTargetCalculator scalingPolicyTargetCalculator;

    @Override
    public void onApplicationEvent(ScalingEvent event) {
        BaseAlert alert = event.getAlert();
        Cluster cluster = clusterService.findById(alert.getCluster().getId());
        MDCBuilder.buildMdcContext(cluster);
        ScalingPolicy policy = alert.getScalingPolicy();

        int hostGroupNodeCount = event.getHostGroupNodeCount();
        int desiredAbsoluteHostGroupNodeCount = event.getDesiredAbsoluteHostGroupNodeCount();
        if (hostGroupNodeCount != desiredAbsoluteHostGroupNodeCount) {
            Runnable scalingRequest = (Runnable) applicationContext.getBean("ScalingRequest", cluster, policy,
                    hostGroupNodeCount, desiredAbsoluteHostGroupNodeCount, event.getDecommissionNodeIds());

            executorService.submit(scalingRequest);
            rejectedThreadService.remove(cluster.getId());
            cluster.setLastScalingActivityCurrent();
            clusterService.save(cluster);
        } else {
            LOGGER.debug("No scaling activity required for cluster crn {}", cluster.getStackCrn());
        }
    }
}