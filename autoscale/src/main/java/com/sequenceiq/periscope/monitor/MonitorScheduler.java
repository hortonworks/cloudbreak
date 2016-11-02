package com.sequenceiq.periscope.monitor;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.util.List;

import javax.annotation.PostConstruct;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class MonitorScheduler {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private List<Monitor> monitorList;

    @Autowired
    private Scheduler scheduler;

    @PostConstruct
    public void scheduleMonitors() throws SchedulerException {
        for (Monitor monitor : monitorList) {
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put(MonitorContext.APPLICATION_CONTEXT.name(), applicationContext);
            JobDetail jobDetail = newJob(monitor.getClass()).withIdentity(monitor.getIdentifier()).setJobData(jobDataMap).build();
            CronScheduleBuilder cronBuilder = CronScheduleBuilder.cronSchedule(monitor.getTriggerExpression());
            Trigger trigger = newTrigger().startNow().withSchedule(cronBuilder).build();
            scheduler.scheduleJob(jobDetail, trigger);
        }
    }

}
