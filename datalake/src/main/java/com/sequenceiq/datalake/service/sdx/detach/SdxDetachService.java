package com.sequenceiq.datalake.service.sdx.detach;

import java.util.Date;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.repository.SdxClusterRepository;

/**
 * This class detaches the data lake from the environment.
 * <p>
 * It is achieved by renaming name and cluster CRN. CLuster associated with the stack is also renamed as well.
 * After this operation is performed, the stack associated with this cluster is also renamed so that DL name and stack same are the same.
 * <p>
 */
@Component
public class SdxDetachService {

    private static final String DELIMITER = "-";

    @Inject
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    public SdxCluster detach(Long sdxId) {
        return sdxClusterRepository.findById(sdxId).map(cluster -> {
            String newCrn = regionAwareCrnGenerator.generateCrnStringWithUuid(CrnResourceDescriptor.DATALAKE, cluster.getAccountId());
            String originalCrn = cluster.getCrn();
            cluster.setCrn(newCrn);
            cluster.setClusterName(getNameToDetach(cluster.getClusterName()));
            cluster.setDetached(true);
            cluster.setOriginalCrn(originalCrn);
            sdxClusterRepository.save(cluster);
            return cluster;
        }).orElseThrow(() -> new NotFoundException("SdxCluster was not found with ID: " + sdxId));
    }

    String getNameToDetach(String resourceCrn) {
        return resourceCrn + DELIMITER + new Date().getTime();
    }
}
