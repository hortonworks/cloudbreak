package com.sequenceiq.datalake.job.salt;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;
import com.sequenceiq.cloudbreak.quartz.saltstatuschecker.SaltStatusCheckerConfig;
import com.sequenceiq.datalake.repository.SdxClusterRepository;

@Component
public class SdxSaltStatusCheckerJobInitializer implements JobInitializer {

    @Inject
    private SaltStatusCheckerConfig saltStatusCheckerConfig;

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private SdxSaltStatusCheckerJobService jobService;

    @Override
    public void initJobs() {
        if (saltStatusCheckerConfig.isEnabled()) {
            sdxClusterRepository.findAllAliveView().forEach(s -> jobService.schedule(new SdxSaltStatusCheckerJobAdapter(s)));
        }
    }
}
