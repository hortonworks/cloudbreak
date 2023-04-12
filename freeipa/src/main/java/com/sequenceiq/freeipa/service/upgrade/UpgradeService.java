package com.sequenceiq.freeipa.service.upgrade;

import static com.sequenceiq.freeipa.api.v1.operation.model.OperationState.RUNNING;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceTemplateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.VolumeRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.FreeIpaUpgradeOptions;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.FreeIpaUpgradeRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.FreeIpaUpgradeResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.ImageInfoResponse;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.chain.FlowChainTriggers;
import com.sequenceiq.freeipa.flow.freeipa.upgrade.UpgradeEvent;
import com.sequenceiq.freeipa.flow.stack.migration.handler.AwsMigrationUtil;
import com.sequenceiq.freeipa.service.DefaultRootVolumeSizeProvider;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

@Service
public class UpgradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeService.class);

    @Inject
    private OperationService operationService;

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaFlowManager flowManager;

    @Inject
    private UpgradeImageService imageService;

    @Inject
    private UpgradeValidationService validationService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private AwsMigrationUtil awsMigrationUtil;

    @Inject
    private DefaultRootVolumeSizeProvider rootVolumeSizeProvider;

    @SuppressWarnings("IllegalType")
    public FreeIpaUpgradeResponse upgradeFreeIpa(String accountId, FreeIpaUpgradeRequest request) {
        validationService.validateEntitlement(accountId);
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(request.getEnvironmentCrn(), accountId);
        Set<InstanceMetaData> allInstances = stack.getNotDeletedInstanceMetaDataSet();
        validationService.validateStackForUpgrade(allInstances, stack);
        ImageInfoResponse currentImage = imageService.fetchCurrentImage(stack);
        ImageSettingsRequest imageSettingsRequest = assembleImageSettingsRequest(request, currentImage);
        ImageInfoResponse selectedImage = imageService.selectImage(stack, imageSettingsRequest);
        validationService.validateSelectedImageDifferentFromCurrent(currentImage, selectedImage);
        return triggerUpgrade(request, stack, allInstances, imageSettingsRequest, selectedImage, currentImage, accountId);
    }

    @SuppressWarnings("IllegalType")
    private HashSet<String> selectNonPgwInstanceIds(Set<InstanceMetaData> allInstances) {
        return instanceMetaDataService.getNonPrimaryGwInstances(allInstances).stream()
                .map(InstanceMetaData::getInstanceId)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private ImageSettingsRequest assembleImageSettingsRequest(FreeIpaUpgradeRequest request, ImageInfoResponse currentImage) {
        ImageSettingsRequest imageSettingsRequest = Optional.ofNullable(request.getImage()).orElseGet(ImageSettingsRequest::new);
        if (StringUtils.isBlank(imageSettingsRequest.getCatalog())) {
            imageSettingsRequest.setCatalog(Optional.ofNullable(currentImage.getCatalog()).orElse(currentImage.getCatalogName()));
        }
        return imageSettingsRequest;
    }

    @SuppressWarnings("IllegalType")
    private FreeIpaUpgradeResponse triggerUpgrade(FreeIpaUpgradeRequest request, Stack stack, Set<InstanceMetaData> allInstances,
            ImageSettingsRequest imageSettingsRequest, ImageInfoResponse selectedImage, ImageInfoResponse currentImage, String accountId) {
        String pgwInstanceId = instanceMetaDataService.getPrimaryGwInstance(allInstances).getInstanceId();
        HashSet<String> nonPgwInstanceIds = selectNonPgwInstanceIds(allInstances);
        Operation operation = startUpgradeOperation(stack.getAccountId(), request);
        String triggeredVariant = awsMigrationUtil.calculateUpgradeVariant(stack, accountId);
        boolean needMigration = awsMigrationUtil.isAwsVariantMigrationIsFeasible(stack, triggeredVariant);
        int defaultRootVolumeSize = rootVolumeSizeProvider.getForPlatform(stack.getCloudPlatform());
        List<VerticalScaleRequest> verticalScaleRequests = stack.getInstanceGroups().stream()
                .filter(ig -> ig.getTemplate().getRootVolumeSize() < defaultRootVolumeSize)
                .map(ig -> {
                    VerticalScaleRequest verticalScaleRequest = new VerticalScaleRequest();
                    verticalScaleRequest.setGroup(ig.getGroupName());
                    InstanceTemplateRequest templateRequest = new InstanceTemplateRequest();
                    VolumeRequest rootVolume = new VolumeRequest();
                    rootVolume.setSize(defaultRootVolumeSize);
                    templateRequest.setRootVolume(rootVolume);
                    verticalScaleRequest.setTemplate(templateRequest);
                    return verticalScaleRequest;
                }).collect(Collectors.toList());
        UpgradeEvent upgradeEvent = new UpgradeEvent(FlowChainTriggers.UPGRADE_TRIGGER_EVENT, stack.getId(), nonPgwInstanceIds, pgwInstanceId,
                operation.getOperationId(), imageSettingsRequest, Objects.nonNull(stack.getBackup()), needMigration, triggeredVariant,
                verticalScaleRequests.isEmpty() ? null : verticalScaleRequests.get(0));
        LOGGER.info("Trigger upgrade flow with event: {}", upgradeEvent);
        FlowIdentifier flowIdentifier = flowManager.notify(FlowChainTriggers.UPGRADE_TRIGGER_EVENT, upgradeEvent);
        return new FreeIpaUpgradeResponse(flowIdentifier, selectedImage, currentImage, operation.getOperationId());
    }

    private Operation startUpgradeOperation(String accountId, FreeIpaUpgradeRequest request) {
        Operation operation = operationService.startOperation(accountId, OperationType.UPGRADE, List.of(request.getEnvironmentCrn()), List.of());
        if (RUNNING != operation.getStatus()) {
            LOGGER.warn("Upgrade operation couldn't be started: {}", operation);
            throw new BadRequestException("Upgrade operation couldn't be started with: " + operation.getError());
        }
        return operation;
    }

    public FreeIpaUpgradeOptions collectUpgradeOptions(String accountId, String environmentCrn, String catalog) {
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(environmentCrn, accountId);
        ImageInfoResponse currentImage = imageService.fetchCurrentImage(stack);
        String catalogForRequest = Optional.ofNullable(catalog).or(() -> Optional.ofNullable(currentImage.getCatalog())).orElse(currentImage.getCatalogName());
        List<ImageInfoResponse> targetImages = getTargetImages(catalogForRequest, stack, currentImage);
        return createFreeIpaUpgradeOptions(targetImages, currentImage);
    }

    private FreeIpaUpgradeOptions createFreeIpaUpgradeOptions(List<ImageInfoResponse> targetImages, ImageInfoResponse currentImage) {
        FreeIpaUpgradeOptions freeIpaUpgradeOptions = new FreeIpaUpgradeOptions();
        freeIpaUpgradeOptions.setImages(targetImages);
        freeIpaUpgradeOptions.setCurrentImage(currentImage);
        return freeIpaUpgradeOptions;
    }

    private List<ImageInfoResponse> getTargetImages(String catalog, Stack stack, ImageInfoResponse currentImage) {
        ImageSettingsRequest imageSettingsRequest = new ImageSettingsRequest();
        imageSettingsRequest.setCatalog(catalog);
        LOGGER.debug("Using ImageSettingsRequest to query for possible target images: {}", imageSettingsRequest);
        List<ImageInfoResponse> targetImages = imageService.findTargetImages(stack, imageSettingsRequest, currentImage);
        LOGGER.debug("Found target images: {}", targetImages);
        return targetImages;
    }
}
