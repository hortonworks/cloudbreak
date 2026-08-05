package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.freeipa.flow.stack.modify.tags.event.ModifyUserDefinedTagsStateSelectors.MODIFY_USER_DEFINED_TAGS_FREEIPA_START_EVENT;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.tag.UserDefinedTagValidator;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.converter.operation.OperationToOperationStatusConverter;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.modify.tags.event.ModifyUserDefinedTagsEvent;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.operation.OperationService;

@Service
public class FreeIpaModifyTagsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaModifyTagsService.class);

    @Inject
    private StackService stackService;

    @Inject
    private OperationService operationService;

    @Inject
    private FreeIpaFlowManager flowManager;

    @Inject
    private OperationToOperationStatusConverter operationConverter;

    @Inject
    private UserDefinedTagValidator userDefinedTagValidator;

    public OperationStatus startUserDefinedTagsModificationOperation(String environmentCrn, String accountId, Map<String, String> userDefinedTags) {
        LOGGER.info("Start 'MODIFY_USER_DEFINED_TAGS' operation");
        Stack stack = stackService.getFreeIpaStackWithMdcContext(environmentCrn, accountId);
        validateUserDefinedTagsAgainstDefaultTags(stack, userDefinedTags);
        Operation operation = operationService.startOperation(accountId, OperationType.MODIFY_USER_DEFINED_TAGS,
                Set.of(stack.getEnvironmentCrn()), Collections.emptySet());
        if (OperationState.RUNNING == operation.getStatus()) {
            operation = triggerUserDefinedTagsModification(stack, operation, userDefinedTags);
        } else {
            LOGGER.info("Operation is not in RUNNING state: {}", operation);
        }
        return operationConverter.convert(operation);
    }

    private void validateUserDefinedTagsAgainstDefaultTags(Stack stack, Map<String, String> userDefinedTags) {
        if (stack.getTags() == null) {
            return;
        }
        StackTags stackTags = stack.getTags().getUnchecked(StackTags.class);
        ValidationResult validationResult = userDefinedTagValidator.validateAgainstDefaultTags(userDefinedTags, stackTags.getDefaultTags());
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
    }

    private Operation triggerUserDefinedTagsModification(Stack stack, Operation operation, Map<String, String> userDefinedTags) {
        try {
            LOGGER.info("Starting modify user defined tags flow");
            String startEvent = MODIFY_USER_DEFINED_TAGS_FREEIPA_START_EVENT.event();
            ModifyUserDefinedTagsEvent modifyUserDefinedTagsEvent = new ModifyUserDefinedTagsEvent(startEvent, stack.getId(),
                    operation.getOperationId(), userDefinedTags);
            flowManager.notify(startEvent, modifyUserDefinedTagsEvent);
            LOGGER.info("Started modify user defined tags flow");
            return operation;
        } catch (Exception e) {
            LOGGER.error("Couldn't start modify FreeIPA user defined tags flow", e);
            return operationService.failOperation(stack.getAccountId(), operation.getOperationId(),
                    "Couldn't start modify FreeIPA user defined tags flow: " + e.getMessage());
        }
    }
}