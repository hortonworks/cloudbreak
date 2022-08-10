package com.sequenceiq.datalake.service.sdx.cert;

import static com.sequenceiq.datalake.service.sdx.SdxService.WORKSPACE_ID_DEFAULT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import javax.ws.rs.WebApplicationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dyngr.exception.PollerStoppedException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorUtil;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.flow.cert.renew.event.SdxStartCertRenewalEvent;
import com.sequenceiq.datalake.service.sdx.CloudbreakPoller;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@ExtendWith(MockitoExtension.class)
public class CertRenewalServiceTest {

    @Mock
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Mock
    private TransactionService transactionService;

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private CloudbreakFlowService cloudbreakFlowService;

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private SdxService sdxService;

    @Mock
    private CloudbreakPoller cloudbreakPoller;

    @Mock
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @InjectMocks
    private CertRenewalService underTest;

    @Mock
    private PollingConfig pollingConfig;

    @Mock
    private SdxCluster sdxCluster;

    @Mock
    private FlowIdentifier flowIdentifier;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @Captor
    private ArgumentCaptor<SdxStartCertRenewalEvent> captor;

    @Test
    public void testFailureHandlrSetsCertenewFailedStatus() {
        underTest.handleFailure(1L, "Error");

        verify(sdxStatusService).setStatusForDatalakeAndNotify(DatalakeStatusEnum.CERT_RENEWAL_FAILED, "Error", 1L);
    }

    @Test
    public void testFinalizeCertRenewal() {
        underTest.finalizeCertRenewal(1L);

        verify(sdxStatusService).setStatusForDatalakeAndNotify(DatalakeStatusEnum.RUNNING, "Datalake cert renewal finished", 1L);
    }

    @Test
    public void testCertRenewalPolling() {
        when(sdxService.getById(anyLong())).thenReturn(sdxCluster);

        underTest.waitForCloudbreakClusterCertRenewal(1L, pollingConfig);

        verify(sdxService).getById(1L);
        verify(cloudbreakPoller).pollUpdateUntilAvailable("Certificate renewal", sdxCluster, pollingConfig);
        verify(sdxStatusService).setStatusForDatalakeAndNotify(DatalakeStatusEnum.CERT_RENEWAL_FINISHED, ResourceEvent.DATALAKE_CERT_RENEWAL_FINISHED,
                "Datalake is running", sdxCluster);
    }

    @Test
    public void testCertRenewalPollingWhenExceptionThrown() {
        when(sdxService.getById(anyLong())).thenReturn(sdxCluster);
        doThrow(new PollerStoppedException("Timeout"))
                .when(cloudbreakPoller).pollUpdateUntilAvailable(anyString(), any(), any());

        assertThrows(PollerStoppedException.class, () -> underTest.waitForCloudbreakClusterCertRenewal(1L, pollingConfig));

        verify(sdxService).getById(1L);
        verify(cloudbreakPoller).pollUpdateUntilAvailable("Certificate renewal", sdxCluster, pollingConfig);
        verifyNoInteractions(sdxStatusService);
    }

    @Test
    public void testRenewCertificate() throws TransactionExecutionException {
        when(sdxCluster.getId()).thenReturn(1L);
        when(sdxCluster.getClusterName()).thenReturn("cluster");
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(transactionService).required(any(Runnable.class));
        doAnswer(invocation -> {
            assertTrue(RegionAwareInternalCrnGeneratorUtil.isInternalCrn(ThreadBasedUserCrnProvider.getUserCrn()));
            return flowIdentifier;
        }).when(stackV4Endpoint).renewCertificate(anyLong(), anyString(), anyString());

        underTest.renewCertificate(sdxCluster, "userCrn");

        verify(stackV4Endpoint).renewCertificate(WORKSPACE_ID_DEFAULT, "cluster", "userCrn");
        verify(cloudbreakFlowService).saveLastCloudbreakFlowChainId(sdxCluster, flowIdentifier);
        verify(sdxStatusService).setStatusForDatalakeAndNotify(DatalakeStatusEnum.CERT_RENEWAL_IN_PROGRESS, "Certificate renewal started",
                1L);
    }

    @Test
    public void testInternalRenewCertificate() throws TransactionExecutionException {
        when(sdxCluster.getId()).thenReturn(1L);
        when(sdxCluster.getStackCrn()).thenReturn("crn");
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(transactionService).required(any(Runnable.class));
        doAnswer(invocation -> {
            assertTrue(RegionAwareInternalCrnGeneratorUtil.isInternalCrn(ThreadBasedUserCrnProvider.getUserCrn()));
            return flowIdentifier;
        }).when(stackV4Endpoint).renewInternalCertificate(anyLong(), anyString());

        underTest.renewInternalCertificate(sdxCluster);

        verify(stackV4Endpoint).renewInternalCertificate(WORKSPACE_ID_DEFAULT, "crn");
        verify(cloudbreakFlowService).saveLastCloudbreakFlowChainId(sdxCluster, flowIdentifier);
        verify(sdxStatusService).setStatusForDatalakeAndNotify(DatalakeStatusEnum.CERT_RENEWAL_IN_PROGRESS, "Certificate renewal started",
                1L);
    }

    @Test
    public void testRenewCertificateNotSetStatusWhenExceptionThrown() throws TransactionExecutionException {
        when(sdxCluster.getClusterName()).thenReturn("cluster");
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(transactionService).required(any(Runnable.class));
        doThrow(new BadRequestException("Can't start."))
                .when(stackV4Endpoint).renewCertificate(anyLong(), anyString(), anyString());

        assertThrows(BadRequestException.class, () -> underTest.renewCertificate(sdxCluster, "userCrn"));

        verify(stackV4Endpoint).renewCertificate(WORKSPACE_ID_DEFAULT, "cluster", "userCrn");
        verifyNoInteractions(cloudbreakFlowService, sdxStatusService);
    }

    @Test
    public void testInternalRenewCertificateNotSetStatusWhenExceptionThrown() throws TransactionExecutionException {
        when(sdxCluster.getStackCrn()).thenReturn("crn");
        when(webApplicationExceptionMessageExtractor.getErrorMessage(any())).thenReturn("cant start");
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        doThrow(new WebApplicationException("Can't start."))
                .when(stackV4Endpoint).renewInternalCertificate(anyLong(), anyString());

        assertThrows(RuntimeException.class, () -> underTest.renewInternalCertificate(sdxCluster));

        verify(stackV4Endpoint).renewInternalCertificate(WORKSPACE_ID_DEFAULT, "crn");
        verifyNoInteractions(cloudbreakFlowService, sdxStatusService);
    }

    @Test
    public void testCertRenewalTriggering() {
        when(sdxCluster.getId()).thenReturn(1L);
        when(sdxCluster.getClusterName()).thenReturn("cluster");

        underTest.triggerRenewCertificate(sdxCluster, "userCrn");

        verify(sdxReactorFlowManager).triggerCertRenewal(captor.capture(), anyString());

        SdxStartCertRenewalEvent renewalEvent = captor.getValue();

        assertEquals("userCrn", renewalEvent.getUserId());
        assertEquals(1L, renewalEvent.getResourceId());
    }

    @Test
    public void testInternalCertRenewalTriggering() {
        when(sdxCluster.getId()).thenReturn(1L);
        when(sdxCluster.getClusterName()).thenReturn("cluster");

        ThreadBasedUserCrnProvider.doAs("userCrn", () -> underTest.triggerInternalRenewCertificate(sdxCluster));

        verify(sdxReactorFlowManager).triggerCertRenewal(captor.capture(), anyString());

        SdxStartCertRenewalEvent renewalEvent = captor.getValue();

        assertEquals("userCrn", renewalEvent.getUserId());
        assertEquals(1L, renewalEvent.getResourceId());
    }
}
