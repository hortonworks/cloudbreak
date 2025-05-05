package com.sequenceiq.cloudbreak.service.freeipa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.flow.FreeIpaV1FlowEndpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.util.UtilV1Endpoint;

@ExtendWith(MockitoExtension.class)
class FreeipaClientServiceTest {
    private static final String ACCOUNT_ID = "cloudera";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:" + ACCOUNT_ID + ":environment:" + UUID.randomUUID();

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + ACCOUNT_ID + ":user:test@cloudera.com";

    private static final String ROOT_CERTIFICATE = "rootCertificate";

    private static final String ERROR_MSG = "fatal error";

    private static final String EXTRACTED_ERROR_MSG = "extracted error";

    @Mock
    private FreeIpaV1Endpoint freeIpaV1Endpoint;

    @Mock
    private FreeIpaV1FlowEndpoint freeIpaV1FlowEndpoint;

    @Mock
    private UtilV1Endpoint utilV1Endpoint;

    @Mock
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @InjectMocks
    private FreeipaClientService underTest;

    @Test
    void getRootCertificateByEnvironmentCrnTestRegularActor() {
        when(freeIpaV1Endpoint.getRootCertificate(ENV_CRN)).thenReturn(ROOT_CERTIFICATE);

        String result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.getRootCertificateByEnvironmentCrn(ENV_CRN));

        assertThat(result).isEqualTo(ROOT_CERTIFICATE);
        verify(freeIpaV1Endpoint, never()).getRootCertificateInternal(anyString(), anyString());
        verify(webApplicationExceptionMessageExtractor, never()).getErrorMessage(any(Exception.class));
    }

    @Test
    void getRootCertificateByEnvironmentCrnTestInternalActor() {
        when(freeIpaV1Endpoint.getRootCertificateInternal(ENV_CRN, ACCOUNT_ID)).thenReturn(ROOT_CERTIFICATE);

        String result = ThreadBasedUserCrnProvider.doAsInternalActor(() -> underTest.getRootCertificateByEnvironmentCrn(ENV_CRN));

        assertThat(result).isEqualTo(ROOT_CERTIFICATE);
        verify(freeIpaV1Endpoint, never()).getRootCertificate(anyString());
        verify(webApplicationExceptionMessageExtractor, never()).getErrorMessage(any(Exception.class));
    }

    @Test
    void getRootCertificateByEnvironmentCrnTestWebApplicationException() {
        WebApplicationException e = new WebApplicationException(ERROR_MSG);
        when(freeIpaV1Endpoint.getRootCertificate(ENV_CRN)).thenThrow(e);
        when(webApplicationExceptionMessageExtractor.getErrorMessage(e)).thenReturn(EXTRACTED_ERROR_MSG);

        CloudbreakServiceException cloudbreakServiceException = assertThrows(CloudbreakServiceException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.getRootCertificateByEnvironmentCrn(ENV_CRN)));

        assertThat(cloudbreakServiceException).hasCauseReference(e);
        assertThat(cloudbreakServiceException).hasMessage(String.format("Failed to GET FreeIPA root certificate by environment CRN: %s, due to: %s. %s.",
                ENV_CRN, ERROR_MSG, EXTRACTED_ERROR_MSG));
        verify(freeIpaV1Endpoint, never()).getRootCertificateInternal(anyString(), anyString());
    }

    @Test
    void getRootCertificateByEnvironmentCrnTestProcessingException() {
        getRootCertificateByEnvironmentCrnTestExceptionWithoutExtractionInternal(new ProcessingException(ERROR_MSG));
    }

    private void getRootCertificateByEnvironmentCrnTestExceptionWithoutExtractionInternal(Exception e) {
        when(freeIpaV1Endpoint.getRootCertificate(ENV_CRN)).thenThrow(e);

        CloudbreakServiceException cloudbreakServiceException = assertThrows(CloudbreakServiceException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.getRootCertificateByEnvironmentCrn(ENV_CRN)));

        assertThat(cloudbreakServiceException).hasCauseReference(e);
        assertThat(cloudbreakServiceException).hasMessage(String.format("Failed to GET FreeIPA root certificate by environment CRN: %s, due to: %s.",
                ENV_CRN, ERROR_MSG));
        verify(freeIpaV1Endpoint, never()).getRootCertificateInternal(anyString(), anyString());
        verify(webApplicationExceptionMessageExtractor, never()).getErrorMessage(any(Exception.class));
    }

    @Test
    void getRootCertificateByEnvironmentCrnTestIllegalStateException() {
        getRootCertificateByEnvironmentCrnTestExceptionWithoutExtractionInternal(new IllegalStateException(ERROR_MSG));
    }

    @Test
    void testGetFlowIdShouldReturnNullIfLastFlowNotFound() {
        when(freeIpaV1FlowEndpoint.getLastFlowByResourceCrn(eq(ENV_CRN))).thenThrow(new WebApplicationException("error", Response.Status.NOT_FOUND));
        FlowLogResponse lastFlow = underTest.getLastFlowId(ENV_CRN);
        assertNull(lastFlow);
    }

}
