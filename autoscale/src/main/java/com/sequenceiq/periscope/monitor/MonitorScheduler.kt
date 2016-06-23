package com.sequenceiq.periscope.monitor

import org.quartz.JobBuilder.newJob
import org.quartz.TriggerBuilder.newTrigger

import javax.annotation.PostConstruct

import org.quartz.CronScheduleBuilder
import org.quartz.JobDataMap
import org.quartz.JobDetail
import org.quartz.Scheduler
import org.quartz.SchedulerException
import org.quartz.Trigger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

@Component
class MonitorScheduler {

    @Autowired
    private val applicationContext: ApplicationContext? = null
    @Autowired
    private val monitorList: List<Monitor>? = null
    @Autowired
    private val scheduler: Scheduler? = null

    @PostConstruct
    @Throws(SchedulerException::class)
    fun scheduleMonitors() {
        for (monitor in monitorList!!) {
            val jobDataMap = JobDataMap()
            jobDataMap.put(MonitorContext.APPLICATION_CONTEXT.name, applicationContext)
            val jobDetail = newJob(monitor.javaClass).withIdentity(monitor.identifier).setJobData(jobDataMap).build()
            val cronBuilder = CronScheduleBuilder.cronSchedule(monitor.triggerExpression)
            val trigger = newTrigger().startNow().withSchedule(cronBuilder).build()
            scheduler!!.scheduleJob(jobDetail, trigger)
        }
    }

}
