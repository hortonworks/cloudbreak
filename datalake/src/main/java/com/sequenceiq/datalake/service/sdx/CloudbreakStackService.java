package com.sequenceiq.datalake.service.sdx;

import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.RdsUpgradeV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.exception.CloudbreakApiException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class CloudbreakStackService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakStackService.class);

    private static final long WORKSPACE_ID = 0L;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private WebApplicationExceptionMessageExtractor exceptionMessageExtractor;

    @Inject
    private CloudbreakFlowService cloudbreakFlowService;

    public StackV4Response getStack(SdxCluster cluster) {
        try {
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> stackV4Endpoint.get(WORKSPACE_ID, cluster.getClusterName(), Set.of(), cluster.getAccountId()));
        } catch (WebApplicationException e) {
            String exceptionMessage = exceptionMessageExtractor.getErrorMessage(e);
            String message = String.format("Could not retrieve stack for SDX cluster: [%s]. Message: [%s]", cluster.getClusterName(), exceptionMessage);
            throw new CloudbreakApiException(message, e);
        }
    }

    public void checkUpgradeRdsByClusterNameInternal(SdxCluster sdxCluster, TargetMajorVersion targetMajorVersion) {
        try {
            String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
            ThreadBasedUserCrnProvider.doAsInternalActor(regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> stackV4Endpoint.checkUpgradeRdsByClusterNameInternal(WORKSPACE_ID, sdxCluster.getClusterName(), targetMajorVersion,
                            initiatorUserCrn));
        } catch (RuntimeException e) {
            String exceptionMessage = exceptionMessageExtractor.getErrorMessage(e);
            String message = "Rds upgrade validation failed: " + exceptionMessage;
            LOGGER.warn(message);
            throw new CloudbreakApiException(message, e);
        }
    }

    public RdsUpgradeV4Response upgradeRdsByClusterNameInternal(SdxCluster sdxCluster, TargetMajorVersion targetMajorVersion) {
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        try {
            RdsUpgradeV4Response upgradeResponse =
                    ThreadBasedUserCrnProvider.doAsInternalActor(regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                            () -> stackV4Endpoint.upgradeRdsByClusterNameInternal(WORKSPACE_ID, sdxCluster.getClusterName(), targetMajorVersion,
                                    initiatorUserCrn));
            LOGGER.debug("Launching database server upgrade in core returned: {}", upgradeResponse);
            cloudbreakFlowService.saveLastCloudbreakFlowChainId(sdxCluster, upgradeResponse.getFlowIdentifier());
            return upgradeResponse;
        } catch (WebApplicationException e) {
            String message = String.format("Could not launch database server upgrade in core, reason: %s.", exceptionMessageExtractor.getErrorMessage(e));
            LOGGER.warn(message, e);
            throw new CloudbreakApiException(message, e);
        }
    }

    public void updateSaltByName(SdxCluster sdxCluster) {
        try {
            FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> stackV4Endpoint.updateSaltByName(WORKSPACE_ID, sdxCluster.getClusterName(), sdxCluster.getAccountId()));
            cloudbreakFlowService.saveLastCloudbreakFlowChainId(sdxCluster, flowIdentifier);
        } catch (WebApplicationException e) {
            String message = String.format("Could not launch Salt update in core, reason: %s", exceptionMessageExtractor.getErrorMessage(e));
            LOGGER.warn(message, e);
            throw new CloudbreakApiException(message, e);
        }
    }

}
