package com.sequenceiq.datalake.service.rotation.certificate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.StackDatabaseServerResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.rotaterdscert.StackRotateRdsCertificateV4Response;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.service.EnvironmentClientService;
import com.sequenceiq.datalake.service.sdx.CloudbreakPoller;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXDatabaseServerV1Endpoint;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslCertificateEntryResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.support.SupportV4Endpoint;
import com.sequenceiq.sdx.api.model.rotaterdscert.SdxRotateRdsCertResponseType;
import com.sequenceiq.sdx.api.model.rotaterdscert.SdxRotateRdsCertificateV1Response;

@ExtendWith(MockitoExtension.class)
class SdxDatabaseCertificateRotationServiceTest {

    @Mock
    private EnvironmentClientService environmentService;

    @Mock
    private SdxService sdxService;

    @Mock
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Mock
    private CloudbreakMessagesService messagesService;

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private CloudbreakPoller cloudbreakPoller;

    @Mock
    private DistroXV1Endpoint distroXV1Endpoint;

    @Mock
    private DistroXDatabaseServerV1Endpoint distroXDatabaseServerV1Endpoint;

    @Mock
    private SupportV4Endpoint supportV4Endpoint;

    @Mock
    private CloudbreakFlowService cloudbreakFlowService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @InjectMocks
    private SdxDatabaseCertificateRotationService sdxDatabaseCertificateRotationService;

    @Test
    void testRotateCertificateNoSdxCluster() {
        when(sdxService.getByCrn(any(), eq("stackCrn"))).thenReturn(null);

        // When
        SdxRotateRdsCertificateV1Response response = sdxDatabaseCertificateRotationService.rotateCertificate("stackCrn");

        // Then
        assertNotNull(response);
        assertEquals(SdxRotateRdsCertResponseType.SKIP, response.getResponseType());
    }

    @Test
    void testRotateCertificateStackNotFoundOnCoreSide() {
        String dlCrn = "dummyCrn";
        SdxCluster sdxCluster = mock(SdxCluster.class);
        when(sdxCluster.getClusterName()).thenReturn("clusterName");
        when(sdxService.getByCrn(any(), eq(dlCrn))).thenReturn(sdxCluster);

        when(sdxService.getDetail(anyString(), any(), any())).thenReturn(null);

        SdxRotateRdsCertificateV1Response response = sdxDatabaseCertificateRotationService.rotateCertificate(dlCrn);

        assertNotNull(response);
        assertEquals(SdxRotateRdsCertResponseType.ERROR, response.getResponseType());
    }

    @Test
    void testRotateCertificateSDXWhenRelatedDatahubsHaveLatestAndOutdatedCerts() {
        String latestCert = "latestCert";
        String dlCrn = "dummyCrn";
        setupSDXAndDatahubFetch(dlCrn, latestCert);

        StackV4Response embedded1StackResponse = mock(StackV4Response.class, Answers.RETURNS_DEEP_STUBS);
        when(embedded1StackResponse.getCluster().getDbSslRootCertBundle()).thenReturn(latestCert);
        when(distroXV1Endpoint.getByCrn(eq("crn1"), any())).thenReturn(embedded1StackResponse);
        StackV4Response embedded2StackResponse = mock(StackV4Response.class, Answers.RETURNS_DEEP_STUBS);
        when(embedded2StackResponse.getCluster().getDbSslRootCertBundle()).thenReturn("outDatedCert");
        when(distroXV1Endpoint.getByCrn(eq("crn2"), any())).thenReturn(embedded2StackResponse);

        StackDatabaseServerResponse outdatedDatabaseServerResponse = mock(StackDatabaseServerResponse.class, Answers.RETURNS_DEEP_STUBS);
        when(outdatedDatabaseServerResponse.getSslConfig().getSslCertificateActiveVersion()).thenReturn(2);
        when(distroXDatabaseServerV1Endpoint.getDatabaseServerByCrn("crn3")).thenReturn(outdatedDatabaseServerResponse);
        StackDatabaseServerResponse latestDatabaseServerResponse = mock(StackDatabaseServerResponse.class, Answers.RETURNS_DEEP_STUBS);
        when(latestDatabaseServerResponse.getSslConfig().getSslCertificateActiveVersion()).thenReturn(3);
        when(distroXDatabaseServerV1Endpoint.getDatabaseServerByCrn("crn4")).thenReturn(latestDatabaseServerResponse);

        SdxRotateRdsCertificateV1Response response = sdxDatabaseCertificateRotationService.rotateCertificate(dlCrn);

        assertNotNull(response);
        assertEquals(SdxRotateRdsCertResponseType.ERROR, response.getResponseType());
        assertEquals("Data Hub with name: 'embedded2, external1' is not on the latest certificate version. " +
                "Please update certificate on the Data Hub side before update the Data Lake", response.getReason());
    }

    @Test
    void testRotateCertificateSDXWhenRelatedDatahubsHaveLatestCerts() {
        String latestCert = "latestCert";
        String dlCrn = "dummyCrn";
        setupSDXAndDatahubFetch(dlCrn, latestCert);

        StackV4Response embedded1StackResponse = mock(StackV4Response.class, Answers.RETURNS_DEEP_STUBS);
        when(embedded1StackResponse.getCluster().getDbSslRootCertBundle()).thenReturn(latestCert);
        when(distroXV1Endpoint.getByCrn(anyString(), any())).thenReturn(embedded1StackResponse);

        StackDatabaseServerResponse latestDatabaseServerResponse = mock(StackDatabaseServerResponse.class, Answers.RETURNS_DEEP_STUBS);
        when(latestDatabaseServerResponse.getSslConfig().getSslCertificateActiveVersion()).thenReturn(3);
        when(distroXDatabaseServerV1Endpoint.getDatabaseServerByCrn(anyString())).thenReturn(latestDatabaseServerResponse);

        SdxRotateRdsCertificateV1Response response = sdxDatabaseCertificateRotationService.rotateCertificate(dlCrn);

        assertNotNull(response);
        assertEquals(SdxRotateRdsCertResponseType.TRIGGERED, response.getResponseType());
        verify(distroXV1Endpoint, times(2)).getByCrn(anyString(), any());
        verify(distroXDatabaseServerV1Endpoint, times(2)).getDatabaseServerByCrn(anyString());
    }

    @Test
    void testRotateCertificateStackNotAvailable() {
        String dlCrn = "dummyCrn";
        SdxCluster sdxCluster = mock(SdxCluster.class);
        when(sdxCluster.getClusterName()).thenReturn("clusterName");
        when(sdxService.getByCrn(any(), eq(dlCrn))).thenReturn(sdxCluster);

        StackV4Response stack = mock(StackV4Response.class);
        when(stack.getStatus()).thenReturn(Status.AMBIGUOUS);
        when(sdxService.getDetail(anyString(), any(), any())).thenReturn(stack);

        SdxRotateRdsCertificateV1Response response = sdxDatabaseCertificateRotationService.rotateCertificate(dlCrn);

        assertNotNull(response);
        assertEquals(SdxRotateRdsCertResponseType.ERROR, response.getResponseType());
    }

    @Test
    void testInitAndWaitForStackCertificateRotation() {
        SdxCluster sdxCluster = mock(SdxCluster.class);
        when(sdxCluster.getStackCrn()).thenReturn("stackCrn");
        when(sdxCluster.getName()).thenReturn("datalakeName");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        PollingConfig pollingConfig = mock(PollingConfig.class);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString())
                .thenReturn("internalCrn");
        StackRotateRdsCertificateV4Response stackRotateRdsCertificateV4Response = mock(StackRotateRdsCertificateV4Response.class);
        when(stackRotateRdsCertificateV4Response.getFlowIdentifier()).thenReturn(mock(FlowIdentifier.class));
        when(stackV4Endpoint.rotateRdsCertificateByCrnInternal(anyLong(), anyString(), any()))
                .thenReturn(stackRotateRdsCertificateV4Response);

        sdxDatabaseCertificateRotationService.initAndWaitForStackCertificateRotation(sdxCluster, pollingConfig);

        verify(cloudbreakFlowService).saveLastCloudbreakFlowChainId(eq(sdxCluster), any());
        verify(cloudbreakPoller).pollCertificateRotationUntilAvailable(eq(sdxCluster), eq(pollingConfig));
    }

    private void setupSDXAndDatahubFetch(String dlCrn, String latestCert) {
        SdxCluster sdxCluster = mock(SdxCluster.class);
        when(sdxCluster.getClusterName()).thenReturn("clusterName");
        when(sdxService.getByCrn(any(), eq(dlCrn))).thenReturn(sdxCluster);

        StackV4Response stack = mock(StackV4Response.class);
        ClusterV4Response cluster = mock(ClusterV4Response.class);
        when(stack.getStatus()).thenReturn(Status.AVAILABLE);
        when(stack.getEnvironmentCrn()).thenReturn("envCrn");
        when(stack.getCloudPlatform()).thenReturn(CloudPlatform.MOCK);
        when(stack.getRegion()).thenReturn("region");
        when(stack.getStatus()).thenReturn(Status.AVAILABLE);
        when(cluster.getDatabaseServerCrn()).thenReturn("crn:cdp:redbeams:us-west-1:cloudera:databaseServer:c4a67c2c-3090-4e15-b740-5caa7204c5d2");
        when(stack.getCluster()).thenReturn(cluster);
        when(sdxService.getDetail(anyString(), any(), any())).thenReturn(stack);

        when(regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString()).thenReturn("internalCrn");
        SslCertificateEntryResponse certificateEntryResponse = mock(SslCertificateEntryResponse.class);
        when(certificateEntryResponse.getVersion()).thenReturn(3);
        when(certificateEntryResponse.getCertPem()).thenReturn(latestCert);
        when(supportV4Endpoint.getLatestCertificate(anyString(), anyString())).thenReturn(certificateEntryResponse);

        StackViewV4Responses stackV4Responses = mock(StackViewV4Responses.class);
        StackViewV4Response embWithLatestCert = createStackViewV4ResponseMock("embedded1", "crn1", true);
        StackViewV4Response embWithOutdatedCert = createStackViewV4ResponseMock("embedded2", "crn2", true);
        StackViewV4Response extWitOutdatedCert = createStackViewV4ResponseMock("external1", "crn3", false);
        StackViewV4Response extWithLatestCert = createStackViewV4ResponseMock("external2", "crn4", false);
        when(stackV4Responses.getResponses()).thenReturn(List.of(embWithLatestCert, embWithOutdatedCert, extWitOutdatedCert, extWithLatestCert));
        when(distroXV1Endpoint.list(eq(null), eq("envCrn"))).thenReturn(stackV4Responses);
    }

    private StackViewV4Response createStackViewV4ResponseMock(String name, String crn, boolean embedded) {
        StackViewV4Response stackViewV4ResponseMock = mock(StackViewV4Response.class, Answers.RETURNS_DEEP_STUBS);
        lenient().when(stackViewV4ResponseMock.getName()).thenReturn(name);
        when(stackViewV4ResponseMock.getCrn()).thenReturn(crn);
        when(stackViewV4ResponseMock.getExternalDatabase().getAvailabilityType().isEmbedded()).thenReturn(embedded);
        return stackViewV4ResponseMock;
    }
}