package com.sequenceiq.cloudbreak.job.metering.sync;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.CREATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.CREATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_COMPLETED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOPPED;

import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.metering.config.MeteringConfig;
import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class MeteringSyncJobInitializer implements JobInitializer {

    @Inject
    private StackService stackService;

    @Inject
    private MeteringSyncJobService meteringSyncJobService;

    @Inject
    private MeteringConfig meteringConfig;

    @Override
    public void initJobs() {
        if (meteringConfig.isEnabled()) {
            stackService.getAllAliveDatahubs(Set.of(DELETE_COMPLETED, DELETE_IN_PROGRESS, DELETE_FAILED, CREATE_FAILED, CREATE_IN_PROGRESS, STOPPED))
                    .forEach(s -> meteringSyncJobService.schedule(new MeteringSyncJobAdapter(s)));
        }
    }
}
