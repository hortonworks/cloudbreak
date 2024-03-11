package com.sequenceiq.cloudbreak.quartz.metric;

import static com.sequenceiq.cloudbreak.quartz.metric.QuartzMetricTag.PROVIDER;
import static com.sequenceiq.cloudbreak.quartz.metric.QuartzMetricTag.SCHEDULER;
import static com.sequenceiq.cloudbreak.quartz.metric.QuartzMetricTag.TRIGGER_GROUP;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.Trigger.CompletedExecutionInstruction;
import org.quartz.TriggerKey;
import org.quartz.listeners.TriggerListenerSupport;

import com.sequenceiq.cloudbreak.common.metrics.MetricService;

public class TriggerMetricsListener extends TriggerListenerSupport {

    private MetricService metricService;

    private String schedulerName;

    public TriggerMetricsListener(MetricService metricService, String schedulerName) {
        this.metricService = metricService;
        this.schedulerName = schedulerName;
    }

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
                SCHEDULER.name(), schedulerName,
                TRIGGER_GROUP.name(), triggerKey.getGroup(),
                PROVIDER.name(), QuartzMetricUtil.getProvider(context.getJobDetail().getJobDataMap()));

        metricService.recordTimerMetric(QuartzMetricType.TRIGGER_DELAYED, triggerDelay,
                SCHEDULER.name(), schedulerName,
                TRIGGER_GROUP.name(), triggerKey.getGroup(),
                PROVIDER.name(), QuartzMetricUtil.getProvider(context.getJobDetail().getJobDataMap()));
    }

    @Override
    public void triggerMisfired(Trigger trigger) {
        TriggerKey triggerKey = trigger.getKey();
        getLog().warn("Trigger misfired with group: {}, name: {}", triggerKey.getGroup(), triggerKey.getName());
        metricService.incrementMetricCounter(QuartzMetricType.TRIGGER_MISFIRED,
                SCHEDULER.name(), schedulerName,
                TRIGGER_GROUP.name(), triggerKey.getGroup(),
                PROVIDER.name(), QuartzMetricUtil.getProvider(trigger.getJobDataMap()));
    }

    @Override
    public void triggerComplete(Trigger trigger, JobExecutionContext context, CompletedExecutionInstruction triggerInstructionCode) {
        TriggerKey triggerKey = trigger.getKey();
        getLog().trace("Trigger completed with group: {}, name: {}, triggerInstructionCode: {}",
                triggerKey.getGroup(), triggerKey.getName(), triggerInstructionCode);
        metricService.incrementMetricCounter(QuartzMetricType.TRIGGER_COMPLETED,
                SCHEDULER.name(), schedulerName,
                TRIGGER_GROUP.name(), triggerKey.getGroup(),
                PROVIDER.name(), QuartzMetricUtil.getProvider(context.getJobDetail().getJobDataMap()));
    }
}
