package com.sequenceiq.cloudbreak.sdx.pdl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.cloudera.cdp.servicediscovery.model.ApiRemoteDataContext;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeAsApiRemoteDataContextResponse;
import com.cloudera.thunderhead.service.environments2api.model.DescribeEnvironmentResponse;
import com.cloudera.thunderhead.service.environments2api.model.Environment;
import com.cloudera.thunderhead.service.environments2api.model.GetRootCertificateResponse;
import com.cloudera.thunderhead.service.environments2api.model.PrivateDatalakeDetails;
import com.cloudera.thunderhead.service.environments2api.model.PvcEnvironmentDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.sdx.TargetPlatform;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxAccessView;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.remoteenvironment.api.v1.environment.endpoint.RemoteEnvironmentEndpoint;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.DescribeRemoteEnvironment;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
public class PdlSdxDescribeServiceTest {
    private static final String TENANT = "tenant";

    private static final String PDL_CRN = String.format("crn:altus:environments:us-west-1:%s:environment:crn1", TENANT);

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:tenant:environment:crn1";

    private static final String DATALAKE_NAME = "datalake";

    private static final String CDP_RUNTIME = "7.3.2";

    private static final Boolean ENABLE_RANGER_RAZ = true;

    private static final Long CREATED = 1L;

    private static final String CM_FQDN = "fqdn";

    private static final String CM_IP = "cmIp";

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
    private PdlSdxDescribeService underTest;

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
    public void testGetRemoteDataContextRunTimeException() throws IOException {
        when(remoteEnvironmentEndpoint.getRdcByCrn(any())).thenThrow(new RuntimeException());
        RuntimeException e = assertThrows(RuntimeException.class, () -> underTest.getRemoteDataContext(PDL_CRN));
        assertEquals("Not able to fetch the RDC for PDL from Service Discovery", e.getMessage());
    }

    @Test
    public void testGetRemoteDataContext() throws IOException {
        String rdc = FileReaderUtils.readFileFromClasspath("com/sequenceiq/cloudbreak/sdx/common/service/rdc.json");
        ObjectMapper objectMapper = new ObjectMapper();
        ApiRemoteDataContext apiRemoteDataContext = objectMapper.readValue(rdc, ApiRemoteDataContext.class);
        DescribeDatalakeAsApiRemoteDataContextResponse remoteDataContextResponse = new DescribeDatalakeAsApiRemoteDataContextResponse();
        remoteDataContextResponse.setContext(apiRemoteDataContext);
        when(remoteEnvironmentEndpoint.getRdcByCrn(any())).thenReturn(remoteDataContextResponse);
        Optional<String> expectedRdc = underTest.getRemoteDataContext(PDL_CRN);
        ArgumentCaptor<DescribeRemoteEnvironment> captor = ArgumentCaptor.forClass(DescribeRemoteEnvironment.class);
        verify(remoteEnvironmentEndpoint).getRdcByCrn(captor.capture());
        DescribeRemoteEnvironment describeRemoteEnvironment = captor.getValue();
        assertEquals(PDL_CRN, describeRemoteEnvironment.getCrn());
        assertTrue(expectedRdc.isPresent());
        com.cloudera.api.swagger.model.ApiRemoteDataContext cmApiRemoteDataContext = objectMapper.readValue(rdc,
                com.cloudera.api.swagger.model.ApiRemoteDataContext.class);
        assertEquals("pub-ak-aws3-dl", cmApiRemoteDataContext.getEndPointId());
        assertEquals("CDH 7.3.1", cmApiRemoteDataContext.getClusterVersion());
        assertEquals(7, cmApiRemoteDataContext.getEndPoints().size());
        assertEquals(1, cmApiRemoteDataContext.getEndPoints().stream().filter(apiEndPoint -> apiEndPoint.getName().equals("zookeeper")).count());
        assertEquals(1, cmApiRemoteDataContext.getEndPoints().stream().filter(apiEndPoint -> apiEndPoint.getName().equals("kafka")).count());
        assertEquals(1, cmApiRemoteDataContext.getEndPoints().stream().filter(apiEndPoint -> apiEndPoint.getName().equals("solr")).count());
        assertEquals(1, cmApiRemoteDataContext.getEndPoints().stream().filter(apiEndPoint -> apiEndPoint.getName().equals("ranger")).count());
        assertEquals(1, cmApiRemoteDataContext.getEndPoints().stream().filter(apiEndPoint -> apiEndPoint.getName().equals("atlas")).count());
        assertEquals(1, cmApiRemoteDataContext.getEndPoints().stream().filter(apiEndPoint -> apiEndPoint.getName().equals("knox")).count());
        assertEquals(1, cmApiRemoteDataContext.getEndPoints().stream().filter(apiEndPoint -> apiEndPoint.getName().equals("hive")).count());
        com.cloudera.api.swagger.model.ApiEndPoint zooKeeperApiEndPoint = cmApiRemoteDataContext.getEndPoints().stream()
                .filter(apiEndPoint -> apiEndPoint.getName().equals("zookeeper"))
                .findFirst().get();
        assertEquals("ZOOKEEPER", zooKeeperApiEndPoint.getServiceType());
        assertEquals(1, zooKeeperApiEndPoint.getServiceConfigs().size());
        assertEquals(3, zooKeeperApiEndPoint.getEndPointHostList().size());
    }

    @Test
    public void testListSdxCrnsEmpty() {
        when(detailedEnvironmentResponse.getRemoteEnvironmentCrn()).thenReturn(null);
        assertTrue(underTest.listSdxCrns(ENV_CRN).isEmpty());
        verify(environmentEndpoint).getByCrn(ENV_CRN);
    }

    @Test
    public void testListSdxCrnsNotEmpty() {
        when(detailedEnvironmentResponse.getRemoteEnvironmentCrn()).thenReturn(PDL_CRN);
        Set<String> sdxs = underTest.listSdxCrns(ENV_CRN);
        assertEquals(1, sdxs.size());
        assertEquals(Set.of(PDL_CRN), sdxs);
        verify(environmentEndpoint).getByCrn(ENV_CRN);
    }

    @Test
    public void testGetSdxByEnvironmentCrnPvcEnvironmentNotAvailable() {
        when(environment.getPvcEnvironmentDetails()).thenReturn(null);
        assertTrue(underTest.getSdxByEnvironmentCrn(ENV_CRN).isEmpty());
        verify(environmentEndpoint).getByCrn(ENV_CRN);
        ArgumentCaptor<DescribeRemoteEnvironment> captor = ArgumentCaptor.forClass(DescribeRemoteEnvironment.class);
        verify(remoteEnvironmentEndpoint).getByCrn(captor.capture());
        DescribeRemoteEnvironment describeRemoteEnvironment = captor.getValue();
        assertEquals(PDL_CRN, describeRemoteEnvironment.getCrn());
        verify(pvcEnvironmentDetails, never()).getPrivateDatalakeDetails();
    }

    @Test
    public void testGetSdxByEnvironmentCrnPvcDataLakeNotAvailable() {
        when(pvcEnvironmentDetails.getPrivateDatalakeDetails()).thenReturn(null);
        assertTrue(underTest.getSdxByEnvironmentCrn(ENV_CRN).isEmpty());
        verify(environmentEndpoint).getByCrn(ENV_CRN);
        ArgumentCaptor<DescribeRemoteEnvironment> captor = ArgumentCaptor.forClass(DescribeRemoteEnvironment.class);
        verify(remoteEnvironmentEndpoint).getByCrn(captor.capture());
        DescribeRemoteEnvironment describeRemoteEnvironment = captor.getValue();
        assertEquals(PDL_CRN, describeRemoteEnvironment.getCrn());
    }

    @Test
    public void testGetSdxByEnvironmentCrnDataLakeAvailable() {
        when(environment.getCrn()).thenReturn(PDL_CRN);
        when(environment.getCdpRuntimeVersion()).thenReturn(CDP_RUNTIME);
        when(privateDatalakeDetails.getDatalakeName()).thenReturn(DATALAKE_NAME);
        when(privateDatalakeDetails.getEnableRangerRaz()).thenReturn(ENABLE_RANGER_RAZ);
        when(privateDatalakeDetails.getCreationTimeEpochMillis()).thenReturn(CREATED);
        Optional<SdxBasicView> sdxBasicViewOptional = underTest.getSdxByEnvironmentCrn(ENV_CRN);
        assertTrue(sdxBasicViewOptional.isPresent());
        SdxBasicView sdxBasicView = sdxBasicViewOptional.get();
        assertEquals(PDL_CRN, sdxBasicView.crn());
        assertEquals(DATALAKE_NAME, sdxBasicView.name());
        assertEquals(CDP_RUNTIME, sdxBasicView.runtime());
        assertEquals(CREATED, sdxBasicView.created());
        assertEquals(TargetPlatform.PDL, sdxBasicView.platform());
    }

    @Test
    public void testGetSdxAccessViewByEnvironmentCrnPvcEnvironmentNotAvailable() {
        when(environment.getPvcEnvironmentDetails()).thenReturn(null);
        assertTrue(underTest.getSdxAccessViewByEnvironmentCrn(ENV_CRN).isEmpty());
        verify(environmentEndpoint).getByCrn(ENV_CRN);
        ArgumentCaptor<DescribeRemoteEnvironment> captor = ArgumentCaptor.forClass(DescribeRemoteEnvironment.class);
        verify(remoteEnvironmentEndpoint).getByCrn(captor.capture());
        DescribeRemoteEnvironment describeRemoteEnvironment = captor.getValue();
        assertEquals(PDL_CRN, describeRemoteEnvironment.getCrn());
        verify(pvcEnvironmentDetails, never()).getPrivateDatalakeDetails();
    }

    @Test
    public void testGetSdxAccessViewByEnvironmentPvcDataLakeNotAvailable() {
        when(pvcEnvironmentDetails.getPrivateDatalakeDetails()).thenReturn(null);
        assertTrue(underTest.getSdxAccessViewByEnvironmentCrn(ENV_CRN).isEmpty());
        verify(environmentEndpoint).getByCrn(ENV_CRN);
        ArgumentCaptor<DescribeRemoteEnvironment> captor = ArgumentCaptor.forClass(DescribeRemoteEnvironment.class);
        verify(remoteEnvironmentEndpoint).getByCrn(captor.capture());
        DescribeRemoteEnvironment describeRemoteEnvironment = captor.getValue();
        assertEquals(PDL_CRN, describeRemoteEnvironment.getCrn());
    }

    @Test
    public void testGetSdxAccessViewByEnvironmentCmFqdn() {
        when(privateDatalakeDetails.getCmFQDN()).thenReturn(CM_FQDN);
        when(privateDatalakeDetails.getCmIP()).thenReturn(CM_IP);
        Optional<SdxAccessView> sdxAccessViewOptional = underTest.getSdxAccessViewByEnvironmentCrn(ENV_CRN);
        assertTrue(sdxAccessViewOptional.isPresent());
        SdxAccessView sdxAccessView = sdxAccessViewOptional.get();
        assertEquals(CM_FQDN, sdxAccessView.rangerFqdn());
    }

    @Test
    public void testGetSdxAccessViewByEnvironmentCmIp() {
        when(privateDatalakeDetails.getCmIP()).thenReturn(CM_IP);
        Optional<SdxAccessView> sdxAccessViewOptional = underTest.getSdxAccessViewByEnvironmentCrn(ENV_CRN);
        assertTrue(sdxAccessViewOptional.isPresent());
        SdxAccessView sdxAccessView = sdxAccessViewOptional.get();
        assertEquals(CM_IP, sdxAccessView.rangerFqdn());
    }

    @Test
    void testGetCACertsForEnvironment() {
        when(remoteEnvironmentEndpoint.getRootCertificateByCrn(PDL_CRN)).thenReturn(new GetRootCertificateResponse().contents("certecske"));
        when(environment.getCrn()).thenReturn(PDL_CRN);

        Optional<String> result = underTest.getCACertsForEnvironment(ENV_CRN);

        assertEquals("certecske", result.get());
    }

}
