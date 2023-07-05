package com.sequenceiq.cloudbreak.saas.sdx;

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
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.util.FieldUtils;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.saas.sdx.polling.PollingResult;
import com.sequenceiq.cloudbreak.sdx.TargetPlatform;

@ExtendWith(MockitoExtension.class)
public class PlatformAwareSdxConnectorTest {

    private static final String PAAS_CRN = "crn:cdp:datalake:us-west-1:tenant:datalake:crn1";

    private static final String SAAS_CRN = "crn:cdp:sdxsvc:us-west-1:tenant:instance:crn2";

    @Mock
    private SaasSdxService saasSdxService;

    @Mock
    private PaasSdxService paasSdxService;

    @InjectMocks
    private PlatformAwareSdxConnector underTest;

    @BeforeEach
    public void setup() {
        Map<TargetPlatform, SdxService> map = Maps.newHashMap();
        map.put(TargetPlatform.SAAS, saasSdxService);
        map.put(TargetPlatform.PAAS, paasSdxService);
        FieldUtils.setProtectedFieldValue("platformDependentServiceMap", underTest, map);
    }

    @Test
    public void testSaasDelete() {
        doNothing().when(saasSdxService).deleteSdx(any(), anyBoolean());
        underTest.delete(SAAS_CRN, false);
        verify(saasSdxService).deleteSdx(anyString(), anyBoolean());
        verifyNoInteractions(paasSdxService);
    }

    @Test
    public void testPaasDelete() {
        doNothing().when(paasSdxService).deleteSdx(any(), anyBoolean());
        underTest.delete(PAAS_CRN, false);
        verify(paasSdxService).deleteSdx(anyString(), anyBoolean());
        verifyNoInteractions(saasSdxService);
    }

    @Test
    public void testList() {
        when(paasSdxService.listSdxCrns(anyString(), anyString())).thenReturn(Set.of(PAAS_CRN));
        when(saasSdxService.listSdxCrns(anyString(), anyString())).thenReturn(Set.of(SAAS_CRN));
        assertThrows(IllegalStateException.class, () -> underTest.listSdxCrns("env", "envCrn"));

        when(paasSdxService.listSdxCrns(anyString(), anyString())).thenReturn(Set.of());
        when(saasSdxService.listSdxCrns(anyString(), anyString())).thenReturn(Set.of(SAAS_CRN));
        assertTrue(underTest.listSdxCrns("env", "envCrn").contains(SAAS_CRN));

        when(paasSdxService.listSdxCrns(anyString(), anyString())).thenReturn(Set.of(PAAS_CRN));
        when(saasSdxService.listSdxCrns(anyString(), anyString())).thenReturn(Set.of());
        assertTrue(underTest.listSdxCrns("env", "envCrn").contains(PAAS_CRN));
    }

    @Test
    public void testSaasGetAttemptResult() {
        when(saasSdxService.getPollingResultForDeletion(anyString(), anyString(), any())).thenReturn(Map.of(SAAS_CRN, PollingResult.IN_PROGRESS));
        underTest.getAttemptResultForDeletion("envCrn", "env", Set.of(SAAS_CRN));
        verifyNoInteractions(paasSdxService);
        verify(saasSdxService).getPollingResultForDeletion(anyString(), anyString(), any());
    }

    @Test
    public void testPaasGetAttemptResult() {
        when(paasSdxService.getPollingResultForDeletion(anyString(), anyString(), any())).thenReturn(Map.of(PAAS_CRN, PollingResult.IN_PROGRESS));
        underTest.getAttemptResultForDeletion("envCrn", "env", Set.of(PAAS_CRN));
        verifyNoInteractions(saasSdxService);
        verify(paasSdxService).getPollingResultForDeletion(anyString(), anyString(), any());
    }

    @Test
    public void testBothGetAttemptResult() {
        assertThrows(IllegalStateException.class, () ->
                underTest.getAttemptResultForDeletion("envCrn", "env", Set.of(PAAS_CRN, SAAS_CRN)));
        verifyNoInteractions(paasSdxService, saasSdxService);
    }
}
