package com.sequenceiq.datalake.service.recovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recovery.RecoveryStatus;
import com.sequenceiq.datalake.service.resize.recovery.ResizeRecoveryService;
import com.sequenceiq.datalake.service.upgrade.recovery.SdxUpgradeRecoveryService;
import com.sequenceiq.sdx.api.model.SdxRecoverableResponse;
import com.sequenceiq.sdx.api.model.SdxRecoveryRequest;
import com.sequenceiq.sdx.api.model.SdxRecoveryResponse;

@RunWith(MockitoJUnitRunner.class)
public class RecoveryServiceTest {

    public static final String USER_CRN = "userCrn";

    public static final String RESOURCE_CRN = "resourceCrn";

    public static final NameOrCrn NAME_OR_CRN = NameOrCrn.ofCrn(RESOURCE_CRN);

    @Mock
    private SdxUpgradeRecoveryService mockSdxUpgradeRecoveryService;

    @Mock
    private ResizeRecoveryService mockResizeRecoveryService;

    @InjectMocks
    private RecoveryService recoveryService;

    @Test
    public void testRecoveryServiceCanRecoverFromUpgradeFailureByClusterName() {
        SdxRecoveryRequest request = new SdxRecoveryRequest();
        SdxRecoveryResponse response = new SdxRecoveryResponse();
        when(mockSdxUpgradeRecoveryService.triggerRecovery(USER_CRN, NAME_OR_CRN, request)).thenReturn(response);

        SdxRecoveryResponse result = recoveryService.triggerRecovery(USER_CRN, NAME_OR_CRN, request);

        // Basically, just check that we pass through
        Mockito.verify(mockSdxUpgradeRecoveryService).triggerRecovery(USER_CRN, NAME_OR_CRN, request);
        assertNotNull(result);
        assertEquals(response, result);
    }

    @Test
    public void testRecoveryServiceCanValidateUpgradeFailureByClusterName() {
        SdxRecoverableResponse response = new SdxRecoverableResponse("Some reason", RecoveryStatus.RECOVERABLE);
        when(mockSdxUpgradeRecoveryService.validateRecovery(USER_CRN, NAME_OR_CRN)).thenReturn(response);

        SdxRecoverableResponse result = recoveryService.validateRecovery(USER_CRN, NAME_OR_CRN);

        // Basically, just check that we pass through
        Mockito.verify(mockSdxUpgradeRecoveryService).validateRecovery(USER_CRN, NAME_OR_CRN);
        assertNotNull(result);
        assertEquals(response, result);
    }

    @Test
    public void testRecoveryServiceCanSwitchToResizeRecovery() {
        SdxRecoveryRequest request = new SdxRecoveryRequest();
        SdxRecoveryResponse response = new SdxRecoveryResponse();
        when(mockResizeRecoveryService.triggerRecovery()).thenReturn(response);
        when(mockResizeRecoveryService.canRecover()).thenReturn(true);

        SdxRecoveryResponse result = recoveryService.triggerRecovery(USER_CRN, NAME_OR_CRN, request);

        // Basically, just check that we pass through to the Resize Recovery Service
        Mockito.verifyNoInteractions(mockSdxUpgradeRecoveryService);
        Mockito.verify(mockResizeRecoveryService).canRecover();
        Mockito.verify(mockResizeRecoveryService).triggerRecovery();
        assertNotNull(result);
        assertEquals(response, result);
    }

    @Test
    public void testRecoveryServiceCanValidateResizeRecovery() {
        SdxRecoverableResponse resizeRecoverableResponse = new SdxRecoverableResponse("Resize recovery is allowed.", RecoveryStatus.RECOVERABLE);
        when(mockResizeRecoveryService.validateRecovery()).thenReturn(resizeRecoverableResponse);
        when(mockResizeRecoveryService.canRecover()).thenReturn(true);

        SdxRecoverableResponse result = recoveryService.validateRecovery(USER_CRN, NAME_OR_CRN);

        // Basically, just check that we pass through
        Mockito.verify(mockResizeRecoveryService).validateRecovery();
        assertNotNull(result);
        assertEquals(resizeRecoverableResponse, result);
    }
}