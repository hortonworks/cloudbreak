package com.sequenceiq.redbeams.sync;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.statuschecker.service.StatusCheckerJobService;
import com.sequenceiq.redbeams.domain.stack.DBStack;

@Component
public class DBStackJobService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBStackJobService.class);

    @Inject
    private AutoSyncConfig autoSyncConfig;

    @Inject
    private StatusCheckerJobService jobService;

    public void schedule(DBStack dbStack) {
        if (autoSyncConfig.isEnabled()) {
            jobService.schedule(new DBStackJobAdapter(dbStack));
            LOGGER.info("{} is scheduled for auto sync", dbStack.getName());
        }
    }

    public void unschedule(DBStack dbStack) {
        jobService.unschedule(new DBStackJobAdapter(dbStack).getLocalId());
        LOGGER.info("{} is unscheduled, it will not auto sync anymore", dbStack.getName());
    }
}
