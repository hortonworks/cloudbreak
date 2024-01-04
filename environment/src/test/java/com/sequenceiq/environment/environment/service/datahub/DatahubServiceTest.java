package com.sequenceiq.environment.environment.service.datahub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
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

    @Mock
    private DistroXV1Endpoint distroXV1Endpoint;

    @Mock
    private DistroXUpgradeV1Endpoint distroXUpgradeV1Endpoint;

    @Mock
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @InjectMocks
    private DatahubService underTest;

    @Mock
    private FlowIdentifier flowIdentifier;

    @BeforeEach
    void setUp() {
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn(INTERNAL_USER_CRN);
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

}
