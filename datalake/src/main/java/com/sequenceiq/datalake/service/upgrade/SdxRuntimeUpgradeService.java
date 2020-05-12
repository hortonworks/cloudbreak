package com.sequenceiq.datalake.service.upgrade;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageComponentVersions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.util.NullUtil;
import com.sequenceiq.datalake.controller.exception.BadRequestException;
import com.sequenceiq.datalake.controller.sdx.SdxUpgradeClusterConverter;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.service.sdx.SdxService;
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
    private EntitlementService entitlementService;

    public SdxUpgradeResponse checkForUpgradeByName(String userCrn, String clusterName, SdxUpgradeRequest upgradeSdxClusterRequest) {
        SdxUpgradeResponse sdxUpgradeResponse = checkForSdxUpgradeResponse(userCrn, upgradeSdxClusterRequest, clusterName);
        validateUpgradeCandidates(clusterName, sdxUpgradeResponse);
        return sdxUpgradeResponse;
    }

    public SdxUpgradeResponse checkForUpgradeByCrn(String userCrn, String crn, SdxUpgradeRequest upgradeSdxClusterRequest) {
        SdxCluster cluster = sdxService.getByCrn(userCrn, crn);
        String clusterName = cluster.getClusterName();
        SdxUpgradeResponse sdxUpgradeResponse = checkForSdxUpgradeResponse(userCrn, upgradeSdxClusterRequest, clusterName);
        validateUpgradeCandidates(clusterName, sdxUpgradeResponse);
        return sdxUpgradeResponse;
    }

    public SdxUpgradeResponse triggerUpgradeByName(String userCrn, String clusterName, SdxUpgradeRequest upgradeRequest) {
        SdxCluster cluster = sdxService.getSdxByNameInAccount(userCrn, clusterName);
        SdxUpgradeResponse sdxUpgradeResponse = checkForUpgradeByName(userCrn, clusterName, upgradeRequest);
        validateUpgradeCandidates(clusterName, sdxUpgradeResponse);
        return initSdxUpgrade(userCrn, upgradeRequest, cluster);
    }

    public SdxUpgradeResponse triggerUpgradeByCrn(String userCrn, String clusterCrn, SdxUpgradeRequest upgradeRequest) {
        SdxCluster cluster = sdxService.getByCrn(userCrn, clusterCrn);
        SdxUpgradeResponse sdxUpgradeResponse = checkForUpgradeByCrn(userCrn, clusterCrn, upgradeRequest);
        validateUpgradeCandidates(cluster.getClusterName(), sdxUpgradeResponse);
        return initSdxUpgrade(userCrn, upgradeRequest, cluster);
    }

    public boolean isRuntimeUpgradeEnabled(String userCrn) {
        String accountId = sdxService.getAccountIdFromCrn(userCrn);
        return entitlementService.runtimeUpgradeEnabled(INTERNAL_ACTOR_CRN, accountId);
    }

    private SdxUpgradeResponse checkForSdxUpgradeResponse(String userCrn, SdxUpgradeRequest upgradeSdxClusterRequest, String clusterName) {
        verifyRuntimeUpgradeEntitlement(userCrn);
        UpgradeV4Response upgradeV4Response = stackV4Endpoint.checkForClusterUpgradeByName(WORKSPACE_ID, clusterName,
                sdxUpgradeClusterConverter.sdxUpgradeRequestToUpgradeV4Request(upgradeSdxClusterRequest));
        return sdxUpgradeClusterConverter.upgradeResponseToSdxUpgradeResponse(upgradeV4Response);
    }

    private SdxUpgradeResponse initSdxUpgrade(String userCrn, SdxUpgradeRequest upgradeRequest, SdxCluster cluster) {
        String imageId = determineImageId(userCrn, cluster.getClusterName(), upgradeRequest);
        FlowIdentifier flowIdentifier = triggerDatalakeUpgradeFlow(imageId, cluster, shouldReplaceVmsAfterUpgrade(upgradeRequest));
        String message = getMessage(imageId);
        return new SdxUpgradeResponse(message, flowIdentifier);
    }

    private void verifyRuntimeUpgradeEntitlement(String userCrn) {
        if (!isRuntimeUpgradeEnabled(userCrn)) {
            throw new BadRequestException("Runtime upgrade feature is not enabled");
        }
    }

    private FlowIdentifier triggerDatalakeUpgradeFlow(String imageId, SdxCluster cluster, SdxUpgradeReplaceVms replaceVms) {
        MDCBuilder.buildMdcContext(cluster);
        return sdxReactorFlowManager.triggerDatalakeRuntimeUpgradeFlow(cluster, imageId, replaceVms);
    }

    private String getMessage(String imageId) {
        return messagesService.getMessage(ResourceEvent.DATALAKE_UPGRADE.getMessage(), Collections.singletonList(imageId));
    }

    private String determineImageId(String userCrn, String clusterName, SdxUpgradeRequest upgradeRequest) {
        String imageId;
        SdxUpgradeResponse upgradeResponse = checkForUpgradeByName(userCrn, clusterName, upgradeRequest);
        List<ImageInfoV4Response> upgradeCandidates = validateUpgradeCandidates(clusterName, upgradeResponse);

        if (Objects.isNull(upgradeRequest) || upgradeRequest.isEmpty() || Boolean.TRUE.equals(upgradeRequest.getLockComponents())) {
            ImageInfoV4Response imageInfoV4Response = upgradeCandidates.stream().max(getComparator()).orElseThrow();
            imageId = imageInfoV4Response.getImageId();
        } else {
            String requestImageId = upgradeRequest.getImageId();
            String runtime = upgradeRequest.getRuntime();

            if (StringUtils.isNotEmpty(requestImageId)) {
                imageId = validateImageId(upgradeCandidates, requestImageId);
            } else if (StringUtils.isNotEmpty(runtime)) {
                imageId = validateRuntime(upgradeCandidates, runtime);
            } else {
                throw new BadRequestException(String.format("Invalid upgrade request, please validate the contents: %s", upgradeRequest.toString()));
            }
        }
        return imageId;
    }

    private List<ImageInfoV4Response> validateUpgradeCandidates(String clusterName, SdxUpgradeResponse upgradeResponse) {
        List<ImageInfoV4Response> upgradeCandidates = upgradeResponse.getUpgradeCandidates();
        if (StringUtils.isNotEmpty(upgradeResponse.getReason())) {
            throw new BadRequestException(String.format("The following error prevents the cluster upgrade process, please fix it and try again: %s",
                    upgradeResponse.getReason()));
        } else if (CollectionUtils.isEmpty(upgradeCandidates)) {
            throw new BadRequestException(String.format("There is no compatible image to upgrade for stack %s", clusterName));
        }
        return upgradeCandidates;
    }

    private String validateRuntime(List<ImageInfoV4Response> upgradeCandidates, String runtime) {
        String imageId;
        Supplier<Stream<ImageInfoV4Response>> imagesWithMatchingRuntime = () -> upgradeCandidates.stream().filter(
                imageInfoV4Response -> runtime.equals(imageInfoV4Response.getComponentVersions().getCdp()));
        boolean hasCompatbileImageWithRuntime = imagesWithMatchingRuntime.get().anyMatch(e -> true);
        if (!hasCompatbileImageWithRuntime) {
            String availableRuntimes = upgradeCandidates
                    .stream()
                    .map(ImageInfoV4Response::getComponentVersions)
                    .map(ImageComponentVersions::getCdp)
                    .distinct()
                    .collect(Collectors.joining(","));
            throw new BadRequestException(String.format("There is no image eligible for upgrading the cluster with runtime: %s. "
                    + "Please choose a runtime from the following image(s): %s", runtime, availableRuntimes));
        } else {
            ImageInfoV4Response imageInfoV4Response = imagesWithMatchingRuntime.get().max(getComparator()).orElseThrow();
            imageId = imageInfoV4Response.getImageId();
        }
        return imageId;
    }

    private String validateImageId(List<ImageInfoV4Response> upgradeCandidates, String requestImageId) {
        if (upgradeCandidates.stream().noneMatch(imageInfoV4Response -> imageInfoV4Response.getImageId().equalsIgnoreCase(requestImageId))) {
            String candidates = upgradeCandidates.stream().map(ImageInfoV4Response::getImageId).collect(Collectors.joining(","));
            throw new BadRequestException(String.format("The given image (%s) is not eligible for upgrading the cluster. "
                    + "Please choose an id from the following image(s): %s", requestImageId, candidates));
        }
        return requestImageId;
    }

    private SdxUpgradeReplaceVms shouldReplaceVmsAfterUpgrade(SdxUpgradeRequest upgradeRequest) {
        return Optional.ofNullable(upgradeRequest)
                .map(SdxUpgradeRequest::getReplaceVms)
                .orElse(NullUtil.getIfNotNullOtherwise(upgradeRequest, this::determineReplaceVmsIfParamIsMissing, SdxUpgradeReplaceVms.DISABLED));
    }

    private SdxUpgradeReplaceVms determineReplaceVmsIfParamIsMissing(SdxUpgradeRequest request) {
        SdxUpgradeReplaceVms replaceVms = isOsUpgrade(request) ? SdxUpgradeReplaceVms.ENABLED : SdxUpgradeReplaceVms.DISABLED;
        LOGGER.debug("VM-s replacement after the upgrade process is {}", replaceVms.name());
        return replaceVms;
    }

    public boolean isOsUpgrade(SdxUpgradeRequest request) {
        return Boolean.TRUE.equals(request.getLockComponents()) && StringUtils.isEmpty(request.getRuntime());
    }

    private Comparator<ImageInfoV4Response> getComparator() {
        return Comparator.comparing(ImageInfoV4Response::getCreated);
    }

}
