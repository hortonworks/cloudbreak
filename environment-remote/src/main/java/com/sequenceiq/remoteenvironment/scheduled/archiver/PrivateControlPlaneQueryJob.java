package com.sequenceiq.remoteenvironment.scheduled.archiver;

import java.util.Optional;

import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
    protected void executeTracedJob(JobExecutionContext context) {
        queryPrivateControlPlaneConfigs();
    }

    public void queryPrivateControlPlaneConfigs() {
        LOGGER.trace("query all config from classic clusters");
    }
}
