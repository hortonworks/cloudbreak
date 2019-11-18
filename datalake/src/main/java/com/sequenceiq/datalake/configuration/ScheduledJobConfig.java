package com.sequenceiq.datalake.configuration;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.SdxJobService;

@Component
public class ScheduledJobConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledJobConfig.class);

    @Value("${datalake.autosync.enabled:false}")
    private boolean autoSyncEnabled;

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private SdxJobService sdxJobService;

    @PostConstruct
    private void init() {
        if (autoSyncEnabled) {
            List<SdxCluster> clusters = sdxClusterRepository.findAll();
            sdxJobService.deleteAll();
            for (SdxCluster cluster : clusters) {
                if (cluster.getDeleted() == null) {
                    sdxJobService.schedule(cluster);
                }
            }
        }
    }

}
