package com.sequenceiq.datalake.job;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;
import com.sequenceiq.datalake.repository.SdxClusterRepository;

@Component
public class SdxRollForwardJobInitializer implements JobInitializer {

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Override
    public void initJobs() {
        sdxClusterRepository.findAllAliveView();
    }
}
