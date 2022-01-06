package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.freeipa.api.v1.operation.model.OperationState.RUNNING;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.client.util.Lists;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.FormFactor;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.DownscaleRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.DownscaleResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.UpscaleRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.UpscaleResponse;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleEvent;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;
import com.sequenceiq.freeipa.service.upgrade.UpgradeImageService;

@Service
public class FreeIpaScalingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaScalingService.class);

    @Inject
    private OperationService operationService;

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaFlowManager flowManager;

    @Inject
    private UpgradeImageService imageService;

    @Inject
    private FreeIpaScalingValidationService validationService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    public UpscaleResponse upscale(String accountId, UpscaleRequest request) {
        Stack stack = getStack(accountId, request.getEnvironmentCrn());
        Set<InstanceMetaData> allInstances = stack.getNotDeletedInstanceMetaDataSet();
        FormFactor originalFormFactor = FormFactor.getByInstanceCount(allInstances.size());

        validationService.validateStackForUpscale(allInstances, stack);
        return triggerUpscale(request, stack, originalFormFactor);
    }

    public DownscaleResponse downscale(String accountId, DownscaleRequest request) {
        Stack stack = getStack(accountId, request.getEnvironmentCrn());
        Set<InstanceMetaData> allInstances = stack.getNotDeletedInstanceMetaDataSet();
        FormFactor originalFormFactor = FormFactor.getByInstanceCount(allInstances.size());
//        HashSet<String> nonPgwInstanceIds = instanceMetaDataService.getNonPrimaryGwInstances(allInstances);
        validationService.validateStackForDownscale(allInstances, stack);
        return triggerDownscale(request, stack, originalFormFactor);
    }

    private Stack getStack(String accountId, String envCrn) {
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(envCrn, accountId);
        MDCBuilder.buildMdcContext(stack);
        return stack;
    }

    private UpscaleResponse triggerUpscale(UpscaleRequest request, Stack stack, FormFactor originalFormFactor) {
        Operation operation = startScalingOperation(stack.getAccountId(), request.getEnvironmentCrn(), OperationType.UPSCALE);
        UpscaleEvent upscaleEvent = new UpscaleEvent(UpscaleFlowEvent.UPSCALE_EVENT.event(),
                stack.getId(), request.getTargetFormFactor().getInstanceCount(), false, false, false, operation.getOperationId());
        LOGGER.info("Trigger upscale flow with event: {}", upscaleEvent);
        FlowIdentifier flowIdentifier = flowManager.notify(UpscaleFlowEvent.UPSCALE_EVENT.event(), upscaleEvent);
        UpscaleResponse response = new UpscaleResponse();
        response.setOperationId(operation.getOperationId());
        response.setOriginalFormFactor(originalFormFactor);
        response.setTargetFormFactor(request.getTargetFormFactor());
        response.setFlowIdentifier(flowIdentifier);
        return response;
    }

    private DownscaleResponse triggerDownscale(DownscaleRequest request, Stack stack, FormFactor originalFormFactor) {
        Operation operation = startScalingOperation(stack.getAccountId(), request.getEnvironmentCrn(), OperationType.DOWNSCALE);
        DownscaleEvent downscaleEvent = new DownscaleEvent(DownscaleFlowEvent.DOWNSCALE_EVENT.event(),
                stack.getId(), Lists.newArrayList(), request.getTargetFormFactor().getInstanceCount(), false, false, false, operation.getOperationId());
        LOGGER.info("Trigger downscale flow with event: {}", downscaleEvent);
        FlowIdentifier flowIdentifier = flowManager.notify(DownscaleFlowEvent.DOWNSCALE_EVENT.event(), downscaleEvent);
        DownscaleResponse response = new DownscaleResponse();
        response.setOperationId(operation.getOperationId());
        response.setOriginalFormFactor(originalFormFactor);
        response.setTargetFormFactor(request.getTargetFormFactor());
        response.setFlowIdentifier(flowIdentifier);
        return response;
    }

    private Operation startScalingOperation(String accountId, String envCrn, OperationType operationType) {
        Operation operation = operationService.startOperation(accountId, operationType, List.of(envCrn), List.of());
        if (RUNNING != operation.getStatus()) {
            LOGGER.warn("{} operation couldn't be started: {}", operationType.name(), operation);
            throw new BadRequestException(operationType.name().toLowerCase() + " operation couldn't be started with: " + operation.getError());
        }
        return operation;
    }
}
