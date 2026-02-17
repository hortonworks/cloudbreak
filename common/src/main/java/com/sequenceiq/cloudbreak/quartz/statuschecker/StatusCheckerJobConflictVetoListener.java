package com.sequenceiq.cloudbreak.quartz.statuschecker;

import java.util.Optional;

import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.listeners.TriggerListenerSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatusCheckerJobConflictVetoListener extends TriggerListenerSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatusCheckerJobConflictVetoListener.class);

    private final String jobGroupName;

    public StatusCheckerJobConflictVetoListener(String jobGroupName) {
        this.jobGroupName = jobGroupName;
    }

    @Override
    public String getName() {
        return String.format("%s-%s", jobGroupName, getClass().getSimpleName());
    }

    @Override
    public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) {
        StatusCheckerJobKey triggerJobKey = StatusCheckerJobKey.fromQuartzJobKey(trigger.getJobKey());
        if (triggerJobKey.groupName().equals(jobGroupName)) {
            LOGGER.debug("Checking trigger {} for veto", triggerJobKey);
            try {
                Optional<StatusCheckerJobKey> conflictingJobKey = getConflictingJobKey(context, triggerJobKey);
                if (conflictingJobKey.isPresent()) {
                    LOGGER.warn("Vetoing trigger {} because of conflict with {}", triggerJobKey, conflictingJobKey.get());
                    return true;
                }
            } catch (SchedulerException e) {
                LOGGER.error("Quartz sceduler threw exception, so vetoing trigger {}", triggerJobKey, e);
                return true;
            }
        }

        LOGGER.debug("Letting trigger {} fire", triggerJobKey);
        return false;
    }

    private Optional<StatusCheckerJobKey> getConflictingJobKey(JobExecutionContext context, StatusCheckerJobKey triggerJobKey) throws SchedulerException {
        return context.getScheduler().getCurrentlyExecutingJobs().stream()
                .map(jobExecutionContext -> StatusCheckerJobKey.fromQuartzJobKey(jobExecutionContext.getJobDetail().getKey()))
                .filter(triggerJobKey::hasConflictWith)
                .findFirst();
    }

    public void init(Scheduler scheduler) throws SchedulerException {
        LOGGER.debug("Adding StatusCheckerJobConflictVetoListener for {}", jobGroupName);
        scheduler.getListenerManager().addTriggerListener(this);
    }
}
