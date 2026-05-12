package com.sequenceiq.environment.environment.service.datahub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import jakarta.ws.rs.WebApplicationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Responses;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXInternalV1Endpoint;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXUpgradeV1Endpoint;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.environment.exception.DatahubOperationFailedException;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@ExtendWith(MockitoExtension.class)
class DatahubServiceTest {

    public static final String INTERNAL_USER_CRN = "internal-user-crn";

    public static final String INITIATOR_USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String DH_CRN_1 = "crn-1";

    private static final String EXTRACTED_ERROR = "extracted-error";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:1234:environment:e1";

    @Mock
    private DistroXV1Endpoint distroXV1Endpoint;

    @Mock
    private DistroXInternalV1Endpoint distroXInternalV1Endpoint;

    @Mock
    private DistroXUpgradeV1Endpoint distroXUpgradeV1Endpoint;

    @Mock
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @InjectMocks
    private DatahubService underTest;

    @Mock
    private FlowIdentifier flowIdentifier;

    @BeforeEach
    void setUp() {
        lenient().when(webApplicationExceptionMessageExtractor.getErrorMessage(any())).thenReturn(EXTRACTED_ERROR);
    }

    @Test
    void modifyProxySuccess() {
        String previousProxyConfigCrn = "prev-proxy-crn";
        when(distroXV1Endpoint.modifyProxyInternal(DH_CRN_1, previousProxyConfigCrn, INITIATOR_USER_CRN)).thenReturn(flowIdentifier);

        FlowIdentifier result = ThreadBasedUserCrnProvider.doAs(INITIATOR_USER_CRN, () -> underTest.modifyProxy(DH_CRN_1, previousProxyConfigCrn));

        assertThat(result).isEqualTo(flowIdentifier);
        verify(distroXV1Endpoint).modifyProxyInternal(DH_CRN_1, previousProxyConfigCrn, INITIATOR_USER_CRN);
    }

    @Test
    void modifyProxyFailure() {
        String previousProxyConfigCrn = "prev-proxy-crn";
        WebApplicationException cause = new WebApplicationException("cause");
        when(distroXV1Endpoint.modifyProxyInternal(eq(DH_CRN_1), eq(previousProxyConfigCrn), any())).thenThrow(cause);

        assertThatThrownBy(() -> underTest.modifyProxy(DH_CRN_1, previousProxyConfigCrn))
                .isInstanceOf(DatahubOperationFailedException.class)
                .hasMessage("Failed to trigger modify proxy config for Data Hub CRN '%s' due to '%s'.", DH_CRN_1, EXTRACTED_ERROR);
    }

    @Test
    void getStatusesByEnvironmentCrnSuccess() {
        StackStatusV4Response statusResponse = new StackStatusV4Response();
        statusResponse.setCrn(DH_CRN_1);
        StackStatusV4Responses expected = new StackStatusV4Responses(Set.of(statusResponse));
        when(distroXInternalV1Endpoint.getStatusByEnvironmentCrn(ENV_CRN)).thenReturn(expected);

        StackStatusV4Responses result = ThreadBasedUserCrnProvider.doAs(INITIATOR_USER_CRN, () -> underTest.getStatusesByEnvironmentCrn(ENV_CRN));

        assertThat(result).isSameAs(expected);
        verify(distroXInternalV1Endpoint).getStatusByEnvironmentCrn(ENV_CRN);
    }

    @Test
    void getStatusesByEnvironmentCrnFailure() {
        WebApplicationException cause = new WebApplicationException("cause");
        when(distroXInternalV1Endpoint.getStatusByEnvironmentCrn(ENV_CRN)).thenThrow(cause);

        assertThatThrownBy(() -> ThreadBasedUserCrnProvider.doAs(INITIATOR_USER_CRN, () -> underTest.getStatusesByEnvironmentCrn(ENV_CRN)))
                .isInstanceOf(DatahubOperationFailedException.class)
                .hasCause(cause)
                .hasMessage(EXTRACTED_ERROR);
    }

}
