package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateType.IMDS_HTTP_TOKEN_REQUIRED;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.function.Supplier;

import jakarta.ws.rs.WebApplicationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.imdupdate.StackInstanceMetadataUpdateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.RdsUpgradeV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

@ExtendWith(MockitoExtension.class)
public class CloudbreakStackServiceTest {

    private static final String SDX_NAME = "sdxName";

    private static final String SDX_ACCOUNT_ID = "sdxAccountId";

    private static final long WORKSPACE_ID = 0L;

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String ERROR_MSG = "error";

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private WebApplicationExceptionMessageExtractor exceptionMessageExtractor;

    @Mock
    private CloudbreakFlowService cloudbreakFlowService;

    @InjectMocks
    private CloudbreakStackService underTest;

    private static RdsUpgradeV4Response setupResponse(FlowIdentifier flowIdentifier) {
        RdsUpgradeV4Response rdsUpgradeV4Response = new RdsUpgradeV4Response();
        rdsUpgradeV4Response.setFlowIdentifier(flowIdentifier);
        return rdsUpgradeV4Response;
    }

    private static SdxCluster setupSdxCluster() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName(SDX_NAME);
        sdxCluster.setAccountId(SDX_ACCOUNT_ID);
        return sdxCluster;
    }

    @Test
    void testGetStack() {
        SdxCluster sdxCluster = setupSdxCluster();
        StackV4Response stackV4Response = new StackV4Response();
        when(stackV4Endpoint.get(WORKSPACE_ID, SDX_NAME, Set.of(), SDX_ACCOUNT_ID)).thenReturn(stackV4Response);

        StackV4Response response = underTest.getStack(sdxCluster);

        assertEquals(stackV4Response, response);
        verify(stackV4Endpoint).get(WORKSPACE_ID, SDX_NAME, Set.of(), SDX_ACCOUNT_ID);
    }

    @Test
    void testGetStackWhenWebApplicationException() {
        SdxCluster sdxCluster = setupSdxCluster();
        when(stackV4Endpoint.get(WORKSPACE_ID, SDX_NAME, Set.of(), SDX_ACCOUNT_ID)).thenThrow(WebApplicationException.class);

        assertThrows(CloudbreakServiceException.class, () ->
                underTest.getStack(sdxCluster)
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testUpgradeRdsByClusterNameInternal(boolean forced) {
        try (MockedStatic<ThreadBasedUserCrnProvider> threadBasedUserCrnProvider = mockStatic(ThreadBasedUserCrnProvider.class)) {
            SdxCluster sdxCluster = setupSdxCluster();
            TargetMajorVersion targetMajorVersion = TargetMajorVersion.VERSION_11;
            FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, "pollableId");
            RdsUpgradeV4Response rdsUpgradeV4Response = setupResponse(flowIdentifier);
            threadBasedUserCrnProvider.when(ThreadBasedUserCrnProvider::getUserCrn).thenReturn(USER_CRN);
            threadBasedUserCrnProvider.when(() -> ThreadBasedUserCrnProvider.doAsInternalActor(any(Supplier.class))).thenReturn(rdsUpgradeV4Response);

            RdsUpgradeV4Response response = underTest.upgradeRdsByClusterNameInternal(sdxCluster, targetMajorVersion, forced);

            assertEquals(rdsUpgradeV4Response, response);
            verify(cloudbreakFlowService).saveLastCloudbreakFlowChainId(sdxCluster, flowIdentifier);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testUpgradeRdsByClusterNameInternalWhenErrorResponse(boolean forced) {
        try (MockedStatic<ThreadBasedUserCrnProvider> threadBasedUserCrnProvider = mockStatic(ThreadBasedUserCrnProvider.class)) {
            SdxCluster sdxCluster = setupSdxCluster();
            TargetMajorVersion targetMajorVersion = TargetMajorVersion.VERSION_11;
            threadBasedUserCrnProvider.when(ThreadBasedUserCrnProvider::getUserCrn).thenReturn(USER_CRN);
            threadBasedUserCrnProvider.when(() -> ThreadBasedUserCrnProvider.doAsInternalActor(any(Supplier.class)))
                    .thenThrow(new WebApplicationException());

            assertThrows(CloudbreakServiceException.class, () ->
                    underTest.upgradeRdsByClusterNameInternal(sdxCluster, targetMajorVersion, forced)
            );

            verify(cloudbreakFlowService, never()).saveLastCloudbreakFlowChainId(any(), any());
        }
    }

    @Test
    void testCheckUpgradeRdsByClusterNameInternal() {
        SdxCluster sdxCluster = setupSdxCluster();
        TargetMajorVersion targetMajorVersion = TargetMajorVersion.VERSION_11;

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.checkUpgradeRdsByClusterNameInternal(sdxCluster, targetMajorVersion));

        verify(stackV4Endpoint).checkUpgradeRdsByClusterNameInternal(WORKSPACE_ID, sdxCluster.getName(), targetMajorVersion, USER_CRN);
    }

    @Test
    void testCheckUpgradeRdsByClusterNameInternalThrowsException() {
        SdxCluster sdxCluster = setupSdxCluster();
        TargetMajorVersion targetMajorVersion = TargetMajorVersion.VERSION_11;
        when(exceptionMessageExtractor.getErrorMessage(any(Exception.class))).thenReturn(ERROR_MSG);
        doThrow(new RuntimeException(ERROR_MSG)).when(stackV4Endpoint)
                .checkUpgradeRdsByClusterNameInternal(WORKSPACE_ID, sdxCluster.getName(), targetMajorVersion, USER_CRN);

        assertThatCode(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.checkUpgradeRdsByClusterNameInternal(sdxCluster, targetMajorVersion)))
                .isInstanceOf(CloudbreakServiceException.class)
                .hasCauseInstanceOf(RuntimeException.class)
                .hasRootCauseMessage(ERROR_MSG)
                .hasMessage("Rds upgrade validation failed: " + ERROR_MSG);
    }

    @ValueSource(booleans = {true, false})
    @ParameterizedTest
    void testUpdateSaltByName(boolean skipHighstate) {
        SdxCluster sdxCluster = setupSdxCluster();

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.updateSaltByName(sdxCluster, skipHighstate));

        verify(stackV4Endpoint).updateSaltByName(WORKSPACE_ID, sdxCluster.getClusterName(), sdxCluster.getAccountId(), skipHighstate);
    }

    @ValueSource(booleans = {true, false})
    @ParameterizedTest
    void testUpdateSaltByNameThrowsError(boolean skipHighstate) {
        SdxCluster sdxCluster = setupSdxCluster();
        when(exceptionMessageExtractor.getErrorMessage(any(WebApplicationException.class))).thenReturn(ERROR_MSG);
        doThrow(new WebApplicationException(ERROR_MSG)).when(stackV4Endpoint)
                .updateSaltByName(WORKSPACE_ID, sdxCluster.getClusterName(), sdxCluster.getAccountId(), skipHighstate);

        assertThatCode(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.updateSaltByName(sdxCluster, skipHighstate)))
                .isInstanceOf(CloudbreakServiceException.class)
                .hasCauseInstanceOf(RuntimeException.class)
                .hasRootCauseMessage(ERROR_MSG)
                .hasMessage("Could not launch Salt update in core, reason: " + ERROR_MSG);
    }

    @Test
    void testImdUpdate() {
        SdxCluster sdxCluster = setupSdxCluster();
        when(stackV4Endpoint.instanceMetadataUpdate(any(), any(), any())).thenReturn(new FlowIdentifier(FlowType.FLOW, "1"));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.updateInstanceMetadata(sdxCluster, IMDS_HTTP_TOKEN_REQUIRED));

        ArgumentCaptor<StackInstanceMetadataUpdateV4Request> captor = ArgumentCaptor.forClass(StackInstanceMetadataUpdateV4Request.class);
        verify(stackV4Endpoint).instanceMetadataUpdate(any(), any(), captor.capture());
        assertEquals(captor.getValue().getUpdateType(), IMDS_HTTP_TOKEN_REQUIRED);
    }

    @Test
    void testImdUpdateFailure() {
        SdxCluster sdxCluster = setupSdxCluster();
        when(exceptionMessageExtractor.getErrorMessage(any(WebApplicationException.class))).thenReturn(ERROR_MSG);
        doThrow(new WebApplicationException(ERROR_MSG)).when(stackV4Endpoint).instanceMetadataUpdate(any(), any(), any());

        assertThatCode(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.updateInstanceMetadata(sdxCluster, IMDS_HTTP_TOKEN_REQUIRED)))
                .isInstanceOf(CloudbreakServiceException.class)
                .hasCauseInstanceOf(RuntimeException.class)
                .hasRootCauseMessage(ERROR_MSG)
                .hasMessage("Could not launch instance metadata update in core, reason: " + ERROR_MSG);
    }

    @Test
    void testUpdatePublicDnsEntriesSuccess() {
        SdxCluster sdxCluster = setupSdxCluster();
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, "1");
        when(stackV4Endpoint.updatePublicDnsEntriesByCrn(any(), any(), any())).thenReturn(flowIdentifier);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.updatePublicDnsEntries(sdxCluster));

        verify(cloudbreakFlowService).saveLastCloudbreakFlowChainId(sdxCluster, flowIdentifier);
    }

    @Test
    void testUpdatePublicDnsEntriesFailure() {
        SdxCluster sdxCluster = setupSdxCluster();
        when(stackV4Endpoint.updatePublicDnsEntriesByCrn(any(), any(), any())).thenThrow(new WebApplicationException(ERROR_MSG));
        when(exceptionMessageExtractor.getErrorMessage(any(WebApplicationException.class))).thenReturn(ERROR_MSG);

        assertThatCode(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.updatePublicDnsEntries(sdxCluster)))
                .isInstanceOf(CloudbreakServiceException.class)
                .hasCauseInstanceOf(WebApplicationException.class)
                .hasRootCauseMessage(ERROR_MSG)
                .hasMessage("Could not update public DNS entries in core, reason: " + ERROR_MSG);

        verifyNoInteractions(cloudbreakFlowService);
    }
}
