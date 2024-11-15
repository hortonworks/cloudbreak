package com.sequenceiq.redbeams.service.rotate;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.security.cert.X509Certificate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.redbeams.configuration.DatabaseServerSslCertificateConfig;
import com.sequenceiq.redbeams.configuration.SslCertificateEntry;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.service.sslcertificate.DatabaseServerSslCertificatePrescriptionService;
import com.sequenceiq.redbeams.service.sslcertificate.DatabaseServerSslCertificateSyncService;
import com.sequenceiq.redbeams.service.sslcertificate.DatabaseServerSslCertificateUpdateService;
import com.sequenceiq.redbeams.service.sslcertificate.SslConfigService;
import com.sequenceiq.redbeams.service.stack.DBStackService;

@ExtendWith(MockitoExtension.class)
class CloudProviderCertRotatorTest {

    private static final long DB_STACK_ID = 1L;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private DBStackService dbStackService;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private Authenticator authenticator;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private DatabaseServerSslCertificateSyncService databaseServerSslCertificateSyncService;

    @Mock
    private DatabaseServerSslCertificatePrescriptionService databaseServerSslCertificatePrescriptionService;

    @Mock
    private DatabaseServerSslCertificateConfig databaseServerSslCertificateConfig;

    @Mock
    private SslConfigService sslConfigService;

    @Mock
    private DatabaseServerSslCertificateUpdateService databaseServerSslCertificateUpdateService;

    @InjectMocks
    private CloudProviderCertRotator cloudProviderCertRotator;

    @Test
    void testRotateCertWhenEverythingHappensAndRotationRequired() throws Exception {
        CloudContext cloudContext = mock(CloudContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        DatabaseStack databaseStack = mock(DatabaseStack.class);
        DBStack dbStack = mock(DBStack.class);
        ResourceConnector resources = mock(ResourceConnector.class);
        X509Certificate x509Certificate = mock(X509Certificate.class);
        SslCertificateEntry sslCertificateEntry = new SslCertificateEntry(1, "1", "aws", "aws", "pem", x509Certificate);

        when(dbStackService.getById(anyLong())).thenReturn(dbStack);
        when(dbStack.getCloudPlatform()).thenReturn("AWS");
        when(cloudContext.getPlatform()).thenReturn(platform("aws"));
        when(cloudContext.getPlatformVariant())
                .thenReturn(new CloudPlatformVariant("CloudPlatformVariant", "CloudPlatformVariant"));
        when(cloudContext.getLocation()).thenReturn(location(region("ap"), availabilityZone("1")));
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        when(cloudConnector.resources()).thenReturn(resources);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(databaseServerSslCertificateConfig.getCertByCloudPlatformAndRegionAndVersion(any(), any(), anyInt()))
                .thenReturn(sslCertificateEntry);
        when(authenticator.authenticate(any(), any())).thenReturn(authenticatedContext);
        when(resources.getDatabaseServerStatus(any(), any())).thenReturn(ExternalDatabaseStatus.STARTED);
        when(databaseServerSslCertificatePrescriptionService.prescribeSslCertificateIfNeeded(any(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.of("desiredCertificate"));

        cloudProviderCertRotator.rotate(DB_STACK_ID, cloudContext, cloudCredential, databaseStack, false);

        verify(dbStackService).getById(DB_STACK_ID);
        verify(cloudConnector.authentication()).authenticate(cloudContext, cloudCredential);
        verify(cloudConnector.resources()).getDatabaseServerStatus(authenticatedContext, databaseStack);
        verify(databaseServerSslCertificatePrescriptionService).prescribeSslCertificateIfNeeded(cloudContext,
                cloudCredential,
                dbStack,
                "aws",
                databaseStack,
                Optional.empty());
        verify(resources).updateDatabaseServerActiveSslRootCertificate(authenticatedContext, databaseStack, "desiredCertificate");
        verify(databaseServerSslCertificateSyncService).syncSslCertificateIfNeeded(cloudContext, cloudCredential, dbStack, databaseStack);
    }

    @Test
    void testRotateCertWhenEverythingHappensAndRotationNotRequired() throws Exception {
        CloudContext cloudContext = mock(CloudContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        DatabaseStack databaseStack = mock(DatabaseStack.class);
        DBStack dbStack = mock(DBStack.class);
        ResourceConnector resources = mock(ResourceConnector.class);
        X509Certificate x509Certificate = mock(X509Certificate.class);
        Optional<String> desiredCertificate = Optional.of("desiredCertificate");
        SslCertificateEntry sslCertificateEntry = new SslCertificateEntry(1, "1", "aws", "aws", "pem", x509Certificate);

        when(dbStackService.getById(anyLong())).thenReturn(dbStack);
        when(dbStack.getCloudPlatform()).thenReturn("AWS");
        when(cloudContext.getPlatform()).thenReturn(platform("aws"));
        when(cloudContext.getPlatformVariant())
                .thenReturn(new CloudPlatformVariant("CloudPlatformVariant", "CloudPlatformVariant"));
        when(cloudContext.getLocation()).thenReturn(location(region("ap"), availabilityZone("1")));
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        when(cloudConnector.resources()).thenReturn(resources);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(databaseServerSslCertificateConfig.getCertByCloudPlatformAndRegionAndVersion(any(), any(), anyInt()))
                .thenReturn(sslCertificateEntry);
        when(authenticator.authenticate(any(), any())).thenReturn(authenticatedContext);
        when(resources.getDatabaseServerStatus(any(), any())).thenReturn(ExternalDatabaseStatus.STARTED);
        when(databaseServerSslCertificatePrescriptionService.prescribeSslCertificateIfNeeded(any(), any(), any(), any(), any(), any()))
                .thenReturn(desiredCertificate);

        cloudProviderCertRotator.rotate(DB_STACK_ID, cloudContext, cloudCredential, databaseStack, true);

        verify(dbStackService).getById(DB_STACK_ID);
        verify(cloudConnector.authentication()).authenticate(cloudContext, cloudCredential);
        verify(cloudConnector.resources()).getDatabaseServerStatus(authenticatedContext, databaseStack);
        verify(databaseServerSslCertificatePrescriptionService).prescribeSslCertificateIfNeeded(cloudContext,
                cloudCredential,
                dbStack,
                "aws",
                databaseStack,
                Optional.empty());
        verify(databaseServerSslCertificateUpdateService).updateSslCertificateIfNeeded(dbStack, desiredCertificate);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = CloudPlatform.class, names = {"AWS"}, mode = EnumSource.Mode.EXCLUDE)
    void testRotateCertWhenUnsupportedCloudPlatform(CloudPlatform cloudPlatform) throws Exception {
        CloudContext cloudContext = mock(CloudContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        DatabaseStack databaseStack = mock(DatabaseStack.class);
        DBStack dbStack = mock(DBStack.class);

        when(dbStackService.getById(DB_STACK_ID)).thenReturn(dbStack);
        when(dbStack.getCloudPlatform()).thenReturn(cloudPlatform.name());

        cloudProviderCertRotator.rotate(DB_STACK_ID, cloudContext, cloudCredential, databaseStack, true);

        verifyNoInteractions(cloudPlatformConnectors);
        verifyNoInteractions(databaseServerSslCertificateSyncService);
        verifyNoInteractions(databaseServerSslCertificateUpdateService);
        verifyNoInteractions(databaseServerSslCertificatePrescriptionService);
        verifyNoInteractions(databaseServerSslCertificateConfig);
    }

}