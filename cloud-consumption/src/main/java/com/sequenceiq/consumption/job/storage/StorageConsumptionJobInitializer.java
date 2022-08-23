package com.sequenceiq.consumption.job.storage;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;
import com.sequenceiq.consumption.domain.Consumption;
import com.sequenceiq.consumption.service.ConsumptionService;

@Component
public class StorageConsumptionJobInitializer implements JobInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageConsumptionJobInitializer.class);

    private static final boolean AGGREGATION_REQUIRED = true;

    @Inject
    private StorageConsumptionJobService jobService;

    @Inject
    private ConsumptionService consumptionService;

    @Inject
    private StorageConsumptionConfig storageConsumptionConfig;

    @Override
    public void initJobs() {
        if (storageConsumptionConfig.isStorageConsumptionEnabled()) {
            LOGGER.info("Starting storage consumption collection job initialization.");
            Map<Boolean, List<Consumption>> consumptionsPartitionedByAggregation = consumptionService.findAllConsumption().stream()
                    .collect(Collectors.partitioningBy(consumptionService::isAggregationRequired));

            List<List<Consumption>> consumptionAggregationGroups = consumptionService
                    .groupConsumptionsByEnvCrnAndBucketName(consumptionsPartitionedByAggregation.get(AGGREGATION_REQUIRED));

            LOGGER.info("Scheduling consumptions that require aggregation.");
            consumptionAggregationGroups.forEach(aggregationGroup ->
                    aggregationGroup.stream()
                            .findFirst()
                            .ifPresent(consumption -> jobService.schedule(consumption.getId())));

            LOGGER.info("Scheduling consumptions that do not require aggregation.");
            consumptionsPartitionedByAggregation.get(!AGGREGATION_REQUIRED)
                    .forEach(consumption -> jobService.schedule(consumption.getId()));
        } else {
            LOGGER.info("Skipping scheduling storage consumption collection jobs as they are disabled");
        }
    }
}
