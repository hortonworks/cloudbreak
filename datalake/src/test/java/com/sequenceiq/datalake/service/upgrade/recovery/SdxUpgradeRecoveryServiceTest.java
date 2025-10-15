package com.sequenceiq.datalake.service.upgrade.recovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Optional;

import jakarta.ws.rs.WebApplicationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recovery.RecoveryStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recovery.RecoveryValidationV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.service.sdx.dr.SdxBackupRestoreService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.sdx.api.model.SdxRecoverableResponse;
import com.sequenceiq.sdx.api.model.SdxRecoveryRequest;
import com.sequenceiq.sdx.api.model.SdxRecoveryResponse;
import com.sequenceiq.sdx.api.model.SdxRecoveryType;

@ExtendWith(MockitoExtension.class)
public class SdxUpgradeRecoveryServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:cloudera:user:bob@cloudera.com";

    private static final String ACCOUNT_ID = "cloudera";

    private static final String CLUSTER_NAME = "dummyCluster";

    private static final long WORKSPACE_ID = 0L;

    private static final String RUNTIME = "7.2.2";

    @Mock
    private SdxCluster cluster;

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Mock
    private SdxBackupRestoreService backupRestoreService;

    @Mock
    private WebApplicationExceptionMessageExtractor exceptionMessageExtractor;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private SdxUpgradeRecoveryService underTest;

    private SdxRecoveryRequest request;

    @BeforeEach
    public void setup() {
        request = new SdxRecoveryRequest();
        request.setType(SdxRecoveryType.RECOVER_WITHOUT_DATA);
        when(cluster.getClusterName()).thenReturn(CLUSTER_NAME);
        lenient().when(cluster.getAccountId()).thenReturn(ACCOUNT_ID);
    }

    @Test
    public void testGetClusterRecoverableByNameInternalThrowsExceptionShouldThrowApiException() {
        WebApplicationException webApplicationException = new WebApplicationException();
        doThrow(webApplicationException).when(stackV4Endpoint).getClusterRecoverableByNameInternal(WORKSPACE_ID, CLUSTER_NAME, USER_CRN);
        when(exceptionMessageExtractor.getErrorMessage(webApplicationException)).thenReturn("web-error");

        CloudbreakServiceException actual = assertThrows(CloudbreakServiceException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validateRecovery(cluster)));
        assertEquals("Stack recovery validation failed on cluster: [dummyCluster]. Message: [web-error]", actual.getMessage());
    }

    @Test
    public void testNonRecoverableStatusShouldReturnNonRecoverable() {
        String errorMessage = "error message";

        RecoveryValidationV4Response recoveryV4Response = new RecoveryValidationV4Response(errorMessage, RecoveryStatus.NON_RECOVERABLE);

        when(stackV4Endpoint.getClusterRecoverableByNameInternal(WORKSPACE_ID, CLUSTER_NAME, USER_CRN)).thenReturn(recoveryV4Response);

        SdxRecoverableResponse sdxRecoverableResponse =
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validateRecovery(cluster));
        assertEquals(errorMessage, sdxRecoverableResponse.getReason());
    }

    @Test
    public void testValidateStatusSuccessfulShouldStartRecoveryFlow() {
        String reason = "Datalake upgrade recovery requested. Cluster will be terminated and re-launched with the original runtime.";
        RecoveryValidationV4Response recoveryV4Response = new RecoveryValidationV4Response(reason, RecoveryStatus.RECOVERABLE);

        when(stackV4Endpoint.getClusterRecoverableByNameInternal(WORKSPACE_ID, CLUSTER_NAME, USER_CRN)).thenReturn(recoveryV4Response);
        when(sdxReactorFlowManager.triggerDatalakeRuntimeRecoveryFlow(cluster, SdxRecoveryType.RECOVER_WITHOUT_DATA))
                .thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));
        when(entitlementService.isUpgradeRecoveryEnabled(ACCOUNT_ID)).thenReturn(true);

        SdxRecoverableResponse sdxRecoverableResponse = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.validateRecovery(cluster));
        assertEquals(reason, sdxRecoverableResponse.getReason());

        SdxRecoveryResponse response = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.triggerRecovery(cluster, request));
        assertEquals(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"), response.getFlowIdentifier());
    }

    @Test
    public void testValidateWithDataAndSuccessfulBackupShouldStartRecoveryFlow() {
        String reason = "There is no successful backup taken yet for data lake cluster with runtime 7.2.2.";
        RecoveryValidationV4Response recoveryV4Response = new RecoveryValidationV4Response(reason, RecoveryStatus.RECOVERABLE);

        request.setType(SdxRecoveryType.RECOVER_WITH_DATA);
        datalakeDRProto.DatalakeBackupInfo datalakeBackupInfo = datalakeDRProto.DatalakeBackupInfo
                .newBuilder()
                .setRuntimeVersion(RUNTIME)
                .setOverallState("SUCCESSFUL")
                .build();

        when(cluster.getRuntime()).thenReturn(RUNTIME);
        when(stackV4Endpoint.getClusterRecoverableByNameInternal(WORKSPACE_ID, CLUSTER_NAME, USER_CRN)).thenReturn(recoveryV4Response);
        when(backupRestoreService.getLastSuccessfulBackupInfoWithRuntime(CLUSTER_NAME, USER_CRN, RUNTIME)).thenReturn(Optional.of(datalakeBackupInfo));
        when(sdxReactorFlowManager.triggerDatalakeRuntimeRecoveryFlow(cluster, SdxRecoveryType.RECOVER_WITH_DATA))
                .thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));
        when(entitlementService.isUpgradeRecoveryEnabled(ACCOUNT_ID)).thenReturn(true);

        SdxRecoverableResponse sdxRecoverableResponse = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.validateRecovery(cluster, request));
        assertEquals(reason, sdxRecoverableResponse.getReason());

        SdxRecoveryResponse response = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.triggerRecovery(cluster, request));
        assertEquals(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"), response.getFlowIdentifier());
    }

    @Test
    public void testValidateWithDataAndNonExistentBackupShouldThrowValidationError() {
        String errorMessage = "There is no successful backup taken yet for data lake cluster with runtime 7.2.2.";

        request.setType(SdxRecoveryType.RECOVER_WITH_DATA);

        when(cluster.getRuntime()).thenReturn(RUNTIME);
        when(backupRestoreService.getLastSuccessfulBackupInfoWithRuntime(CLUSTER_NAME, USER_CRN, RUNTIME)).thenReturn(Optional.empty());

        SdxRecoverableResponse sdxRecoverableResponse = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.validateRecovery(cluster, request));
        assertEquals(errorMessage, sdxRecoverableResponse.getReason());
    }

    @Test
    public void testValidateStatusSuccessfulMissingEntitlementShouldFail() {
        String reason = "Datalake upgrade recovery requested. Cluster will be terminated and re-launched with the original runtime.";
        RecoveryValidationV4Response recoveryV4Response = new RecoveryValidationV4Response(reason, RecoveryStatus.RECOVERABLE);

        when(stackV4Endpoint.getClusterRecoverableByNameInternal(WORKSPACE_ID, CLUSTER_NAME, USER_CRN)).thenReturn(recoveryV4Response);
        when(entitlementService.isUpgradeRecoveryEnabled(ACCOUNT_ID)).thenReturn(false);

        SdxRecoverableResponse sdxRecoverableResponse = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.validateRecovery(cluster));
        assertEquals("Missing CDP_CB_UPGRADE_RECOVERY entitlement. Please contact support.", sdxRecoverableResponse.getReason());
        assertTrue(sdxRecoverableResponse.getStatus().nonRecoverable());
    }
}
