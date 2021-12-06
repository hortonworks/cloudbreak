package com.sequenceiq.cloudbreak.job.instancemetadata;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.job.AbstractStackJobInitializer;

@Component
public class ArchiveInstanceMetaDataJobInitializer extends AbstractStackJobInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveInstanceMetaDataJobInitializer.class);

    @Inject
    private ArchiveInstanceMetaDataConfig config;

    @Inject
    private ArchiveInstanceMetaDataJobService jobService;

    @Override
    public void initJobs() {
        if (config.isArchiveInstanceMetaDataEnabled()) {
            LOGGER.info("Scheduling archive InstanceMetaData jobs");
            getAliveAndNotDeleteInProgressStacksStream()
                    .forEach(s -> jobService.schedule(new ArchiveInstanceMetaDataJobAdapter(s)));
        } else {
            LOGGER.info("Skipping scheduling archive InstanceMetaData jobs, as they are disabled");
        }
    }
}
