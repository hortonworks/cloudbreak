package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.freeipa.flow.stack.modify.tags.event.ModifyUserDefinedTagsStateSelectors.MODIFY_USER_DEFINED_TAGS_FREEIPA_START_EVENT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.converter.operation.OperationToOperationStatusConverter;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.modify.tags.event.ModifyUserDefinedTagsEvent;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.operation.OperationService;

@ExtendWith(MockitoExtension.class)
class FreeIpaModifyTagsServiceTest {
    @Mock
    private StackService stackService;

    @Mock
    private OperationService operationService;

    @Mock
    private Operation operation;

    @Mock
    private FreeIpaFlowManager flowManager;

    @Mock
    private FlowIdentifier flowIdentifier;

    @Mock
    private OperationToOperationStatusConverter operationConverter;

    @InjectMocks
    private FreeIpaModifyTagsService underTest;

    @Test
    void testModifyUserDefinedTags() {
        String operationId = "operationId";
        String accountId = "accountId";
        Stack stack = new Stack();
        String environmentCrn = "crn:cdp:environments:us-west-1:cloudera2:environment:f5e1a52e-54df-4f77-aa46-25252b879ecd";
        String resourceCrn = "crn:cdp:freeipa:us-west-1:cloudera2:freeipa:15f194a5-25e7-4ce6-8555-56ad8cbf9956";
        stack.setEnvironmentCrn(environmentCrn);
        stack.setResourceCrn(resourceCrn);

        Map<String, String> userDefinedTags = new HashMap<>(Map.of("custom", "value"));

        ModifyUserDefinedTagsEvent expectedEvent = new ModifyUserDefinedTagsEvent(MODIFY_USER_DEFINED_TAGS_FREEIPA_START_EVENT.event(), stack.getId(),
                operationId, userDefinedTags);

        when(stackService.getFreeIpaStackWithMdcContext(environmentCrn, accountId)).thenReturn(stack);
        when(operationService.startOperation(accountId, OperationType.MODIFY_USER_DEFINED_TAGS, Set.of(stack.getEnvironmentCrn()), Collections.emptySet()))
                .thenReturn(operation);
        when(operation.getOperationId()).thenReturn(operationId);
        when(operation.getStatus()).thenReturn(OperationState.RUNNING);

        underTest.startUserDefinedTagsModificationOperation(environmentCrn, accountId, userDefinedTags);
        verify(flowManager).notify(eq(MODIFY_USER_DEFINED_TAGS_FREEIPA_START_EVENT.event()), any(ModifyUserDefinedTagsEvent.class));
    }
}