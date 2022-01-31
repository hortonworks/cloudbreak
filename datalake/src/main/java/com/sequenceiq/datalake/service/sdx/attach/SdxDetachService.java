package com.sequenceiq.datalake.service.sdx.attach;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;

/**
 * This class detaches the data lake from the environment.
 * <p>
 * It is achieved by renaming name and cluster CRN. CLuster associated with the stack is also renamed as well.
 * After this operation is performed, the stack associated with this cluster is also renamed so that DL name and stack same are the same.
 * <p>
 */
@Component
public class SdxDetachService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SdxDetachService.class);

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @Inject
    private SdxDetachNameGenerator sdxDetachNameGenerator;

    @Inject
    private SdxAttachDetachUtils sdxAttachDetachUtils;

    /**
     * Detaches the internal SDX cluster by assigning it a new "detached" name and CRN.
     */
    public SdxCluster detachCluster(Long sdxID) {
        LOGGER.info("Started detaching SDX cluster with ID: {}.", sdxID);
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.STOPPED, "Datalake detach in progress.", sdxID);

        SdxCluster cluster = sdxClusterRepository.findById(sdxID)
                .orElseThrow(() -> new NotFoundException("SdxCluster was not found for ID: " + sdxID));

        String originalName = cluster.getClusterName();
        sdxAttachDetachUtils.updateClusterNameAndCrn(
                cluster, sdxDetachNameGenerator.generateDetachedClusterName(originalName),
                regionAwareCrnGenerator.generateCrnStringWithUuid(CrnResourceDescriptor.DATALAKE, cluster.getAccountId())
        );
        cluster.setDetached(true);
        SdxCluster saved = sdxClusterRepository.save(cluster);

        LOGGER.info("Finished detaching SDX cluster with ID: {}. Modified name from {} to {} and crn from {} to {}.",
                saved.getId(), originalName, saved.getClusterName(), saved.getOriginalCrn(), saved.getCrn());
        return saved;
    }

    /**
     * Can throw the following exceptions:
     *      - NotFoundException : Internal stack is not found by the cluster's name.
     *      - TransactionExecutionException : If updating the stack fails due to the stack not being
     *              able to be saved due to the stack object being null.
     */
    public void detachStack(SdxCluster cluster, String originalName) throws Exception {
        LOGGER.info("Started detaching stack of SDX cluster with ID: {}.", cluster.getId());
        sdxAttachDetachUtils.updateStack(cluster, originalName);
        LOGGER.info("Finished detaching stack of SDX cluster with ID: {}.", cluster.getId());
    }

    /**
     * Can throw the following exceptions:
     *      - NotFoundException : If the database is not found for the cluster CRN and environment CRN.
     *      - IllegalArgumentException : If the database could not be saved due to the object for it being null.
     */
    public void detachExternalDatabase(SdxCluster cluster) throws Exception {
        LOGGER.info("Started detaching external database of SDX cluster with ID {} so it has crn {} " +
                "instead of {}.", cluster.getId(), cluster.getCrn(), cluster.getOriginalCrn());
        sdxAttachDetachUtils.updateExternalDatabase(cluster, cluster.getOriginalCrn());
        LOGGER.info("Finished detaching external database of SDX cluster with ID {}.", cluster.getId());
    }

    public void markAsDetached(Long detachedClusterID) {
        sdxStatusService.setStatusForDatalakeAndNotify(
                DatalakeStatusEnum.STOPPED, "Datalake is detached.", detachedClusterID
        );
    }
}
