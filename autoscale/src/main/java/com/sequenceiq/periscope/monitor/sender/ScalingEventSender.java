package com.sequenceiq.periscope.monitor.sender;

import static com.sequenceiq.periscope.model.ScalingAdjustmentType.REGULAR;
import static com.sequenceiq.periscope.model.ScalingAdjustmentType.STOPSTART;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.domain.BaseAlert;
import com.sequenceiq.periscope.model.ScalingAdjustmentType;
import com.sequenceiq.periscope.monitor.evaluator.EventPublisher;
import com.sequenceiq.periscope.monitor.event.ScalingEvent;

@Component
public class ScalingEventSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScalingEventSender.class);

    @Inject
    private EventPublisher eventPublisher;

    public void sendScaleUpEvent(BaseAlert baseAlert, Integer existingClusterNodeCount, Integer existingHostGroupSize, Integer servicesHealthyHostGroupSize,
            Integer targetScaleUpCount, Long scalingActivityId) {
        ScalingEvent scalingEvent = new ScalingEvent(baseAlert);
        scalingEvent.setExistingHostGroupNodeCount(existingHostGroupSize);
        scalingEvent.setExistingServiceHealthyHostGroupNodeCount(servicesHealthyHostGroupSize);
        scalingEvent.setExistingClusterNodeCount(existingClusterNodeCount);
        scalingEvent.setDesiredAbsoluteHostGroupNodeCount(existingHostGroupSize + targetScaleUpCount);
        scalingEvent.setScalingAdjustmentType(REGULAR);
        scalingEvent.setActivityId(scalingActivityId);
        LOGGER.info("Triggering scaleUp event: {} for cluster: {}", scalingEvent, baseAlert.getCluster().getStackCrn());
        eventPublisher.publishEvent(scalingEvent);
    }

    public void sendStopStartScaleUpEvent(BaseAlert baseAlert, Integer existingClusterNodeCount, Integer servicesHealthyHostGroupSize,
            Integer targetScaleUpCount, Long scalingActivityId) {
        ScalingEvent scalingEvent = new ScalingEvent(baseAlert);
        scalingEvent.setExistingHostGroupNodeCount(servicesHealthyHostGroupSize);
        scalingEvent.setExistingServiceHealthyHostGroupNodeCount(servicesHealthyHostGroupSize);
        scalingEvent.setExistingClusterNodeCount(existingClusterNodeCount);
        scalingEvent.setDesiredAbsoluteHostGroupNodeCount(servicesHealthyHostGroupSize + targetScaleUpCount);
        scalingEvent.setScalingAdjustmentType(STOPSTART);
        scalingEvent.setActivityId(scalingActivityId);
        LOGGER.info("Triggering stop-start scaleUp event: {} for cluster: {}", scalingEvent, baseAlert.getCluster().getStackCrn());
        eventPublisher.publishEvent(scalingEvent);
    }

    public void sendScaleDownEvent(BaseAlert baseAlert, Integer existingHostGroupSize, List<String> hostsToDecommission,
            Integer servicesHealthyHostGroupSize, ScalingAdjustmentType adjustmentType, Long scalingActivityId) {
        ScalingEvent scalingEvent = new ScalingEvent(baseAlert);
        scalingEvent.setExistingHostGroupNodeCount(existingHostGroupSize);
        scalingEvent.setExistingServiceHealthyHostGroupNodeCount(servicesHealthyHostGroupSize);
        scalingEvent.setDesiredAbsoluteHostGroupNodeCount(existingHostGroupSize - hostsToDecommission.size());
        scalingEvent.setDecommissionNodeIds(hostsToDecommission);
        scalingEvent.setScalingAdjustmentType(adjustmentType);
        scalingEvent.setActivityId(scalingActivityId);
        LOGGER.info("Triggering scaleDown event: {} for cluster: {}", scalingEvent, baseAlert.getCluster().getStackCrn());
        eventPublisher.publishEvent(scalingEvent);
    }
}
