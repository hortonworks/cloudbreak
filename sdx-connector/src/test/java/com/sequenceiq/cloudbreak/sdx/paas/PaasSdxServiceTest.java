package com.sequenceiq.cloudbreak.sdx.paas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.sdx.common.polling.PollingResult;
import com.sequenceiq.cloudbreak.sdx.common.status.StatusCheckResult;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@ExtendWith(MockitoExtension.class)
public class PaasSdxServiceTest {

    private static final String PAAS_CRN = "crn:cdp:datalake:us-west-1:tenant:datalake:crn1";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:tenant:environment:crn1";

    @Mock
    private SdxEndpoint sdxEndpoint;

    @InjectMocks
    private PaasSdxService underTest;

    @Test
    public void testDelete() {
        when(sdxEndpoint.deleteByCrn(any(), any())).thenReturn(new FlowIdentifier(FlowType.FLOW, "1"));
        underTest.deleteSdx(PAAS_CRN, true);
        verify(sdxEndpoint).deleteByCrn(eq(PAAS_CRN), anyBoolean());
    }

    @Test
    public void testListCrn() {
        when(sdxEndpoint.getByEnvCrn(any())).thenReturn(List.of(getSdxClusterResponse()));
        Set<String> sdxCrns = underTest.listSdxCrns(ENV_CRN);
        assertTrue(sdxCrns.contains(PAAS_CRN));
        verify(sdxEndpoint).getByEnvCrn(eq(ENV_CRN));
    }

    @Test
    public void testListStatusPairsCrn() {
        SdxClusterResponse sdxClusterResponse = getSdxClusterResponse();
        when(sdxEndpoint.getByEnvCrn(any())).thenReturn(List.of(sdxClusterResponse));
        assertTrue(underTest.listSdxCrnStatusPair(ENV_CRN, Set.of(PAAS_CRN))
                .contains(Pair.of(PAAS_CRN, SdxClusterStatusResponse.RUNNING)));
        verify(sdxEndpoint).getByEnvCrn(any());
    }

    @Test
    public void testGetDeletePollingResult() {
        assertEquals(PollingResult.IN_PROGRESS, underTest.getDeletePollingResultByStatus(SdxClusterStatusResponse.STACK_DELETION_IN_PROGRESS));
        assertEquals(PollingResult.FAILED, underTest.getDeletePollingResultByStatus(SdxClusterStatusResponse.DELETE_FAILED));
    }

    @Test
    public void testGetAvailabilityStatusCheckResult() {
        assertEquals(StatusCheckResult.AVAILABLE, underTest.getAvailabilityStatusCheckResult(SdxClusterStatusResponse.RUNNING));
        assertEquals(StatusCheckResult.AVAILABLE, underTest.getAvailabilityStatusCheckResult(SdxClusterStatusResponse.DATALAKE_BACKUP_INPROGRESS));
        assertEquals(StatusCheckResult.NOT_AVAILABLE, underTest.getAvailabilityStatusCheckResult(SdxClusterStatusResponse.RECOVERY_IN_PROGRESS));
        assertEquals(StatusCheckResult.NOT_AVAILABLE, underTest.getAvailabilityStatusCheckResult(SdxClusterStatusResponse.DATALAKE_UPGRADE_CCM_IN_PROGRESS));
        assertEquals(StatusCheckResult.ROLLING_UPGRADE_IN_PROGRESS,
                underTest.getAvailabilityStatusCheckResult(SdxClusterStatusResponse.DATALAKE_ROLLING_UPGRADE_IN_PROGRESS));
    }

    @Test
    void testGetPaasSdxLocally() throws IllegalAccessException {
        LocalPaasSdxService mockLocalSdxService = mock(LocalPaasSdxService.class);
        FieldUtils.writeField(underTest, "localPaasSdxService", Optional.of(mockLocalSdxService), true);
        when(mockLocalSdxService.getSdxBasicView(anyString())).thenReturn(Optional.of(new SdxBasicView(null, "crn", null, null, false, 1L, null)));

        underTest.getSdxByEnvironmentCrn("envCrn");

        verifyNoInteractions(sdxEndpoint);
        verify(mockLocalSdxService).getSdxBasicView(anyString());
    }

    @Test
    void testGetPaasSdxUsingDlService() throws IllegalAccessException {
        FieldUtils.writeField(underTest, "localPaasSdxService", Optional.empty(), true);
        when(sdxEndpoint.getByEnvCrn(anyString())).thenReturn(List.of(getSdxClusterResponse()));

        underTest.getSdxByEnvironmentCrn("envCrn");

        verify(sdxEndpoint).getByEnvCrn(anyString());
    }

    private SdxClusterResponse getSdxClusterResponse() {
        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        sdxClusterResponse.setCrn(PAAS_CRN);
        sdxClusterResponse.setEnvironmentCrn(ENV_CRN);
        sdxClusterResponse.setStatus(SdxClusterStatusResponse.RUNNING);
        return sdxClusterResponse;
    }
}
