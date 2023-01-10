package com.sequenceiq.consumption.job.storage;

import java.util.Optional;

import javax.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.consumption.domain.Consumption;
import com.sequenceiq.consumption.flow.ConsumptionReactorFlowManager;
import com.sequenceiq.consumption.service.ConsumptionService;

@DisallowConcurrentExecution
@Component
public class StorageConsumptionJob extends StatusCheckerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageConsumptionJob.class);

    @Inject
    private ConsumptionService consumptionService;

    @Inject
    private ConsumptionReactorFlowManager flowManager;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Override
    protected Optional<MdcContextInfoProvider> getMdcContextConfigProvider() {
        return Optional.empty();
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) throws JobExecutionException {
        try {
            Consumption consumption = consumptionService.findConsumptionById(getLocalIdAsLong());
            LOGGER.info("Triggering storage consumption collection flow for consumption with ID [{}].", consumption.getId());
            RegionAwareInternalCrnGenerator crnGenerator = regionAwareInternalCrnGeneratorFactory.consumption();
            flowManager.triggerStorageConsumptionCollectionFlow(consumption, crnGenerator.getInternalCrnForServiceAsString());
        } catch (Exception e) {
            LOGGER.error("Failed triggering storage consumption collection flow for consumption with ID [{}].", getLocalIdAsLong(), e);
            throw new JobExecutionException(String.format("Failed triggering storage consumption collection flow for consumption with ID [%s], exception: %s",
                    getLocalId(), e));
        }
    }

    @Override
    protected Optional<Object> getMdcContextObject() {
        return Optional.ofNullable(consumptionService.findConsumptionById(getLocalIdAsLong()));
    }
}
