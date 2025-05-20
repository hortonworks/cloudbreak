package com.sequenceiq.datalake.service.upgrade;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.InternalUpgradeSettings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageComponentVersions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.datalakedr.DatalakeDrSkipOptions;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.datalake.controller.sdx.SdxUpgradeClusterConverter;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.validation.upgrade.SdxUpgradeValidator;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.sdx.api.model.SdxUpgradeReplaceVms;
import com.sequenceiq.sdx.api.model.SdxUpgradeRequest;
import com.sequenceiq.sdx.api.model.SdxUpgradeResponse;

@Component
public class SdxRuntimeUpgradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxRuntimeUpgradeService.class);

    private static final long WORKSPACE_ID = 0L;

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
    private SdxUpgradeFilter upgradeFilter;

    @Inject
    private SdxUpgradeValidator sdxUpgradeValidator;

    public SdxUpgradeResponse checkForUpgradeByName(String userCrn, String clusterName, SdxUpgradeRequest upgradeRequest, boolean upgradePreparation) {
        SdxCluster cluster = sdxService.getByNameInAccount(userCrn, clusterName);
        return checkForSdxUpgradeResponse(upgradeRequest, cluster, userCrn, upgradePreparation);
    }

    public SdxUpgradeResponse checkForUpgradeByCrn(String userCrn, String clusterCrn, SdxUpgradeRequest upgradeRequest, boolean upgradePreparation) {
        SdxCluster cluster = sdxService.getByCrn(userCrn, clusterCrn);
        return checkForSdxUpgradeResponse(upgradeRequest, cluster, userCrn, upgradePreparation);
    }

    public SdxUpgradeResponse triggerUpgradeByName(String userCrn, String clusterName, SdxUpgradeRequest upgradeRequest, boolean upgradePreparation) {
        SdxCluster cluster = sdxService.getByNameInAccount(userCrn, clusterName);
        return triggerUpgrade(userCrn, cluster, upgradeRequest, upgradePreparation);
    }

    public SdxUpgradeResponse triggerUpgradeByCrn(String userCrn, String clusterCrn, SdxUpgradeRequest upgradeRequest, boolean upgradePreparation) {
        SdxCluster cluster = sdxService.getByCrn(userCrn, clusterCrn);
        return triggerUpgrade(userCrn, cluster, upgradeRequest, upgradePreparation);
    }

    private SdxUpgradeResponse triggerUpgrade(String userCrn, SdxCluster cluster, SdxUpgradeRequest upgradeRequest, boolean upgradePreparation) {
        boolean skipBackup = upgradeRequest != null && Boolean.TRUE.equals(upgradeRequest.getSkipBackup());
        MDCBuilder.buildMdcContext(cluster);
        SdxUpgradeResponse sdxUpgradeResponse = checkForSdxUpgradeResponse(upgradeRequest, cluster, userCrn, upgradePreparation);
        validateUpgradeCandidates(cluster.getClusterName(), sdxUpgradeResponse);
        sdxUpgradeValidator.verifyPaywallAccess(userCrn, upgradeRequest);
        return upgradePreparation ? initSdxUpgradePreparation(sdxUpgradeResponse, upgradeRequest, cluster, skipBackup)
                : initSdxUpgrade(sdxUpgradeResponse, upgradeRequest, cluster);
    }

    private SdxUpgradeResponse checkForSdxUpgradeResponse(SdxUpgradeRequest upgradeRequest, SdxCluster cluster, String userCrn, boolean upgradePreparation) {
        UpgradeV4Request request = createUpgradeV4Request(upgradeRequest, upgradePreparation);
        UpgradeV4Response upgradeV4Response = ThreadBasedUserCrnProvider
                .doAsInternalActor(
                        () -> stackV4Endpoint.checkForClusterUpgradeByName(WORKSPACE_ID, cluster.getClusterName(), request,
                                Crn.safeFromString(userCrn).getAccountId()));
        UpgradeV4Response filteredUpgradeV4Response = upgradeFilter.filterSdxUpgradeResponse(upgradeRequest, upgradeV4Response,
                cluster.getClusterShape());
        return sdxUpgradeClusterConverter.upgradeResponseToSdxUpgradeResponse(filteredUpgradeV4Response);
    }

    private UpgradeV4Request createUpgradeV4Request(SdxUpgradeRequest upgradeSdxClusterRequest, boolean upgradePreparation) {
        UpgradeV4Request request = sdxUpgradeClusterConverter.sdxUpgradeRequestToUpgradeV4Request(upgradeSdxClusterRequest);
        InternalUpgradeSettings internalUpgradeSettings = new InternalUpgradeSettings(false, upgradePreparation,
                Boolean.TRUE.equals(upgradeSdxClusterRequest.getRollingUpgradeEnabled()));
        request.setInternalUpgradeSettings(internalUpgradeSettings);
        return request;
    }

    private SdxUpgradeResponse initSdxUpgrade(SdxUpgradeResponse sdxUpgradeResponse, SdxUpgradeRequest request, SdxCluster cluster) {
        List<ImageInfoV4Response> upgradeCandidates = sdxUpgradeResponse.getUpgradeCandidates();
        sdxUpgradeValidator.validateRollingUpgradeByClusterShape(request, cluster.getClusterShape());
        String targetImageId = determineImageId(request, upgradeCandidates, sdxUpgradeResponse.getCurrent().getComponentVersions().getOs());
        String targetCdhVersion = getTargetCdhVersion(upgradeCandidates, targetImageId);
        FlowIdentifier flowIdentifier = triggerDatalakeUpgradeFlow(request, cluster, targetImageId);
        String message = messagesService.getMessage(ResourceEvent.DATALAKE_UPGRADE.getMessage(), List.of(targetCdhVersion, targetImageId));
        return new SdxUpgradeResponse(message, flowIdentifier);
    }

    private static String getTargetCdhVersion(List<ImageInfoV4Response> upgradeCandidates, String targetImageId) {
        return upgradeCandidates.stream().filter(image -> image.getImageId().equals(targetImageId)).findFirst()
                .orElseThrow(() -> new BadRequestException(String.format("The %s image id is not present among the candidates", targetImageId)))
                .getComponentVersions().getCdp();
    }

    private FlowIdentifier triggerDatalakeUpgradeFlow(SdxUpgradeRequest request, SdxCluster cluster, String imageId) {
        boolean skipBackup = request != null && Boolean.TRUE.equals(request.getSkipBackup());
        boolean skipValidation = request != null && Boolean.TRUE.equals(request.isSkipValidation());
        boolean skipAtlasMetadata = request != null && Boolean.TRUE.equals(request.isSkipAtlasMetadata());
        boolean skipRangerAudits = request != null && Boolean.TRUE.equals(request.isSkipRangerAudits());
        boolean skipRangerMetadata = request != null && Boolean.TRUE.equals(request.isSkipRangerMetadata());
        boolean rollingUpgradeEnabled = request != null && Boolean.TRUE.equals(request.getRollingUpgradeEnabled());
        boolean keepVariant = request != null && Boolean.TRUE.equals(request.isKeepVariant());
        DatalakeDrSkipOptions skipOptions = new DatalakeDrSkipOptions(skipValidation, skipAtlasMetadata, skipRangerAudits, skipRangerMetadata);
        return sdxReactorFlowManager.triggerDatalakeRuntimeUpgradeFlow(cluster, imageId, shouldReplaceVmsAfterUpgrade(request), skipBackup, skipOptions,
                rollingUpgradeEnabled, keepVariant);
    }

    private SdxUpgradeResponse initSdxUpgradePreparation(SdxUpgradeResponse upgradeResponse, SdxUpgradeRequest request, SdxCluster cluster, boolean skipBackup) {
        if (Boolean.TRUE.equals(request.getLockComponents())) {
            throw new BadRequestException("Upgrade preparation is not necessary in case of OS upgrade.");
        }
        List<ImageInfoV4Response> upgradeCandidates = upgradeResponse.getUpgradeCandidates();
        String imageId = determineImageId(request, upgradeCandidates, upgradeResponse.getCurrent().getComponentVersions().getOs());
        FlowIdentifier flowIdentifier = sdxReactorFlowManager.triggerDatalakeRuntimeUpgradePreparationFlow(cluster, imageId, skipBackup);
        String targetCdhVersion = getTargetCdhVersion(upgradeCandidates, imageId);
        String message = messagesService.getMessage(ResourceEvent.DATALAKE_UPGRADE_PREPARATION.getMessage(), List.of(targetCdhVersion, imageId));
        return new SdxUpgradeResponse(message, flowIdentifier);
    }

    private String determineImageId(SdxUpgradeRequest upgradeRequest, List<ImageInfoV4Response> upgradeCandidates, String currentOs) {
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
                String imageId = validateRuntime(upgradeCandidates, runtime, currentOs);
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

    private String validateRuntime(List<ImageInfoV4Response> upgradeCandidates, String runtime, String currentOs) {
        Supplier<Stream<ImageInfoV4Response>> imagesWithMatchingRuntime = () -> upgradeCandidates.stream()
                .filter(imageInfoV4Response -> runtime.equals(imageInfoV4Response.getComponentVersions().getCdp())
                        && currentOs.equals(imageInfoV4Response.getComponentVersions().getOs()));
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
