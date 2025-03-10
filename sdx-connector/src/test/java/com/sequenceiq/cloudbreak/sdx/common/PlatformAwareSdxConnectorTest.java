package com.sequenceiq.cloudbreak.sdx.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.sdx.TargetPlatform;
import com.sequenceiq.cloudbreak.sdx.cdl.service.CdlSdxDeleteService;
import com.sequenceiq.cloudbreak.sdx.cdl.service.CdlSdxDescribeService;
import com.sequenceiq.cloudbreak.sdx.cdl.service.CdlSdxStartStopService;
import com.sequenceiq.cloudbreak.sdx.cdl.service.CdlSdxStatusService;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.sdx.common.polling.PollingResult;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxDeleteService;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxDescribeService;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxStartStopService;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxStatusService;
import com.sequenceiq.cloudbreak.sdx.paas.service.PaasSdxDeleteService;
import com.sequenceiq.cloudbreak.sdx.paas.service.PaasSdxDescribeService;
import com.sequenceiq.cloudbreak.sdx.paas.service.PaasSdxStartStopService;
import com.sequenceiq.cloudbreak.sdx.paas.service.PaasSdxStatusService;

@ExtendWith(MockitoExtension.class)
class PlatformAwareSdxConnectorTest {

    private static final String PAAS_CRN = "crn:cdp:datalake:us-west-1:tenant:datalake:crn1";

    private static final String INVALID_CRN = "crn:cdp:environments:us-west-1:tenant:environment:crn1";

    private static final String LEGACY_PAAS_CRN = "crn:cdp:datahub:us-west-1:tenant:cluster:crn1";

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

    @Mock
    private PaasSdxStartStopService paasSdxStartStopService;

    @Mock
    private CdlSdxStartStopService cdlSdxStartStopService;

    @InjectMocks
    private PlatformAwareSdxConnector underTest;

    @BeforeEach
    public void setup() throws IllegalAccessException {
        Map<TargetPlatform, PlatformAwareSdxStatusService<?>> status = Maps.newHashMap();
        status.put(TargetPlatform.CDL, cdlSdxStatusService);
        status.put(TargetPlatform.PAAS, paasSdxStatusService);
        Map<TargetPlatform, PlatformAwareSdxDescribeService> describe = Maps.newHashMap();
        describe.put(TargetPlatform.CDL, cdlSdxDescribeService);
        describe.put(TargetPlatform.PAAS, paasSdxDescribeService);
        Map<TargetPlatform, PlatformAwareSdxDeleteService<?>> delete = Maps.newHashMap();
        delete.put(TargetPlatform.CDL, cdlSdxDeleteService);
        delete.put(TargetPlatform.PAAS, paasSdxDeleteService);
        Map<TargetPlatform, PlatformAwareSdxStartStopService> startStop = Maps.newHashMap();
        startStop.put(TargetPlatform.CDL, cdlSdxStartStopService);
        startStop.put(TargetPlatform.PAAS, paasSdxStartStopService);
        FieldUtils.writeField(underTest, "platformDependentSdxStatusServicesMap", status, true);
        FieldUtils.writeField(underTest, "platformDependentSdxDescribeServices", describe, true);
        FieldUtils.writeField(underTest, "platformDependentSdxDeleteServices", delete, true);
        FieldUtils.writeField(underTest, "platformDependentSdxStartStopServices", startStop, true);
    }

    @Test
    public void testOtherPlatformValidationFailure() {
        when(cdlSdxDescribeService.listSdxCrns(anyString())).thenReturn(Set.of(SAAS_CRN));
        assertThrows(BadRequestException.class, () -> underTest.validateIfOtherPlatformsHasSdx("env", TargetPlatform.PAAS));
        verify(cdlSdxDescribeService).listSdxCrns(any());
        verifyNoInteractions(paasSdxDescribeService);
    }

    @Test
    public void testOtherPlatformValidationSuccess() {
        when(cdlSdxDescribeService.listSdxCrns(anyString())).thenReturn(Set.of());
        underTest.validateIfOtherPlatformsHasSdx("env", TargetPlatform.PAAS);
        verify(cdlSdxDescribeService).listSdxCrns(any());
        verifyNoInteractions(paasSdxDescribeService);
    }

    @Test
    public void testDelete() {
        when(paasSdxDescribeService.listSdxCrnsDetachedIncluded(anyString())).thenReturn(Set.of(PAAS_CRN));
        when(cdlSdxDescribeService.listSdxCrnsDetachedIncluded(anyString())).thenReturn(Set.of(SAAS_CRN));
        doNothing().when(cdlSdxDeleteService).deleteSdx(any(), anyBoolean());
        doNothing().when(paasSdxDeleteService).deleteSdx(any(), anyBoolean());
        underTest.deleteByEnvironment("env", false);
        verify(cdlSdxDeleteService).deleteSdx(anyString(), anyBoolean());
        verify(paasSdxDeleteService).deleteSdx(anyString(), anyBoolean());
    }

    @Test
    public void testList() {
        when(paasSdxDescribeService.listSdxCrns(anyString())).thenReturn(Set.of(PAAS_CRN));
        when(cdlSdxDescribeService.listSdxCrns(anyString())).thenReturn(Set.of(SAAS_CRN));
        assertTrue(underTest.listSdxCrns("envCrn").contains(PAAS_CRN));

        when(paasSdxDescribeService.listSdxCrns(anyString())).thenReturn(Set.of());
        when(cdlSdxDescribeService.listSdxCrns(anyString())).thenReturn(Set.of(SAAS_CRN));
        assertTrue(underTest.listSdxCrns("envCrn").contains(SAAS_CRN));

        when(paasSdxDescribeService.listSdxCrns(anyString())).thenReturn(Set.of(PAAS_CRN));
        assertTrue(underTest.listSdxCrns("envCrn").contains(PAAS_CRN));
    }

    @Test
    public void testGetAttemptResult() {
        when(cdlSdxDeleteService.getPollingResultForDeletion(anyString())).thenReturn(Map.of(SAAS_CRN, PollingResult.IN_PROGRESS));
        when(paasSdxDeleteService.getPollingResultForDeletion(anyString())).thenReturn(Map.of(PAAS_CRN, PollingResult.IN_PROGRESS));
        underTest.getAttemptResultForDeletion("envCrn");
        verify(paasSdxDeleteService).getPollingResultForDeletion(anyString());
        verify(cdlSdxDeleteService).getPollingResultForDeletion(anyString());
    }

    @Test
    public void testGetSdxCrnByEnvironmentCrnCDL() {
        when(cdlSdxDescribeService.getSdxByEnvironmentCrn(anyString())).thenReturn(Optional.of(SdxBasicView.builder().withCrn(SAAS_CRN).build()));
        when(paasSdxDescribeService.listSdxCrns(anyString())).thenReturn(Set.of());
        when(cdlSdxDescribeService.listSdxCrns(anyString())).thenReturn(Set.of(SAAS_CRN));
        Optional<SdxBasicView> sdx = underTest.getSdxBasicViewByEnvironmentCrn("envCrn");
        assertEquals(SAAS_CRN, sdx.get().crn());
    }

    @Test
    public void testGetSdxCrnByEnvironmentCrnPaaS() {
        when(paasSdxDescribeService.getSdxByEnvironmentCrn(anyString())).thenReturn(Optional.of(SdxBasicView.builder().withCrn(PAAS_CRN).build()));
        when(paasSdxDescribeService.listSdxCrns(anyString())).thenReturn(Set.of(PAAS_CRN));
        Optional<SdxBasicView> sdx = underTest.getSdxBasicViewByEnvironmentCrn("envCrn");
        assertEquals(PAAS_CRN, sdx.get().crn());
    }

    @Test
    public void testGetSdxBasicViewWithInvalidCrn() {
        when(paasSdxDescribeService.listSdxCrns(any())).thenReturn(Set.of(INVALID_CRN));

        assertThrows(IllegalStateException.class, () -> underTest.getSdxBasicViewByEnvironmentCrn(""));

        verify(paasSdxDescribeService, never()).getSdxAccessViewByEnvironmentCrn(any());
    }

    @Test
    public void testGetSdxBasicViewWithLegacyPaasCrn() {
        when(paasSdxDescribeService.listSdxCrns(any())).thenReturn(Set.of(LEGACY_PAAS_CRN));
        when(paasSdxDescribeService.getSdxByEnvironmentCrn(any())).thenReturn(Optional.of(SdxBasicView.builder().build()));

        underTest.getSdxBasicViewByEnvironmentCrn("");

        verify(paasSdxDescribeService).getSdxByEnvironmentCrn(any());
    }

    @Test
    public void testStopPaaS() {
        when(paasSdxDescribeService.listSdxCrns(anyString())).thenReturn(Set.of(PAAS_CRN));
        when(paasSdxDescribeService.getSdxByEnvironmentCrn(anyString())).thenReturn(Optional.of(SdxBasicView.builder().withCrn(PAAS_CRN).build()));
        underTest.stopByEnvironment("env");
        verify(paasSdxStartStopService).stopSdx(anyString());
    }

    @Test
    public void testStopCDL() {
        when(cdlSdxDescribeService.listSdxCrns(anyString())).thenReturn(Set.of(SAAS_CRN));
        when(cdlSdxDescribeService.getSdxByEnvironmentCrn(anyString())).thenReturn(Optional.of(SdxBasicView.builder().withCrn(SAAS_CRN).build()));
        underTest.stopByEnvironment("env");
        verify(cdlSdxStartStopService).stopSdx(anyString());
    }

    @Test
    public void testStartPaaS() {
        when(paasSdxDescribeService.listSdxCrns(anyString())).thenReturn(Set.of(PAAS_CRN));
        when(paasSdxDescribeService.getSdxByEnvironmentCrn(anyString())).thenReturn(Optional.of(SdxBasicView.builder().withCrn(PAAS_CRN).build()));
        underTest.startByEnvironment("env");
        verify(paasSdxStartStopService).startSdx(anyString());
    }

    @Test
    public void testStartCDL() {
        when(cdlSdxDescribeService.listSdxCrns(anyString())).thenReturn(Set.of(SAAS_CRN));
        when(cdlSdxDescribeService.getSdxByEnvironmentCrn(anyString())).thenReturn(Optional.of(SdxBasicView.builder().withCrn(SAAS_CRN).build()));
        underTest.startByEnvironment("env");
        verify(cdlSdxStartStopService).startSdx(anyString());
    }
}
