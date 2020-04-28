package com.sequenceiq.redbeams.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.statuschecker.service.JobService;

@Component
public class DBStackJobService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBStackJobService.class);

    @Inject
    private AutoSyncConfig autoSyncConfig;

    @Inject
    private JobService jobService;

    public void deleteAll() {
        jobService.deleteAll();
    }

    public void schedule(DBStack dbStack) {
        if (dbStack.getCloudPlatform().equalsIgnoreCase(CloudPlatform.AWS.name()) && autoSyncConfig.isEnabled()) {
            jobService.schedule(new DBStackJobAdapter(dbStack));
            LOGGER.info("{} is scheduled for auto sync", dbStack.getName());
        }
    }

    public void unschedule(DBStack dbStack) {
        if (dbStack.getCloudPlatform().equalsIgnoreCase(CloudPlatform.AWS.name())) {
            jobService.unschedule(new DBStackJobAdapter(dbStack).getLocalId());
            LOGGER.info("{} is unscheduled, it will not auto sync anymore", dbStack.getName());
        }
    }
}
