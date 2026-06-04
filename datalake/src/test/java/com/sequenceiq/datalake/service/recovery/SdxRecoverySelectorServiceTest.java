package com.sequenceiq.datalake.service.recovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    private RecoveryService secondaryRecoveryService;

    @Mock
    private ResizeRecoveryService mockResizeRecoveryService;

    @InjectMocks
    private SdxRecoverySelectorService sdxRecoverySelectorService;

    private SdxRecoveryRequest request;

    @BeforeEach
    public void setUp() {
        sdxRecoverySelectorService = new SdxRecoverySelectorService(List.of(mockResizeRecoveryService, secondaryRecoveryService));
        request = new SdxRecoveryRequest();
    }

    @Test
    void testRecoveryServiceCanRecoverWithSecondaryService() {
        when(mockResizeRecoveryService.validateRecovery(cluster, request)).thenReturn(nonrecoverableResponse);
        when(secondaryRecoveryService.validateRecovery(cluster, request)).thenReturn(recoverableResponse);

        SdxRecoveryResponse response = new SdxRecoveryResponse();
        when(secondaryRecoveryService.triggerRecovery(cluster, request)).thenReturn(response);
        SdxRecoveryResponse result = sdxRecoverySelectorService.triggerRecovery(cluster, request);

        verify(secondaryRecoveryService).validateRecovery(cluster, request);
        verify(secondaryRecoveryService).triggerRecovery(cluster, request);
        verify(mockResizeRecoveryService, never()).triggerRecovery(cluster, request);
        assertNotNull(result);
        assertEquals(response, result);
    }

    @Test
    void testRecoveryServiceCanValidateWithSecondaryService() {
        when(mockResizeRecoveryService.validateRecovery(cluster)).thenReturn(nonrecoverableResponse);
        when(secondaryRecoveryService.validateRecovery(cluster)).thenReturn(recoverableResponse);

        SdxRecoverableResponse result = sdxRecoverySelectorService.validateRecovery(cluster);

        verify(secondaryRecoveryService).validateRecovery(cluster);

        assertNotNull(result);
        assertEquals(recoverableResponse, result);
    }

    @Test
    void testRecoveryServiceCanSwitchDespiteValidationError() {
        when(mockResizeRecoveryService.validateRecovery(cluster, request)).thenThrow(new RuntimeException("Error!"));
        when(secondaryRecoveryService.validateRecovery(cluster, request)).thenReturn(recoverableResponse);

        SdxRecoveryResponse response = new SdxRecoveryResponse();
        when(secondaryRecoveryService.triggerRecovery(cluster, request)).thenReturn(response);

        SdxRecoveryResponse result = sdxRecoverySelectorService.triggerRecovery(cluster, request);

        verify(mockResizeRecoveryService, never()).triggerRecovery(cluster, request);
        verify(secondaryRecoveryService).triggerRecovery(cluster, request);

        assertNotNull(result);
        assertEquals(response, result);
    }

    @Test
    void testRecoveryServiceCanSwitchToResizeRecovery() {
        when(mockResizeRecoveryService.validateRecovery(cluster, request)).thenReturn(recoverableResponse);

        SdxRecoveryResponse response = new SdxRecoveryResponse();
        when(mockResizeRecoveryService.triggerRecovery(cluster, request)).thenReturn(response);

        SdxRecoveryResponse result = sdxRecoverySelectorService.triggerRecovery(cluster, request);

        verifyNoInteractions(secondaryRecoveryService);
        verify(mockResizeRecoveryService).triggerRecovery(cluster, request);

        assertNotNull(result);
        assertEquals(response, result);
    }

    @Test
    void testRecoveryServiceCanValidateResizeRecovery() {
        when(mockResizeRecoveryService.validateRecovery(cluster)).thenReturn(recoverableResponse);

        SdxRecoverableResponse result = sdxRecoverySelectorService.validateRecovery(cluster);

        verify(mockResizeRecoveryService).validateRecovery(cluster);
        verify(secondaryRecoveryService, never()).validateRecovery(cluster);
        assertNotNull(result);
        assertEquals(recoverableResponse, result);
    }

    @Test
    void testNoValidRecoveryValidation() {
        SdxRecoverableResponse nonrecoverableResponse2 = new SdxRecoverableResponse("Some non reason2", RecoveryStatus.NON_RECOVERABLE);

        when(mockResizeRecoveryService.validateRecovery(cluster)).thenReturn(nonrecoverableResponse);
        when(secondaryRecoveryService.validateRecovery(cluster)).thenReturn(nonrecoverableResponse2);

        SdxRecoverableResponse result = sdxRecoverySelectorService.validateRecovery(cluster);

        verify(mockResizeRecoveryService).validateRecovery(cluster);
        verify(secondaryRecoveryService).validateRecovery(cluster);

        assertNotNull(result);
        assertTrue(result.getStatus().nonRecoverable());
        assertEquals(33, result.getReason().length(), result.getReason());

    }

    @Test
    void testNoValidRecoveryTrigger() {
        SdxRecoverableResponse nonrecoverableResponse2 = new SdxRecoverableResponse("Some non reason2", RecoveryStatus.NON_RECOVERABLE);
        SdxRecoveryRequest request = new SdxRecoveryRequest();

        when(mockResizeRecoveryService.validateRecovery(cluster, request)).thenReturn(nonrecoverableResponse);
        when(secondaryRecoveryService.validateRecovery(cluster, request)).thenReturn(nonrecoverableResponse2);

        BadRequestException result = assertThrows(BadRequestException.class, () -> sdxRecoverySelectorService.triggerRecovery(cluster, request));

        verify(mockResizeRecoveryService).validateRecovery(cluster, request);
        verify(secondaryRecoveryService).validateRecovery(cluster, request);
        verify(secondaryRecoveryService, never()).triggerRecovery(cluster, request);

        assertNotNull(result);
        assertEquals(33, result.getMessage().length(), result.getMessage());

    }
}