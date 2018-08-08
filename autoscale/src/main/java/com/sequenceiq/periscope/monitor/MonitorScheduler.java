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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class MonitorScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonitorScheduler.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private List<Monitor> monitorList;

    @Autowired
    private Scheduler scheduler;

    @PostConstruct
    public void scheduleMonitors() throws SchedulerException {
        for (Monitor monitor : monitorList) {
            LOGGER.info("Monitor sceduled: {}, id: {}, cron: {}", monitor.getClass(), monitor.getIdentifier(), monitor.getTriggerExpression());
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put(MonitorContext.APPLICATION_CONTEXT.name(), applicationContext);
            JobDetail jobDetail = newJob(monitor.getClass()).withIdentity(monitor.getIdentifier()).setJobData(jobDataMap).build();
            CronScheduleBuilder cronBuilder = CronScheduleBuilder.cronSchedule(monitor.getTriggerExpression());
            Trigger trigger = newTrigger().startNow().withSchedule(cronBuilder).build();
            scheduler.scheduleJob(jobDetail, trigger);
        }
    }

}
