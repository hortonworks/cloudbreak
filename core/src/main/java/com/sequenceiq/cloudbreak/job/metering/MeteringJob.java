package com.sequenceiq.cloudbreak.job.metering;

import java.util.Optional;

import javax.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.cloudbreak.service.metering.MeteringService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@DisallowConcurrentExecution
@Component
public class MeteringJob extends StatusCheckerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeteringJob.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private MeteringService meteringService;

    @Override
    protected Optional<MdcContextInfoProvider> getMdcContextConfigProvider() {
        return Optional.ofNullable(stackDtoService.getStackViewById(getLocalIdAsLong()));
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) throws JobExecutionException {
        meteringService.sendMeteringSyncEventForStack(getLocalIdAsLong());
    }
}
