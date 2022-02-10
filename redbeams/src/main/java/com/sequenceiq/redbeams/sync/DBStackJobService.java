package com.sequenceiq.redbeams.sync;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.quartz.statuschecker.service.StatusCheckerJobService;

@Component
public class DBStackJobService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBStackJobService.class);

    @Inject
    private AutoSyncConfig autoSyncConfig;

    @Inject
    private StatusCheckerJobService jobService;

    public void schedule(JobResource jobResource) {
        if (autoSyncConfig.isEnabled()) {
            jobService.schedule(new DBStackJobAdapter(jobResource));
        }
    }

    public void schedule(Long id) {
        if (autoSyncConfig.isEnabled()) {
            jobService.schedule(id, DBStackJobAdapter.class);
        }
    }

    public void unschedule(Long id, String name) {
        jobService.unschedule(String.valueOf(id));
        LOGGER.info("{} is unscheduled, it will not auto sync anymore", name);
    }
}
