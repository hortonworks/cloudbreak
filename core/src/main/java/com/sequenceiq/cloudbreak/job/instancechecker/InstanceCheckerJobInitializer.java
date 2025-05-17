package com.sequenceiq.cloudbreak.job.instancechecker;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOPPED;

import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.conf.InstanceCheckerConfig;
import com.sequenceiq.cloudbreak.metering.config.MeteringConfig;
import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;
import com.sequenceiq.cloudbreak.quartz.model.StaleAwareJobRescheduler;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class InstanceCheckerJobInitializer implements JobInitializer, StaleAwareJobRescheduler {

    @Inject
    private StackService stackService;

    @Inject
    private InstanceCheckerJobService instanceCheckerJobService;

    @Inject
    private InstanceCheckerConfig instanceCheckerConfig;

    @Inject
    private MeteringConfig meteringConfig;

    @Override
    public void initJobs() {
        if (instanceCheckerConfig.isEnabled() || (meteringConfig.isEnabled() && meteringConfig.isInstanceCheckerEnabled())) {
            stackService.getAllWhereStatusNotIn(Sets.union(Status.getUnschedulableStatuses(), Set.of(STOPPED)))
                    .forEach(s -> instanceCheckerJobService.schedule(new InstanceCheckerJobAdapter(s)));
        }
    }

    @Override
    public void rescheduleForStaleCluster(Long id) {
        instanceCheckerJobService.schedule(id);
    }
}
