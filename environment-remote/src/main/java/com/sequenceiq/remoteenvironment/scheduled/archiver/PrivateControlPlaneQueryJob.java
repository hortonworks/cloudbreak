package com.sequenceiq.remoteenvironment.scheduled.archiver;

import java.util.Optional;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.quartz.MdcQuartzJob;

@Component
public class PrivateControlPlaneQueryJob extends MdcQuartzJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrivateControlPlaneQueryJob.class);

    @Override
    protected Optional<MdcContextInfoProvider> getMdcContextConfigProvider() {
        return Optional.empty();
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) throws JobExecutionException {
        try {
            queryPrivateControlPlaneConfigs();
        } catch (TransactionService.TransactionExecutionException e) {
            LOGGER.error("Transaction failed for classic cluster fetch.", e);
            throw new JobExecutionException(e);
        }
    }

    public void queryPrivateControlPlaneConfigs() throws TransactionService.TransactionExecutionException {
        LOGGER.debug("query all config from classic clusters");
        //TODO here comes the classic cluster fetch and merging with our database
    }
}
