package com.sequenceiq.cloudbreak.quartz.metric;

import static com.sequenceiq.cloudbreak.quartz.metric.QuartzMetricTag.JOB_GROUP;
import static com.sequenceiq.cloudbreak.quartz.metric.QuartzMetricTag.TRIGGER_GROUP;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.listeners.SchedulerListenerSupport;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.metrics.MetricService;

@Component
public class SchedulerMetricsListener extends SchedulerListenerSupport {

    @Inject
    private MetricService metricService;

    private Map<String, AtomicInteger> jobCreatedCountByGroup = new HashMap<>();

    private Map<String, AtomicInteger> jobScheduledCountByGroup = new HashMap<>();

    @Override
    public void jobAdded(JobDetail jobDetail) {
        JobKey jobKey = jobDetail.getKey();
        String group = jobKey.getGroup();
        if (!jobCreatedCountByGroup.containsKey(group)) {
            jobCreatedCountByGroup.put(group,
                    metricService.registerGaugeMetric(QuartzMetricType.JOB_CREATED_COUNT, new AtomicInteger(0), Map.of(JOB_GROUP.name(), group)));
        }
        int updatedJobCount = jobCreatedCountByGroup.get(group).incrementAndGet();
        getLog().debug("Job added with group: {}, name: {}, count: {}", group, jobKey.getName(), updatedJobCount);
    }

    @Override
    public void jobDeleted(JobKey jobKey) {
        String group = jobKey.getGroup();
        if (!jobCreatedCountByGroup.containsKey(group)) {
            jobCreatedCountByGroup.put(group,
                    metricService.registerGaugeMetric(QuartzMetricType.JOB_CREATED_COUNT, new AtomicInteger(0), Map.of(JOB_GROUP.name(), group)));
        }
        int updatedJobCount = jobCreatedCountByGroup.get(group).updateAndGet(jobCount -> jobCount > 0 ? jobCount - 1 : jobCount);
        getLog().debug("Job deleted with group: {}, name: {}, count: {}", group, jobKey.getName(), updatedJobCount);
    }

    @Override
    public void jobScheduled(Trigger trigger) {
        TriggerKey triggerKey = trigger.getKey();
        String group = triggerKey.getGroup();
        if (!jobScheduledCountByGroup.containsKey(group)) {
            jobScheduledCountByGroup.put(group,
                    metricService.registerGaugeMetric(QuartzMetricType.TRIGGER_SCHEDULED_COUNT, new AtomicInteger(0),
                            Map.of(TRIGGER_GROUP.name(), group)));
        }
        int updatedJobCount = jobScheduledCountByGroup.get(group).incrementAndGet();
        getLog().debug("Trigger scheduled with group: {}, name: {}, count: {}", group, triggerKey.getName(), updatedJobCount);
    }

    @Override
    public void jobUnscheduled(TriggerKey triggerKey) {
        String group = triggerKey.getGroup();
        if (!jobScheduledCountByGroup.containsKey(group)) {
            jobScheduledCountByGroup.put(group,
                    metricService.registerGaugeMetric(QuartzMetricType.TRIGGER_SCHEDULED_COUNT, new AtomicInteger(0),
                            Map.of(TRIGGER_GROUP.name(), group)));
        }
        int updatedJobCount = jobScheduledCountByGroup.get(group).updateAndGet(jobCount -> jobCount > 0 ? jobCount - 1 : jobCount);
        getLog().debug("Trigger unscheduled with group: {}, name: {}, count: {}", group, triggerKey.getName(), updatedJobCount);
    }

    @Override
    public void schedulerError(String msg, SchedulerException cause) {
        getLog().warn("Scheduler error occured: {}", msg, cause);
        metricService.incrementMetricCounter(QuartzMetricType.SCHEDULER_ERROR);
    }

    @Override
    public void schedulingDataCleared() {
        getLog().debug("Scheduling data cleared!");
        jobCreatedCountByGroup.values().forEach(jobCreatedCount -> jobCreatedCount.set(0));
        jobScheduledCountByGroup.values().forEach(jobScheduledCount -> jobScheduledCount.set(0));
    }
}
