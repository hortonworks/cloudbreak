package com.sequenceiq.cloudbreak.quartz;

import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TransactionalScheduler;

public interface JobSchedulerService {

    String getJobGroup();

    TransactionalScheduler getScheduler();
}
