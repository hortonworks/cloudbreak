package com.sequenceiq.cloudbreak.quartz.configuration;

import static com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;

import java.util.Set;

import javax.inject.Inject;

import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.ListenerManager;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.service.TransactionService;

@Primary
@Component
public class TransactionalScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionalScheduler.class);

    @Inject
    private Scheduler scheduler;

    @Inject
    private TransactionService transactionService;

    public void clear() throws TransactionExecutionException {
        transactionService.required(() -> {
            try {
                getScheduler().clear();
            } catch (SchedulerException e) {
                LOGGER.error("Scheduler clear failed.", e);
                throw new SchedulerRuntimeException("Scheduler clear failed", e);
            }
        });
    }

    public void scheduleJob(JobDetail jobDetail, Trigger trigger) throws TransactionExecutionException {
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

    public void deleteJob(JobKey jobKey) throws TransactionExecutionException {
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

    protected Scheduler getScheduler() {
        return scheduler;
    }
}