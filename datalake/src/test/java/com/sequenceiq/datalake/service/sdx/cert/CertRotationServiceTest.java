package com.sequenceiq.datalake.service.sdx.cert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;

import javax.ws.rs.WebApplicationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.CertificatesRotationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.CertificatesRotationV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.flow.cert.rotation.event.SdxStartCertRotationEvent;
import com.sequenceiq.datalake.service.sdx.CloudbreakPoller;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

@ExtendWith(MockitoExtension.class)
class CertRotationServiceTest {
    private static final String TEST_USER_CRN = "crn:altus:iam:us-west-1:cloudera:user:testUserCrn";

    private static final PollingConfig POLLING_CONFIG = new PollingConfig(100L, TimeUnit.MILLISECONDS, 300L, TimeUnit.MILLISECONDS);

    @Mock
    private SdxReactorFlowManager flowManager;

    @Mock
    private SdxService sdxService;

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private CloudbreakFlowService cloudbreakFlowService;

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private CloudbreakPoller statusChecker;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @InjectMocks
    private CertRotationService underTest;

    @Test
    public void testFlowTrigger() {
        SdxCluster cluster = new SdxCluster();
        cluster.setId(1L);
        cluster.setClusterName("testclustername");
        CertificatesRotationV4Request request = new CertificatesRotationV4Request();
        ArgumentCaptor<SdxStartCertRotationEvent> eventArgumentCaptor = ArgumentCaptor.forClass(SdxStartCertRotationEvent.class);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, "pollid");
        when(flowManager.triggerCertRotation(eventArgumentCaptor.capture(), anyString())).thenReturn(flowIdentifier);

        FlowIdentifier result = ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.rotateAutoTlsCertificates(cluster, request));

        assertEquals(flowIdentifier, result);
        SdxStartCertRotationEvent event = eventArgumentCaptor.getValue();
        assertEquals(request, event.getRequest());
        assertEquals(1L, event.getResourceId());
        assertEquals(TEST_USER_CRN, event.getUserId());
    }

    @Test
    public void testStartCertRotation() {
        CertificatesRotationV4Request request = new CertificatesRotationV4Request();
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("testclustername");
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(sdxService.getById(1L)).thenReturn(sdxCluster);
        CertificatesRotationV4Response response = new CertificatesRotationV4Response();
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, "pollid");
        response.setFlowIdentifier(flowIdentifier);
        when(stackV4Endpoint.rotateAutoTlsCertificates(0L, sdxCluster.getClusterName(), TEST_USER_CRN, request)).thenReturn(response);

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.startCertRotation(1L, request));

        verify(cloudbreakFlowService).saveLastCloudbreakFlowChainId(sdxCluster, flowIdentifier);
        verify(sdxStatusService).setStatusForDatalakeAndNotify(DatalakeStatusEnum.CERT_ROTATION_IN_PROGRESS, "Datalake cert rotation in progress", sdxCluster);
    }

    @Test
    public void testStartCertRotationWebAppEx() {
        CertificatesRotationV4Request request = new CertificatesRotationV4Request();
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("testclustername");
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(sdxService.getById(1L)).thenReturn(sdxCluster);
        when(stackV4Endpoint.rotateAutoTlsCertificates(0L, sdxCluster.getClusterName(), TEST_USER_CRN, request)).thenThrow(new WebApplicationException("asdf"));

        assertThrows(RuntimeException.class, () -> ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.startCertRotation(1L, request)));

        verifyNoInteractions(cloudbreakFlowService);
        verifyNoInteractions(sdxStatusService);
    }

    @Test
    public void testFinalizeCertRotation() {
        underTest.finalizeCertRotation(1L);

        verify(sdxStatusService).setStatusForDatalakeAndNotify(DatalakeStatusEnum.RUNNING, "Datalake cert rotation finished", 1L);
    }

    @Test
    public void testHandleFailure() {
        Exception exception = new Exception("testMessage");

        underTest.handleFailure(1L, exception);

        verify(sdxStatusService).setStatusForDatalakeAndNotify(DatalakeStatusEnum.CERT_ROTATION_FAILED, exception.getMessage(), 1L);
    }

    @Test
    public void testHandleFailureDefaultMessage() {
        Exception exception = new Exception("");

        underTest.handleFailure(1L, exception);

        verify(sdxStatusService).setStatusForDatalakeAndNotify(DatalakeStatusEnum.CERT_ROTATION_FAILED, "Datalake certificate rotation failed", 1L);
    }

    @Test
    public void testWaitForCloudbreakClusterCertRotation() {
        SdxCluster sdxCluster = new SdxCluster();
        when(sdxService.getById(1L)).thenReturn(sdxCluster);

        underTest.waitForCloudbreakClusterCertRotation(1L, POLLING_CONFIG);

        verify(statusChecker).pollUpdateUntilAvailable(eq("Certificate rotation"), eq(sdxCluster), any());
        verify(sdxStatusService).setStatusForDatalakeAndNotify(DatalakeStatusEnum.CERT_ROTATION_FINISHED, ResourceEvent.SDX_CERT_ROTATION_FINISHED,
                "Datalake is running", sdxCluster);
    }

    @Test
    public void testWaitForCloudbreakClusterCertRotationBreak() {
        SdxCluster sdxCluster = new SdxCluster();
        when(sdxService.getById(1L)).thenReturn(sdxCluster);
        doThrow(new UserBreakException("failure"))
                .when(statusChecker).pollUpdateUntilAvailable(any(), eq(sdxCluster), any());

        assertThrows(UserBreakException.class, () -> underTest.waitForCloudbreakClusterCertRotation(1L, POLLING_CONFIG));

        verifyNoInteractions(sdxStatusService);
    }
}
