package com.sequenceiq.datalake.job;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;
import com.sequenceiq.cloudbreak.quartz.statuschecker.service.StatusCheckerJobService;
import com.sequenceiq.datalake.repository.SdxClusterRepository;

@Component
public class SdxClusterJobInitializer implements JobInitializer {

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private StatusCheckerJobService jobService;

    @Override
    public void initJobs() {
        sdxClusterRepository.findAllAliveView().forEach(s -> jobService.schedule(new SdxClusterJobAdapter(s)));
    }
}
