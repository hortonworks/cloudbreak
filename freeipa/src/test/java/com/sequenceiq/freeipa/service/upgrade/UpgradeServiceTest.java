package com.sequenceiq.freeipa.service.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.FreeIpaUpgradeRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.FreeIpaUpgradeResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.ImageInfoResponse;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.chain.FlowChainTriggers;
import com.sequenceiq.freeipa.flow.freeipa.upgrade.UpgradeEvent;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class UpgradeServiceTest {

    public static final String ACCOUNT_ID = "accId";

    public static final String ENVIRONMENT_CRN = "ENV_CRN";

    @Mock
    private OperationService operationService;

    @Mock
    private StackService stackService;

    @Mock
    private FreeIpaFlowManager flowManager;

    @Mock
    private UpgradeImageService imageService;

    @Mock
    private UpgradeValidationService validationService;

    @InjectMocks
    private UpgradeService underTest;

    @Test
    public void testUpgradeTriggered() {
        FreeIpaUpgradeRequest request = new FreeIpaUpgradeRequest();
        request.setImage(new ImageSettingsRequest());
        request.setEnvironmentCrn(ENVIRONMENT_CRN);

        Stack stack = mock(Stack.class);
        when(stackService.getByEnvironmentCrnAndAccountIdWithLists(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);
        Set<InstanceMetaData> allInstances = createValidImSet();
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(allInstances);
        ImageInfoResponse selectedImage = new ImageInfoResponse();
        when(imageService.selectImage(stack, request.getImage())).thenReturn(selectedImage);
        ImageInfoResponse currentImage = new ImageInfoResponse();
        when(imageService.currentImage(stack)).thenReturn(currentImage);
        Operation operation = new Operation();
        operation.setStatus(OperationState.RUNNING);
        operation.setOperationId("opId");
        when(operationService.startOperation(any(), any(), any(), any())).thenReturn(operation);
        ArgumentCaptor<Acceptable> eventCaptor = ArgumentCaptor.forClass(Acceptable.class);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW_CHAIN, "flowId");
        when(flowManager.notify(eq(FlowChainTriggers.UPGRADE_TRIGGER_EVENT), eventCaptor.capture())).thenReturn(flowIdentifier);

        FreeIpaUpgradeResponse response = underTest.upgradeFreeIpa(ACCOUNT_ID, request);

        assertEquals(flowIdentifier, response.getFlowIdentifier());
        assertEquals(operation.getOperationId(), response.getOperationId());
        assertEquals(currentImage, response.getOriginalImage());
        assertEquals(selectedImage, response.getTargetImage());

        UpgradeEvent upgradeEvent = (UpgradeEvent) eventCaptor.getValue();
        assertEquals(request.getImage(), upgradeEvent.getImageSettingsRequest());
        assertEquals(operation.getOperationId(), upgradeEvent.getOperationId());
        assertEquals("pgw", upgradeEvent.getPrimareGwInstanceId());
        assertEquals(2, upgradeEvent.getInstanceIds().size());
        assertTrue(Set.of("im2", "im3").containsAll(upgradeEvent.getInstanceIds()));

        verify(validationService).validateEntitlement(ACCOUNT_ID);
        verify(validationService).validateStackForUpgrade(allInstances, stack);
    }

    @Test
    public void testImageSettingsCreatedIfMissingAndUpgradeTriggered() {
        FreeIpaUpgradeRequest request = new FreeIpaUpgradeRequest();
        request.setEnvironmentCrn(ENVIRONMENT_CRN);

        Stack stack = mock(Stack.class);
        when(stackService.getByEnvironmentCrnAndAccountIdWithLists(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);
        Set<InstanceMetaData> allInstances = createValidImSet();
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(allInstances);
        ImageInfoResponse selectedImage = new ImageInfoResponse();
        when(imageService.selectImage(eq(stack), any(ImageSettingsRequest.class))).thenReturn(selectedImage);
        ImageInfoResponse currentImage = new ImageInfoResponse();
        when(imageService.currentImage(stack)).thenReturn(currentImage);
        Operation operation = new Operation();
        operation.setStatus(OperationState.RUNNING);
        operation.setOperationId("opId");
        when(operationService.startOperation(any(), any(), any(), any())).thenReturn(operation);
        ArgumentCaptor<Acceptable> eventCaptor = ArgumentCaptor.forClass(Acceptable.class);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW_CHAIN, "flowId");
        when(flowManager.notify(eq(FlowChainTriggers.UPGRADE_TRIGGER_EVENT), eventCaptor.capture())).thenReturn(flowIdentifier);

        FreeIpaUpgradeResponse response = underTest.upgradeFreeIpa(ACCOUNT_ID, request);

        assertEquals(flowIdentifier, response.getFlowIdentifier());
        assertEquals(operation.getOperationId(), response.getOperationId());
        assertEquals(currentImage, response.getOriginalImage());
        assertEquals(selectedImage, response.getTargetImage());

        UpgradeEvent upgradeEvent = (UpgradeEvent) eventCaptor.getValue();
        assertNotNull(upgradeEvent.getImageSettingsRequest());
        assertEquals(operation.getOperationId(), upgradeEvent.getOperationId());
        assertEquals("pgw", upgradeEvent.getPrimareGwInstanceId());
        assertEquals(2, upgradeEvent.getInstanceIds().size());
        assertTrue(Set.of("im2", "im3").containsAll(upgradeEvent.getInstanceIds()));

        verify(validationService).validateEntitlement(ACCOUNT_ID);
        verify(validationService).validateStackForUpgrade(allInstances, stack);
    }

    @Test
    public void testFailureWhenNoPrimaryGateway() {
        FreeIpaUpgradeRequest request = new FreeIpaUpgradeRequest();
        request.setImage(new ImageSettingsRequest());
        request.setEnvironmentCrn(ENVIRONMENT_CRN);

        Stack stack = mock(Stack.class);
        when(stackService.getByEnvironmentCrnAndAccountIdWithLists(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);
        Set<InstanceMetaData> allInstances = Set.of();
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(allInstances);

        assertThrows(BadRequestException.class, () -> underTest.upgradeFreeIpa(ACCOUNT_ID, request));

        verify(validationService).validateEntitlement(ACCOUNT_ID);
        verify(validationService).validateStackForUpgrade(allInstances, stack);
    }

    @Test
    public void testFailureIfOperationFailedToStart() {
        FreeIpaUpgradeRequest request = new FreeIpaUpgradeRequest();
        request.setImage(new ImageSettingsRequest());
        request.setEnvironmentCrn(ENVIRONMENT_CRN);

        Stack stack = mock(Stack.class);
        when(stackService.getByEnvironmentCrnAndAccountIdWithLists(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);
        Set<InstanceMetaData> allInstances = createValidImSet();
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(allInstances);
        ImageInfoResponse selectedImage = new ImageInfoResponse();
        when(imageService.selectImage(stack, request.getImage())).thenReturn(selectedImage);
        ImageInfoResponse currentImage = new ImageInfoResponse();
        when(imageService.currentImage(stack)).thenReturn(currentImage);
        Operation operation = new Operation();
        operation.setStatus(OperationState.REJECTED);
        operation.setOperationId("opId");
        when(operationService.startOperation(any(), any(), any(), any())).thenReturn(operation);

        assertThrows(BadRequestException.class, () -> underTest.upgradeFreeIpa(ACCOUNT_ID, request));

        verify(validationService).validateEntitlement(ACCOUNT_ID);
        verify(validationService).validateStackForUpgrade(allInstances, stack);
    }

    private Set<InstanceMetaData> createValidImSet() {
        InstanceMetaData im1 = new InstanceMetaData();
        im1.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        im1.setInstanceId("pgw");
        InstanceMetaData im2 = new InstanceMetaData();
        im2.setInstanceMetadataType(InstanceMetadataType.GATEWAY);
        im2.setInstanceId("im2");
        InstanceMetaData im3 = new InstanceMetaData();
        im3.setInstanceMetadataType(InstanceMetadataType.GATEWAY);
        im3.setInstanceId("im3");
        return Set.of(im1, im2, im3);
    }
}