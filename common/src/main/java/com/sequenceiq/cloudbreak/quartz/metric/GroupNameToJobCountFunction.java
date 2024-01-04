package com.sequenceiq.cloudbreak.quartz.metric;

import java.util.Set;
import java.util.function.ToDoubleFunction;

import jakarta.inject.Inject;

import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.configuration.TransactionalScheduler;

@Component
public class GroupNameToJobCountFunction implements ToDoubleFunction<String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroupNameToJobCountFunction.class);

    @Inject
    private TransactionalScheduler scheduler;

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
