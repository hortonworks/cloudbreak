package com.sequenceiq.cloudbreak.service.rdsconfig;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.redbeams.api.endpoint.v1.RedBeamsFlowEndpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.ClusterDatabaseServerCertificateStatusV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.RotateDatabaseServerSecretV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.UpgradeDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.UpgradeTargetMajorVersion;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.ClusterDatabaseServerCertificateStatusV4Responses;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslCertificateEntryResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.UpgradeDatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.support.SupportV4Endpoint;

@ExtendWith(MockitoExtension.class)
class RedbeamsClientServiceTest {

    private static final String DATABASE_SERVER_CRN = "databaseServerCrn";

    private static final String FLOW_CHAIN_ID = "flowChainId";

    private static final String SECRET = "secret";

    @Mock
    private DatabaseServerV4Endpoint redbeamsServerEndpoint;

    @Mock
    private RedBeamsFlowEndpoint redBeamsFlowEndpoint;

    @Mock
    private SupportV4Endpoint supportV4Endpoint;

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
        assertThatThrownBy(() -> underTest.deleteByCrn("crn", true)).isExactlyInstanceOf(NotFoundException.class);
    }

    @Test
    void rotateSslCert() {
        when(redbeamsServerEndpoint.rotateSslCert(any())).thenReturn(new FlowIdentifier(FlowType.FLOW_CHAIN, "123"));

        FlowIdentifier flowIdentifier = underTest.rotateSslCert("crn");

        verify(redbeamsServerEndpoint, times(1)).rotateSslCert(any());
        assertEquals("123", flowIdentifier.getPollableId());
        assertEquals(FlowType.FLOW_CHAIN, flowIdentifier.getType());
    }

    @Test
    void getLatestCertificate() {
        SslCertificateEntryResponse sslCertificateEntryResponse = new SslCertificateEntryResponse();
        when(supportV4Endpoint.getLatestCertificate(any(), anyString())).thenReturn(sslCertificateEntryResponse);

        SslCertificateEntryResponse aws = underTest.getLatestCertificate("AWS", "eu-central-1");

        verify(supportV4Endpoint, times(1)).getLatestCertificate(any(), anyString());
        assertEquals(sslCertificateEntryResponse, aws);
    }

    @Test
    void updateToLatestSslCert() {
        when(redbeamsServerEndpoint.updateToLatestSslCert(any())).thenReturn(new FlowIdentifier(FlowType.FLOW_CHAIN, "123"));

        FlowIdentifier flowIdentifier = underTest.updateToLatestSslCert("crn");

        verify(redbeamsServerEndpoint, times(1)).updateToLatestSslCert(any());
        assertEquals("123", flowIdentifier.getPollableId());
        assertEquals(FlowType.FLOW_CHAIN, flowIdentifier.getType());
    }

    @Test
    void getByClusterCrnNotFoundIsRethrownAsIs() {
        when(redbeamsServerEndpoint.getByClusterCrn(anyString(), anyString())).thenThrow(new NotFoundException("not found"));
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
        ClusterDatabaseServerCertificateStatusV4Responses mockResponse = new ClusterDatabaseServerCertificateStatusV4Responses();

        when(redbeamsServerEndpoint.listDatabaseServersCertificateStatusByStackCrns(request, "usercrn")).thenReturn(mockResponse);

        ClusterDatabaseServerCertificateStatusV4Responses result = underTest.listDatabaseServersCertificateStatusByStackCrns(request, "usercrn");

        assertNotNull(result);
        assertEquals(mockResponse, result);
        verify(redbeamsServerEndpoint, times(1)).listDatabaseServersCertificateStatusByStackCrns(request, "usercrn");
    }

    @Test
    public void testValidateUpgradeReturnsCorrectResponse() {
        // Given
        String crn = "crn:altus:iam:us-west-1:123:user:456";
        UpgradeDatabaseServerV4Request request = new UpgradeDatabaseServerV4Request();
        UpgradeDatabaseServerV4Response expectedResponse = new UpgradeDatabaseServerV4Response();
        FlowIdentifier flowId = new FlowIdentifier(FlowType.FLOW, "flow-123");
        expectedResponse.setFlowIdentifier(flowId);

        when(redbeamsServerEndpoint.validateUpgrade(crn, request)).thenReturn(expectedResponse);

        // When
        UpgradeDatabaseServerV4Response actualResponse = underTest.validateUpgrade(crn, request);

        // Then
        assertNotNull(actualResponse);
        assertEquals(flowId, actualResponse.getFlowIdentifier());
    }

    @Test
    public void testValidateUpgradeHandlesProcessingException() {
        // Given
        String crn = "crn:altus:iam:us-west-1:123:user:456";
        UpgradeDatabaseServerV4Request request = new UpgradeDatabaseServerV4Request();
        request.setUpgradeTargetMajorVersion(UpgradeTargetMajorVersion.VERSION_14);
        ProcessingException processingException = new ProcessingException("Processing error");

        when(redbeamsServerEndpoint.validateUpgrade(crn, request)).thenThrow(processingException);

        // When
        CloudbreakServiceException thrownException = assertThrows(
                CloudbreakServiceException.class,
                () -> underTest.validateUpgrade(crn, request)
        );

        // Then
        assertTrue(thrownException.getMessage().contains("Failed to validate upgrade DatabaseServer with CRN"));
        assertInstanceOf(ProcessingException.class, thrownException.getCause());
        verify(redbeamsServerEndpoint).validateUpgrade(crn, request);
    }

    @Test
    public void testValidateUpgradeCleanupReturnsCorrectResponse() {
        // Given
        String crn = "crn:altus:iam:us-west-1:123:user:456";
        UpgradeDatabaseServerV4Response expectedResponse = new UpgradeDatabaseServerV4Response();
        FlowIdentifier flowId = new FlowIdentifier(FlowType.FLOW, "flow-123");
        expectedResponse.setFlowIdentifier(flowId);

        when(redbeamsServerEndpoint.validateUpgradeCleanup(crn)).thenReturn(expectedResponse);

        // When
        UpgradeDatabaseServerV4Response actualResponse = underTest.validateUpgradeCleanup(crn);

        // Then
        assertNotNull(actualResponse);
        assertEquals(flowId, actualResponse.getFlowIdentifier());
    }

    @Test
    public void testValidateUpgradeCleanupHandlesProcessingException() {
        // Given
        String crn = "crn:altus:iam:us-west-1:123:user:456";
        ProcessingException processingException = new ProcessingException("Processing error");

        when(redbeamsServerEndpoint.validateUpgradeCleanup(crn)).thenThrow(processingException);

        // When
        CloudbreakServiceException thrownException = assertThrows(
                CloudbreakServiceException.class,
                () -> underTest.validateUpgradeCleanup(crn)
        );

        // Then
        assertTrue(thrownException.getMessage().contains("Failed to clean up validate upgrade DatabaseServer with CRN"));
        assertInstanceOf(ProcessingException.class, thrownException.getCause());
        verify(redbeamsServerEndpoint).validateUpgradeCleanup(crn);
    }

    @Test
    void testGetFlowIdShouldReturnNullIfLastFlowNotFound() {
        when(redBeamsFlowEndpoint.getLastFlowByResourceCrn(eq(DATABASE_SERVER_CRN))).thenThrow(new WebApplicationException("error", Response.Status.NOT_FOUND));
        FlowLogResponse lastFlow = underTest.getLastFlowId(DATABASE_SERVER_CRN);
        assertNull(lastFlow);
    }

    @Test
    void turnOnSslOnProvider() {
        underTest.turnOnSslOnProvider("crn");
        verify(redbeamsServerEndpoint).turnOnSslEnforcementOnProviderByCrnInternal(eq("crn"));
    }

    @Test
    void turnOnSslOnProviderThrowsException() {
        doThrow(new ProcessingException("Test")).when(redbeamsServerEndpoint).turnOnSslEnforcementOnProviderByCrnInternal(eq("crn"));
        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.turnOnSslOnProvider("crn"));
        verify(redbeamsServerEndpoint).turnOnSslEnforcementOnProviderByCrnInternal(eq("crn"));
        assertEquals("Failed to turn on certificate DatabaseServer with CRN crn", exception.getMessage());
    }

    @Test
    void migrateRdsToTls() {
        underTest.migrateRdsToTls("crn");
        verify(redbeamsServerEndpoint).migrateDatabaseToSslByCrnInternal(eq("crn"), any(String.class));
    }

    @Test
    void migrateRdsToTlsThrowsException() {
        doThrow(new ProcessingException("Test")).when(redbeamsServerEndpoint).migrateDatabaseToSslByCrnInternal(eq("crn"), any(String.class));
        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.migrateRdsToTls("crn"));
        verify(redbeamsServerEndpoint).migrateDatabaseToSslByCrnInternal(eq("crn"), any(String.class));
        assertEquals("Failed to migrate DatabaseServer with CRN crn", exception.getMessage());
    }
}