package com.sequenceiq.datalake.service.recovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recovery.RecoveryStatus;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.resize.recovery.ResizeRecoveryService;
import com.sequenceiq.datalake.service.upgrade.recovery.SdxUpgradeRecoveryService;
import com.sequenceiq.sdx.api.model.SdxRecoverableResponse;
import com.sequenceiq.sdx.api.model.SdxRecoveryRequest;
import com.sequenceiq.sdx.api.model.SdxRecoveryResponse;

@ExtendWith(MockitoExtension.class)
class SdxRecoverySelectorServiceTest {

    private final SdxRecoverableResponse recoverableResponse = new SdxRecoverableResponse("Some reason", RecoveryStatus.RECOVERABLE);

    private final SdxRecoverableResponse nonrecoverableResponse = new SdxRecoverableResponse("Some non reason", RecoveryStatus.NON_RECOVERABLE);

    @Mock
    private SdxCluster cluster;

    @Mock
    private SdxUpgradeRecoveryService mockSdxUpgradeRecoveryService;

    @Mock
    private ResizeRecoveryService mockResizeRecoveryService;

    @InjectMocks
    private SdxRecoverySelectorService sdxRecoverySelectorService;

    private SdxRecoveryRequest request;

    @BeforeEach
    public void setUp() {
        sdxRecoverySelectorService = new SdxRecoverySelectorService(List.of(mockResizeRecoveryService, mockSdxUpgradeRecoveryService));
        request = new SdxRecoveryRequest();
    }

    public void setResizeTest() {
        lenient().when(mockSdxUpgradeRecoveryService.validateRecovery(cluster)).thenReturn(nonrecoverableResponse);
        lenient().when(mockResizeRecoveryService.validateRecovery(cluster)).thenReturn(recoverableResponse);
        lenient().when(mockSdxUpgradeRecoveryService.validateRecovery(cluster, request)).thenReturn(nonrecoverableResponse);
        lenient().when(mockResizeRecoveryService.validateRecovery(cluster, request)).thenReturn(recoverableResponse);
    }

    public void setUpgradeTest() {
        lenient().when(mockSdxUpgradeRecoveryService.validateRecovery(cluster)).thenReturn(recoverableResponse);
        lenient().when(mockResizeRecoveryService.validateRecovery(cluster)).thenReturn(nonrecoverableResponse);
        lenient().when(mockSdxUpgradeRecoveryService.validateRecovery(cluster, request)).thenReturn(recoverableResponse);
        lenient().when(mockResizeRecoveryService.validateRecovery(cluster, request)).thenReturn(nonrecoverableResponse);

    }

    @Test
    void testRecoveryServiceCanRecoverFromUpgradeFailureByClusterName() {
        setUpgradeTest();

        SdxRecoveryResponse response = new SdxRecoveryResponse();
        when(mockSdxUpgradeRecoveryService.triggerRecovery(cluster, request)).thenReturn(response);
        SdxRecoveryResponse result = sdxRecoverySelectorService.triggerRecovery(cluster, request);

        // Basically, just check that we pass through
        verify(mockSdxUpgradeRecoveryService).validateRecovery(cluster, request);
        verify(mockSdxUpgradeRecoveryService).triggerRecovery(cluster, request);
        verify(mockResizeRecoveryService, never()).triggerRecovery(cluster, request);
        assertNotNull(result);
        assertEquals(response, result);
    }

    @Test
    void testRecoveryServiceCanValidateUpgradeFailureByClusterName() {
        setUpgradeTest();
        SdxRecoverableResponse result = sdxRecoverySelectorService.validateRecovery(cluster);

        // Basically, just check that we pass through
        verify(mockSdxUpgradeRecoveryService).validateRecovery(cluster);

        assertNotNull(result);
        assertEquals(recoverableResponse, result);
    }

    @Test
    void testRecoveryServiceCanSwitchDespiteValidationError() {
        setUpgradeTest();
        when(mockResizeRecoveryService.validateRecovery(cluster, request)).thenAnswer(invocation -> {
            throw new Exception("Error!");
        });

        SdxRecoveryResponse response = new SdxRecoveryResponse();
        when(mockSdxUpgradeRecoveryService.triggerRecovery(cluster, request)).thenReturn(response);

        SdxRecoveryResponse result = sdxRecoverySelectorService.triggerRecovery(cluster, request);

        // Basically, just check that we pass through to the Upgrade Recovery Service despite error during resize recovery validation.
        verify(mockResizeRecoveryService, never()).triggerRecovery(cluster, request);
        verify(mockSdxUpgradeRecoveryService).triggerRecovery(cluster, request);

        assertNotNull(result);
        assertEquals(response, result);
    }

    @Test
    void testRecoveryServiceCanSwitchToResizeRecovery() {
        setResizeTest();

        SdxRecoveryResponse response = new SdxRecoveryResponse();
        when(mockResizeRecoveryService.triggerRecovery(cluster, request)).thenReturn(response);

        SdxRecoveryResponse result = sdxRecoverySelectorService.triggerRecovery(cluster, request);

        // Basically, just check that we pass through to the Resize Recovery Service
        verifyNoInteractions(mockSdxUpgradeRecoveryService);
        verify(mockResizeRecoveryService).triggerRecovery(cluster, request);
        verify(mockSdxUpgradeRecoveryService, never()).triggerRecovery(cluster, request);

        assertNotNull(result);
        assertEquals(response, result);
    }

    @Test
    void testRecoveryServiceCanValidateResizeRecovery() {
        setResizeTest();

        SdxRecoverableResponse result = sdxRecoverySelectorService.validateRecovery(cluster);

        // Basically, just check that we pass through
        verify(mockResizeRecoveryService).validateRecovery(cluster);
        verify(mockSdxUpgradeRecoveryService, never()).validateRecovery(cluster);
        assertNotNull(result);
        assertEquals(recoverableResponse, result);
    }

    @Test
    void testNoValidRecoveryValidation() {
        SdxRecoverableResponse nonrecoverableResponse2 = new SdxRecoverableResponse("Some non reason2", RecoveryStatus.NON_RECOVERABLE);

        when(mockResizeRecoveryService.validateRecovery(cluster)).thenReturn(nonrecoverableResponse);
        when(mockSdxUpgradeRecoveryService.validateRecovery(cluster)).thenReturn(nonrecoverableResponse2);

        SdxRecoverableResponse result = sdxRecoverySelectorService.validateRecovery(cluster);

        verify(mockResizeRecoveryService).validateRecovery(cluster);
        verify(mockSdxUpgradeRecoveryService).validateRecovery(cluster);

        assertNotNull(result);
        assertTrue(result.getStatus().nonRecoverable());
        //"Some non reason, Some non reason2" order independent
        assertEquals(33, result.getReason().length(), result.getReason());

    }

    @Test
    void testNoValidRecoveryTrigger() {
        SdxRecoverableResponse nonrecoverableResponse2 = new SdxRecoverableResponse("Some non reason2", RecoveryStatus.NON_RECOVERABLE);
        SdxRecoveryRequest request = new SdxRecoveryRequest();

        when(mockResizeRecoveryService.validateRecovery(cluster, request)).thenReturn(nonrecoverableResponse);
        when(mockSdxUpgradeRecoveryService.validateRecovery(cluster, request)).thenReturn(nonrecoverableResponse2);

        BadRequestException result = assertThrows(BadRequestException.class, () -> sdxRecoverySelectorService.triggerRecovery(cluster, request));

        verify(mockResizeRecoveryService).validateRecovery(cluster, request);
        verify(mockSdxUpgradeRecoveryService).validateRecovery(cluster, request);
        verify(mockSdxUpgradeRecoveryService, never()).triggerRecovery(cluster, request);
        verify(mockSdxUpgradeRecoveryService, never()).triggerRecovery(cluster, request);

        assertNotNull(result);
        assertEquals(33, result.getMessage().length(), result.getMessage());

    }
}