package com.sequenceiq.cloudbreak.quartz.configuration.scheduler;

import java.util.Set;

import jakarta.inject.Inject;

import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.ListenerManager;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.quartz.configuration.SchedulerRuntimeException;

public abstract class TransactionalScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionalScheduler.class);

    private static final String UNKNOWN_SCHEDULER_NAME = "UNKNOWN";

    @Inject
    private TransactionService transactionService;

    public abstract Scheduler getScheduler();

    public void clear() throws TransactionService.TransactionExecutionException {
        transactionService.required(() -> {
            try {
                getScheduler().clear();
            } catch (SchedulerException e) {
                LOGGER.error("Scheduler clear failed.", e);
                throw new SchedulerRuntimeException("Scheduler clear failed", e);
            }
        });
    }

    public void scheduleJob(JobDetail jobDetail, Trigger trigger) throws TransactionService.TransactionExecutionException {
        transactionService.required(() -> {
            try {
                getScheduler().scheduleJob(jobDetail, trigger);
            } catch (SchedulerException e) {
                JobKey jobKey = jobDetail.getKey();
                LOGGER.error("Scheduling job failed, jobKey: {}, jobGroup: {}", jobKey.getName(), jobKey.getGroup(), e);
                throw new SchedulerRuntimeException("Scheduling job failed", e);
            }
        });
    }

    public void deleteJob(JobKey jobKey) throws TransactionService.TransactionExecutionException {
        transactionService.required(() -> {
            try {
                getScheduler().deleteJob(jobKey);
            } catch (SchedulerException e) {
                LOGGER.error("Deleting job failed, jobKey: {}, jobGroup: {}", jobKey.getName(), jobKey.getGroup(), e);
                throw new SchedulerRuntimeException("Deleting job failed", e);
            }
        });
    }

    public JobDetail getJobDetail(JobKey jobKey) throws SchedulerException {
        return getScheduler().getJobDetail(jobKey);
    }

    public ListenerManager getListenerManager() throws SchedulerException {
        return getScheduler().getListenerManager();
    }

    public Set<JobKey> getJobKeys(GroupMatcher<JobKey> groupMatcher) throws SchedulerException {
        return getScheduler().getJobKeys(groupMatcher);
    }

    public String getSchedulerName() {
        try {
            return getScheduler().getSchedulerName();
        } catch (SchedulerException e) {
            LOGGER.warn("Getting scheduler name failed", e);
            return UNKNOWN_SCHEDULER_NAME;
        }
    }
}
