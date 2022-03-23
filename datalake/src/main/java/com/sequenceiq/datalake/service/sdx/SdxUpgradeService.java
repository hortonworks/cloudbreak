package com.sequenceiq.datalake.service.sdx;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackImageChangeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeOptionV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.exception.CloudbreakApiException;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.datalake.service.upgrade.SdxUpgradeValidationResultProvider;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class SdxUpgradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxUpgradeService.class);

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private CloudbreakFlowService cloudbreakFlowService;

    @Inject
    private WebApplicationExceptionMessageExtractor exceptionMessageExtractor;

    @Inject
    private SdxUpgradeValidationResultProvider cloudbreakFlowResultProvider;

    @Inject
    private CloudbreakPoller cloudbreakPoller;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public void changeImage(Long id, UpgradeOptionV4Response upgradeOption) {
        SdxCluster cluster = sdxService.getById(id);
        String targetImageId = upgradeOption.getUpgrade().getImageId();
        StackImageChangeV4Request stackImageChangeRequest = new StackImageChangeV4Request();
        stackImageChangeRequest.setImageId(targetImageId);
        stackImageChangeRequest.setImageCatalogName(upgradeOption.getUpgrade().getImageCatalogName());
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.CHANGE_IMAGE_IN_PROGRESS, "Changing image", cluster);
        try {
            String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
            FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () ->
                    stackV4Endpoint.changeImageInternal(0L, cluster.getClusterName(), stackImageChangeRequest, initiatorUserCrn));
            cloudbreakFlowService.saveLastCloudbreakFlowChainId(cluster, flowIdentifier);
        } catch (WebApplicationException e) {
            String exceptionMessage = exceptionMessageExtractor.getErrorMessage(e);
            String message = String.format("Stack change image failed on cluster: [%s]. Message: [%s]", cluster.getClusterName(), exceptionMessage);
            throw new CloudbreakApiException(message, e);
        }
    }

    public void upgradeRuntime(Long id, String imageId) {
        SdxCluster sdxCluster = sdxService.getById(id);
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_UPGRADE_IN_PROGRESS, "Upgrading datalake stack", sdxCluster);
        try {
            String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
            FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () ->
                    stackV4Endpoint.upgradeClusterByNameInternal(0L, sdxCluster.getClusterName(), imageId, initiatorUserCrn));
            cloudbreakFlowService.saveLastCloudbreakFlowChainId(sdxCluster, flowIdentifier);
        } catch (WebApplicationException e) {
            String exceptionMessage = exceptionMessageExtractor.getErrorMessage(e);
            String message = String.format("Stack upgrade failed on cluster: [%s]. Message: [%s]", sdxCluster.getClusterName(), exceptionMessage);
            throw new CloudbreakApiException(message, e);
        }
    }

    public String getImageId(Long id) {
        SdxCluster cluster = sdxService.getById(id);
        try {
            StackV4Response stackV4Response = ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () ->
                    stackV4Endpoint.get(0L, cluster.getClusterName(), Set.of(), cluster.getAccountId()));
            return stackV4Response.getImage().getId();
        } catch (WebApplicationException e) {
            String exceptionMessage = exceptionMessageExtractor.getErrorMessage(e);
            String message = String.format("Couldn't get image id for cluster: [%s]. Message: [%s]", cluster.getClusterName(), exceptionMessage);
            throw new CloudbreakApiException(message, e);
        }
    }

    public Optional<String> updateRuntimeVersionFromCloudbreak(Long sdxId) {
        SdxCluster sdxCluster = sdxService.getById(sdxId);
        String clusterName = sdxCluster.getClusterName();
        LOGGER.info("Trying to update the runtime version from Cloudbreak for cluster: {}", clusterName);
        StackV4Response stack = ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> stackV4Endpoint.get(0L, clusterName, Set.of(), sdxCluster.getAccountId()));
        return sdxService.updateRuntimeVersionFromStackResponse(sdxCluster, stack);
    }

    public String getCurrentImageCatalogName(Long id) {
        SdxCluster cluster = sdxService.getById(id);
        try {
            StackV4Response stackV4Response = ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> stackV4Endpoint.get(0L, cluster.getClusterName(), Set.of(), cluster.getAccountId()));
            return stackV4Response.getImage().getCatalogName();
        } catch (WebApplicationException e) {
            String exceptionMessage = exceptionMessageExtractor.getErrorMessage(e);
            String message = String.format("Couldn't fetch image catalog for cluster: [%s]. Message: [%s]", cluster.getClusterName(), exceptionMessage);
            throw new CloudbreakApiException(message, e);
        }
    }

    public void upgradeOs(Long id) {
        SdxCluster cluster = sdxService.getById(id);
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_UPGRADE_IN_PROGRESS, "OS upgrade started", cluster);
        try {
            String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
            FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> stackV4Endpoint.upgradeOsInternal(0L, cluster.getClusterName(), initiatorUserCrn));
            cloudbreakFlowService.saveLastCloudbreakFlowChainId(cluster, flowIdentifier);
        } catch (WebApplicationException e) {
            String exceptionMessage = exceptionMessageExtractor.getErrorMessage(e);
            String message = String.format("Stack upgrade failed on cluster: [%s]. Message: [%s]", cluster.getClusterName(), exceptionMessage);
            throw new CloudbreakApiException(message, e);
        }
    }

    public void waitCloudbreakFlow(Long id, PollingConfig pollingConfig, String pollingMessage) {
        SdxCluster sdxCluster = sdxService.getById(id);
        cloudbreakPoller.pollUpdateUntilAvailable(pollingMessage, sdxCluster, pollingConfig);
        if (cloudbreakFlowResultProvider.isValidationFailed(sdxCluster)) {
            throw new UserBreakException(new UpgradeValidationFailedException("Upgrade validation failed."));
        }
    }
}
