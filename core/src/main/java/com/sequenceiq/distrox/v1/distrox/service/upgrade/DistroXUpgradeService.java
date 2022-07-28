package com.sequenceiq.distrox.v1.distrox.service.upgrade;

import java.util.List;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
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
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.StackCommonService;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.image.locked.LockedComponentService;
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
    private ClouderaManagerLicenseProvider clouderaManagerLicenseProvider;

    @Inject
    private LockedComponentService lockedComponentService;

    public UpgradeV4Response triggerUpgrade(NameOrCrn cluster, Long workspaceId, String userCrn, UpgradeV4Request request, boolean upgradePreparation) {
        UpgradeV4Response upgradeV4Response = upgradeAvailabilityService.checkForUpgrade(cluster, workspaceId, request, userCrn);
        validateUpgradeCandidates(cluster, upgradeV4Response);
        verifyPaywallAccess(userCrn, request);
        ImageInfoV4Response targetImage = imageSelector.determineImageId(request, upgradeV4Response.getUpgradeCandidates());
        Stack stack = stackService.getByNameOrCrnInWorkspace(cluster, workspaceId);
        MDCBuilder.buildMdcContext(stack);
        boolean lockComponents = determineLockComponentsParam(request, targetImage, stack);
        return upgradePreparation ?
                initUpgradePreparation(cluster, workspaceId, targetImage, upgradeV4Response.getUpgradeCandidates(), lockComponents, stack) :
                initUpgrade(upgradeV4Response, cluster, workspaceId, userCrn, targetImage, lockComponents, stack);
    }

    private UpgradeV4Response initUpgrade(UpgradeV4Response upgradeV4Response, NameOrCrn cluster, Long workspaceId, String userCrn,
            ImageInfoV4Response targetImage, boolean lockComponents, Stack stack) {
        LOGGER.debug("Initializing cluster upgrade. Target image: {}, lockComponents: {}", targetImage.getImageId(), lockComponents);
        boolean replaceVms = determineReplaceVmsParam(upgradeV4Response, lockComponents, stack);
        String upgradeVariant = calculateUpgradeVariant(stack, userCrn);
        FlowIdentifier flowIdentifier = reactorFlowManager.triggerDistroXUpgrade(stack.getId(), createImageChangeDto(cluster, workspaceId, targetImage),
                replaceVms, lockComponents, upgradeVariant);
        return new UpgradeV4Response("Upgrade started with Image: " + targetImage.getImageId(), flowIdentifier, replaceVms);
    }

    private UpgradeV4Response initUpgradePreparation(NameOrCrn cluster, Long workspaceId, ImageInfoV4Response targetImage,
            List<ImageInfoV4Response> upgradeCandidates, boolean lockComponents, Stack stack) {
        LOGGER.debug("Initializing cluster upgrade preparation. Target image: {}, lockComponents: {}", targetImage.getImageId(), lockComponents);
        if (lockComponents) {
            return new UpgradeV4Response(targetImage, upgradeCandidates, "Upgrade preparation is not necessary in case of OS upgrade.");
        } else {
            FlowIdentifier flowIdentifier = reactorFlowManager.triggerClusterUpgradePreparation(stack.getId(),
                    createImageChangeDto(cluster, workspaceId, targetImage), lockComponents);
            return new UpgradeV4Response("Upgrade preparation started with Image: " + targetImage.getImageId(), flowIdentifier, false);
        }
    }

    private boolean determineLockComponentsParam(UpgradeV4Request request, ImageInfoV4Response targetImage, Stack stack) {
        boolean lockComponents = request.getLockComponents() != null ? request.getLockComponents() : isComponentsLocked(stack, targetImage);
        validateOsUpgradeEntitled(lockComponents, request);
        return lockComponents;
    }

    String calculateUpgradeVariant(Stack stack, String userCrn) {
        String variant = stack.getPlatformVariant();
        String accountId = Crn.safeFromString(userCrn).getAccountId();
        boolean migrationEnable = entitlementService.awsVariantMigrationEnable(accountId);
        if (migrationEnable) {
            if (AwsConstants.AwsVariant.AWS_VARIANT.variant().value().equals(variant)) {
                variant = AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant().value();
            }
        }
        return variant;
    }

    private ImageChangeDto createImageChangeDto(NameOrCrn cluster, Long workspaceId, ImageInfoV4Response image) {
        StackImageChangeV4Request stackImageChangeRequest = new StackImageChangeV4Request();
        stackImageChangeRequest.setImageId(image.getImageId());
        stackImageChangeRequest.setImageCatalogName(image.getImageCatalogName());
        return stackCommonService.createImageChangeDto(cluster, workspaceId, stackImageChangeRequest);
    }

    private void validateOsUpgradeEntitled(boolean lockComponents, UpgradeV4Request request) {
        if (lockComponents && !request.getInternalUpgradeSettings().isDataHubOsUpgradeEntitled()) {
            throw new BadRequestException("The OS upgrade is not allowed for DataHub clusters.");
        }
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

    private boolean isComponentsLocked(Stack stack, ImageInfoV4Response targetImage) {
        return lockedComponentService.isComponentsLocked(stack, targetImage.getImageId());
    }

    private boolean determineReplaceVmsParam(UpgradeV4Response upgradeV4Response, boolean lockComponents, Stack stack) {
        boolean originalReplaceVms = upgradeV4Response.isReplaceVms();
        if (originalReplaceVms) {
            try {
                if (!lockComponents) {
                    LOGGER.info("ReplaceVms parameter has been overridden to false for stack {} in case of distrox runtime upgrade.", stack.getName());
                    return false;
                }
            } catch (Exception ex) {
                LOGGER.warn("Exception during override the replaceVms parameter of upgrade response.", ex);
            }
        }
        return originalReplaceVms;
    }
}
