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
import com.sequenceiq.datalake.service.upgrade.recovery.UpgradeRecoveryService;
import com.sequenceiq.sdx.api.model.SdxRecoverableResponse;
import com.sequenceiq.sdx.api.model.UpgradeRecoveryRequest;
import com.sequenceiq.sdx.api.model.SdxRecoveryResponse;

@RunWith(MockitoJUnitRunner.class)
public class SdxRecoveryServiceTest {

    public static final String USER_CRN = "userCrn";

    public static final String RESOURCE_CRN = "resourceCrn";

    public static final NameOrCrn NAME_OR_CRN = NameOrCrn.ofCrn(RESOURCE_CRN);

    @Mock
    private UpgradeRecoveryService mockUpgradeRecoveryService;

    @Mock
    private ResizeRecoveryService mockResizeRecoveryService;

    @InjectMocks
    private SdxRecoveryService sdxRecoveryService;

    @Test
    public void testRecoveryServiceCanRecoverFromUpgradeFailureByClusterName() {
        UpgradeRecoveryRequest request = new UpgradeRecoveryRequest();
        SdxRecoveryResponse response = new SdxRecoveryResponse();
        when(mockUpgradeRecoveryService.triggerRecovery(USER_CRN, NAME_OR_CRN, request)).thenReturn(response);

        SdxRecoveryResponse result = sdxRecoveryService.triggerRecovery(USER_CRN, NAME_OR_CRN, request);

        // Basically, just check that we pass through
        Mockito.verify(mockUpgradeRecoveryService).triggerRecovery(USER_CRN, NAME_OR_CRN, request);
        assertNotNull(result);
        assertEquals(response, result);
    }

    @Test
    public void testRecoveryServiceCanValidateUpgradeFailureByClusterName() {
        SdxRecoverableResponse response = new SdxRecoverableResponse("Some reason", RecoveryStatus.RECOVERABLE);
        when(mockUpgradeRecoveryService.validateRecovery(USER_CRN, NAME_OR_CRN)).thenReturn(response);

        SdxRecoverableResponse result = sdxRecoveryService.validateRecovery(USER_CRN, NAME_OR_CRN);

        // Basically, just check that we pass through
        Mockito.verify(mockUpgradeRecoveryService).validateRecovery(USER_CRN, NAME_OR_CRN);
        assertNotNull(result);
        assertEquals(response, result);
    }

    @Test
    public void testRecoveryServiceCanSwitchToResizeRecovery() {
        UpgradeRecoveryRequest request = new UpgradeRecoveryRequest();
        SdxRecoveryResponse response = new SdxRecoveryResponse();
        when(mockResizeRecoveryService.triggerRecovery(null, null, null)).thenReturn(response);
        when(mockResizeRecoveryService.canRecover()).thenReturn(true);

        SdxRecoveryResponse result = sdxRecoveryService.triggerRecovery(USER_CRN, NAME_OR_CRN, request);

        // Basically, just check that we pass through to the Resize Recovery Service
        Mockito.verifyNoInteractions(mockUpgradeRecoveryService);
        Mockito.verify(mockResizeRecoveryService).canRecover();
        Mockito.verify(mockResizeRecoveryService).triggerRecovery(null, null, null);
        assertNotNull(result);
        assertEquals(response, result);
    }

    @Test
    public void testRecoveryServiceCanValidateResizeRecovery() {
        SdxRecoverableResponse resizeRecoverableResponse = new SdxRecoverableResponse("Resize recovery is allowed.", RecoveryStatus.RECOVERABLE);
        when(mockResizeRecoveryService.validateRecovery(null, null)).thenReturn(resizeRecoverableResponse);
        when(mockResizeRecoveryService.canRecover()).thenReturn(true);

        SdxRecoverableResponse result = sdxRecoveryService.validateRecovery(USER_CRN, NAME_OR_CRN);

        // Basically, just check that we pass through
        Mockito.verify(mockResizeRecoveryService).validateRecovery(null, null);
        assertNotNull(result);
        assertEquals(resizeRecoverableResponse, result);
    }
}