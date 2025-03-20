package com.sequenceiq.cloudbreak.job.metering.sync;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOPPED;
import static com.sequenceiq.cloudbreak.job.StackStatusCheckerJob.LONG_SYNCABLE_STATES;

import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.cloudbreak.service.metering.MeteringService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;

@DisallowConcurrentExecution
@Component
public class MeteringSyncJob extends StatusCheckerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeteringSyncJob.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private MeteringService meteringService;

    @Inject
    private MeteringSyncJobService meteringSyncJobService;

    @Override
    protected Optional<MdcContextInfoProvider> getMdcContextConfigProvider() {
        return Optional.ofNullable(stackDtoService.getStackViewById(getLocalIdAsLong()));
    }

    @Override
    protected void executeJob(JobExecutionContext context) {
        StackView stack = stackDtoService.getStackViewById(getLocalIdAsLong());
        if (Sets.union(Status.getUnschedulableStatuses(), Set.of(STOPPED)).contains(stack.getStatus())) {
            LOGGER.info("Metering sync job will be unscheduled, stack state is {}", stack.getStatus());
            meteringSyncJobService.unschedule(getLocalId());
        } else if (LONG_SYNCABLE_STATES.contains(stack.getStatus())) {
            LOGGER.info("Metering sync job will be skipped, stack state is {}", stack.getStatus());
        } else {
            meteringService.sendMeteringSyncEventForStack(getLocalIdAsLong());
        }
    }
}
