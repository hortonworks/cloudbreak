package com.sequenceiq.cloudbreak.quartz.metric;

import java.util.Set;
import java.util.function.ToDoubleFunction;

import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TransactionalScheduler;

public class GroupNameToJobCountFunction implements ToDoubleFunction<String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroupNameToJobCountFunction.class);

    private TransactionalScheduler scheduler;

    public GroupNameToJobCountFunction(TransactionalScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public double applyAsDouble(String groupName) {
        try {
            Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.groupEquals(groupName));
            return jobKeys.size();
        } catch (SchedulerException e) {
            LOGGER.error("Cannot get job count for group {}", groupName, e);
        }
        return 0.0;
    }
}
