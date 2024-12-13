package com.sequenceiq.cloudbreak.sdx.paas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.sdx.common.status.StatusCheckResult;
import com.sequenceiq.cloudbreak.sdx.paas.service.PaasSdxStatusService;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@ExtendWith(MockitoExtension.class)
public class PaasSdxStatusServiceTest {

    private static final String PAAS_CRN = "crn:cdp:datalake:us-west-1:tenant:datalake:crn1";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:tenant:environment:crn1";

    private static final String INTERNAL_ACTOR = "crn:cdp:iam:us-west-1:cloudera:user:__internal__actor__";

    @Mock
    private SdxEndpoint sdxEndpoint;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @InjectMocks
    private PaasSdxStatusService underTest;

    @BeforeEach
    void setup() {
        RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator = mock(RegionAwareInternalCrnGenerator.class);
        lenient().when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        lenient().when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn(INTERNAL_ACTOR);
    }

    @Test
    public void testListStatusPairsCrn() {
        SdxClusterResponse sdxClusterResponse = getSdxClusterResponse();
        when(sdxEndpoint.getByEnvCrn(any(), eq(false))).thenReturn(List.of(sdxClusterResponse));
        assertTrue(underTest.listSdxCrnStatusPair(ENV_CRN)
                .contains(Pair.of(PAAS_CRN, SdxClusterStatusResponse.RUNNING)));
        verify(sdxEndpoint).getByEnvCrn(any(), eq(false));
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
