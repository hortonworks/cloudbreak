package com.sequenceiq.periscope.monitor.handler;

import java.util.List;
import java.util.concurrent.ExecutorService;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.periscope.api.model.AlertType;
import com.sequenceiq.periscope.api.model.ScalingStatus;
import com.sequenceiq.periscope.common.MessageCode;
import com.sequenceiq.periscope.domain.BaseAlert;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.domain.TimeAlert;
import com.sequenceiq.periscope.monitor.event.ScalingEvent;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.HistoryService;
import com.sequenceiq.periscope.service.RejectedThreadService;
import com.sequenceiq.periscope.utils.LoggingUtils;

@Component
public class ScalingHandler implements ApplicationListener<ScalingEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScalingHandler.class);

    @Inject
    @Qualifier("periscopeListeningScheduledExecutorService")
    private ExecutorService executorService;

    @Inject
    @Qualifier("periscopeTimeMonitorScheduledExecutorService")
    private ExecutorService executorTimeMonitorService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private HistoryService historyService;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private RejectedThreadService rejectedThreadService;

    @Inject
    private CloudbreakMessagesService messagesService;

    @Override
    public void onApplicationEvent(ScalingEvent event) {
        BaseAlert alert = event.getAlert();
        Cluster cluster = clusterService.findById(alert.getCluster().getId());
        LoggingUtils.buildMdcContext(cluster);
        ScalingPolicy policy = alert.getScalingPolicy();

        int hostGroupNodeCount = event.getExistingHostGroupNodeCount();
        int desiredAbsoluteHostGroupNodeCount = event.getDesiredAbsoluteHostGroupNodeCount();
        if (hostGroupNodeCount != desiredAbsoluteHostGroupNodeCount) {
            Runnable scalingRequest = (Runnable) applicationContext.getBean("ScalingRequest", cluster, policy,
                    event.getExistingClusterNodeCount(), hostGroupNodeCount, desiredAbsoluteHostGroupNodeCount,
                    event.getDecommissionNodeIds(),
                    event.getExistingServiceHealthyHostGroupNodeCount(), event.getScalingAdjustmentType(), event.getActivityId());

            if (alert.getAlertType().equals(AlertType.TIME)) {
                executorTimeMonitorService.submit(scalingRequest);
            } else {
                executorService.submit(scalingRequest);
            }
            rejectedThreadService.remove(cluster.getId());
            clusterService.setLastScalingActivity(cluster.getId(), System.currentTimeMillis());
        } else {
            LOGGER.info("Autoscaling activity not required for config '{}', cluster '{}'.", alert.getName(), cluster.getStackCrn());
            if (alert instanceof TimeAlert) {
                historyService.createEntry(ScalingStatus.SUCCESS, messagesService.getMessage(MessageCode.AUTOSCALING_ACTIVITY_NOT_REQUIRED,
                        List.of(alert.getAlertType(), alert.getName(), alert.getScalingPolicy().getHostGroup(), hostGroupNodeCount)),
                        hostGroupNodeCount, 0, policy);
            }
        }
    }
}
