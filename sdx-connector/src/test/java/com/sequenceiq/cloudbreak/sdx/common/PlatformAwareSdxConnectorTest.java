package com.sequenceiq.cloudbreak.sdx.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.util.FieldUtils;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.sdx.TargetPlatform;
import com.sequenceiq.cloudbreak.sdx.cdl.service.CdlSdxDeleteService;
import com.sequenceiq.cloudbreak.sdx.cdl.service.CdlSdxDescribeService;
import com.sequenceiq.cloudbreak.sdx.cdl.service.CdlSdxStatusService;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.sdx.common.polling.PollingResult;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxDeleteService;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxDescribeService;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxStatusService;
import com.sequenceiq.cloudbreak.sdx.paas.service.PaasSdxDeleteService;
import com.sequenceiq.cloudbreak.sdx.paas.service.PaasSdxDescribeService;
import com.sequenceiq.cloudbreak.sdx.paas.service.PaasSdxStatusService;

@ExtendWith(MockitoExtension.class)
public class PlatformAwareSdxConnectorTest {

    private static final String PAAS_CRN = "crn:cdp:datalake:us-west-1:tenant:datalake:crn1";

    private static final String SAAS_CRN = "crn:cdp:sdxsvc:us-west-1:tenant:instance:crn2";

    @Mock
    private CdlSdxStatusService cdlSdxStatusService;

    @Mock
    private PaasSdxStatusService paasSdxStatusService;

    @Mock
    private CdlSdxDescribeService cdlSdxDescribeService;

    @Mock
    private PaasSdxDescribeService paasSdxDescribeService;

    @Mock
    private CdlSdxDeleteService cdlSdxDeleteService;

    @Mock
    private PaasSdxDeleteService paasSdxDeleteService;

    @InjectMocks
    private PlatformAwareSdxConnector underTest;

    @BeforeEach
    public void setup() {
        Map<TargetPlatform, PlatformAwareSdxStatusService<?>> status = Maps.newHashMap();
        status.put(TargetPlatform.CDL, cdlSdxStatusService);
        status.put(TargetPlatform.PAAS, paasSdxStatusService);
        Map<TargetPlatform, PlatformAwareSdxDescribeService> describe = Maps.newHashMap();
        describe.put(TargetPlatform.CDL, cdlSdxDescribeService);
        describe.put(TargetPlatform.PAAS, paasSdxDescribeService);
        Map<TargetPlatform, PlatformAwareSdxDeleteService<?>> delete = Maps.newHashMap();
        delete.put(TargetPlatform.CDL, cdlSdxDeleteService);
        delete.put(TargetPlatform.PAAS, paasSdxDeleteService);
        FieldUtils.setProtectedFieldValue("platformDependentSdxStatusServicesMap", underTest, status);
        FieldUtils.setProtectedFieldValue("platformDependentSdxDescribeServices", underTest, describe);
        FieldUtils.setProtectedFieldValue("platformDependentSdxDeleteServices", underTest, delete);
    }

    @Test
    public void testSaasDelete() {
        doNothing().when(cdlSdxDeleteService).deleteSdx(any(), anyBoolean());
        underTest.delete(SAAS_CRN, false);
        verify(cdlSdxDeleteService).deleteSdx(anyString(), anyBoolean());
        verifyNoInteractions(paasSdxDeleteService);
    }

    @Test
    public void testPaasDelete() {
        doNothing().when(paasSdxDeleteService).deleteSdx(any(), anyBoolean());
        underTest.delete(PAAS_CRN, false);
        verify(paasSdxDeleteService).deleteSdx(anyString(), anyBoolean());
        verifyNoInteractions(cdlSdxDeleteService);
    }

    @Test
    public void testList() {
        when(paasSdxDescribeService.listSdxCrns(anyString())).thenReturn(Set.of(PAAS_CRN));
        when(cdlSdxDescribeService.listSdxCrns(anyString())).thenReturn(Set.of(SAAS_CRN));
        assertThrows(IllegalStateException.class, () -> underTest.listSdxCrns("envCrn"));

        when(paasSdxDescribeService.listSdxCrns(anyString())).thenReturn(Set.of());
        when(cdlSdxDescribeService.listSdxCrns(anyString())).thenReturn(Set.of(SAAS_CRN));
        assertTrue(underTest.listSdxCrns("envCrn").contains(SAAS_CRN));

        when(paasSdxDescribeService.listSdxCrns(anyString())).thenReturn(Set.of(PAAS_CRN));
        when(cdlSdxDescribeService.listSdxCrns(anyString())).thenReturn(Set.of());
        assertTrue(underTest.listSdxCrns("envCrn").contains(PAAS_CRN));
    }

    @Test
    public void testSaasGetAttemptResult() {
        when(cdlSdxDeleteService.getPollingResultForDeletion(anyString(), any())).thenReturn(Map.of(SAAS_CRN, PollingResult.IN_PROGRESS));
        underTest.getAttemptResultForDeletion("envCrn", Set.of(SAAS_CRN));
        verifyNoInteractions(paasSdxDeleteService);
        verify(cdlSdxDeleteService).getPollingResultForDeletion(anyString(), any());
    }

    @Test
    public void testPaasGetAttemptResult() {
        when(paasSdxDeleteService.getPollingResultForDeletion(anyString(), any())).thenReturn(Map.of(PAAS_CRN, PollingResult.IN_PROGRESS));
        underTest.getAttemptResultForDeletion("envCrn", Set.of(PAAS_CRN));
        verifyNoInteractions(cdlSdxDeleteService);
        verify(paasSdxDeleteService).getPollingResultForDeletion(anyString(), any());
    }

    @Test
    public void testBothGetAttemptResult() {
        assertThrows(IllegalStateException.class, () ->
                underTest.getAttemptResultForDeletion("envCrn", Set.of(PAAS_CRN, SAAS_CRN)));
        verifyNoInteractions(paasSdxDeleteService, paasSdxDeleteService);
    }

    @Test
    public void testGetSdxCrnByEnvironmentCrnCDL() {
        when(paasSdxDescribeService.getSdxByEnvironmentCrn(anyString())).thenReturn(Optional.empty());
        when(cdlSdxDescribeService.getSdxByEnvironmentCrn(anyString())).thenReturn(
                Optional.of(new SdxBasicView(null, SAAS_CRN, null, null, false, 1L, null)));
        Optional<SdxBasicView> sdx = underTest.getSdxBasicViewByEnvironmentCrn("envCrn");
        assertEquals(SAAS_CRN, sdx.get().crn());
    }

    @Test
    public void testGetSdxCrnByEnvironmentCrnPaaS() {
        when(paasSdxDescribeService.getSdxByEnvironmentCrn(anyString())).thenReturn(
                Optional.of(new SdxBasicView(null, PAAS_CRN, null, null, false, 1L, null)));
        Optional<SdxBasicView> sdx = underTest.getSdxBasicViewByEnvironmentCrn("envCrn");
        assertEquals(PAAS_CRN, sdx.get().crn());
    }
}
