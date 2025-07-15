package com.sequenceiq.cloudbreak.sdx.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
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

import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptState;
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
import com.sequenceiq.cloudbreak.sdx.pdl.service.PdlSdxDeleteService;
import com.sequenceiq.cloudbreak.sdx.pdl.service.PdlSdxDescribeService;
import com.sequenceiq.cloudbreak.sdx.pdl.service.PdlSdxStartStopService;
import com.sequenceiq.cloudbreak.sdx.pdl.service.PdlSdxStatusService;

@ExtendWith(MockitoExtension.class)
class PlatformAwareSdxConnectorTest {

    private static final String PAAS_CRN = "crn:cdp:datalake:us-west-1:tenant:datalake:crn1";

    private static final String INVALID_CRN = "crn:cdp:datahub:us-west-1:default:recipe:crn1";

    private static final String LEGACY_PAAS_CRN = "crn:cdp:datahub:us-west-1:tenant:cluster:crn1";

    private static final String SAAS_CRN = "crn:cdp:sdxsvc:us-west-1:tenant:instance:crn2";

    private static final String PDL_CRN =  "crn:altus:environments:us-west-1:tenant:environment:crn1";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:default:environment:5b32fd24-17e1-46df-8ad9-0f7fbacda529";

    @Mock
    private CdlSdxStatusService cdlSdxStatusService;

    @Mock
    private PaasSdxStatusService paasSdxStatusService;

    @Mock
    private PdlSdxStatusService pdlSdxStatusService;

    @Mock
    private CdlSdxDescribeService cdlSdxDescribeService;

    @Mock
    private PaasSdxDescribeService paasSdxDescribeService;

    @Mock
    private PdlSdxDescribeService pdlSdxDescribeService;

    @Mock
    private CdlSdxDeleteService cdlSdxDeleteService;

    @Mock
    private PaasSdxDeleteService paasSdxDeleteService;

    @Mock
    private PdlSdxDeleteService pdlSdxDeleteService;

    @Mock
    private PaasSdxStartStopService paasSdxStartStopService;

    @Mock
    private CdlSdxStartStopService cdlSdxStartStopService;

    @Mock
    private PdlSdxStartStopService pdlSdxStartStopService;

    @InjectMocks
    private PlatformAwareSdxConnector underTest;

    @BeforeEach
    public void setup() throws IllegalAccessException {
        Map<TargetPlatform, PlatformAwareSdxStatusService<?>> status = Maps.newHashMap();
        status.put(TargetPlatform.CDL, cdlSdxStatusService);
        status.put(TargetPlatform.PAAS, paasSdxStatusService);
        status.put(TargetPlatform.PDL, pdlSdxStatusService);
        Map<TargetPlatform, PlatformAwareSdxDescribeService> describe = Maps.newHashMap();
        describe.put(TargetPlatform.CDL, cdlSdxDescribeService);
        describe.put(TargetPlatform.PAAS, paasSdxDescribeService);
        describe.put(TargetPlatform.PDL, pdlSdxDescribeService);
        Map<TargetPlatform, PlatformAwareSdxDeleteService<?>> delete = Maps.newHashMap();
        delete.put(TargetPlatform.CDL, cdlSdxDeleteService);
        delete.put(TargetPlatform.PAAS, paasSdxDeleteService);
        delete.put(TargetPlatform.PDL, pdlSdxDeleteService);
        Map<TargetPlatform, PlatformAwareSdxStartStopService> startStop = Maps.newHashMap();
        startStop.put(TargetPlatform.CDL, cdlSdxStartStopService);
        startStop.put(TargetPlatform.PAAS, paasSdxStartStopService);
        startStop.put(TargetPlatform.PDL, pdlSdxStartStopService);
        FieldUtils.writeField(underTest, "platformDependentSdxStatusServicesMap", status, true);
        FieldUtils.writeField(underTest, "platformDependentSdxDescribeServices", describe, true);
        FieldUtils.writeField(underTest, "platformDependentSdxDeleteServices", delete, true);
        FieldUtils.writeField(underTest, "platformDependentSdxStartStopServices", startStop, true);
    }

    @Test
    public void testOtherPlatformValidationFailureWhenSaasExist() {
        when(cdlSdxDescribeService.listSdxCrns(ENV_CRN)).thenReturn(Set.of(SAAS_CRN));
        assertThrows(BadRequestException.class, () -> underTest.validateIfOtherPlatformsHasSdx(ENV_CRN, TargetPlatform.PAAS));
        verify(cdlSdxDescribeService).listSdxCrns(ENV_CRN);
        verifyNoInteractions(paasSdxDescribeService);
    }

    @Test
    public void testOtherPlatformValidationFailureWhenPdlExist() {
        when(pdlSdxDescribeService.listSdxCrns(ENV_CRN)).thenReturn(Set.of(PDL_CRN));
        assertThrows(BadRequestException.class, () -> underTest.validateIfOtherPlatformsHasSdx(ENV_CRN, TargetPlatform.PAAS));
        verify(pdlSdxDescribeService).listSdxCrns(ENV_CRN);
        verifyNoInteractions(paasSdxDescribeService);
    }

    @Test
    public void testOtherPlatformValidationSuccess() {
        when(cdlSdxDescribeService.listSdxCrns(anyString())).thenReturn(Set.of());
        when(pdlSdxDescribeService.listSdxCrns(anyString())).thenReturn(Set.of());
        underTest.validateIfOtherPlatformsHasSdx(ENV_CRN, TargetPlatform.PAAS);
        verify(cdlSdxDescribeService).listSdxCrns(ENV_CRN);
        verify(pdlSdxDescribeService).listSdxCrns(ENV_CRN);
        verifyNoInteractions(paasSdxDescribeService);
    }

    @Test
    public void testDelete() {
        when(paasSdxDescribeService.listSdxCrnsDetachedIncluded(ENV_CRN)).thenReturn(Set.of(PAAS_CRN));
        when(cdlSdxDescribeService.listSdxCrnsDetachedIncluded(ENV_CRN)).thenReturn(Set.of(SAAS_CRN));
        when(pdlSdxDescribeService.listSdxCrnsDetachedIncluded(ENV_CRN)).thenReturn(Set.of(PDL_CRN));
        doNothing().when(cdlSdxDeleteService).deleteSdx(SAAS_CRN, false);
        doNothing().when(paasSdxDeleteService).deleteSdx(PAAS_CRN, false);
        doNothing().when(pdlSdxDeleteService).deleteSdx(PDL_CRN, false);
        underTest.deleteByEnvironment(ENV_CRN, false);
        verify(cdlSdxDeleteService).deleteSdx(SAAS_CRN, false);
        verify(paasSdxDeleteService).deleteSdx(PAAS_CRN, false);
        verify(pdlSdxDeleteService).deleteSdx(PDL_CRN, false);
    }

    @Test
    public void testList() {
        when(paasSdxDescribeService.listSdxCrns(ENV_CRN)).thenReturn(Set.of(PAAS_CRN));
        lenient().when(cdlSdxDescribeService.listSdxCrns(ENV_CRN)).thenReturn(Set.of(SAAS_CRN));
        lenient().when(pdlSdxDescribeService.listSdxCrns(ENV_CRN)).thenReturn(Set.of(PDL_CRN));
        assertTrue(underTest.listSdxCrns(ENV_CRN).contains(PAAS_CRN));

        when(paasSdxDescribeService.listSdxCrns(anyString())).thenReturn(Set.of());
        when(cdlSdxDescribeService.listSdxCrns(anyString())).thenReturn(Set.of(SAAS_CRN));
        lenient().when(pdlSdxDescribeService.listSdxCrns(ENV_CRN)).thenReturn(Set.of(PDL_CRN));
        assertTrue(underTest.listSdxCrns(ENV_CRN).contains(SAAS_CRN));

        when(paasSdxDescribeService.listSdxCrns(ENV_CRN)).thenReturn(Set.of());
        when(cdlSdxDescribeService.listSdxCrns(ENV_CRN)).thenReturn(Set.of());
        when(pdlSdxDescribeService.listSdxCrns(ENV_CRN)).thenReturn(Set.of(PDL_CRN));
        assertTrue(underTest.listSdxCrns(ENV_CRN).contains(PDL_CRN));

        when(paasSdxDescribeService.listSdxCrns(ENV_CRN)).thenReturn(Set.of(PAAS_CRN));
        assertTrue(underTest.listSdxCrns(ENV_CRN).contains(PAAS_CRN));
    }

    @Test
    public void testGetAttemptResultInProgress() {
        when(cdlSdxDeleteService.getPollingResultForDeletion(ENV_CRN)).thenReturn(Map.of(SAAS_CRN, PollingResult.IN_PROGRESS));
        when(paasSdxDeleteService.getPollingResultForDeletion(ENV_CRN)).thenReturn(Map.of(PAAS_CRN, PollingResult.IN_PROGRESS));
        when(pdlSdxDeleteService.getPollingResultForDeletion(ENV_CRN)).thenReturn(Map.of(PDL_CRN, PollingResult.IN_PROGRESS));
        AttemptResult<Object> result = underTest.getAttemptResultForDeletion(ENV_CRN);
        assertEquals(AttemptState.CONTINUE, result.getState());
        verify(paasSdxDeleteService).getPollingResultForDeletion(ENV_CRN);
        verify(cdlSdxDeleteService).getPollingResultForDeletion(ENV_CRN);
        verify(pdlSdxDeleteService).getPollingResultForDeletion(ENV_CRN);
    }

    @Test
    public void testGetAttemptResultFailed() {
        when(cdlSdxDeleteService.getPollingResultForDeletion(ENV_CRN)).thenReturn(Map.of(SAAS_CRN, PollingResult.FAILED));
        when(paasSdxDeleteService.getPollingResultForDeletion(ENV_CRN)).thenReturn(Map.of(PAAS_CRN, PollingResult.IN_PROGRESS));
        when(pdlSdxDeleteService.getPollingResultForDeletion(ENV_CRN)).thenReturn(Map.of(PDL_CRN, PollingResult.COMPLETED));
        AttemptResult<Object> result = underTest.getAttemptResultForDeletion(ENV_CRN);
        assertEquals(AttemptState.BREAK, result.getState());
        assertEquals("Data Lake delete failed for " + SAAS_CRN, result.getCause().getMessage());
        verify(paasSdxDeleteService).getPollingResultForDeletion(ENV_CRN);
        verify(cdlSdxDeleteService).getPollingResultForDeletion(ENV_CRN);
        verify(pdlSdxDeleteService).getPollingResultForDeletion(ENV_CRN);
    }

    @Test
    public void testGetAttemptResultCompleted() {
        when(cdlSdxDeleteService.getPollingResultForDeletion(ENV_CRN)).thenReturn(Map.of(SAAS_CRN, PollingResult.COMPLETED));
        when(paasSdxDeleteService.getPollingResultForDeletion(ENV_CRN)).thenReturn(Map.of(PAAS_CRN, PollingResult.COMPLETED));
        when(pdlSdxDeleteService.getPollingResultForDeletion(ENV_CRN)).thenReturn(Map.of(PDL_CRN, PollingResult.COMPLETED));
        AttemptResult<Object> result = underTest.getAttemptResultForDeletion(ENV_CRN);
        assertEquals(AttemptState.FINISH, result.getState());
        verify(paasSdxDeleteService).getPollingResultForDeletion(ENV_CRN);
        verify(cdlSdxDeleteService).getPollingResultForDeletion(ENV_CRN);
        verify(pdlSdxDeleteService).getPollingResultForDeletion(ENV_CRN);
    }

    @Test
    public void testGetSdxCrnByEnvironmentCrnCDL() {
        when(cdlSdxDescribeService.getSdxByEnvironmentCrn(anyString())).thenReturn(Optional.of(SdxBasicView.builder().withCrn(SAAS_CRN).build()));
        when(paasSdxDescribeService.listSdxCrns(anyString())).thenReturn(Set.of());
        when(cdlSdxDescribeService.listSdxCrns(anyString())).thenReturn(Set.of(SAAS_CRN));
        Optional<SdxBasicView> sdx = underTest.getSdxBasicViewByEnvironmentCrn(ENV_CRN);
        assertEquals(SAAS_CRN, sdx.get().crn());
    }

    @Test
    public void testGetSdxCrnByEnvironmentCrnPaaS() {
        when(paasSdxDescribeService.getSdxByEnvironmentCrn(anyString())).thenReturn(Optional.of(SdxBasicView.builder().withCrn(PAAS_CRN).build()));
        when(paasSdxDescribeService.listSdxCrns(anyString())).thenReturn(Set.of(PAAS_CRN));
        Optional<SdxBasicView> sdx = underTest.getSdxBasicViewByEnvironmentCrn(ENV_CRN);
        assertEquals(PAAS_CRN, sdx.get().crn());
    }

    @Test
    public void testGetSdxCrnByEnvironmentCrnPDL() {
        when(pdlSdxDescribeService.getSdxByEnvironmentCrn(anyString())).thenReturn(Optional.of(SdxBasicView.builder().withCrn(PDL_CRN).build()));
        when(pdlSdxDescribeService.listSdxCrns(anyString())).thenReturn(Set.of(PDL_CRN));
        Optional<SdxBasicView> sdx = underTest.getSdxBasicViewByEnvironmentCrn(ENV_CRN);
        assertEquals(PDL_CRN, sdx.get().crn());
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
    public void testStopPDL() {
        when(pdlSdxDescribeService.listSdxCrns(anyString())).thenReturn(Set.of(PDL_CRN));
        when(pdlSdxDescribeService.getSdxByEnvironmentCrn(anyString())).thenReturn(Optional.of(SdxBasicView.builder().withCrn(PDL_CRN).build()));
        underTest.stopByEnvironment(PDL_CRN);
        verify(pdlSdxStartStopService).stopSdx(anyString());
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

    @Test
    public void testStartPDL() {
        when(pdlSdxDescribeService.listSdxCrns(anyString())).thenReturn(Set.of(PDL_CRN));
        when(pdlSdxDescribeService.getSdxByEnvironmentCrn(anyString())).thenReturn(Optional.of(SdxBasicView.builder().withCrn(PDL_CRN).build()));
        underTest.startByEnvironment(ENV_CRN);
        verify(pdlSdxStartStopService).startSdx(anyString());
    }

    @Test
    public void testGetCACertsForEnvironment() {
        when(pdlSdxDescribeService.listSdxCrns(anyString())).thenReturn(Set.of(PDL_CRN));
        when(pdlSdxDescribeService.getCACertsForEnvironment(ENV_CRN)).thenReturn(Optional.of("certecske"));

        Optional<String> response = underTest.getCACertsForEnvironment(ENV_CRN);

        assertEquals("certecske", response.get());
    }
}
