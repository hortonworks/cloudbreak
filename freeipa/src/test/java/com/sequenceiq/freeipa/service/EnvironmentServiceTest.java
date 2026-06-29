package com.sequenceiq.freeipa.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.util.TestConstants;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentEditRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
class EnvironmentServiceTest {

    private static final String USER_CRN = TestConstants.CRN;

    private static final String ENV_CRN = "envCrn";

    private static final String PLATFORM_VARIANT = "AWS_NATIVE";

    private static final int NODE_COUNT = 3;

    @Mock
    private EnvironmentEndpoint environmentEndpoint;

    @Mock
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @InjectMocks
    private EnvironmentService underTest;

    @Test
    void testSetFreeIpaPlatformVariantSendsEditRequestWithVariant() {
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.setFreeIpaPlatformVariant(ENV_CRN, PLATFORM_VARIANT));

        EnvironmentEditRequest sent = captureEditRequest();
        assertThat(sent.getFreeIpaPlatformVariant()).isEqualTo(PLATFORM_VARIANT);
    }

    @Test
    void testSetFreeIpaEnableMultiAzSendsEditRequestWithTrueFlag() {
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.setFreeIpaEnableMultiAz(ENV_CRN));

        EnvironmentEditRequest sent = captureEditRequest();
        assertThat(sent.getFreeIpaEnableMultiAz()).isTrue();
    }

    @Test
    void testSetFreeIpaNodeCountSendsEditRequestWithNodeCount() {
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.setFreeIpaNodeCount(ENV_CRN, NODE_COUNT));

        EnvironmentEditRequest sent = captureEditRequest();
        assertThat(sent.getFreeIpaNodeCount()).isEqualTo(NODE_COUNT);
    }

    @Test
    void testSetFreeIpaPlatformVariantThrowsBadRequestWhenEnvironmentNotFound() {
        when(environmentEndpoint.editByCrn(eq(ENV_CRN), any(EnvironmentEditRequest.class)))
                .thenThrow(new ClientErrorException(Response.status(Response.Status.NOT_FOUND).build()));

        assertThatThrownBy(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.setFreeIpaPlatformVariant(ENV_CRN, PLATFORM_VARIANT)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(ENV_CRN);
    }

    @Test
    void testSetFreeIpaEnableMultiAzThrowsBadRequestWhenEnvironmentNotFound() {
        when(environmentEndpoint.editByCrn(eq(ENV_CRN), any(EnvironmentEditRequest.class)))
                .thenThrow(new ClientErrorException(Response.status(Response.Status.NOT_FOUND).build()));

        assertThatThrownBy(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.setFreeIpaEnableMultiAz(ENV_CRN)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(ENV_CRN);
    }

    @Test
    void testSetFreeIpaNodeCountThrowsBadRequestWhenEnvironmentNotFound() {
        when(environmentEndpoint.editByCrn(eq(ENV_CRN), any(EnvironmentEditRequest.class)))
                .thenThrow(new ClientErrorException(Response.status(Response.Status.NOT_FOUND).build()));

        assertThatThrownBy(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.setFreeIpaNodeCount(ENV_CRN, NODE_COUNT)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(ENV_CRN);
    }

    @Test
    void testSetFreeIpaPlatformVariantThrowsCloudbreakServiceExceptionOnOtherClientErrors() {
        ClientErrorException clientErrorException = new ClientErrorException(Response.status(Response.Status.BAD_REQUEST).build());
        when(environmentEndpoint.editByCrn(eq(ENV_CRN), any(EnvironmentEditRequest.class))).thenThrow(clientErrorException);
        when(webApplicationExceptionMessageExtractor.getErrorMessage(clientErrorException)).thenReturn("boom");

        assertThatThrownBy(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.setFreeIpaPlatformVariant(ENV_CRN, PLATFORM_VARIANT)))
                .isInstanceOf(CloudbreakServiceException.class)
                .hasMessageContaining("Failed to update environment")
                .hasMessageContaining("boom");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testIsSecretEncryptionEnabled(boolean secretEncryptionEnabled) {
        DetailedEnvironmentResponse detailedEnvironmentResponse = mock(DetailedEnvironmentResponse.class);
        when(detailedEnvironmentResponse.isEnableSecretEncryption()).thenReturn(secretEncryptionEnabled);
        when(environmentEndpoint.getByCrn(ENV_CRN)).thenReturn(detailedEnvironmentResponse);
        boolean result = underTest.isSecretEncryptionEnabled(ENV_CRN);
        assertEquals(secretEncryptionEnabled, result);
    }

    private EnvironmentEditRequest captureEditRequest() {
        ArgumentCaptor<EnvironmentEditRequest> captor = ArgumentCaptor.forClass(EnvironmentEditRequest.class);
        verify(environmentEndpoint).editByCrn(eq(ENV_CRN), captor.capture());
        return captor.getValue();
    }
}