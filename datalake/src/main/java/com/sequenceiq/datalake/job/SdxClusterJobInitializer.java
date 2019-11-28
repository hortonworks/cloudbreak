package com.sequenceiq.datalake.job;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.statuschecker.model.JobInitializer;
import com.sequenceiq.statuschecker.service.JobService;

@Component
public class SdxClusterJobInitializer implements JobInitializer {

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private JobService jobService;

    @Override
    public void initJobs() {
        List<SdxCluster> clusters = sdxClusterRepository.findAll();
        jobService.deleteAll();
        for (SdxCluster cluster : clusters) {
            if (cluster.getDeleted() == null) {
                jobService.schedule(new SdxClusterJobAdapter(cluster));
            }
        }
    }

}
