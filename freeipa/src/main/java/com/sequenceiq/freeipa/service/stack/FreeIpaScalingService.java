package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.freeipa.api.v1.operation.model.OperationState.RUNNING;
import static com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIpaVerticalScaleEvent.STACK_VERTICALSCALE_EVENT;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.common.api.type.OrchestratorType;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityInfo;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.DownscaleRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.DownscaleResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.ScaleRequestBase;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.ScalingPath;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.UpscaleRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.UpscaleResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleResponse;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.chain.FlowChainTriggers;
import com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleEvent;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event.FreeIpaRollingVerticalScaleChainTriggerEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleEvent;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIpaVerticalScalingTriggerEvent;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.model.FreeIpaVerticalScaleParameters;
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

    @Inject
    private FreeipaDownscaleNodeCalculatorService freeipaDownscaleNodeCalculatorService;

    @Inject
    private EntitlementService entitlementService;

    public UpscaleResponse upscale(String accountId, UpscaleRequest request) {
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(request.getEnvironmentCrn(), accountId);
        Set<InstanceMetaData> allInstances = stack.getNotDeletedInstanceMetaDataSet();
        AvailabilityInfo availabilityInfo = new AvailabilityInfo(allInstances.size());
        logRequest(OperationType.UPSCALE, request, availabilityInfo);
        validationService.validateStackForUpscale(allInstances, stack,
                new ScalingPath(availabilityInfo.getAvailabilityType(), request.getTargetAvailabilityType()));
        return triggerUpscale(request, stack, availabilityInfo);
    }

    public VerticalScaleResponse verticalScale(String accountId, String environmentCrn, VerticalScaleRequest request) {
        boolean oneByOneRequested = request.getOrchestratorType() != null
                && OrchestratorType.ONE_BY_ONE == OrchestratorType.valueOf(request.getOrchestratorType().toUpperCase(Locale.ROOT));
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(environmentCrn, accountId);
        boolean stopped = Status.STOPPED.equals(stack.getStackStatus().getStatus());
        boolean entitled = !stopped && entitlementService.isFreeIpaRollingVerticalScaleEnabled(accountId);
        boolean cloudFormation = isAwsCloudFormation(stack);
        if (!stopped && oneByOneRequested && cloudFormation) {
            throw new BadRequestException(String.format(
                    "Rolling (ONE_BY_ONE) vertical scale is not supported for FreeIPA '%s' on AWS CloudFormation. "
                            + "To use the rolling path, migrate the deployment to AWS native first.",
                    stack.getName()));
        }
        boolean rolling = !stopped && !cloudFormation && (oneByOneRequested || entitled);
        LOGGER.info("FreeIPA vertical scale routing for stack={}: oneByOne={}, entitled={}, stopped={}, cloudFormation={}, rolling={}",
                stack.getResourceCrn(), oneByOneRequested, entitled, stopped, cloudFormation, rolling);
        if (rolling) {
            return rollingVerticalScale(accountId, entitled, stack, request);
        } else {
            validationService.validateStackForVerticalUpscale(stack, request);
            return triggerVerticalScale(request, stack);
        }
    }

    private boolean isAwsCloudFormation(Stack stack) {
        return CloudConstants.AWS.equals(stack.getCloudPlatform()) && CloudConstants.AWS.equals(stack.getPlatformvariant());
    }

    private VerticalScaleResponse rollingVerticalScale(String accountId, boolean entitled, Stack stack, VerticalScaleRequest request) {
        if (!entitled) {
            throw new BadRequestException("Rolling vertical scale for FreeIPA is not enabled for this account.");
        }
        validationService.validateStackForRollingVerticalScale(stack, request);
        Operation operation = startScalingOperation(accountId, stack.getEnvironmentCrn(), OperationType.VERTICAL_SCALE);
        try {
            FreeIpaVerticalScaleParameters scaleConfig = FreeIpaVerticalScaleParameters.fromRequest(request);
            FreeIpaRollingVerticalScaleChainTriggerEvent event = new FreeIpaRollingVerticalScaleChainTriggerEvent(
                    stack.getId(), scaleConfig, operation.getOperationId());
            LOGGER.info("Triggering rolling vertical scale chain for stack {}", stack.getId());
            FlowIdentifier flowIdentifier = flowManager.notify(FlowChainTriggers.FREEIPA_ROLLING_VERTICAL_SCALE_CHAIN_TRIGGER_EVENT, event);
            VerticalScaleResponse response = new VerticalScaleResponse();
            response.setOperationId(operation.getOperationId());
            response.setFlowIdentifier(flowIdentifier);
            return response;
        } catch (Exception e) {
            String exception = handleFlowException(operation, e, stack);
            throw new CloudbreakServiceException("Failed to trigger rolling vertical scale: " + exception, e);
        }
    }

    private void logRequest(OperationType operationType, ScaleRequestBase request, AvailabilityInfo availabilityType) {
        LOGGER.debug("{} request received with original availability type {} and request {}", operationType, availabilityType, request);
    }

    public DownscaleResponse downscale(String accountId, DownscaleRequest request) {
        LOGGER.debug("Freeipa downscale request: {}", request);
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(request.getEnvironmentCrn(), accountId);
        Set<InstanceMetaData> allInstances = stack.getNotTerminatedInstanceMetaDataSet();
        AvailabilityInfo originalAvailabilityInfo = new AvailabilityInfo(allInstances.size());
        AvailabilityType targetAvailabilityType = freeipaDownscaleNodeCalculatorService.calculateTargetAvailabilityType(request, allInstances.size());
        ScalingPath scalingPath = new ScalingPath(originalAvailabilityInfo.getAvailabilityType(), targetAvailabilityType);
        logRequest(OperationType.DOWNSCALE, request, originalAvailabilityInfo);
        validationService.validateStackForDownscale(allInstances, stack, scalingPath, request.getInstanceIds(), request.isForce());
        return triggerDownscale(request, stack, originalAvailabilityInfo, targetAvailabilityType, request.getInstanceIds());
    }

    private UpscaleResponse triggerUpscale(UpscaleRequest request, Stack stack, AvailabilityInfo originalAvailabilityType) {
        Operation operation = startScalingOperation(stack.getAccountId(), request.getEnvironmentCrn(), OperationType.UPSCALE);
        UpscaleEvent upscaleEvent = new UpscaleEvent(UpscaleFlowEvent.UPSCALE_EVENT.event(), stack.getId(), new ArrayList<>(),
                request.getTargetAvailabilityType().getInstanceCount(), false, false, false, operation.getOperationId(), null);
        try {
            FlowIdentifier flowIdentifier = flowManager.notify(UpscaleFlowEvent.UPSCALE_EVENT.event(), upscaleEvent);
            LOGGER.info("Trigger upscale flow with event: {} and flowId: {}", upscaleEvent, flowIdentifier);
            UpscaleResponse response = new UpscaleResponse();
            response.setOperationId(operation.getOperationId());
            response.setOriginalAvailabilityType(originalAvailabilityType.getAvailabilityType());
            response.setTargetAvailabilityType(request.getTargetAvailabilityType());
            response.setFlowIdentifier(flowIdentifier);
            return response;
        } catch (Exception e) {
            String exception = handleFlowException(operation, e, stack);
            throw new BadRequestException(exception);
        }
    }

    private VerticalScaleResponse triggerVerticalScale(VerticalScaleRequest request, Stack stack) {
        try {
            String selector = STACK_VERTICALSCALE_EVENT.event();
            FreeIpaVerticalScalingTriggerEvent event = new FreeIpaVerticalScalingTriggerEvent(selector, stack.getId(), request);
            LOGGER.info("Trigger vertical scale flow with event: {}", event);
            FlowIdentifier flowIdentifier = flowManager.notify(STACK_VERTICALSCALE_EVENT.event(), event);
            VerticalScaleResponse response = new VerticalScaleResponse();
            response.setRequest(request);
            response.setFlowIdentifier(flowIdentifier);
            return response;
        } catch (Exception e) {
            String exception = handleFlowException(e, stack);
            throw new BadRequestException(exception);
        }
    }

    private String handleFlowException(Operation operation, Exception e, Stack stack) {
        String message = String.format("Couldn't start %s flow (operation-id): %s",
                operation.getOperationType().name().toLowerCase(Locale.ROOT), e.getMessage());
        LOGGER.error(message, e);
        operationService.failOperation(stack.getAccountId(), operation.getOperationId(), message);
        return message;
    }

    private String handleFlowException(Exception e, Stack stack) {
        String message = String.format("Couldn't start operation on %s FreeIPA: %s", stack.getName(), e.getMessage());
        LOGGER.error(message, e);
        return message;
    }

    private DownscaleResponse triggerDownscale(DownscaleRequest request, Stack stack, AvailabilityInfo originalAvailabilityInfo,
            AvailabilityType targetAvailabilityType, Set<String> instanceIdsToDelete) {
        Operation operation = startScalingOperation(stack.getAccountId(), request.getEnvironmentCrn(), OperationType.DOWNSCALE);
        ArrayList<String> downscaleCandidates = freeipaDownscaleNodeCalculatorService
                .calculateDownscaleCandidates(stack, originalAvailabilityInfo, request.getTargetAvailabilityType(), instanceIdsToDelete);
        DownscaleEvent downscaleEvent = new DownscaleEvent(DownscaleFlowEvent.DOWNSCALE_EVENT.event(),
                stack.getId(), downscaleCandidates, targetAvailabilityType.getInstanceCount(), false, false, false, operation.getOperationId());
        try {
            LOGGER.info("Trigger downscale flow with event: {}", downscaleEvent);
            FlowIdentifier flowIdentifier = flowManager.notify(DownscaleFlowEvent.DOWNSCALE_EVENT.event(), downscaleEvent);
            DownscaleResponse response = new DownscaleResponse();
            response.setOperationId(operation.getOperationId());
            response.setOriginalAvailabilityType(originalAvailabilityInfo.getAvailabilityType());
            response.setTargetAvailabilityType(targetAvailabilityType);
            response.setDownscaleCandidates(new HashSet<>(downscaleCandidates));
            response.setFlowIdentifier(flowIdentifier);
            return response;
        } catch (Exception e) {
            String exception = handleFlowException(operation, e, stack);
            throw new BadRequestException(exception);
        }
    }

    private Operation startScalingOperation(String accountId, String envCrn, OperationType operationType) {
        Operation operation = operationService.startOperation(accountId, operationType, List.of(envCrn), List.of());
        if (RUNNING != operation.getStatus()) {
            LOGGER.warn("{} operation couldn't be started: {}", operationType.name(), operation);
            throw new BadRequestException(operationType.name().toLowerCase(Locale.ROOT) + " operation couldn't be started with: " + operation.getError());
        }
        return operation;
    }
}