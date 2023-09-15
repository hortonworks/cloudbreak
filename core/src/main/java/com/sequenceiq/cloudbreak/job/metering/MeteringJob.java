package com.sequenceiq.cloudbreak.job.metering;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETED_ON_PROVIDER_SIDE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOPPED;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
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
public class MeteringJob extends StatusCheckerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeteringJob.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private MeteringService meteringService;

    @Inject
    private MeteringJobService meteringJobService;

    @Override
    protected Optional<MdcContextInfoProvider> getMdcContextConfigProvider() {
        return Optional.ofNullable(stackDtoService.getStackViewById(getLocalIdAsLong()));
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) throws JobExecutionException {
        StackView stack = stackDtoService.getStackViewById(getLocalIdAsLong());
        if (Sets.union(Status.getUnschedulableStatuses(), Set.of(STOPPED, DELETED_ON_PROVIDER_SIDE)).contains(stack.getStatus())) {
            LOGGER.info("Metering sync job will be unscheduled, stack state is {}", stack.getStatus());
            meteringJobService.unschedule(getLocalId());
        } else {
            meteringService.sendMeteringSyncEventForStack(getLocalIdAsLong());
        }
    }
}
