package com.sequenceiq.freeipa.service.upgrade;

import static com.sequenceiq.freeipa.api.v1.operation.model.OperationState.RUNNING;
import static com.sequenceiq.freeipa.service.validation.SeLinuxValidationService.SELINUX_SUPPORTED_TAG;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.common.model.SeLinux;
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
import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.chain.FlowChainTriggers;
import com.sequenceiq.freeipa.flow.freeipa.upgrade.UpgradeEvent;
import com.sequenceiq.freeipa.flow.stack.migration.handler.AwsMigrationUtil;
import com.sequenceiq.freeipa.service.DefaultRootVolumeSizeProvider;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.image.FreeIpaImageFilterSettings;
import com.sequenceiq.freeipa.service.image.FreeipaPlatformStringTransformer;
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

    @Inject
    private FreeipaPlatformStringTransformer platformStringTransformer;

    @SuppressWarnings("IllegalType")
    public FreeIpaUpgradeResponse upgradeFreeIpa(String accountId, FreeIpaUpgradeRequest request) {
        validationService.validateUpgradeRequest(request);
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(request.getEnvironmentCrn(), accountId);
        Set<InstanceMetaData> allInstances = stack.getNotDeletedInstanceMetaDataSet();
        validationService.validateStackForUpgrade(allInstances, stack);
        ImageInfoResponse currentImage = imageService.fetchCurrentImage(stack);
        ImageSettingsRequest imageSettingsRequest = assembleImageSettingsRequest(request, currentImage);
        FreeIpaImageFilterSettings freeIpaImageFilterSettings = createFreeIpaImageFilterSettings(stack, request, imageSettingsRequest, currentImage.getOs());
        ImageInfoResponse selectedImage = imageService.selectImage(freeIpaImageFilterSettings);
        HashSet<String> instancesOnOldImage = selectInstancesWithOldImage(allInstances, selectedImage);
        validationService.validateSelectedImageDifferentFromCurrent(currentImage, selectedImage, instancesOnOldImage);
        return triggerUpgrade(request, stack, allInstances, selectedImage, currentImage, accountId);
    }

    private FreeIpaImageFilterSettings createFreeIpaImageFilterSettings(Stack stack, FreeIpaUpgradeRequest request, ImageSettingsRequest imageSettingsRequest,
            String currentOs) {
        if (Optional.ofNullable(stack.getSecurityConfig())
                .map(SecurityConfig::getSeLinux)
                .orElse(SeLinux.PERMISSIVE) == SeLinux.ENFORCING) {
            return new FreeIpaImageFilterSettings(imageSettingsRequest.getId(), imageSettingsRequest.getCatalog(), currentOs, imageSettingsRequest.getOs(),
                    stack.getRegion(), platformStringTransformer.getPlatformString(stack), Boolean.TRUE.equals(request.getAllowMajorOsUpgrade()),
                    stack.getArchitecture(), Map.of(SELINUX_SUPPORTED_TAG, Boolean.TRUE.toString()));
        } else {
            return new FreeIpaImageFilterSettings(imageSettingsRequest.getId(), imageSettingsRequest.getCatalog(), currentOs, imageSettingsRequest.getOs(),
                    stack.getRegion(), platformStringTransformer.getPlatformString(stack), Boolean.TRUE.equals(request.getAllowMajorOsUpgrade()),
                    stack.getArchitecture());
        }
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
            imageSettingsRequest.setCatalog(Optional.ofNullable(currentImage.getCatalogName()).orElse(currentImage.getCatalog()));
        }
        return imageSettingsRequest;
    }

    @SuppressWarnings("IllegalType")
    private FreeIpaUpgradeResponse triggerUpgrade(FreeIpaUpgradeRequest request, Stack stack, Set<InstanceMetaData> allInstances,
            ImageInfoResponse selectedImage, ImageInfoResponse currentImage, String accountId) {
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
                }).toList();
        HashSet<String> instancesOnOldImage = selectInstancesWithOldImage(allInstances, selectedImage);
        ImageSettingsRequest imageSettingsRequest = new ImageSettingsRequest();
        imageSettingsRequest.setId(selectedImage.getId());
        imageSettingsRequest.setOs(selectedImage.getOs());
        imageSettingsRequest.setCatalog(Optional.ofNullable(selectedImage.getCatalog()).orElse(selectedImage.getCatalogName()));
        UpgradeEvent upgradeEvent = new UpgradeEvent(FlowChainTriggers.UPGRADE_TRIGGER_EVENT, stack.getId(), nonPgwInstanceIds, pgwInstanceId,
                operation.getOperationId(), imageSettingsRequest, Objects.nonNull(stack.getBackup()), needMigration, triggeredVariant,
                verticalScaleRequests.isEmpty() ? null : verticalScaleRequests.getFirst(), instancesOnOldImage);
        LOGGER.info("Trigger upgrade flow with event: {}", upgradeEvent);
        try {
            FlowIdentifier flowIdentifier = flowManager.notify(FlowChainTriggers.UPGRADE_TRIGGER_EVENT, upgradeEvent);
            return new FreeIpaUpgradeResponse(flowIdentifier, selectedImage, currentImage, operation.getOperationId());
        } catch (Exception e) {
            LOGGER.error("Couldn't start Freeipa upgrade flow", e);
            operationService.failOperation(accountId, operation.getOperationId(),
                    "Couldn't start Freeipa upgrade flow: " + e.getMessage());
            throw e;
        }
    }

    @SuppressWarnings("IllegalType")
    private HashSet<String> selectInstancesWithOldImage(Set<InstanceMetaData> allInstances, ImageInfoResponse imageInfoResponse) {
        LOGGER.debug("Instances for image check: {} and selected image: {}", allInstances, imageInfoResponse);
        HashSet<String> instancesWithOldImage = allInstances.stream().filter(im -> {
                    if (im.getImage() != null && StringUtils.isNotBlank(im.getImage().getValue())) {
                        Image image = im.getImage().getSilent(Image.class);
                        return !(Objects.equals(image.getImageId(), imageInfoResponse.getId())
                                && (Objects.equals(image.getImageCatalogName(), imageInfoResponse.getCatalog())
                                || Objects.equals(image.getImageCatalogUrl(), imageInfoResponse.getCatalog())));
                    } else {
                        return true;
                    }
                })
                .map(InstanceMetaData::getInstanceId)
                .collect(Collectors.toCollection(HashSet::new));
        LOGGER.info("Instances with outdated image or without image info: {}", instancesWithOldImage);
        return instancesWithOldImage;
    }

    private Operation startUpgradeOperation(String accountId, FreeIpaUpgradeRequest request) {
        Operation operation = operationService.startOperation(accountId, OperationType.UPGRADE, List.of(request.getEnvironmentCrn()), List.of());
        if (RUNNING != operation.getStatus()) {
            LOGGER.warn("Upgrade operation couldn't be started: {}", operation);
            throw new BadRequestException("Upgrade operation couldn't be started with: " + operation.getError());
        }
        return operation;
    }

    public FreeIpaUpgradeOptions collectUpgradeOptions(String accountId, String environmentCrn, String catalog, Boolean allowMajorOsUpgrade) {
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(environmentCrn, accountId);
        Map<String, String> tagFilters = new HashMap<>();
        if (SeLinux.ENFORCING.equals(stack.getSecurityConfig().getSeLinux())) {
            tagFilters.put(SELINUX_SUPPORTED_TAG, Boolean.TRUE.toString());
        }
        ImageInfoResponse currentImage = imageService.fetchCurrentImage(stack);
        String catalogForRequest = Optional.ofNullable(catalog).or(() -> Optional.ofNullable(currentImage.getCatalog())).orElse(currentImage.getCatalogName());
        List<ImageInfoResponse> targetImages = getTargetImages(catalogForRequest, stack, currentImage, allowMajorOsUpgrade, tagFilters);
        return createFreeIpaUpgradeOptions(targetImages, currentImage);
    }

    private FreeIpaUpgradeOptions createFreeIpaUpgradeOptions(List<ImageInfoResponse> targetImages, ImageInfoResponse currentImage) {
        FreeIpaUpgradeOptions freeIpaUpgradeOptions = new FreeIpaUpgradeOptions();
        freeIpaUpgradeOptions.setImages(targetImages);
        freeIpaUpgradeOptions.setCurrentImage(currentImage);
        return freeIpaUpgradeOptions;
    }

    private List<ImageInfoResponse> getTargetImages(String catalog, Stack stack, ImageInfoResponse currentImage,
            Boolean allowMajorOsUpgrade, Map<String, String> tagFilters) {
        LOGGER.debug("Using ImageSettingsRequest to query for possible target images: {}", catalog);
        List<ImageInfoResponse> targetImages = imageService.findTargetImages(stack, catalog, currentImage, allowMajorOsUpgrade, tagFilters);
        if (targetImages.isEmpty()) {
            Set<String> instancesWithOldImage = selectInstancesWithOldImage(stack.getNotDeletedInstanceMetaDataSet(), currentImage);
            LOGGER.debug("Target image is empty, if there is any instance on old image, return with current image");
            return instancesWithOldImage.isEmpty() ? List.of() : List.of(currentImage);
        } else {
            LOGGER.debug("Found target images: {}", targetImages);
            return targetImages;
        }
    }
}