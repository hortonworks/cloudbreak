package com.sequenceiq.environment.environment.service.stackpatch;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.service.DefaultAvailabilityZoneService;
import com.sequenceiq.environment.network.NetworkService;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.stackpatcher.domain.StackPatchType;
import com.sequenceiq.stackpatcher.service.ExistingStackPatchApplyException;
import com.sequenceiq.stackpatcher.service.ExistingStackPatchService;

/**
 * Backfills the environment level availability zone list of GCP environments created before it was introduced, so that FreeIPA nodes can be distributed
 * across availability zones (CB-34539). Without it multi AZ FreeIPA deployments on such environments end up with every node in the same zone.
 * The default zones are the same ones environment creation picks, see {@link DefaultAvailabilityZoneService}. Only the zone list of the network is written,
 * the network CIDRs are deliberately not refreshed, as that would trigger the CIDR propagation flow (CB-31470) on every environment whose CIDRs were never
 * recorded. The zones are fetched from the provider first, then the network is re-read and written in a short transaction, so that a concurrent network
 * edit committed during the provider call is not overwritten with the stale network loaded by the job. An environment that is not AVAILABLE returns false
 * so the patch stays SKIPPED and is picked up by the next sweep. If the provider returned no zone for the region of the environment, the patch fails, so
 * the failure is reported on each attempt.
 */
@Service
public class GcpEnvironmentNetworkAzBackfillPatchService extends ExistingStackPatchService<Environment> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpEnvironmentNetworkAzBackfillPatchService.class);

    @Inject
    private DefaultAvailabilityZoneService defaultAvailabilityZoneService;

    @Inject
    private NetworkService networkService;

    @Inject
    private TransactionService transactionService;

    @Override
    public StackPatchType getStackPatchType() {
        return StackPatchType.GCP_ENVIRONMENT_NETWORK_AZ_BACKFILL;
    }

    @Override
    public boolean isAffected(Environment environment) {
        if (!CloudPlatform.GCP.name().equals(environment.getCloudPlatform())) {
            return false;
        }
        return defaultAvailabilityZoneService.isPopulationNeeded(environment, environment.getNetwork());
    }

    @Override
    protected boolean doApply(Environment environment) throws ExistingStackPatchApplyException {
        if (!EnvironmentStatus.AVAILABLE.equals(environment.getStatus())) {
            LOGGER.info("Environment '{}' is in {} state, the patch will be retried when it becomes available",
                    environment.getResourceCrn(), environment.getStatus());
            return false;
        }
        CloudRegions cloudRegions = defaultAvailabilityZoneService.getRegions(environment);
        if (!populateOnCurrentNetworkInTransaction(environment, cloudRegions)) {
            throw new ExistingStackPatchApplyException(
                    String.format("The availability zones of environment '%s' could not be determined", environment.getResourceCrn()));
        }
        LOGGER.info("Backfilled the availability zones of environment '{}'", environment.getResourceCrn());
        return true;
    }

    private boolean populateOnCurrentNetworkInTransaction(Environment environment, CloudRegions cloudRegions) {
        try {
            return transactionService.required(() -> {
                BaseNetwork network = networkService.<BaseNetwork>findByEnvironment(environment.getId())
                        .orElseThrow(() -> new IllegalStateException(String.format("Environment '%s' has no network", environment.getResourceCrn())));
                defaultAvailabilityZoneService.populateIfMissing(environment, network, cloudRegions);
                if (defaultAvailabilityZoneService.isPopulationNeeded(environment, network)) {
                    return false;
                }
                networkService.save(network);
                return true;
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    /**
     * The patch starts no environment flow, so a pending retryable flow on the environment is irrelevant to it.
     */
    @Override
    protected boolean shouldCheckForFailedRetryableFlow() {
        return false;
    }
}
