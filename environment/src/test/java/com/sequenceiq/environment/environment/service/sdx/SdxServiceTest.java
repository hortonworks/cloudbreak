package com.sequenceiq.environment.environment.service.sdx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;

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
import com.sequenceiq.environment.exception.SdxOperationFailedException;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.sdx.api.endpoint.OperationEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxInternalEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxUpgradeEndpoint;

@ExtendWith(MockitoExtension.class)
class SdxServiceTest {

    private static final String INTERNAL_USER_CRN = "internal-crn";

    private static final String CURRENT_USER_CRN = "crn:cdp:iam:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:user:f3b8ed82-e712-4f89-bda7-be07183720d3";

    private static final String SDX_CRN = "sdx-crn";

    private static final String PREVIOUS_PROXY_CRN = "prev-proxy-crn";

    private static final String EXTRACTED_MESSAGE = "extracted-message";

    @InjectMocks
    private SdxService underTest;

    @Mock
    private SdxEndpoint sdxEndpoint;

    @Mock
    private SdxUpgradeEndpoint sdxUpgradeEndpoint;

    @Mock
    private SdxInternalEndpoint sdxInternalEndpoint;

    @Mock
    private OperationEndpoint sdxOperationEndpoint;

    @Mock
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @Mock
    private FlowIdentifier flowIdentifier;

    @BeforeEach
    void setUp() {
        lenient().when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        lenient().when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn(INTERNAL_USER_CRN);
        lenient().when(webApplicationExceptionMessageExtractor.getErrorMessage(any())).thenReturn(EXTRACTED_MESSAGE);
    }

    @Test
    void modifyProxySuccess() {
        when(sdxInternalEndpoint.modifyProxy(SDX_CRN, PREVIOUS_PROXY_CRN, CURRENT_USER_CRN)).thenReturn(flowIdentifier);
        FlowIdentifier result = doAsCurrentUserCrn(() -> underTest.modifyProxy(SDX_CRN, PREVIOUS_PROXY_CRN));

        assertThat(result).isEqualTo(flowIdentifier);
        verify(sdxInternalEndpoint).modifyProxy(SDX_CRN, PREVIOUS_PROXY_CRN, CURRENT_USER_CRN);
    }

    @Test
    void modifyProxyFailure() {
        WebApplicationException cause = new WebApplicationException("cause");
        when(sdxInternalEndpoint.modifyProxy(SDX_CRN, PREVIOUS_PROXY_CRN, CURRENT_USER_CRN)).thenThrow(cause);

        assertThatThrownBy(() -> doAsCurrentUserCrn(() -> underTest.modifyProxy(SDX_CRN, PREVIOUS_PROXY_CRN)))
                .isInstanceOf(SdxOperationFailedException.class)
                .hasCause(cause)
                .hasMessage(EXTRACTED_MESSAGE);
        verify(webApplicationExceptionMessageExtractor).getErrorMessage(cause);
    }

    private <T> T doAsCurrentUserCrn(Supplier<T> callable) {
        return ThreadBasedUserCrnProvider.doAs(CURRENT_USER_CRN, callable);
    }

}
