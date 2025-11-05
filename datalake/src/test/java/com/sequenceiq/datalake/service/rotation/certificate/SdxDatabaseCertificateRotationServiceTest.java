package com.sequenceiq.datalake.service.rotation.certificate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.AccountIdService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.service.sdx.CloudbreakPoller;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.StackService;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.sdx.api.model.rotaterdscert.SdxRotateRdsCertResponseType;
import com.sequenceiq.sdx.api.model.rotaterdscert.SdxRotateRdsCertificateV1Response;

@ExtendWith(MockitoExtension.class)
class SdxDatabaseCertificateRotationServiceTest {
    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

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
    private CloudbreakFlowService cloudbreakFlowService;

    @Mock
    private StackService stackService;

    @Mock
    private AccountIdService accountIdService;

    @InjectMocks
    private SdxDatabaseCertificateRotationService underTest;

    @Test
    void testRotateCertificateNoSdxCluster() {
        when(sdxService.getByCrn(eq(USER_CRN), eq("stackCrn"))).thenReturn(null);

        SdxRotateRdsCertificateV1Response response = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.rotateCertificate("stackCrn"));

        verify(sdxService, times(1)).getByCrn(eq(USER_CRN), eq("stackCrn"));
        verifyNoMoreInteractions(sdxService);
        verifyNoInteractions(sdxReactorFlowManager);
        assertNotNull(response);
        assertEquals(SdxRotateRdsCertResponseType.SKIP, response.getResponseType());
    }

    @Test
    void testRotateCertificateWhenValidationFailsDueToAccountNotEntitledAndServiceRollingRestartIsNotSupported() {
        String expectedMessage = "Uh-oh, it could not be triggered";
        SdxCluster sdxCluster = mock(SdxCluster.class);
        when(sdxService.getByCrn(eq(USER_CRN), eq("stackCrn"))).thenReturn(sdxCluster);
        doThrow(new BadRequestException(expectedMessage)).when(stackService).validateRdsSslCertRotation(anyString());

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.rotateCertificate("stackCrn")));

        assertEquals(expectedMessage, exception.getMessage());
        verify(stackService, times(1)).validateRdsSslCertRotation(anyString());
        verifyNoMoreInteractions(sdxService);
        verifyNoInteractions(sdxReactorFlowManager);
    }

    @Test
    void testRotateCertificateStackNotFoundOnCoreSide() {
        SdxCluster sdxCluster = mock(SdxCluster.class);
        String stackCrn = "crn:cdp:datalake:us-west-1:cloudera:datalake:6b2b1600-8ac6-4c26-aa34-dab36f4bd243";
        when(sdxCluster.getClusterName()).thenReturn("clusterName");
        when(sdxService.getByCrn(eq(USER_CRN), eq(stackCrn))).thenReturn(sdxCluster);
        when(stackService.getDetail(anyString(), any(), any())).thenReturn(null);

        SdxRotateRdsCertificateV1Response response = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.rotateCertificate(stackCrn));

        assertNotNull(response);
        assertEquals(SdxRotateRdsCertResponseType.ERROR, response.getResponseType());
    }

    @Test
    void testRotateCertificateStackNotAvailable() {
        String dlCrn = "crn:cdp:datalake:us-west-1:cloudera:datalake:6b2b1600-8ac6-4c26-aa34-dab36f4bd243";
        SdxCluster sdxCluster = mock(SdxCluster.class);
        when(sdxCluster.getClusterName()).thenReturn("clusterName");
        when(sdxService.getByCrn(any(), eq(dlCrn))).thenReturn(sdxCluster);

        StackV4Response stack = mock(StackV4Response.class);
        when(stack.getStatus()).thenReturn(Status.AMBIGUOUS);
        when(stackService.getDetail(anyString(), any(), any())).thenReturn(stack);

        SdxRotateRdsCertificateV1Response response = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.rotateCertificate(dlCrn));

        assertNotNull(response);
        assertEquals(SdxRotateRdsCertResponseType.ERROR, response.getResponseType());
    }

    @Test
    void testInitAndWaitForStackCertificateRotation() {
        SdxCluster sdxCluster = mock(SdxCluster.class);
        when(sdxCluster.getStackCrn()).thenReturn("stackCrn");
        when(sdxCluster.getName()).thenReturn("datalakeName");
        PollingConfig pollingConfig = mock(PollingConfig.class);
        StackRotateRdsCertificateV4Response stackRotateRdsCertificateV4Response = mock(StackRotateRdsCertificateV4Response.class);
        when(stackRotateRdsCertificateV4Response.getFlowIdentifier()).thenReturn(mock(FlowIdentifier.class));
        when(stackV4Endpoint.rotateRdsCertificateByCrnInternal(anyLong(), anyString(), any()))
                .thenReturn(stackRotateRdsCertificateV4Response);

        underTest.initAndWaitForStackCertificateRotation(sdxCluster, pollingConfig);

        verify(cloudbreakFlowService).saveLastCloudbreakFlowChainId(eq(sdxCluster), any());
        verify(cloudbreakPoller).pollCertificateRotationUntilAvailable(eq(sdxCluster), eq(pollingConfig));
    }
}