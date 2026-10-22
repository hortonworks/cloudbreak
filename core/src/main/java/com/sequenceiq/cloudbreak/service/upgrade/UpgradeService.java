package com.sequenceiq.cloudbreak.service.upgrade;

import static com.sequenceiq.flow.api.model.FlowType.NOT_TRIGGERED;

import java.util.List;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.osupgrade.OrderedOSUpgradeSet;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.converter.UpgradeComponentsConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterRepairService;
import com.sequenceiq.cloudbreak.service.cluster.OSUpgradeByUpgradeSetsService;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.image.locked.LockedComponentService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Component
public class UpgradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeService.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private ClusterRepairService clusterRepairService;

    @Inject
    private OSUpgradeByUpgradeSetsService osUpgradeByUpgradeSetsService;

    @Inject
    private ReactorFlowManager flowManager;

    @Inject
    private ClusterBootstrapper clusterBootstrapper;

    @Inject
    private LockedComponentService lockedComponentService;

    @Inject
    private BaseImageUpgradeService baseImageUpgradeService;

    public FlowIdentifier upgradeOsByUpgradeSets(StackView stack, String imageId, List<OrderedOSUpgradeSet> upgradeSets) {
        Image image = findImage(stack);
        ImageChangeDto imageChangeDto = getImageChangeDto(imageId, stack.getId(), image);
        return upgradeOsByUpgradeSets(stack, imageChangeDto, upgradeSets);
    }

    public FlowIdentifier upgradeOsByUpgradeSets(StackView stack, ImageChangeDto imageChangeDto, List<OrderedOSUpgradeSet> upgradeSets) {
        MDCBuilder.buildMdcContext(stack);
        ClusterComponent clusterComponent = clusterBootstrapper.updateSaltComponent(stack);
        try {
            FlowIdentifier flowIdentifier = osUpgradeByUpgradeSetsService.osUpgradeByUpgradeSets(stack, imageChangeDto, upgradeSets);
            if (flowIdentifier != null && NOT_TRIGGERED == flowIdentifier.getType()) {
                LOGGER.warn("Upgrade flow not triggered, reverting salt state upgrade");
                clusterComponentConfigProvider.restorePreviousVersion(clusterComponent);
            }
            return flowIdentifier;
        } catch (RuntimeException e) {
            LOGGER.error("Failed to start OS upgrade by upgrade sets, reverting salt state upgrade", e);
            clusterComponentConfigProvider.restorePreviousVersion(clusterComponent);
            throw e;
        }
    }

    public FlowIdentifier upgradeOs(String accountId, NameOrCrn stackNameOrCrn, boolean keepVariant) {
        StackView stack = stackDtoService.getStackViewByNameOrCrn(stackNameOrCrn, accountId);
        MDCBuilder.buildMdcContext(stack);
        ClusterComponent clusterComponent = clusterBootstrapper.updateSaltComponent(stack);
        try {
            FlowIdentifier flowIdentifier = clusterRepairService.repairAll(stack, true, keepVariant);
            if (flowIdentifier != null && NOT_TRIGGERED == flowIdentifier.getType()) {
                LOGGER.warn("Upgrade flow not triggered, reverting salt state upgrade");
                clusterComponentConfigProvider.restorePreviousVersion(clusterComponent);
            }
            return flowIdentifier;
        } catch (RuntimeException e) {
            LOGGER.error("Failed to start OS upgrade, reverting salt state upgrade", e);
            clusterComponentConfigProvider.restorePreviousVersion(clusterComponent);
            throw e;
        }
    }

    public FlowIdentifier upgradeCluster(String accountId, NameOrCrn stackNameOrCrn, String imageId, Boolean rollingUpgradeEnabled) {
        StackView stack = stackDtoService.getStackViewByNameOrCrn(stackNameOrCrn, accountId);
        MDCBuilder.buildMdcContext(stack);
        return flowManager.triggerDataLakeClusterUpgrade(stack.getId(), imageId, Boolean.TRUE.equals(rollingUpgradeEnabled));
    }

    public FlowIdentifier upgradeCluster(String accountId, NameOrCrn stackNameOrCrn, String imageId, Boolean rollingUpgradeEnabled,
            ClouderaManagerV4Request componentsRequest) {
        if (componentsRequest == null) {
            return upgradeCluster(accountId, stackNameOrCrn, imageId, rollingUpgradeEnabled);
        }
        StackDto stack = stackDtoService.getByNameOrCrn(stackNameOrCrn, accountId);
        MDCBuilder.buildMdcContext(stack);
        UpgradeComponents components = UpgradeComponentsConverter.fromRequest(componentsRequest);
        components.validate();
        ImageChangeDto imageChangeDto = getComponentImageChangeDto(stack, imageId, components);
        boolean rollingUpgrade = Boolean.TRUE.equals(rollingUpgradeEnabled);
        LOGGER.info("Starting Data Lake runtime upgrade: runtime={}, imageId={}, rollingUpgradeEnabled={}",
                components.getRuntimeVersion(), imageChangeDto.getImageId(), rollingUpgrade);
        return flowManager.triggerDataLakeClusterUpgrade(stack.getId(), imageChangeDto, rollingUpgrade);
    }

    public FlowIdentifier prepareClusterUpgrade(String accountId, NameOrCrn stackNameOrCrn, String imageId) {
        return prepareClusterUpgrade(accountId, stackNameOrCrn, imageId, null);
    }

    public FlowIdentifier prepareClusterUpgrade(String accountId, NameOrCrn stackNameOrCrn, String imageId, ClouderaManagerV4Request componentsRequest) {
        StackDto stack = stackDtoService.getByNameOrCrn(stackNameOrCrn, accountId);
        MDCBuilder.buildMdcContext(stack);
        if (componentsRequest != null) {
            UpgradeComponents components = UpgradeComponentsConverter.fromRequest(componentsRequest);
            components.validate();
            ImageChangeDto imageChangeDto = getComponentImageChangeDto(stack, imageId, components);
            String runtimeVersion = components.getRuntimeVersion();
            LOGGER.info("Starting Data Lake upgrade preparation: runtime={}, imageId={}", runtimeVersion, imageChangeDto.getImageId());
            return flowManager.triggerClusterUpgradePreparation(stack.getId(), imageChangeDto, runtimeVersion);
        }
        if (lockedComponentService.isComponentsLocked(stack, imageId)) {
            throw new BadRequestException("Upgrade preparation is not necessary in case of OS upgrade.");
        }
        Image image = findImage(stack.getStack());
        ImageChangeDto imageChangeDto = getImageChangeDto(imageId, stack.getId(), image);
        String runtimeVersion = image.getPackageVersion(ImagePackageVersion.STACK);
        return flowManager.triggerClusterUpgradePreparation(stack.getId(), imageChangeDto, runtimeVersion);
    }

    private ImageChangeDto getComponentImageChangeDto(StackDto stack, String imageId, UpgradeComponents components) {
        return StringUtils.isBlank(imageId) ? baseImageUpgradeService.createImageChangeDto(stack, components)
                : baseImageUpgradeService.createImageChangeDto(stack, components, imageId);
    }

    private ImageChangeDto getImageChangeDto(String imageId, Long stackId, Image image) {
        return new ImageChangeDto(stackId, imageId, image.getImageCatalogName(), image.getImageCatalogUrl());
    }

    private Image findImage(StackView stack) {
        try {
            return componentConfigProviderService.getImage(stack.getId());
        } catch (CloudbreakImageNotFoundException e) {
            throw new NotFoundException(String.format("Image not found for stack [%s]", stack.getName()), e);
        }
    }
}
