package com.sequenceiq.cloudbreak.quartz.metric;

import static com.sequenceiq.cloudbreak.quartz.metric.QuartzMetricTag.PROVIDER;
import static com.sequenceiq.cloudbreak.quartz.metric.QuartzMetricTag.TRIGGER_GROUP;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import jakarta.inject.Inject;

import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.Trigger.CompletedExecutionInstruction;
import org.quartz.TriggerKey;
import org.quartz.listeners.TriggerListenerSupport;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.metrics.MetricService;

@Component
public class TriggerMetricsListener extends TriggerListenerSupport {

    @Qualifier("CommonMetricService")
    @Inject
    private MetricService metricService;

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public void triggerFired(Trigger trigger, JobExecutionContext context) {
        TriggerKey triggerKey = trigger.getKey();
        Date triggerTime = trigger.getPreviousFireTime() == null ? trigger.getStartTime() : trigger.getPreviousFireTime();
        Duration triggerDelay = Duration.between(triggerTime.toInstant(), Instant.now());
        getLog().trace("Trigger fired with group: {}, name: {}, delay: {} ms", triggerKey.getGroup(), triggerKey.getName(), triggerDelay.toMillis());
        metricService.incrementMetricCounter(QuartzMetricType.TRIGGER_FIRED,
                TRIGGER_GROUP.name(), triggerKey.getGroup(),
                PROVIDER.name(), QuartzMetricUtil.getProvider(context.getJobDetail().getJobDataMap()));

        metricService.recordTimerMetric(QuartzMetricType.TRIGGER_DELAYED, triggerDelay,
                TRIGGER_GROUP.name(), triggerKey.getGroup(),
                PROVIDER.name(), QuartzMetricUtil.getProvider(context.getJobDetail().getJobDataMap()));
    }

    @Override
    public void triggerMisfired(Trigger trigger) {
        TriggerKey triggerKey = trigger.getKey();
        getLog().warn("Trigger misfired with group: {}, name: {}", triggerKey.getGroup(), triggerKey.getName());
        metricService.incrementMetricCounter(QuartzMetricType.TRIGGER_MISFIRED,
                TRIGGER_GROUP.name(), triggerKey.getGroup(),
                PROVIDER.name(), QuartzMetricUtil.getProvider(trigger.getJobDataMap()));
    }

    @Override
    public void triggerComplete(Trigger trigger, JobExecutionContext context, CompletedExecutionInstruction triggerInstructionCode) {
        TriggerKey triggerKey = trigger.getKey();
        getLog().trace("Trigger completed with group: {}, name: {}, triggerInstructionCode: {}",
                triggerKey.getGroup(), triggerKey.getName(), triggerInstructionCode);
        metricService.incrementMetricCounter(QuartzMetricType.TRIGGER_COMPLETED,
                TRIGGER_GROUP.name(), triggerKey.getGroup(),
                PROVIDER.name(), QuartzMetricUtil.getProvider(context.getJobDetail().getJobDataMap()));
    }
}
