package com.sequenceiq.cloudbreak.job.metering.sync;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOPPED;

import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.metering.config.MeteringConfig;
import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;
import com.sequenceiq.cloudbreak.quartz.model.StaleAwareJobRescheduler;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class MeteringSyncJobInitializer implements JobInitializer, StaleAwareJobRescheduler {

    @Inject
    private StackService stackService;

    @Inject
    private MeteringSyncJobService meteringSyncJobService;

    @Inject
    private MeteringConfig meteringConfig;

    @Override
    public void initJobs() {
        if (meteringConfig.isEnabled()) {
            stackService.getAllAliveDatahubs(Sets.union(Status.getUnschedulableStatuses(), Set.of(STOPPED)))
                    .forEach(s -> meteringSyncJobService.schedule(new MeteringSyncJobAdapter(s)));
        }
    }

    @Override
    public void rescheduleForStaleCluster(Long id) {
        meteringSyncJobService.schedule(id);
    }
}
