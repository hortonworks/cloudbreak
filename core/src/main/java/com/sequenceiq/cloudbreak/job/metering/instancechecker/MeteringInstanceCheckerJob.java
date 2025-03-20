package com.sequenceiq.cloudbreak.job.metering.instancechecker;

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
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.cloudbreak.service.metering.MeteringInstanceCheckerService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stackstatus.StackStatusService;

@DisallowConcurrentExecution
@Component
public class MeteringInstanceCheckerJob extends StatusCheckerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeteringInstanceCheckerJob.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private StackStatusService stackStatusService;

    @Inject
    private MeteringInstanceCheckerService meteringInstanceCheckerService;

    @Inject
    private MeteringInstanceCheckerJobService meteringInstanceCheckerJobService;

    @Override
    protected Optional<MdcContextInfoProvider> getMdcContextConfigProvider() {
        return Optional.ofNullable(stackDtoService.getStackViewById(getLocalIdAsLong()));
    }

    @Override
    protected void executeJob(JobExecutionContext context) {
        StackStatus stackStatus = stackStatusService.findFirstByStackIdOrderByCreatedDesc(getLocalIdAsLong())
                .orElseThrow(NotFoundException.notFound("stack", getLocalIdAsLong()));
        if (Sets.union(Status.getUnschedulableStatuses(), Set.of(STOPPED)).contains(stackStatus.getStatus())) {
            LOGGER.info("Metering instance checker job will be unscheduled, stack state is {}", stackStatus.getStatus());
            meteringInstanceCheckerJobService.unschedule(getLocalId());
        } else if (LONG_SYNCABLE_STATES.contains(stackStatus.getStatus())) {
            LOGGER.info("Metering instance checker job will be skipped, stack state is {}", stackStatus.getStatus());
        } else {
            meteringInstanceCheckerService.checkInstanceTypes(getLocalIdAsLong());
        }
    }
}
