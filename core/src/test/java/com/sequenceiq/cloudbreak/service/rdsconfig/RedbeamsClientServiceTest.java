package com.sequenceiq.cloudbreak.service.rdsconfig;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.ClusterDatabaseServerCertificateStatusV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.RotateDatabaseServerSecretV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.ClusterDatabaseServerCertificateStatusV4Responses;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslCertificateEntryResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.support.SupportV4Endpoint;

@ExtendWith(MockitoExtension.class)
class RedbeamsClientServiceTest {

    private static final String DATABASE_SERVER_CRN = "databaseServerCrn";

    private static final String FLOW_CHAIN_ID = "flowChainId";

    private static final String SECRET = "secret";

    @Mock
    private DatabaseServerV4Endpoint redbeamsServerEndpoint;

    @Mock
    private SupportV4Endpoint supportV4Endpoint;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @InjectMocks
    private RedbeamsClientService underTest;

    static Stream<Arguments> emptyStringValues() {
        return Stream.of(
                Arguments.of("Null string", null),
                Arguments.of("Empty string", ""),
                Arguments.of("Blank string", "  ")
        );
    }

    @Test
    void deleteByCrnNotFoundIsRethrownAsIs() {
        when(redbeamsServerEndpoint.deleteByCrn(any(), anyBoolean())).thenThrow(new NotFoundException("not found"));
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        assertThatThrownBy(() -> underTest.deleteByCrn("crn", true)).isExactlyInstanceOf(NotFoundException.class);
    }

    @Test
    void rotateSslCert() {
        when(redbeamsServerEndpoint.rotateSslCert(any())).thenReturn(new FlowIdentifier(FlowType.FLOW_CHAIN, "123"));
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);

        FlowIdentifier flowIdentifier = underTest.rotateSslCert("crn");

        verify(redbeamsServerEndpoint, times(1)).rotateSslCert(any());
        assertEquals("123", flowIdentifier.getPollableId());
        assertEquals(FlowType.FLOW_CHAIN, flowIdentifier.getType());
    }

    @Test
    void getLatestCertificate() {
        SslCertificateEntryResponse sslCertificateEntryResponse = new SslCertificateEntryResponse();
        when(supportV4Endpoint.getLatestCertificate(any(), anyString())).thenReturn(sslCertificateEntryResponse);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);

        SslCertificateEntryResponse aws = underTest.getLatestCertificate("AWS", "eu-central-1");

        verify(supportV4Endpoint, times(1)).getLatestCertificate(any(), anyString());
        assertEquals(sslCertificateEntryResponse, aws);
    }

    @Test
    void updateToLatestSslCert() {
        when(redbeamsServerEndpoint.updateToLatestSslCert(any())).thenReturn(new FlowIdentifier(FlowType.FLOW_CHAIN, "123"));
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);

        FlowIdentifier flowIdentifier = underTest.updateToLatestSslCert("crn");

        verify(redbeamsServerEndpoint, times(1)).updateToLatestSslCert(any());
        assertEquals("123", flowIdentifier.getPollableId());
        assertEquals(FlowType.FLOW_CHAIN, flowIdentifier.getType());
    }

    @Test
    void getByClusterCrnNotFoundIsRethrownAsIs() {
        when(redbeamsServerEndpoint.getByClusterCrn(anyString(), anyString())).thenThrow(new NotFoundException("not found"));
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        assertThatThrownBy(() -> underTest.getByClusterCrn("crn", "crn2")).isExactlyInstanceOf(NotFoundException.class);
    }

    @MethodSource("emptyStringValues")
    @ParameterizedTest(name = "{0}")
    void getByClusterCrnThrowsIfNullEnvCrn(String testName, String value) {
        assertThatThrownBy(() -> underTest.getByClusterCrn(value, "crn2")).isExactlyInstanceOf(CloudbreakServiceException.class);
    }

    @MethodSource("emptyStringValues")
    @ParameterizedTest(name = "{0}")
    void getByClusterCrnThrowsIfNullClusterCrn(String testName, String value) {
        assertThatThrownBy(() -> underTest.getByClusterCrn("crn", value)).isExactlyInstanceOf(CloudbreakServiceException.class);
    }

    @Test
    void rotateSecretShouldCallRedbeamsClient() {
        when(redbeamsServerEndpoint.rotateSecret(any(), any())).thenReturn(new FlowIdentifier(FlowType.FLOW_CHAIN, FLOW_CHAIN_ID));
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        RotateDatabaseServerSecretV4Request request = new RotateDatabaseServerSecretV4Request();
        request.setSecret(SECRET);
        request.setCrn(DATABASE_SERVER_CRN);
        FlowIdentifier flowIdentifier = underTest.rotateSecret(request);
        verify(redbeamsServerEndpoint, times(1)).rotateSecret(eq(request), any());
        assertEquals("flowChainId", flowIdentifier.getPollableId());
        assertEquals(FlowType.FLOW_CHAIN, flowIdentifier.getType());
    }

    @Test
    void rotateSecretShouldThrowCloudbreakServiceExceptionWhenClientCallFails() {
        when(redbeamsServerEndpoint.rotateSecret(any(), any())).thenThrow(new BadRequestException("bad request"));
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        RotateDatabaseServerSecretV4Request request = new RotateDatabaseServerSecretV4Request();
        request.setSecret(SECRET);
        request.setCrn(DATABASE_SERVER_CRN);
        CloudbreakServiceException cloudbreakServiceException = assertThrows(CloudbreakServiceException.class,
                () -> underTest.rotateSecret(request));
        assertEquals(String.format(
                "Failed to rotate DatabaseServer secret %s with CRN %s due to error: %s", SECRET, DATABASE_SERVER_CRN, "bad request"),
                cloudbreakServiceException.getMessage());
        verify(redbeamsServerEndpoint, times(1)).rotateSecret(eq(request), any());
    }

    @Test
    public void testListDatabaseServersCertificateStatusByStackCrns() {
        ClusterDatabaseServerCertificateStatusV4Request request = new ClusterDatabaseServerCertificateStatusV4Request();
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        ClusterDatabaseServerCertificateStatusV4Responses mockResponse = new ClusterDatabaseServerCertificateStatusV4Responses();

        when(redbeamsServerEndpoint.listDatabaseServersCertificateStatusByStackCrns(request, "usercrn")).thenReturn(mockResponse);

        ClusterDatabaseServerCertificateStatusV4Responses result = underTest.listDatabaseServersCertificateStatusByStackCrns(request, "usercrn");

        assertNotNull(result);
        assertEquals(mockResponse, result);
        verify(redbeamsServerEndpoint, times(1)).listDatabaseServersCertificateStatusByStackCrns(request, "usercrn");
    }
}
