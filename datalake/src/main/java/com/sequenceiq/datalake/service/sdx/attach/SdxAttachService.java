package com.sequenceiq.datalake.service.sdx.attach;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.quartz.statuschecker.service.StatusCheckerJobService;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.job.SdxClusterJobAdapter;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;

/**
 * Provides core logic and functionality for attaching or reattaching an SDX cluster.
 */
@Service
public class SdxAttachService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SdxAttachService.class);

    @Inject
    private SdxService sdxService;

    @Inject
    private OwnerAssignmentService ownerAssignmentService;

    @Inject
    private SdxDetachNameGenerator sdxDetachNameGenerator;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private SdxAttachDetachUtils sdxAttachDetachUtils;

    @Inject
    private StatusCheckerJobService jobService;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    /**
     * Can throw the following exceptions:
     *      - Exceptions thrown by reattachCluster.
     *      - Exceptions thrown by reattachStack.
     *      - Exceptions thrown by reattachExternalDatabase.
     *      - Exceptions thrown by attachSdx.
     */
    public SdxCluster reattachDetachedSdxCluster(SdxCluster clusterToReattach) {
        String detachedName = clusterToReattach.getClusterName();
        String detachedCrn = clusterToReattach.getCrn();
        SdxCluster reattached = reattachCluster(clusterToReattach);
        boolean stackReattached = false;

        try {
            reattachStack(reattached, detachedName);
            stackReattached = true;
            if (clusterToReattach.hasExternalDatabase()) {
                reattachExternalDatabase(reattached, detachedCrn);
            }
            reRegisterClusterProxyConfig(reattached, false, detachedCrn);
            return reattached;
        } catch (Exception e) {
            LOGGER.error("Failed to reattach SDX cluster with CRN '" + detachedCrn +
                    "'. Putting detached cluster back to its original state.", e);
            undoReattach(reattached, detachedName, detachedCrn, stackReattached);
            throw e;
        }
    }

    /**
     * Throws RuntimeException if cluster is not detached or could not revert name and CRN.
     */
    public SdxCluster reattachCluster(SdxCluster cluster) {
        LOGGER.info("Started reattaching SDX cluster with ID: {}", cluster.getId());

        if (!cluster.isDetached()) {
            throw new SdxDetachException("Attempting to reattach a cluster which was not detached!");
        }

        sdxAttachDetachUtils.updateClusterNameAndCrn(
                cluster, sdxDetachNameGenerator.generateOriginalNameFromDetached(cluster.getClusterName()),
                cluster.getOriginalCrn()
        );
        cluster.setOriginalCrn(null);
        cluster.setDetached(false);
        SdxCluster saved = sdxService.save(cluster);

        LOGGER.info("Finished reattaching SDX cluster with ID: {}. Cluster now has name {} and crn {}.",
                saved.getId(), saved.getClusterName(), saved.getCrn());
        return saved;
    }

    /**
     * Can throw the following exceptions:
     *      - NotFoundException : Internal stack is not found by the cluster's name.
     *      - TransactionExecutionException : If updating the stack fails.
     */
    public void reattachStack(SdxCluster cluster, String originalName) {
        LOGGER.info("Started reattaching stack of SDX cluster with ID: {}", cluster.getId());
        sdxAttachDetachUtils.updateStack(originalName, cluster.getClusterName(), cluster.getCrn(), false);
        jobService.schedule(cluster.getId(), SdxClusterJobAdapter.class);
        LOGGER.info("Finished reattaching stack of SDX cluster with ID: {}. Now has name {} and crn {}.",
                cluster.getId(), cluster.getClusterName(), cluster.getCrn());
    }

    /**
     * Throws a NotFoundException if the database is not found for the cluster CRN and environment CRN.
     */
    private void reattachExternalDatabase(SdxCluster cluster, String detachedCrn) {
        LOGGER.info("Started reattaching external database for SDX cluster with ID: {}", cluster.getId());
        sdxAttachDetachUtils.updateExternalDatabase(cluster, detachedCrn);
        LOGGER.info("Finished reattaching external database for SDX cluster with ID: {}. Now has crn: {}.",
                cluster.getId(), cluster.getCrn());
    }

    /**
     * This method is needed for cases in which the cluster proxy configuration has been reset or deleted.
     * This explicitly occurs during recovery due to the resized DL being deleted.
     * Note that this method has no undesired side effects and is safe to call even if not necessarily required.
     */
    public void reRegisterClusterProxyConfig(SdxCluster cluster, boolean skipFullReRegistration, String detachedCrn) {
        LOGGER.info("Started re-registering cluster proxy config for SDX cluster with ID: {}", cluster.getId());
        sdxAttachDetachUtils.reRegisterClusterProxyConfig(cluster, skipFullReRegistration, detachedCrn);
        LOGGER.info("Finished re-registering cluster proxy config for SDX cluster with ID: {}. Now has crn: {}.",
                cluster.getId(), cluster.getCrn());
    }

    /**
     * Note that this method returns a possibly different version of the original cluster, so the original should
     * not be used anymore.
     *
     * Will throw a TransactionExecutionException if:
     *      - Cluster cannot be saved.
     *      - The resource owner role assignment fails its UMS GRPC call.
     */
    public SdxCluster saveSdxAndAssignResourceOwnerRole(SdxCluster cluster, String userCrn) {
        SdxCluster created = sdxService.save(cluster);
        try {
            ownerAssignmentService.assignResourceOwnerRoleIfEntitled(userCrn, created.getCrn(), created.getAccountId());
            return created;
        } catch (Exception e) {
            LOGGER.error("Failed to assign resource owner role to new SDX with CRN '" + created.getCrn() +
                    "'. Removing it from database.", e);
            sdxService.delete(created);
            throw e;
        }
    }

    public void markAsAttached(SdxCluster attached) {
        sdxStatusService.setStatusForDatalake(
                DatalakeStatusEnum.REQUESTED, "Newly attached datalake requested.", attached
        );
    }

    private void undoReattach(SdxCluster cluster, String detachedName, String detachedCrn, boolean stackReattached) {
        sdxAttachDetachUtils.updateClusterNameAndCrn(cluster, detachedName, detachedCrn);
        cluster.setDetached(true);
        sdxService.save(cluster);

        if (stackReattached) {
            sdxAttachDetachUtils.updateStack(cluster.getClusterName(), detachedName, detachedCrn, true);
            jobService.schedule(cluster.getId(), SdxClusterJobAdapter.class);
        }
    }
}
