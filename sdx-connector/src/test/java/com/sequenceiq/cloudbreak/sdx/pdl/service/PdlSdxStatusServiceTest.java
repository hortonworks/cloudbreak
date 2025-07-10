package com.sequenceiq.cloudbreak.sdx.pdl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.cloudera.thunderhead.service.environments2api.model.DescribeEnvironmentResponse;
import com.cloudera.thunderhead.service.environments2api.model.Environment;
import com.cloudera.thunderhead.service.environments2api.model.PrivateDatalakeDetails;
import com.cloudera.thunderhead.service.environments2api.model.PvcEnvironmentDetails;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.sdx.common.status.StatusCheckResult;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.remoteenvironment.api.v1.environment.endpoint.RemoteEnvironmentEndpoint;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.DescribeRemoteEnvironment;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
public class PdlSdxStatusServiceTest {
    private static final String TENANT = "tenant";

    private static final String PDL_CRN = String.format("crn:altus:environments:us-west-1:%s:environment:crn1", TENANT);

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:tenant:environment:crn1";

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private EnvironmentEndpoint environmentEndpoint;

    @Mock
    private RemoteEnvironmentEndpoint remoteEnvironmentEndpoint;

    @Mock
    private DetailedEnvironmentResponse detailedEnvironmentResponse;

    @Mock
    private DescribeEnvironmentResponse describeEnvironmentResponse;

    @Mock
    private Environment environment;

    @Mock
    private PvcEnvironmentDetails pvcEnvironmentDetails;

    @Mock
    private PrivateDatalakeDetails privateDatalakeDetails;

    @InjectMocks
    private PdlSdxStatusService underTest;

    @BeforeEach
    void setup() {
        when(detailedEnvironmentResponse.getRemoteEnvironmentCrn()).thenReturn(PDL_CRN);
        when(entitlementService.hybridCloudEnabled(TENANT)).thenReturn(true);
        when(environmentEndpoint.getByCrn(ENV_CRN)).thenReturn(detailedEnvironmentResponse);
        when(pvcEnvironmentDetails.getPrivateDatalakeDetails()).thenReturn(privateDatalakeDetails);
        when(environment.getPvcEnvironmentDetails()).thenReturn(pvcEnvironmentDetails);
        when(describeEnvironmentResponse.getEnvironment()).thenReturn(environment);
        when(remoteEnvironmentEndpoint.getByCrn(any())).thenReturn(describeEnvironmentResponse);
    }

    @Test
    public void testListSdxCrnStatusPairRemoteEnvCrnEmpty() {
        when(detailedEnvironmentResponse.getRemoteEnvironmentCrn()).thenReturn(null);
        assertTrue(underTest.listSdxCrnStatusPair(ENV_CRN).isEmpty());
        verify(environmentEndpoint).getByCrn(ENV_CRN);
        verify(remoteEnvironmentEndpoint, never()).getByCrn(any());
    }

    @Test
    public void testListSdxCrnStatusPairEnvironmentNotAvailable() {
        when(describeEnvironmentResponse.getEnvironment()).thenReturn(null);
        assertTrue(underTest.listSdxCrnStatusPair(ENV_CRN).isEmpty());
        verify(environmentEndpoint).getByCrn(ENV_CRN);
        ArgumentCaptor<DescribeRemoteEnvironment> captor = ArgumentCaptor.forClass(DescribeRemoteEnvironment.class);
        verify(remoteEnvironmentEndpoint).getByCrn(captor.capture());
        DescribeRemoteEnvironment describeRemoteEnvironment = captor.getValue();
        assertEquals(PDL_CRN, describeRemoteEnvironment.getCrn());
        verify(environment, never()).getPvcEnvironmentDetails();
    }

    @Test
    public void testListSdxCrnStatusPairPvcEnvironmentNotAvailable() {
        when(environment.getPvcEnvironmentDetails()).thenReturn(null);
        assertTrue(underTest.listSdxCrnStatusPair(ENV_CRN).isEmpty());
        verify(environmentEndpoint).getByCrn(ENV_CRN);
        ArgumentCaptor<DescribeRemoteEnvironment> captor = ArgumentCaptor.forClass(DescribeRemoteEnvironment.class);
        verify(remoteEnvironmentEndpoint).getByCrn(captor.capture());
        DescribeRemoteEnvironment describeRemoteEnvironment = captor.getValue();
        assertEquals(PDL_CRN, describeRemoteEnvironment.getCrn());
        verify(pvcEnvironmentDetails, never()).getPrivateDatalakeDetails();
    }

    @Test
    public void testListSdxCrnStatusPairPvcDataLakeNotAvailable() {
        when(pvcEnvironmentDetails.getPrivateDatalakeDetails()).thenReturn(null);
        assertTrue(underTest.listSdxCrnStatusPair(ENV_CRN).isEmpty());
        verify(environmentEndpoint).getByCrn(ENV_CRN);
        ArgumentCaptor<DescribeRemoteEnvironment> captor = ArgumentCaptor.forClass(DescribeRemoteEnvironment.class);
        verify(remoteEnvironmentEndpoint).getByCrn(captor.capture());
        DescribeRemoteEnvironment describeRemoteEnvironment = captor.getValue();
        assertEquals(PDL_CRN, describeRemoteEnvironment.getCrn());
    }

    @Test
    public void testListSdxCrnStatusPair() {
        when(environment.getCrn()).thenReturn(PDL_CRN);
        when(privateDatalakeDetails.getStatus()).thenReturn(PrivateDatalakeDetails.StatusEnum.AVAILABLE);
        Set<Pair<String, PrivateDatalakeDetails.StatusEnum>> sdxSet = underTest.listSdxCrnStatusPair(ENV_CRN);
        assertEquals(1, sdxSet.size());
        Pair<String, PrivateDatalakeDetails.StatusEnum> sdx = sdxSet.iterator().next();
        assertEquals(PDL_CRN, sdx.getKey());
        assertEquals(PrivateDatalakeDetails.StatusEnum.AVAILABLE, sdx.getValue());
        verify(environmentEndpoint).getByCrn(ENV_CRN);
        ArgumentCaptor<DescribeRemoteEnvironment> captor = ArgumentCaptor.forClass(DescribeRemoteEnvironment.class);
        verify(remoteEnvironmentEndpoint).getByCrn(captor.capture());
        DescribeRemoteEnvironment describeRemoteEnvironment = captor.getValue();
        assertEquals(PDL_CRN, describeRemoteEnvironment.getCrn());
    }

    @Test
    public void testGetAvailabilityStatusCheckResult() {
        assertEquals(StatusCheckResult.AVAILABLE, underTest.getAvailabilityStatusCheckResult(PrivateDatalakeDetails.StatusEnum.AVAILABLE));
        assertEquals(StatusCheckResult.NOT_AVAILABLE, underTest.getAvailabilityStatusCheckResult(PrivateDatalakeDetails.StatusEnum.NOT_AVAILABLE));
    }

}
