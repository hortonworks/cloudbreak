package com.sequenceiq.datalake.service.upgrade;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.InternalUpgradeSettings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageComponentVersions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.auth.ClouderaManagerLicenseProvider;
import com.sequenceiq.cloudbreak.auth.JsonCMLicense;
import com.sequenceiq.cloudbreak.auth.PaywallAccessChecker;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.datalake.controller.sdx.SdxUpgradeClusterConverter;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.flow.dr.DatalakeDrSkipOptions;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.sdx.api.model.SdxUpgradeReplaceVms;
import com.sequenceiq.sdx.api.model.SdxUpgradeRequest;
import com.sequenceiq.sdx.api.model.SdxUpgradeResponse;

@Component
public class SdxRuntimeUpgradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxRuntimeUpgradeService.class);

    private static final long WORKSPACE_ID = 0L;

    @Value("${sdx.paywall.url}")
    private String paywallUrl;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private SdxUpgradeClusterConverter sdxUpgradeClusterConverter;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private PaywallAccessChecker paywallAccessChecker;

    @Inject
    private ClouderaManagerLicenseProvider clouderaManagerLicenseProvider;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private SdxUpgradeFilter upgradeFilter;

    public SdxUpgradeResponse checkForUpgradeByName(String clusterName, SdxUpgradeRequest upgradeSdxClusterRequest, String accountId,
            boolean upgradePreparation) {
        return checkForSdxUpgradeResponse(upgradeSdxClusterRequest, clusterName, accountId, upgradePreparation);
    }

    public SdxUpgradeResponse checkForUpgradeByCrn(String userCrn, String crn, SdxUpgradeRequest upgradeSdxClusterRequest, String accountId,
            boolean upgradePreparation) {
        String clusterName = getClusterName(userCrn, crn);
        return checkForSdxUpgradeResponse(upgradeSdxClusterRequest, clusterName, accountId, upgradePreparation);
    }

    private String getClusterName(String userCrn, String clusterCrn) {
        SdxCluster cluster = sdxService.getByCrn(userCrn, clusterCrn);
        MDCBuilder.buildMdcContext(cluster);
        return cluster.getClusterName();
    }

    public SdxUpgradeResponse triggerUpgradeByName(String userCrn, String clusterName, SdxUpgradeRequest upgradeRequest, String accountId,
            boolean upgradePreparation) {
        SdxCluster cluster = sdxService.getByNameInAccount(userCrn, clusterName);
        MDCBuilder.buildMdcContext(cluster);
        SdxUpgradeResponse sdxUpgradeResponse = checkForUpgradeByName(clusterName, upgradeRequest, accountId, upgradePreparation);
        validateUpgradeCandidates(clusterName, sdxUpgradeResponse);
        return upgradePreparation ? initSdxUpgradePreparation(userCrn, sdxUpgradeResponse.getUpgradeCandidates(), upgradeRequest, cluster)
                : initSdxUpgrade(userCrn, sdxUpgradeResponse.getUpgradeCandidates(), upgradeRequest, cluster);
    }

    public SdxUpgradeResponse triggerUpgradeByCrn(String userCrn, String clusterCrn, SdxUpgradeRequest upgradeRequest, String accountId,
            boolean upgradePreparation) {
        SdxCluster cluster = sdxService.getByCrn(userCrn, clusterCrn);
        MDCBuilder.buildMdcContext(cluster);
        SdxUpgradeResponse sdxUpgradeResponse = checkForUpgradeByCrn(userCrn, clusterCrn, upgradeRequest, accountId, upgradePreparation);
        validateUpgradeCandidates(cluster.getClusterName(), sdxUpgradeResponse);
        return upgradePreparation ? initSdxUpgradePreparation(userCrn, sdxUpgradeResponse.getUpgradeCandidates(), upgradeRequest, cluster)
                : initSdxUpgrade(userCrn, sdxUpgradeResponse.getUpgradeCandidates(), upgradeRequest, cluster);
    }

    private boolean isInternalRepoAllowedForUpgrade(String userCrn) {
        String accountId = sdxService.getAccountIdFromCrn(userCrn);
        return entitlementService.isInternalRepositoryForUpgradeAllowed(accountId);
    }

    private SdxUpgradeResponse checkForSdxUpgradeResponse(SdxUpgradeRequest upgradeSdxClusterRequest,
            String clusterName, String accountId, boolean upgradePreparation) {
        UpgradeV4Request request = sdxUpgradeClusterConverter.sdxUpgradeRequestToUpgradeV4Request(upgradeSdxClusterRequest);
        if (upgradePreparation) {
            InternalUpgradeSettings internalUpgradeSettings = new InternalUpgradeSettings(false, false, false, true);
            request.setInternalUpgradeSettings(internalUpgradeSettings);
        }
        UpgradeV4Response upgradeV4Response = ThreadBasedUserCrnProvider
                .doAsInternalActor(
                        regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                        () -> stackV4Endpoint.checkForClusterUpgradeByName(WORKSPACE_ID, clusterName,
                                request, accountId));
        UpgradeV4Response filteredUpgradeV4Response =
                upgradeFilter.filterSdxUpgradeResponse(accountId, clusterName, upgradeSdxClusterRequest, upgradeV4Response);
        return sdxUpgradeClusterConverter.upgradeResponseToSdxUpgradeResponse(filteredUpgradeV4Response);
    }

    private SdxUpgradeResponse initSdxUpgrade(String userCrn, List<ImageInfoV4Response> upgradeCandidates, SdxUpgradeRequest request, SdxCluster cluster) {
        verifyPaywallAccess(userCrn, request);
        String imageId = determineImageId(request, upgradeCandidates);
        boolean skipBackup = request != null && Boolean.TRUE.equals(request.getSkipBackup());
        boolean skipAtlasMetadata = request != null && Boolean.TRUE.equals(request.isSkipAtlasMetadata());
        boolean skipRangerAudits = request != null && Boolean.TRUE.equals(request.isSkipRangerAudits());
        boolean skipRangerMetadata = request != null && Boolean.TRUE.equals(request.isSkipRangerMetadata());
        DatalakeDrSkipOptions skipOptions = new DatalakeDrSkipOptions(skipAtlasMetadata, skipRangerAudits, skipRangerMetadata);
        FlowIdentifier flowIdentifier = triggerDatalakeUpgradeFlow(imageId, cluster, shouldReplaceVmsAfterUpgrade(request), skipBackup, skipOptions);
        String message = messagesService.getMessage(ResourceEvent.DATALAKE_UPGRADE.getMessage(), Collections.singletonList(imageId));
        return new SdxUpgradeResponse(message, flowIdentifier);
    }

    private SdxUpgradeResponse initSdxUpgradePreparation(String userCrn, List<ImageInfoV4Response> upgradeCandidates, SdxUpgradeRequest request,
            SdxCluster cluster) {
        if (Boolean.TRUE.equals(request.getLockComponents())) {
            throw new BadRequestException("Upgrade preparation is not necessary in case of OS upgrade.");
        }
        verifyPaywallAccess(userCrn, request);
        String imageId = determineImageId(request, upgradeCandidates);
        FlowIdentifier flowIdentifier = sdxReactorFlowManager.triggerDatalakeRuntimeUpgradePreparationFlow(cluster, imageId);
        String message = messagesService.getMessage(ResourceEvent.DATALAKE_UPGRADE_PREPARATION.getMessage(), Collections.singletonList(imageId));
        return new SdxUpgradeResponse(message, flowIdentifier);
    }

    private void verifyPaywallAccess(String userCrn, SdxUpgradeRequest upgradeRequest) {
        if (upgradeRequest != null && !Boolean.TRUE.equals(upgradeRequest.getLockComponents())) {
            if (!isInternalRepoAllowedForUpgrade(userCrn)) {
                verifyCMLicenseValidity(userCrn);
            } else {
                LOGGER.info("Internal repo is allowed for upgrade, skip CM license validation");
            }
        }
    }

    private void verifyCMLicenseValidity(String userCrn) {
        LOGGER.info("Verify if the CM license is valid to authenticate to {}", paywallUrl);
        JsonCMLicense license = clouderaManagerLicenseProvider.getLicense(userCrn);
        paywallAccessChecker.checkPaywallAccess(license, paywallUrl);
    }

    private FlowIdentifier triggerDatalakeUpgradeFlow(String imageId, SdxCluster cluster, SdxUpgradeReplaceVms replaceVms,
            boolean skipBackup, DatalakeDrSkipOptions skipOptions) {
        return sdxReactorFlowManager.triggerDatalakeRuntimeUpgradeFlow(cluster, imageId, replaceVms, skipBackup, skipOptions);
    }

    private String determineImageId(SdxUpgradeRequest upgradeRequest, List<ImageInfoV4Response> upgradeCandidates) {
        if (Objects.isNull(upgradeRequest) || upgradeRequest.isEmpty() || Boolean.TRUE.equals(upgradeRequest.getLockComponents())) {
            ImageInfoV4Response imageInfoV4Response = upgradeCandidates.stream().max(ImageInfoV4Response.creationBasedComparator()).orElseThrow();
            String imageId = imageInfoV4Response.getImageId();
            LOGGER.debug("Choosing latest image with id {} as either upgrade request is empty or lockComponents is true", imageId);
            return imageId;
        } else {
            String requestImageId = upgradeRequest.getImageId();
            String runtime = upgradeRequest.getRuntime();
            if (StringUtils.isNotEmpty(requestImageId)) {
                String imageId = validateImageId(upgradeCandidates, requestImageId);
                LOGGER.debug("Chosen image with id {} as it was specified in the request", imageId);
                return imageId;
            } else if (StringUtils.isNotEmpty(runtime)) {
                String imageId = validateRuntime(upgradeCandidates, runtime);
                LOGGER.debug("Chosen image with id {} for {} runtime specified in the request", imageId, runtime);
                return imageId;
            } else {
                throw new BadRequestException(String.format("Invalid upgrade request, please validate the contents: %s", upgradeRequest));
            }
        }
    }

    private void validateUpgradeCandidates(String clusterName, SdxUpgradeResponse upgradeResponse) {
        List<ImageInfoV4Response> upgradeCandidates = upgradeResponse.getUpgradeCandidates();
        if (StringUtils.isNotEmpty(upgradeResponse.getReason())) {
            throw new BadRequestException(String.format("The following error prevents the cluster upgrade process, please fix it and try again: %s",
                    upgradeResponse.getReason()));
        } else if (CollectionUtils.isEmpty(upgradeCandidates)) {
            throw new BadRequestException(String.format("There is no compatible image to upgrade for stack %s", clusterName));
        }
    }

    private String validateRuntime(List<ImageInfoV4Response> upgradeCandidates, String runtime) {
        Supplier<Stream<ImageInfoV4Response>> imagesWithMatchingRuntime = () -> upgradeCandidates.stream().filter(
                imageInfoV4Response -> runtime.equals(imageInfoV4Response.getComponentVersions().getCdp()));
        boolean hasCompatibleImageWithRuntime = imagesWithMatchingRuntime.get().anyMatch(e -> true);
        if (!hasCompatibleImageWithRuntime) {
            String availableRuntimes = upgradeCandidates
                    .stream()
                    .map(ImageInfoV4Response::getComponentVersions)
                    .map(ImageComponentVersions::getCdp)
                    .distinct()
                    .collect(Collectors.joining(","));
            String errorMessage = StringUtils.isEmpty(availableRuntimes) ?
                    String.format("There is no image eligible for the cluster upgrade with runtime: %s.", runtime)
                    : String.format("There is no image eligible for the cluster upgrade with runtime: %s. Please choose a runtime from the following: %s",
                    runtime, availableRuntimes);
            throw new BadRequestException(errorMessage);
        } else {
            ImageInfoV4Response imageInfoV4Response = imagesWithMatchingRuntime.get().max(ImageInfoV4Response.creationBasedComparator()).orElseThrow();
            return imageInfoV4Response.getImageId();
        }
    }

    private String validateImageId(List<ImageInfoV4Response> upgradeCandidates, String requestImageId) {
        if (upgradeCandidates.stream().noneMatch(imageInfoV4Response -> imageInfoV4Response.getImageId().equalsIgnoreCase(requestImageId))) {
            String candidates = upgradeCandidates.stream().map(ImageInfoV4Response::getImageId).collect(Collectors.joining(","));
            throw new BadRequestException(String.format("The given image (%s) is not eligible for the cluster upgrade. "
                    + "Please choose an id from the following: %s", requestImageId, candidates));
        }
        return requestImageId;
    }

    private SdxUpgradeReplaceVms shouldReplaceVmsAfterUpgrade(SdxUpgradeRequest upgradeRequest) {
        SdxUpgradeReplaceVms replaceVms = Optional.ofNullable(upgradeRequest)
                .map(SdxUpgradeRequest::getReplaceVms)
                .orElse(SdxUpgradeReplaceVms.ENABLED);
        LOGGER.debug("VM-s replacement after the upgrade process is {}", replaceVms.name());
        return replaceVms;
    }
}
