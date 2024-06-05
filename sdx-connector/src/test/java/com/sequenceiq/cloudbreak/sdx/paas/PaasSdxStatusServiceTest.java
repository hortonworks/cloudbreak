package com.sequenceiq.cloudbreak.sdx.paas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.sdx.common.status.StatusCheckResult;
import com.sequenceiq.cloudbreak.sdx.paas.service.PaasSdxStatusService;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@ExtendWith(MockitoExtension.class)
public class PaasSdxStatusServiceTest {

    private static final String PAAS_CRN = "crn:cdp:datalake:us-west-1:tenant:datalake:crn1";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:tenant:environment:crn1";

    @Mock
    private SdxEndpoint sdxEndpoint;

    @InjectMocks
    private PaasSdxStatusService underTest;

    @Test
    public void testListStatusPairsCrn() {
        SdxClusterResponse sdxClusterResponse = getSdxClusterResponse();
        when(sdxEndpoint.getByEnvCrn(any())).thenReturn(List.of(sdxClusterResponse));
        assertTrue(underTest.listSdxCrnStatusPair(ENV_CRN, Set.of(PAAS_CRN))
                .contains(Pair.of(PAAS_CRN, SdxClusterStatusResponse.RUNNING)));
        verify(sdxEndpoint).getByEnvCrn(any());
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

    private SdxClusterResponse getSdxClusterResponse() {
        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        sdxClusterResponse.setCrn(PAAS_CRN);
        sdxClusterResponse.setEnvironmentCrn(ENV_CRN);
        sdxClusterResponse.setStatus(SdxClusterStatusResponse.RUNNING);
        return sdxClusterResponse;
    }
}
