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
import com.sequenceiq.cloudbreak.sdx.cdl.CdlSdxService;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.sdx.common.polling.PollingResult;
import com.sequenceiq.cloudbreak.sdx.paas.PaasSdxService;

@ExtendWith(MockitoExtension.class)
public class PlatformAwareSdxConnectorTest {

    private static final String PAAS_CRN = "crn:cdp:datalake:us-west-1:tenant:datalake:crn1";

    private static final String SAAS_CRN = "crn:cdp:sdxsvc:us-west-1:tenant:instance:crn2";

    @Mock
    private CdlSdxService cdlSdxService;

    @Mock
    private PaasSdxService paasSdxService;

    @InjectMocks
    private PlatformAwareSdxConnector underTest;

    @BeforeEach
    public void setup() {
        Map<TargetPlatform, SdxService> map = Maps.newHashMap();
        map.put(TargetPlatform.CDL, cdlSdxService);
        map.put(TargetPlatform.PAAS, paasSdxService);
        FieldUtils.setProtectedFieldValue("platformDependentServiceMap", underTest, map);
    }

    @Test
    public void testSaasDelete() {
        doNothing().when(cdlSdxService).deleteSdx(any(), anyBoolean());
        underTest.delete(SAAS_CRN, false);
        verify(cdlSdxService).deleteSdx(anyString(), anyBoolean());
        verifyNoInteractions(paasSdxService);
    }

    @Test
    public void testPaasDelete() {
        doNothing().when(paasSdxService).deleteSdx(any(), anyBoolean());
        underTest.delete(PAAS_CRN, false);
        verify(paasSdxService).deleteSdx(anyString(), anyBoolean());
        verifyNoInteractions(cdlSdxService);
    }

    @Test
    public void testList() {
        when(paasSdxService.listSdxCrns(anyString())).thenReturn(Set.of(PAAS_CRN));
        when(cdlSdxService.listSdxCrns(anyString())).thenReturn(Set.of(SAAS_CRN));
        assertThrows(IllegalStateException.class, () -> underTest.listSdxCrns("envCrn"));

        when(paasSdxService.listSdxCrns(anyString())).thenReturn(Set.of());
        when(cdlSdxService.listSdxCrns(anyString())).thenReturn(Set.of(SAAS_CRN));
        assertTrue(underTest.listSdxCrns("envCrn").contains(SAAS_CRN));

        when(paasSdxService.listSdxCrns(anyString())).thenReturn(Set.of(PAAS_CRN));
        when(cdlSdxService.listSdxCrns(anyString())).thenReturn(Set.of());
        assertTrue(underTest.listSdxCrns("envCrn").contains(PAAS_CRN));
    }

    @Test
    public void testSaasGetAttemptResult() {
        when(cdlSdxService.getPollingResultForDeletion(anyString(), any())).thenReturn(Map.of(SAAS_CRN, PollingResult.IN_PROGRESS));
        underTest.getAttemptResultForDeletion("envCrn", Set.of(SAAS_CRN));
        verifyNoInteractions(paasSdxService);
        verify(cdlSdxService).getPollingResultForDeletion(anyString(), any());
    }

    @Test
    public void testPaasGetAttemptResult() {
        when(paasSdxService.getPollingResultForDeletion(anyString(), any())).thenReturn(Map.of(PAAS_CRN, PollingResult.IN_PROGRESS));
        underTest.getAttemptResultForDeletion("envCrn", Set.of(PAAS_CRN));
        verifyNoInteractions(cdlSdxService);
        verify(paasSdxService).getPollingResultForDeletion(anyString(), any());
    }

    @Test
    public void testBothGetAttemptResult() {
        assertThrows(IllegalStateException.class, () ->
                underTest.getAttemptResultForDeletion("envCrn", Set.of(PAAS_CRN, SAAS_CRN)));
        verifyNoInteractions(paasSdxService, cdlSdxService);
    }

    @Test
    public void testGetSdxCrnByEnvironmentCrnCDL() {
        when(paasSdxService.getSdxByEnvironmentCrn(anyString())).thenReturn(Optional.empty());
        when(cdlSdxService.getSdxByEnvironmentCrn(anyString())).thenReturn(
                Optional.of(new SdxBasicView(null, SAAS_CRN, null, null, false, 1L, null)));
        Optional<SdxBasicView> sdx = underTest.getSdxBasicViewByEnvironmentCrn("envCrn");
        assertEquals(SAAS_CRN, sdx.get().crn());
    }

    @Test
    public void testGetSdxCrnByEnvironmentCrnPaaS() {
        when(paasSdxService.getSdxByEnvironmentCrn(anyString())).thenReturn(
                Optional.of(new SdxBasicView(null, PAAS_CRN, null, null, false, 1L, null)));
        Optional<SdxBasicView> sdx = underTest.getSdxBasicViewByEnvironmentCrn("envCrn");
        assertEquals(PAAS_CRN, sdx.get().crn());
    }
}
