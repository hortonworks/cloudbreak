package com.sequenceiq.freeipa.service.upgrade;

import static com.sequenceiq.freeipa.api.v1.operation.model.OperationState.RUNNING;
import static java.util.function.Predicate.not;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.FreeIpaUpgradeRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.FreeIpaUpgradeResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.ImageInfoResponse;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.chain.FlowChainTriggers;
import com.sequenceiq.freeipa.flow.freeipa.upgrade.UpgradeEvent;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackService;

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

    public FreeIpaUpgradeResponse upgradeFreeIpa(String accountId, FreeIpaUpgradeRequest request) {
        validationService.validateEntitlement(accountId);
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(request.getEnvironmentCrn(), accountId);
        MDCBuilder.buildMdcContext(stack);
        Set<InstanceMetaData> allInstances = stack.getNotDeletedInstanceMetaDataSet();
        validationService.validateStackForUpgrade(allInstances, stack);
        String pgwInstanceId = selectPrimaryGwInstanceId(allInstances);
        Set<String> nonPgwInstanceIds = collectNonPrimaryGwInstanceIds(allInstances, pgwInstanceId);
        ImageSettingsRequest imageSettingsRequest = Optional.ofNullable(request.getImage()).orElseGet(ImageSettingsRequest::new);
        ImageInfoResponse selectedImage = imageService.selectImage(stack, imageSettingsRequest);
        ImageInfoResponse currentImage = imageService.currentImage(stack);
        return triggerUpgrade(request, stack, pgwInstanceId, nonPgwInstanceIds, imageSettingsRequest, selectedImage, currentImage);
    }

    private FreeIpaUpgradeResponse triggerUpgrade(FreeIpaUpgradeRequest request, Stack stack, String pgwInstanceId,
            Set<String> nonPgwInstanceIds, ImageSettingsRequest imageSettingsRequest, ImageInfoResponse selectedImage, ImageInfoResponse currentImage) {
        Operation operation = startUpgradeOperation(stack.getAccountId(), request);
        UpgradeEvent upgradeEvent = new UpgradeEvent(FlowChainTriggers.UPGRADE_TRIGGER_EVENT, stack.getId(), nonPgwInstanceIds, pgwInstanceId,
                operation.getOperationId(), imageSettingsRequest, Objects.nonNull(stack.getBackup()));
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

    private Set<String> collectNonPrimaryGwInstanceIds(Set<InstanceMetaData> allInstances, String pgwInstanceId) {
        Set<String> nonPgwInstanceIds = allInstances.stream()
                .map(InstanceMetaData::getInstanceId)
                .filter(not(pgwInstanceId::equals))
                .collect(Collectors.toSet());
        LOGGER.debug("Non primary gateway instance IDs: {}", nonPgwInstanceIds);
        return nonPgwInstanceIds;
    }

    private String selectPrimaryGwInstanceId(Set<InstanceMetaData> allInstances) {
        String pgwInstanceId = allInstances.stream()
                .filter(instanceMetaData -> InstanceMetadataType.GATEWAY_PRIMARY == instanceMetaData.getInstanceMetadataType())
                .findFirst().orElseThrow(() -> new BadRequestException("No primary Gateway found")).getInstanceId();
        LOGGER.debug("Found primary gateway with instance id: [{}]", pgwInstanceId);
        return pgwInstanceId;
    }
}
