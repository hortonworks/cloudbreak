package com.sequenceiq.freeipa.service.upgrade;

import static com.sequenceiq.freeipa.api.v1.operation.model.OperationState.RUNNING;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.FreeIpaPrepareUpgradeResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.FreeIpaUpgradeRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.ImageInfoResponse;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeEvent;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event.PrepareUpgradeTriggerEvent;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.operation.OperationService;

@Service
public class PrepareUpgradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrepareUpgradeService.class);

    @Inject
    private UpgradeService upgradeService;

    @Inject
    private OperationService operationService;

    @Inject
    private FreeIpaFlowManager flowManager;

    public FreeIpaPrepareUpgradeResponse prepareUpgrade(String accountId, FreeIpaUpgradeRequest request) {
        FreeIpaUpgradeValidationResult result = upgradeService.validateFreeIpaForUpgrade(accountId, request);
        Stack stack = result.stack();
        Operation operation = operationService.startOperation(accountId, OperationType.PREPARE_UPGRADE,
                List.of(stack.getEnvironmentCrn()), List.of());
        if (RUNNING != operation.getStatus()) {
            throw new BadRequestException("Prepare upgrade operation couldn't be started: " + operation.getError());
        }
        try {
            LOGGER.info("Starting prepare upgrade flow for stack {} in environment {}", stack.getId(), request.getEnvironmentCrn());
            ImageSettingsRequest targetImage = createTargetImageSettings(result.selectedImage());
            PrepareUpgradeTriggerEvent triggerEvent = new PrepareUpgradeTriggerEvent(
                    PrepareUpgradeEvent.PREPARE_UPGRADE_EVENT.event(), stack.getId(), operation.getOperationId(), targetImage);
            FlowIdentifier flowIdentifier = flowManager.notify(PrepareUpgradeEvent.PREPARE_UPGRADE_EVENT.event(), triggerEvent);
            return new FreeIpaPrepareUpgradeResponse(flowIdentifier, operation.getOperationId());
        } catch (Exception e) {
            LOGGER.error("Failed to start prepare upgrade flow", e);
            operationService.failOperation(accountId, operation.getOperationId(),
                    "Couldn't start prepare upgrade flow: " + e.getMessage());
            throw e;
        }
    }

    private ImageSettingsRequest createTargetImageSettings(ImageInfoResponse selectedImage) {
        ImageSettingsRequest imageSettingsRequest = new ImageSettingsRequest();
        imageSettingsRequest.setId(selectedImage.getId());
        imageSettingsRequest.setCatalog(Optional.ofNullable(selectedImage.getCatalogName()).orElse(selectedImage.getCatalog()));
        imageSettingsRequest.setOs(selectedImage.getOs());
        return imageSettingsRequest;
    }
}
