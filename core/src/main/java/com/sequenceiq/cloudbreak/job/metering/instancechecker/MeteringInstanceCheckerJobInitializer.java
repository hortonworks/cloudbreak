package com.sequenceiq.cloudbreak.job.metering.instancechecker;

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
public class MeteringInstanceCheckerJobInitializer implements JobInitializer, StaleAwareJobRescheduler {

    @Inject
    private StackService stackService;

    @Inject
    private MeteringInstanceCheckerJobService meteringInstanceCheckerJobService;

    @Inject
    private MeteringConfig meteringConfig;

    @Override
    public void initJobs() {
        if (meteringConfig.isEnabled() && meteringConfig.isInstanceCheckerEnabled()) {
            stackService.getAllAliveDatahubs(Sets.union(Status.getUnschedulableStatuses(), Set.of(STOPPED)))
                    .forEach(s -> meteringInstanceCheckerJobService.schedule(new MeteringInstanceCheckerJobAdapter(s)));
        }
    }

    @Override
    public void rescheduleForStaleCluster(Long id) {
        meteringInstanceCheckerJobService.schedule(id);
    }
}
