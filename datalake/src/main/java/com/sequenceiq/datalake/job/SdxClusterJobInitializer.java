package com.sequenceiq.datalake.job;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.projection.SdxClusterIdView;
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
        jobService.deleteAll();
        sdxClusterRepository.findAllAliveView().stream()
                .map(this::convertToSdx)
                .forEach(s -> jobService.schedule(new SdxClusterJobAdapter(s)));
    }

    private SdxCluster convertToSdx(SdxClusterIdView view) {
        SdxCluster result = new SdxCluster();
        result.setId(view.getId());
        result.setStackCrn(view.getStackCrn());
        return result;
    }

}
