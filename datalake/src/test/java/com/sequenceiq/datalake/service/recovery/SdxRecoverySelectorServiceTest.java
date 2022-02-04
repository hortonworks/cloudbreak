package com.sequenceiq.datalake.service.recovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recovery.RecoveryStatus;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.resize.recovery.ResizeRecoveryService;
import com.sequenceiq.datalake.service.upgrade.recovery.SdxUpgradeRecoveryService;
import com.sequenceiq.sdx.api.model.SdxRecoverableResponse;
import com.sequenceiq.sdx.api.model.SdxRecoveryRequest;
import com.sequenceiq.sdx.api.model.SdxRecoveryResponse;

@RunWith(MockitoJUnitRunner.class)
public class SdxRecoverySelectorServiceTest {

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

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        sdxRecoverySelectorService = new SdxRecoverySelectorService(List.of(mockResizeRecoveryService, mockSdxUpgradeRecoveryService));

    }

    public void setResizeTest() {
        lenient().when(mockSdxUpgradeRecoveryService.validateRecovery(cluster)).thenReturn(nonrecoverableResponse);
        lenient().when(mockResizeRecoveryService.validateRecovery(cluster)).thenReturn(recoverableResponse);
    }

    public void setUpgradeTest() {
        lenient().when(mockSdxUpgradeRecoveryService.validateRecovery(cluster)).thenReturn(recoverableResponse);
        lenient().when(mockResizeRecoveryService.validateRecovery(cluster)).thenReturn(nonrecoverableResponse);

    }

    @Test
    public void testRecoveryServiceCanRecoverFromUpgradeFailureByClusterName() {
        setUpgradeTest();

        SdxRecoveryRequest request = new SdxRecoveryRequest();
        SdxRecoveryResponse response = new SdxRecoveryResponse();
        when(mockSdxUpgradeRecoveryService.triggerRecovery(cluster, request)).thenReturn(response);
        SdxRecoveryResponse result = sdxRecoverySelectorService.triggerRecovery(cluster, request);

        // Basically, just check that we pass through
        Mockito.verify(mockSdxUpgradeRecoveryService).validateRecovery(cluster);
        Mockito.verify(mockSdxUpgradeRecoveryService).triggerRecovery(cluster, request);
        Mockito.verify(mockResizeRecoveryService, never()).triggerRecovery(cluster, request);
        assertNotNull(result);
        assertEquals(response, result);
    }

    @Test
    public void testRecoveryServiceCanValidateUpgradeFailureByClusterName() {
        setUpgradeTest();
        SdxRecoverableResponse result = sdxRecoverySelectorService.validateRecovery(cluster);

        // Basically, just check that we pass through
        Mockito.verify(mockSdxUpgradeRecoveryService).validateRecovery(cluster);

        assertNotNull(result);
        assertEquals(recoverableResponse, result);
    }

    @Test
    public void testRecoveryServiceCanSwitchToResizeRecovery() {
        setResizeTest();
        SdxRecoveryRequest request = new SdxRecoveryRequest();
        SdxRecoveryResponse response = new SdxRecoveryResponse();
        when(mockResizeRecoveryService.triggerRecovery(cluster, request)).thenReturn(response);

        SdxRecoveryResponse result = sdxRecoverySelectorService.triggerRecovery(cluster, request);

        // Basically, just check that we pass through to the Resize Recovery Service
        Mockito.verifyNoInteractions(mockSdxUpgradeRecoveryService);
        Mockito.verify(mockResizeRecoveryService).triggerRecovery(cluster, request);
        Mockito.verify(mockSdxUpgradeRecoveryService, never()).triggerRecovery(cluster, request);

        assertNotNull(result);
        assertEquals(response, result);
    }

    @Test
    public void testRecoveryServiceCanValidateResizeRecovery() {
        setResizeTest();

        SdxRecoverableResponse result = sdxRecoverySelectorService.validateRecovery(cluster);

        // Basically, just check that we pass through
        Mockito.verify(mockResizeRecoveryService).validateRecovery(cluster);
        Mockito.verify(mockSdxUpgradeRecoveryService, never()).validateRecovery(cluster);
        assertNotNull(result);
        assertEquals(recoverableResponse, result);
    }

    @Test
    public void testNoValidRecoveryValidation() {
        SdxRecoverableResponse nonrecoverableResponse2 = new SdxRecoverableResponse("Some non reason2", RecoveryStatus.NON_RECOVERABLE);

        when(mockResizeRecoveryService.validateRecovery(cluster)).thenReturn(nonrecoverableResponse);
        when(mockSdxUpgradeRecoveryService.validateRecovery(cluster)).thenReturn(nonrecoverableResponse2);

        SdxRecoverableResponse result = sdxRecoverySelectorService.validateRecovery(cluster);

        Mockito.verify(mockResizeRecoveryService).validateRecovery(cluster);
        Mockito.verify(mockSdxUpgradeRecoveryService).validateRecovery(cluster);

        assertNotNull(result);
        assertTrue(result.getStatus().nonRecoverable());
        //"Some non reason, Some non reason2" order independent
        assertEquals(33, result.getReason().length(), result.getReason());

    }

    @Test
    public void testNoValidRecoveryTrigger() {
        SdxRecoverableResponse nonrecoverableResponse2 = new SdxRecoverableResponse("Some non reason2", RecoveryStatus.NON_RECOVERABLE);

        when(mockResizeRecoveryService.validateRecovery(cluster)).thenReturn(nonrecoverableResponse);
        when(mockSdxUpgradeRecoveryService.validateRecovery(cluster)).thenReturn(nonrecoverableResponse2);
        SdxRecoveryRequest request = new SdxRecoveryRequest();

        BadRequestException result = assertThrows(BadRequestException.class, () -> sdxRecoverySelectorService.triggerRecovery(cluster, request));

        Mockito.verify(mockResizeRecoveryService).validateRecovery(cluster);
        Mockito.verify(mockSdxUpgradeRecoveryService).validateRecovery(cluster);
        Mockito.verify(mockSdxUpgradeRecoveryService, never()).triggerRecovery(cluster, request);
        Mockito.verify(mockSdxUpgradeRecoveryService, never()).triggerRecovery(cluster, request);

        assertNotNull(result);
        assertEquals(33, result.getMessage().length(), result.getMessage());

    }
}