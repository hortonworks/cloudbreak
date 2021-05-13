package com.sequenceiq.cloudbreak.quartz.metric;

import java.time.Duration;
import java.time.Instant;

import javax.inject.Inject;

import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.Trigger.CompletedExecutionInstruction;
import org.quartz.TriggerKey;
import org.quartz.listeners.TriggerListenerSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.metrics.MetricService;

@Component
public class TriggerMetricsListener extends TriggerListenerSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(TriggerMetricsListener.class);

    @Inject
    private MetricService metricService;

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public void triggerFired(Trigger trigger, JobExecutionContext context) {
        TriggerKey triggerKey = trigger.getKey();
        Duration triggerDelay = Duration.between(trigger.getStartTime().toInstant(), Instant.now());
        LOGGER.trace("Trigger fired with group: {}, name: {}, delay: {} ms", triggerKey.getGroup(), triggerKey.getName(), triggerDelay.toMillis());
        metricService.incrementMetricCounter(QuartzMetricType.TRIGGER_FIRED,
                "group", triggerKey.getGroup());

        metricService.recordTimerMetric(QuartzMetricType.TRIGGER_DELAYED, triggerDelay,
                "group", triggerKey.getGroup());
    }

    @Override
    public void triggerMisfired(Trigger trigger) {
        TriggerKey triggerKey = trigger.getKey();
        LOGGER.warn("Trigger misfired with group: {}, name: {}", triggerKey.getGroup(), triggerKey.getName());
        metricService.incrementMetricCounter(QuartzMetricType.TRIGGER_MISFIRED,
                "group", triggerKey.getGroup());
    }

    @Override
    public void triggerComplete(Trigger trigger, JobExecutionContext context, CompletedExecutionInstruction triggerInstructionCode) {
        TriggerKey triggerKey = trigger.getKey();
        LOGGER.debug("Trigger completed with group: {}, name: {}, triggerInstructionCode: {}",
                triggerKey.getGroup(), triggerKey.getName(), triggerInstructionCode);
        metricService.incrementMetricCounter(QuartzMetricType.TRIGGER_COMPLETED,
                "group", triggerKey.getGroup());
    }
}
