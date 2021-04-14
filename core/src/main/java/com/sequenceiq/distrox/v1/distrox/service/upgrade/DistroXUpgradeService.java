package com.sequenceiq.distrox.v1.distrox.service.upgrade;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackImageChangeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.auth.ClouderaManagerLicenseProvider;
import com.sequenceiq.cloudbreak.auth.JsonCMLicense;
import com.sequenceiq.cloudbreak.auth.PaywallAccessChecker;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.StackCommonService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogProvider;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.image.ImageProvider;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.ImageFilterParamsFactory;
import com.sequenceiq.cloudbreak.service.upgrade.image.locked.LockedComponentChecker;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class DistroXUpgradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXUpgradeService.class);

    @Value("${cb.paywall.url}")
    private String paywallUrl;

    @Inject
    private DistroXUpgradeAvailabilityService upgradeAvailabilityService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private PaywallAccessChecker paywallAccessChecker;

    @Inject
    private DistroXUpgradeImageSelector imageSelector;

    @Inject
    private StackCommonService stackCommonService;

    @Inject
    private ReactorFlowManager reactorFlowManager;

    @Inject
    private StackService stackService;

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private ClouderaManagerLicenseProvider clouderaManagerLicenseProvider;

    @Inject
    private ImageCatalogProvider imageCatalogProvider;

    @Inject
    private ImageProvider imageProvider;

    @Inject
    private LockedComponentChecker lockedComponentChecker;

    @Inject
    private ImageFilterParamsFactory imageFilterParamsFactory;

    public UpgradeV4Response triggerUpgrade(NameOrCrn cluster, Long workspaceId, String userCrn, UpgradeV4Request request) {
        UpgradeV4Response upgradeV4Response = upgradeAvailabilityService.checkForUpgrade(cluster, workspaceId, request, userCrn);
        validateUpgradeCandidates(cluster, upgradeV4Response);
        verifyPaywallAccess(userCrn, request);
        return initUpgrade(request, upgradeV4Response, cluster, workspaceId);
    }

    private UpgradeV4Response initUpgrade(UpgradeV4Request request, UpgradeV4Response upgradeV4Response, NameOrCrn cluster, Long workspaceId) {
        ImageInfoV4Response image = imageSelector.determineImageId(request, upgradeV4Response.getUpgradeCandidates());
        ImageChangeDto imageChangeDto = createImageChangeDto(cluster, workspaceId, image);
        Stack stack = stackService.getByNameOrCrnInWorkspace(cluster, workspaceId);
        boolean replaceVms = determineReplaceVmsParam(upgradeV4Response, stack, image);
        FlowIdentifier flowIdentifier = reactorFlowManager.triggerDistroXUpgrade(stack.getId(), imageChangeDto, replaceVms);
        UpgradeV4Response response = new UpgradeV4Response("Upgrade started with Image: " + image.getImageId(), flowIdentifier);
        response.setReplaceVms(replaceVms);
        return response;
    }

    private ImageChangeDto createImageChangeDto(NameOrCrn cluster, Long workspaceId, ImageInfoV4Response image) {
        StackImageChangeV4Request stackImageChangeRequest = new StackImageChangeV4Request();
        stackImageChangeRequest.setImageId(image.getImageId());
        stackImageChangeRequest.setImageCatalogName(image.getImageCatalogName());
        return stackCommonService.createImageChangeDto(cluster, workspaceId, stackImageChangeRequest);
    }

    private void validateUpgradeCandidates(NameOrCrn cluster, UpgradeV4Response upgradeResponse) {
        if (StringUtils.isNotEmpty(upgradeResponse.getReason())) {
            throw new BadRequestException(String.format("The following error prevents the cluster upgrade process, please fix it and try again: %s",
                    upgradeResponse.getReason()));
        } else if (CollectionUtils.isEmpty(upgradeResponse.getUpgradeCandidates())) {
            throw new BadRequestException(String.format("There is no compatible image to upgrade for stack %s", cluster.getNameOrCrn()));
        }
    }

    private void verifyPaywallAccess(String userCrn, UpgradeV4Request upgradeRequest) {
        if (upgradeRequest != null && !Boolean.TRUE.equals(upgradeRequest.getLockComponents())) {
            if (!isInternalRepoAllowedForUpgrade(userCrn)) {
                verifyCMLicenseValidity(userCrn);
            } else {
                LOGGER.info("Internal repo is allowed for upgrade, skip CM license validation");
            }
        }
    }

    private boolean isInternalRepoAllowedForUpgrade(String userCrn) {
        String accountId = Crn.safeFromString(userCrn).getAccountId();
        return entitlementService.isInternalRepositoryForUpgradeAllowed(accountId);
    }

    private void verifyCMLicenseValidity(String userCrn) {
        LOGGER.info("Verify if the CM license is valid to authenticate to {}", paywallUrl);
        JsonCMLicense license = clouderaManagerLicenseProvider.getLicense(userCrn);
        paywallAccessChecker.checkPaywallAccess(license, paywallUrl);
    }

    private boolean determineReplaceVmsParam(UpgradeV4Response upgradeV4Response, Stack stack, ImageInfoV4Response targetImage) {
        boolean originalReplaceVms = upgradeV4Response.isReplaceVms();
        if (originalReplaceVms) {
            try {
                Image currentImage = componentConfigProviderService.getImage(stack.getId());
                CloudbreakImageCatalogV3 imageCatalog = imageCatalogProvider.getImageCatalogV3(currentImage.getImageCatalogUrl());
                com.sequenceiq.cloudbreak.cloud.model.catalog.Image currentCatalogImage =
                        imageProvider.getCurrentImageFromCatalog(currentImage.getImageId(), imageCatalog);
                com.sequenceiq.cloudbreak.cloud.model.catalog.Image targetCatalogImage =
                        imageProvider.getCurrentImageFromCatalog(targetImage.getImageId(), imageCatalog);
                if (!lockedComponentChecker.isUpgradePermitted(
                        currentCatalogImage, targetCatalogImage, imageFilterParamsFactory.getStackRelatedParcels(stack))) {
                    LOGGER.info("ReplaceVms parameter has been overridden to false for stack {} in case of distrox runtime upgrade." +
                            " Current image: {}, target image: {}", stack.getName(), currentCatalogImage.getUuid(), targetCatalogImage.getUuid());
                    return false;
                }
            } catch (Exception ex) {
                LOGGER.warn("Exception during override the replaceVms parameter of upgrade response.", ex);
            }
        }
        return originalReplaceVms;
    }
}
