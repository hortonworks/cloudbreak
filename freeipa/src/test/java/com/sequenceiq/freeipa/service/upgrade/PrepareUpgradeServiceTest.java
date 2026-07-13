package com.sequenceiq.freeipa.service.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.FreeIpaPrepareUpgradeResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.FreeIpaUpgradeRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.ImageInfoResponse;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeEvent;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event.PrepareUpgradeTriggerEvent;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.operation.OperationService;

@ExtendWith(MockitoExtension.class)
class PrepareUpgradeServiceTest {

    private static final String ACCOUNT_ID = "accountId";

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:accountId:environment:env-id";

    private static final String OPERATION_ID = "op-123";

    private static final Long STACK_ID = 1L;

    private static final String SELECTED_IMAGE_ID = "selected-image-uuid";

    private static final String SELECTED_IMAGE_CATALOG = "catalog-name";

    private static final String SELECTED_IMAGE_OS = "redhat8";

    @Mock
    private UpgradeService upgradeService;

    @Mock
    private OperationService operationService;

    @Mock
    private FreeIpaFlowManager flowManager;

    @InjectMocks
    private PrepareUpgradeService underTest;

    @Test
    void testPrepareUpgradeSuccess() {
        Stack stack = createAvailableStack();
        FreeIpaUpgradeRequest request = createRequest();
        Operation operation = createRunningOperation();
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, "flow-id");

        when(upgradeService.validateFreeIpaForUpgrade(ACCOUNT_ID, request)).thenReturn(createValidationResult(stack));
        when(operationService.startOperation(eq(ACCOUNT_ID), eq(OperationType.PREPARE_UPGRADE), eq(List.of(ENVIRONMENT_CRN)), eq(List.of())))
                .thenReturn(operation);
        when(flowManager.notify(eq(PrepareUpgradeEvent.PREPARE_UPGRADE_EVENT.event()), any(PrepareUpgradeTriggerEvent.class)))
                .thenReturn(flowIdentifier);

        FreeIpaPrepareUpgradeResponse result = underTest.prepareUpgrade(ACCOUNT_ID, request);

        assertEquals(flowIdentifier, result.getFlowIdentifier());
        assertEquals(OPERATION_ID, result.getOperationId());

        ArgumentCaptor<PrepareUpgradeTriggerEvent> triggerCaptor = ArgumentCaptor.forClass(PrepareUpgradeTriggerEvent.class);
        verify(flowManager).notify(eq(PrepareUpgradeEvent.PREPARE_UPGRADE_EVENT.event()), triggerCaptor.capture());
        PrepareUpgradeTriggerEvent triggerEvent = triggerCaptor.getValue();
        assertEquals(STACK_ID, triggerEvent.getResourceId());
        assertEquals(OPERATION_ID, triggerEvent.getOperationId());
        ImageSettingsRequest targetImage = triggerEvent.getImageSettingsRequest();
        assertEquals(SELECTED_IMAGE_ID, targetImage.getId());
        assertEquals(SELECTED_IMAGE_CATALOG, targetImage.getCatalog());
        assertEquals(SELECTED_IMAGE_OS, targetImage.getOs());
    }

    @Test
    void testPrepareUpgradeValidationFails() {
        FreeIpaUpgradeRequest request = createRequest();

        when(upgradeService.validateFreeIpaForUpgrade(ACCOUNT_ID, request))
                .thenThrow(new BadRequestException("FreeIPA is not in available state"));

        assertThrows(BadRequestException.class, () -> underTest.prepareUpgrade(ACCOUNT_ID, request));
        verifyNoInteractions(operationService, flowManager);
    }

    @Test
    void testPrepareUpgradeOperationNotRunning() {
        Stack stack = createAvailableStack();
        FreeIpaUpgradeRequest request = createRequest();
        Operation operation = createFailedOperation();

        when(upgradeService.validateFreeIpaForUpgrade(ACCOUNT_ID, request)).thenReturn(createValidationResult(stack));
        when(operationService.startOperation(eq(ACCOUNT_ID), eq(OperationType.PREPARE_UPGRADE), eq(List.of(ENVIRONMENT_CRN)), eq(List.of())))
                .thenReturn(operation);

        assertThrows(BadRequestException.class, () -> underTest.prepareUpgrade(ACCOUNT_ID, request));
        verifyNoInteractions(flowManager);
    }

    @Test
    void testPrepareUpgradeFlowNotifyThrows() {
        Stack stack = createAvailableStack();
        FreeIpaUpgradeRequest request = createRequest();
        Operation operation = createRunningOperation();

        when(upgradeService.validateFreeIpaForUpgrade(ACCOUNT_ID, request)).thenReturn(createValidationResult(stack));
        when(operationService.startOperation(eq(ACCOUNT_ID), eq(OperationType.PREPARE_UPGRADE), eq(List.of(ENVIRONMENT_CRN)), eq(List.of())))
                .thenReturn(operation);
        when(flowManager.notify(any(), any(PrepareUpgradeTriggerEvent.class)))
                .thenThrow(new RuntimeException("Flow trigger failed"));

        assertThrows(RuntimeException.class, () -> underTest.prepareUpgrade(ACCOUNT_ID, request));
        verify(operationService).failOperation(eq(ACCOUNT_ID), eq(OPERATION_ID), any());
    }

    private FreeIpaUpgradeRequest createRequest() {
        FreeIpaUpgradeRequest request = new FreeIpaUpgradeRequest();
        request.setEnvironmentCrn(ENVIRONMENT_CRN);
        return request;
    }

    private FreeIpaUpgradeValidationResult createValidationResult(Stack stack) {
        return new FreeIpaUpgradeValidationResult(stack, Set.of(new InstanceMetaData()), new ImageInfoResponse(), createSelectedImage());
    }

    private ImageInfoResponse createSelectedImage() {
        ImageInfoResponse selectedImage = new ImageInfoResponse();
        selectedImage.setId(SELECTED_IMAGE_ID);
        selectedImage.setCatalogName(SELECTED_IMAGE_CATALOG);
        selectedImage.setOs(SELECTED_IMAGE_OS);
        return selectedImage;
    }

    private Stack createAvailableStack() {
        return createStack(Status.AVAILABLE);
    }

    private Stack createStack(Status status) {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        stack.setAccountId(ACCOUNT_ID);
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(status);
        stack.setStackStatus(stackStatus);
        return stack;
    }

    private Operation createRunningOperation() {
        Operation operation = new Operation();
        operation.setOperationId(OPERATION_ID);
        operation.setStatus(OperationState.RUNNING);
        return operation;
    }

    private Operation createFailedOperation() {
        Operation operation = new Operation();
        operation.setOperationId(OPERATION_ID);
        operation.setStatus(OperationState.FAILED);
        operation.setError("Concurrent operation running");
        return operation;
    }
}
