package com.sequenceiq.consumption.job.storage;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;
import com.sequenceiq.consumption.service.ConsumptionService;

@Component
public class StorageConsumptionJobInitializer implements JobInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageConsumptionJobInitializer.class);

    @Inject
    private StorageConsumptionJobService jobService;

    @Inject
    private ConsumptionService consumptionService;

    @Inject
    private StorageConsumptionConfig storageConsumptionConfig;

    @Override
    public void initJobs() {
        if (storageConsumptionConfig.isStorageConsumptionEnabled()) {
            LOGGER.info("Scheduling Storage consumption collection jobs");
            consumptionService.findAllStorageConsumptionJobResource()
                    .forEach(storageConsumption -> jobService.schedule(new StorageConsumptionJobAdapter(storageConsumption)));
        } else {
            LOGGER.info("Skipping scheduling storage consumption collection jobs as they are disabled");
        }
    }
}
