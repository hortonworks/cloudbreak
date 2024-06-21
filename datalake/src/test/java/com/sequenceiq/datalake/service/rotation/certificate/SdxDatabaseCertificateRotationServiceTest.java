package com.sequenceiq.datalake.service.rotation.certificate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.rotaterdscert.StackRotateRdsCertificateV4Response;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
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

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @InjectMocks
    private SdxDatabaseCertificateRotationService sdxDatabaseCertificateRotationService;

    @Test
    public void testRotateCertificateNoSdxCluster() {
        when(sdxService.getByCrn(any(), eq("stackCrn"))).thenReturn(null);

        // When
        SdxRotateRdsCertificateV1Response response = sdxDatabaseCertificateRotationService.rotateCertificate("stackCrn");

        // Then
        assertNotNull(response);
        assertEquals(SdxRotateRdsCertResponseType.SKIP, response.getResponseType());
    }

    @Test
    public void testRotateCertificateStackNotAvailable() {
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
    public void testInitAndWaitForStackCertificateRotation() {
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
}