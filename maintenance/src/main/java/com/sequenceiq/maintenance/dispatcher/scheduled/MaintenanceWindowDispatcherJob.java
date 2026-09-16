package com.sequenceiq.maintenance.dispatcher.scheduled;

import java.util.Optional;

import jakarta.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.quartz.MdcQuartzJob;
import com.sequenceiq.maintenance.dispatcher.MaintenanceWindowDispatchTickService;

@DisallowConcurrentExecution
@Component
public class MaintenanceWindowDispatcherJob extends MdcQuartzJob {

    @Inject
    private MaintenanceWindowDispatchTickService dispatchTickService;

    @Override
    protected Optional<MdcContextInfoProvider> getMdcContextConfigProvider() {
        return Optional.empty();
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) throws JobExecutionException {
        dispatchTickService.tick();
    }
}
