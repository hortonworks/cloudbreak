package com.sequenceiq.datalake.service.upgrade.recovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import javax.ws.rs.WebApplicationException;

import org.assertj.core.util.Strings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recovery.RecoveryStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recovery.RecoveryValidationV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.exception.CloudbreakApiException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.sdx.api.model.SdxRecoveryRequest;
import com.sequenceiq.sdx.api.model.SdxRecoveryResponse;
import com.sequenceiq.sdx.api.model.SdxRecoveryType;

@ExtendWith(MockitoExtension.class)
public class SdxUpgradeRecoveryServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:cloudera:user:bob@cloudera.com";

    private static final String CLUSTER_NAME = "dummyCluster";

    private static final long WORKSPACE_ID = 0L;

    @Mock
    private SdxService sdxService;

    @Mock
    private SdxCluster cluster;

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Mock
    private WebApplicationExceptionMessageExtractor exceptionMessageExtractor;

    @InjectMocks
    private SdxUpgradeRecoveryService underTest;

    private SdxRecoveryRequest request;

    private String userCrn;

    @BeforeEach
    public void setup() {
        userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        if (Strings.isNullOrEmpty(userCrn)) {
            ThreadBasedUserCrnProvider.setUserCrn(USER_CRN);
            userCrn = USER_CRN;
        }
        request = new SdxRecoveryRequest();
        request.setType(SdxRecoveryType.RECOVER_WITHOUT_DATA);
        when(cluster.getClusterName()).thenReturn(CLUSTER_NAME);
        when(sdxService.getByNameOrCrn(userCrn, NameOrCrn.ofName(CLUSTER_NAME))).thenReturn(cluster);
    }

    @Test
    public void testGetClusterRecoverableByNameInternalThrowsExceptionShouldThrowApiException() {

        WebApplicationException webApplicationException = new WebApplicationException();
        doThrow(webApplicationException).when(stackV4Endpoint).getClusterRecoverableByNameInternal(WORKSPACE_ID, CLUSTER_NAME, userCrn);
        when(exceptionMessageExtractor.getErrorMessage(webApplicationException)).thenReturn("web-error");

        CloudbreakApiException actual = Assertions.assertThrows(CloudbreakApiException.class,
                () -> underTest.triggerRecovery(userCrn, NameOrCrn.ofName(CLUSTER_NAME), request));
        Assertions.assertEquals("Stack recovery status validation failed on cluster: [dummyCluster]. Message: [web-error]", actual.getMessage());
    }

    @Test
    public void testNonRecoverableStatusShouldThrowBadRequestException() {
        String errorMessage = "error message";
        RecoveryValidationV4Response recoveryV4Response = new RecoveryValidationV4Response(errorMessage, RecoveryStatus.NON_RECOVERABLE);

        when(stackV4Endpoint.getClusterRecoverableByNameInternal(WORKSPACE_ID, CLUSTER_NAME, userCrn)).thenReturn(recoveryV4Response);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.triggerRecovery(userCrn, NameOrCrn.ofName(CLUSTER_NAME), request));
        assertEquals(errorMessage, exception.getMessage());
    }

    @Test
    public void testValidateStatusSuccessfulShouldStartRecoveryFlow() {
        String reason = "Datalake upgrade recovery requested. Cluster will be terminated and re-launched with the original runtime.";
        RecoveryValidationV4Response recoveryV4Response = new RecoveryValidationV4Response(reason, RecoveryStatus.RECOVERABLE);

        when(stackV4Endpoint.getClusterRecoverableByNameInternal(WORKSPACE_ID, CLUSTER_NAME, userCrn)).thenReturn(recoveryV4Response);
        when(sdxReactorFlowManager.triggerDatalakeRuntimeRecoveryFlow(cluster, SdxRecoveryType.RECOVER_WITHOUT_DATA))
                .thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));

        SdxRecoveryResponse response = underTest.triggerRecovery(userCrn, NameOrCrn.ofName(CLUSTER_NAME), request);
        assertEquals(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"), response.getFlowIdentifier());
    }
}