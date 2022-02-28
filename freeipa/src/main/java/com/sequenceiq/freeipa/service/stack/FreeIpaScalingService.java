package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.freeipa.api.v1.operation.model.OperationState.RUNNING;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.FormFactor;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.DownscaleRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.DownscaleResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.ScaleRequestBase;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.ScalingPath;
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
    private FreeIpaScalingValidationService validationService;

    public UpscaleResponse upscale(String accountId, UpscaleRequest request) {
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(request.getEnvironmentCrn(), accountId);
        Set<InstanceMetaData> allInstances = stack.getNotDeletedInstanceMetaDataSet();
        FormFactor originalFormFactor = FormFactor.getByInstanceCount(allInstances.size());
        logRequest(OperationType.UPSCALE, request, originalFormFactor);
        validationService.validateStackForUpscale(allInstances, stack, new ScalingPath(originalFormFactor, request.getTargetFormFactor()));
        return triggerUpscale(request, stack, originalFormFactor);
    }

    private void logRequest(OperationType operationType, ScaleRequestBase request, FormFactor originalFormFactor) {
        LOGGER.debug("{} request received with original form factor {} and request {}", operationType, originalFormFactor, request);
    }

    public DownscaleResponse downscale(String accountId, DownscaleRequest request) {
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(request.getEnvironmentCrn(), accountId);
        Set<InstanceMetaData> allInstances = stack.getNotDeletedInstanceMetaDataSet();
        FormFactor originalFormFactor = FormFactor.getByInstanceCount(allInstances.size());
        logRequest(OperationType.DOWNSCALE, request, originalFormFactor);
        validationService.validateStackForDownscale(allInstances, stack, new ScalingPath(originalFormFactor, request.getTargetFormFactor()));
        return triggerDownscale(request, stack, originalFormFactor);
    }

    private UpscaleResponse triggerUpscale(UpscaleRequest request, Stack stack, FormFactor originalFormFactor) {
        Operation operation = startScalingOperation(stack.getAccountId(), request.getEnvironmentCrn(), OperationType.UPSCALE);
        UpscaleEvent upscaleEvent = new UpscaleEvent(UpscaleFlowEvent.UPSCALE_EVENT.event(),
                stack.getId(), request.getTargetFormFactor().getInstanceCount(), false, false, false, operation.getOperationId());
        try {
            LOGGER.info("Trigger upscale flow with event: {}", upscaleEvent);
            FlowIdentifier flowIdentifier = flowManager.notify(UpscaleFlowEvent.UPSCALE_EVENT.event(), upscaleEvent);
            UpscaleResponse response = new UpscaleResponse();
            response.setOperationId(operation.getOperationId());
            response.setOriginalFormFactor(originalFormFactor);
            response.setTargetFormFactor(request.getTargetFormFactor());
            response.setFlowIdentifier(flowIdentifier);
            return response;
        } catch (Exception e) {
            String exception = handleFlowException(operation, e, stack);
            throw new BadRequestException(exception);
        }
    }

    private String handleFlowException(Operation operation, Exception e, Stack stack) {
        String message = String.format("Couldn't start %s flow: %s", operation.getOperationType().name().toLowerCase(), e.getMessage());
        LOGGER.error(message, e);
        operationService.failOperation(stack.getAccountId(), operation.getOperationId(), message);
        return message;
    }

    private DownscaleResponse triggerDownscale(DownscaleRequest request, Stack stack, FormFactor originalFormFactor) {
        Operation operation = startScalingOperation(stack.getAccountId(), request.getEnvironmentCrn(), OperationType.DOWNSCALE);
        ArrayList<String> instanceIdList = getDownscaleCandidates(stack, originalFormFactor, request.getTargetFormFactor());
        DownscaleEvent downscaleEvent = new DownscaleEvent(DownscaleFlowEvent.DOWNSCALE_EVENT.event(),
                stack.getId(), instanceIdList, request.getTargetFormFactor().getInstanceCount(), false, false, false, operation.getOperationId());
        try {
            LOGGER.info("Trigger downscale flow with event: {}", downscaleEvent);
            FlowIdentifier flowIdentifier = flowManager.notify(DownscaleFlowEvent.DOWNSCALE_EVENT.event(), downscaleEvent);
            DownscaleResponse response = new DownscaleResponse();
            response.setOperationId(operation.getOperationId());
            response.setOriginalFormFactor(originalFormFactor);
            response.setTargetFormFactor(request.getTargetFormFactor());
            response.setFlowIdentifier(flowIdentifier);
            return response;
        } catch (Exception e) {
            String exception = handleFlowException(operation, e, stack);
            throw new BadRequestException(exception);
        }
    }

    private ArrayList<String> getDownscaleCandidates(Stack stack, FormFactor originalFormFactor, FormFactor targetFormFactor) {
        int instancesToRemove = originalFormFactor.getInstanceCount() - targetFormFactor.getInstanceCount();
        return stack.getNotDeletedInstanceMetaDataSet().stream()
                .filter(imd -> imd.getInstanceMetadataType() != InstanceMetadataType.GATEWAY_PRIMARY)
                .limit(instancesToRemove)
                .map(InstanceMetaData::getInstanceId)
                .collect(Collectors.toCollection(ArrayList::new));
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
