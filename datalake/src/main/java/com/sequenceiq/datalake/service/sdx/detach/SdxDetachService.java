package com.sequenceiq.datalake.service.sdx.detach;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.repository.SdxClusterRepository;

@Component
public class SdxDetachService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxDetachService.class);

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    public SdxCluster detach(Long sdxId) {
        return sdxClusterRepository.findById(sdxId).map(cluster -> {
            cluster.setDetached(true);
            sdxClusterRepository.save(cluster);
            return cluster;
        }).orElseThrow(() -> new NotFoundException("SdxCluster was not found with ID: " + sdxId));
    }
}
