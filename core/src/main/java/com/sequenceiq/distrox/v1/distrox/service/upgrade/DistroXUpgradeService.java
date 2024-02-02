package com.sequenceiq.distrox.v1.distrox.service.upgrade;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.InternalUpgradeSettings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackImageChangeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.osupgrade.OrderedOSUpgradeSet;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.auth.ClouderaManagerLicenseProvider;
import com.sequenceiq.cloudbreak.auth.JsonCMLicense;
import com.sequenceiq.cloudbreak.auth.PaywallAccessChecker;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.StackCommonService;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.StackUpgradeService;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeAvailabilityService;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeService;
import com.sequenceiq.cloudbreak.service.upgrade.image.locked.LockedComponentService;
import com.sequenceiq.cloudbreak.util.VersionNormalizer;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.distrox.v1.distrox.service.upgrade.dto.DistroXUpgradeDto;
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
    private StackDtoService stackDtoService;

    @Inject
    private StackService stackService;

    @Inject
    private ClouderaManagerLicenseProvider clouderaManagerLicenseProvider;

    @Inject
    private LockedComponentService lockedComponentService;

    @Inject
    private UpgradeService upgradeService;

    @Inject
    private ClusterUpgradeAvailabilityService clusterUpgradeAvailabilityService;

    @Inject
    private StackUpgradeService stackUpgradeService;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Value("${cb.cm.gracefulservicestopnodecountthreshold}")
    private int gracefulServiceStopNodeCountThreshold;

    public FlowIdentifier triggerOsUpgradeByUpgradeSets(NameOrCrn nameOrCrn, Long workspaceId, String imageId, List<OrderedOSUpgradeSet> upgradeSets) {
        Stack stack = stackService.getByNameOrCrnInWorkspace(nameOrCrn, workspaceId);
        Crn crn = Crn.safeFromString(stack.getResourceCrn());
        ImageChangeDto imageChangeDto = deteremineImageChangeDto(nameOrCrn, imageId, stack, crn.getAccountId());
        return upgradeService.upgradeOsByUpgradeSets(stack, imageChangeDto, upgradeSets);
    }

    private ImageChangeDto deteremineImageChangeDto(NameOrCrn nameOrCrn, String imageId, Stack stack, String accountId) {
        boolean dataHubRuntimeUpgradeEnabled = upgradeAvailabilityService.isRuntimeUpgradeEnabledByAccountId(accountId);
        LOGGER.info("DH Runtime Upgrade entitlement: {}", dataHubRuntimeUpgradeEnabled);
        boolean getAllImages = imageId != null;
        UpgradeV4Response upgradeOptions = clusterUpgradeAvailabilityService.checkForUpgrades(stack, true,
                new InternalUpgradeSettings(false, dataHubRuntimeUpgradeEnabled), getAllImages);
        if (upgradeOptions.getUpgradeCandidates().isEmpty()) {
            throw new BadRequestException("There is no available image for upgrade.");
        }
        LOGGER.info("Upgrade options: {}", upgradeOptions);
        ImageInfoV4Response targetImage = imageSelector.determineImage(Optional.ofNullable(imageId), upgradeOptions.getUpgradeCandidates());
        LOGGER.info("Target image will be: {}", targetImage);
        return createImageChangeDto(nameOrCrn, stack.getWorkspaceId(), targetImage);
    }

    public UpgradeV4Response triggerUpgrade(NameOrCrn cluster, Long workspaceId, String userCrn, UpgradeV4Request request, boolean upgradePreparation) {
        UpgradeV4Response upgradeV4Response = upgradeAvailabilityService.checkForUpgrade(cluster, workspaceId, request, userCrn);
        validateUpgradeCandidates(cluster, upgradeV4Response);
        verifyPaywallAccess(userCrn, request);
        ImageInfoV4Response targetImage = imageSelector.determineImageId(request, upgradeV4Response.getUpgradeCandidates());
        StackDto stack = stackDtoService.getByNameOrCrn(cluster, ThreadBasedUserCrnProvider.getAccountId());
        MDCBuilder.buildMdcContext(stack);
        boolean lockComponents = determineLockComponentsParam(request, targetImage, stack);
        ImageChangeDto imageChangeDto = createImageChangeDto(cluster, workspaceId, targetImage);
        return upgradePreparation ?
                initUpgradePreparation(new DistroXUpgradeDto(upgradeV4Response, imageChangeDto, targetImage, lockComponents, stack),
                        upgradeV4Response.getUpgradeCandidates()) :
                initUpgrade(new DistroXUpgradeDto(upgradeV4Response, imageChangeDto, targetImage, lockComponents, stack), userCrn,
                        request.getInternalUpgradeSettings().isRollingUpgradeEnabled(), request.isKeepVariant());
    }

    private UpgradeV4Response initUpgrade(DistroXUpgradeDto upgradeDto, String userCrn, boolean rollingUpgradeEnabled, boolean keepVariant) {
        boolean replaceVms = determineReplaceVmsParam(upgradeDto.getUpgradeV4Response(), upgradeDto.isLockComponents(), upgradeDto.getStackDto().getStack());
        LOGGER.debug("Initializing cluster upgrade. Target image: {}, lockComponents: {}, replaceVms: {}, rollingUpgradeEnabled: {}",
                upgradeDto.getImageChangeDto().getImageId(), upgradeDto.isLockComponents(), replaceVms, rollingUpgradeEnabled);
        if (replaceVms && rollingUpgradeEnabled) {
            validateRollingUpgradeNodeCountAndRuntime(upgradeDto.getStackDto());
        }
        String upgradeVariant = stackUpgradeService.calculateUpgradeVariant(upgradeDto.getStackDto().getStack(), userCrn, keepVariant);
        FlowIdentifier flowIdentifier = reactorFlowManager.triggerDistroXUpgrade(upgradeDto.getStackDto().getStack().getId(), upgradeDto.getImageChangeDto(),
                replaceVms, upgradeDto.isLockComponents(), upgradeVariant, rollingUpgradeEnabled);
        return new UpgradeV4Response("Upgrade started with Image: " + upgradeDto.getImageChangeDto().getImageId(), flowIdentifier, replaceVms);
    }

    private void validateRollingUpgradeNodeCountAndRuntime(StackDto stackDto) {
        if (isGracefulStopServicesNeeded(stackDto)) {
            if (stackDto.getFullNodeCount() > gracefulServiceStopNodeCountThreshold) {
                throw new BadRequestException(String.format(
                        "Rolling upgrade with VM replacement (zero downtime upgrade) is only supported if the cluster doesn't contain more nodes than %s.",
                        gracefulServiceStopNodeCountThreshold));
            }
        }
    }

    public boolean isGracefulStopServicesNeeded(StackDto stackDto) {
        return StackType.WORKLOAD.equals(stackDto.getType()) && clusterComponentConfigProvider.getCdhProduct(stackDto.getCluster().getId())
                .filter(clouderaManagerProduct -> {
                    if (CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited(VersionNormalizer.normalizeCdhVersion(clouderaManagerProduct.getVersion()),
                            CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_2_18)) {
                        return true;
                    } else {
                        LOGGER.info("Skipping graceful service stop on {} CM version.", clouderaManagerProduct.getVersion());
                        return false;
                    }
                })
                .isPresent();
    }

    private UpgradeV4Response initUpgradePreparation(DistroXUpgradeDto upgradeDto, List<ImageInfoV4Response> upgradeCandidates) {
        LOGGER.debug("Initializing cluster upgrade preparation. Target image: {}, lockComponents: {}", upgradeDto.getTargetImage().getImageId(),
                upgradeDto.isLockComponents());
        if (upgradeDto.isLockComponents()) {
            return new UpgradeV4Response(upgradeDto.getTargetImage(), upgradeCandidates, "Upgrade preparation is not necessary in case of OS upgrade.");
        } else {
            FlowIdentifier flowIdentifier = reactorFlowManager.triggerClusterUpgradePreparation(upgradeDto.getStackDto().getId(),
                    upgradeDto.getImageChangeDto());
            return new UpgradeV4Response("Upgrade preparation started with Image: " + upgradeDto.getTargetImage().getImageId(), flowIdentifier, false);
        }
    }

    private boolean determineLockComponentsParam(UpgradeV4Request request, ImageInfoV4Response targetImage, StackDto stack) {
        return request.getLockComponents() != null ? request.getLockComponents() : isComponentsLocked(stack, targetImage);
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

    private boolean isComponentsLocked(StackDto stack, ImageInfoV4Response targetImage) {
        return lockedComponentService.isComponentsLocked(stack, targetImage.getImageId());
    }

    private boolean determineReplaceVmsParam(UpgradeV4Response upgradeV4Response, boolean lockComponents, StackView stack) {
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
