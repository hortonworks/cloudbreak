package com.sequenceiq.datalake.service.sdx;

import java.util.Set;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.SetDefaultJavaVersionRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.imdupdate.StackInstanceMetadataUpdateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.RdsUpgradeV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateType;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class CloudbreakStackService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakStackService.class);

    private static final long WORKSPACE_ID = 0L;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private WebApplicationExceptionMessageExtractor exceptionMessageExtractor;

    @Inject
    private CloudbreakFlowService cloudbreakFlowService;

    public StackV4Response getStack(SdxCluster cluster) {
        try {
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> stackV4Endpoint.get(WORKSPACE_ID, cluster.getClusterName(), Set.of(), cluster.getAccountId()));
        } catch (WebApplicationException e) {
            String exceptionMessage = exceptionMessageExtractor.getErrorMessage(e);
            String message = String.format("Could not retrieve stack for SDX cluster: [%s]. Message: [%s]", cluster.getClusterName(), exceptionMessage);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public void checkUpgradeRdsByClusterNameInternal(SdxCluster sdxCluster, TargetMajorVersion targetMajorVersion) {
        try {
            String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
            ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> stackV4Endpoint.checkUpgradeRdsByClusterNameInternal(WORKSPACE_ID, sdxCluster.getClusterName(), targetMajorVersion,
                            initiatorUserCrn));
        } catch (RuntimeException e) {
            String exceptionMessage = exceptionMessageExtractor.getErrorMessage(e);
            String message = "Rds upgrade validation failed: " + exceptionMessage;
            LOGGER.warn(message);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public RdsUpgradeV4Response upgradeRdsByClusterNameInternal(SdxCluster sdxCluster, TargetMajorVersion targetMajorVersion, boolean forced) {
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        try {
            RdsUpgradeV4Response upgradeResponse =
                    ThreadBasedUserCrnProvider.doAsInternalActor(
                            () -> stackV4Endpoint.upgradeRdsByClusterNameInternal(WORKSPACE_ID, sdxCluster.getClusterName(), targetMajorVersion,
                                    initiatorUserCrn, forced));
            LOGGER.debug("Launching database server upgrade in core returned: {}", upgradeResponse);
            cloudbreakFlowService.saveLastCloudbreakFlowChainId(sdxCluster, upgradeResponse.getFlowIdentifier());
            return upgradeResponse;
        } catch (WebApplicationException e) {
            String message = String.format("Could not launch database server upgrade in core, reason: %s.", exceptionMessageExtractor.getErrorMessage(e));
            LOGGER.warn(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public void updateSaltByName(SdxCluster sdxCluster) {
        try {
            FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> stackV4Endpoint.updateSaltByName(WORKSPACE_ID, sdxCluster.getClusterName(), sdxCluster.getAccountId()));
            cloudbreakFlowService.saveLastCloudbreakFlowChainId(sdxCluster, flowIdentifier);
        } catch (WebApplicationException e) {
            String message = String.format("Could not launch Salt update in core, reason: %s", exceptionMessageExtractor.getErrorMessage(e));
            LOGGER.warn(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public void updateInstanceMetadata(SdxCluster sdxCluster, InstanceMetadataUpdateType updateType) {
        try {
            String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
            StackInstanceMetadataUpdateV4Request request = new StackInstanceMetadataUpdateV4Request();
            request.setCrn(sdxCluster.getCrn());
            request.setUpdateType(updateType);
            FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> stackV4Endpoint.instanceMetadataUpdate(WORKSPACE_ID, userCrn, request));
            cloudbreakFlowService.saveLastCloudbreakFlowChainId(sdxCluster, flowIdentifier);
        } catch (WebApplicationException e) {
            String message = String.format("Could not launch instance metadata update in core, reason: %s", exceptionMessageExtractor.getErrorMessage(e));
            LOGGER.warn(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public void setDefaultJavaVersion(SdxCluster sdxCluster, String javaVersion, boolean restartServices, boolean restartCM, boolean rollingRestart) {
        try {
            SetDefaultJavaVersionRequest setDefaultJavaVersionRequest = new SetDefaultJavaVersionRequest();
            setDefaultJavaVersionRequest.setDefaultJavaVersion(javaVersion);
            setDefaultJavaVersionRequest.setRestartServices(restartServices);
            setDefaultJavaVersionRequest.setRestartCM(restartCM);
            setDefaultJavaVersionRequest.setRollingRestart(rollingRestart);
            String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
            FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> stackV4Endpoint.setDefaultJavaVersionByCrnInternal(WORKSPACE_ID, sdxCluster.getCrn(),
                            setDefaultJavaVersionRequest, initiatorUserCrn));
            cloudbreakFlowService.saveLastCloudbreakFlowChainId(sdxCluster, flowIdentifier);
        } catch (WebApplicationException e) {
            String message = String.format("Could not set default java version in core, reason: %s", exceptionMessageExtractor.getErrorMessage(e));
            LOGGER.warn(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public void migrateDatalakeSkus(SdxCluster sdxCluster, boolean force) {
        try {
            String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
            FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> stackV4Endpoint.triggerSkuMigration(WORKSPACE_ID, sdxCluster.getClusterName(), force, initiatorUserCrn));
            cloudbreakFlowService.saveLastCloudbreakFlowChainId(sdxCluster, flowIdentifier);
        } catch (WebApplicationException e) {
            String message = String.format("Could not migrate Skus in core, reason: %s", exceptionMessageExtractor.getErrorMessage(e));
            LOGGER.warn(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public void updatePublicDnsEntries(SdxCluster sdxCluster) {
        try {
            String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
            FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> stackV4Endpoint.updatePublicDnsEntriesByCrn(WORKSPACE_ID, sdxCluster.getStackCrn(), initiatorUserCrn));
            cloudbreakFlowService.saveLastCloudbreakFlowChainId(sdxCluster, flowIdentifier);
        } catch (WebApplicationException e) {
            String message = String.format("Could not update public DNS entries in core, reason: %s", exceptionMessageExtractor.getErrorMessage(e));
            LOGGER.warn(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }
}
