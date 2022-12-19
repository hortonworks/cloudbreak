package com.sequenceiq.redbeams.service.stack;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.cert.X509Certificate;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslCertificateType;
import com.sequenceiq.redbeams.configuration.DatabaseServerSslCertificateConfig;
import com.sequenceiq.redbeams.configuration.SslCertificateEntry;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.SslConfig;

@ExtendWith(MockitoExtension.class)
public class DBStackUpdaterTest {

    private static final Long STACK_ID = 1L;

    private static final X509Certificate X_509_CERT = Mockito.mock(X509Certificate.class);

    @InjectMocks
    private DBStackUpdater underTest;

    @Mock
    private DatabaseServerSslCertificateConfig databaseServerSslCertificateConfig;

    @Mock
    private DBStackService dbStackService;

    @Mock
    private DBStack dbStack;

    @Test
    public void testUpdateSslConfigWhenDBStackExistsEnabledAndActiveCertFound() {
        SslConfig sslConfig = new SslConfig();
        sslConfig.setSslCertificateType(SslCertificateType.CLOUD_PROVIDER_OWNED);
        Set<SslCertificateEntry> certificateEntries = new LinkedHashSet<>();
        certificateEntries.add(new SslCertificateEntry(0, "cloudPlatform", "certPem0", X_509_CERT));
        certificateEntries.add(new SslCertificateEntry(1, "cloudPlatform", "certPem1", X_509_CERT));
        Set<String> certPems = new LinkedHashSet<>();
        certPems.add("certPem0");
        certPems.add("certPem1");
        when(dbStackService.findById(STACK_ID)).thenReturn(Optional.of(dbStack));
        when(dbStack.getSslConfig()).thenReturn(sslConfig);
        when(dbStack.getCloudPlatform()).thenReturn("cloudPlatform");
        when(dbStack.getRegion()).thenReturn("region");
        when(databaseServerSslCertificateConfig.getCertsByCloudPlatformAndRegion("cloudPlatform", "region")).thenReturn(certificateEntries);
        when(databaseServerSslCertificateConfig.getMaxVersionByCloudPlatformAndRegion("cloudPlatform", "region")).thenReturn(1);

        underTest.updateSslConfig(STACK_ID);
        Assertions.assertEquals(1, sslConfig.getSslCertificateActiveVersion());
        Assertions.assertEquals(certPems, sslConfig.getSslCertificates());
        Assertions.assertEquals("cloudPlatform", sslConfig.getSslCertificateActiveCloudProviderIdentifier());
        Assertions.assertEquals(SslCertificateType.CLOUD_PROVIDER_OWNED, sslConfig.getSslCertificateType());

        verify(dbStackService).save(dbStack);
    }

    @Test
    public void testUpdateSslConfigWhenDBStackExistsAndActiveCertNotFound() {
        SslConfig sslConfig = new SslConfig();
        sslConfig.setSslCertificateType(SslCertificateType.CLOUD_PROVIDER_OWNED);
        Set<SslCertificateEntry> certificateEntries = new LinkedHashSet<>();
        certificateEntries.add(new SslCertificateEntry(0, "cloudPlatform", "certPem0", X_509_CERT));
        when(dbStackService.findById(STACK_ID)).thenReturn(Optional.of(dbStack));
        when(dbStack.getSslConfig()).thenReturn(sslConfig);
        when(dbStack.getCloudPlatform()).thenReturn("cloudPlatform");
        when(dbStack.getRegion()).thenReturn("region");
        when(dbStack.getName()).thenReturn("name");
        when(databaseServerSslCertificateConfig.getCertsByCloudPlatformAndRegion("cloudPlatform", "region")).thenReturn(certificateEntries);
        when(databaseServerSslCertificateConfig.getMaxVersionByCloudPlatformAndRegion("cloudPlatform", "region")).thenReturn(1);

        NotFoundException actual = Assertions.assertThrows(NotFoundException.class, () -> underTest.updateSslConfig(STACK_ID));
        Assertions.assertEquals("Active SSL cert cannot be found for name", actual.getMessage());

        verify(dbStackService, never()).save(dbStack);
    }

    @Test
    public void testUpdateSslConfigWhenDBStackNotExists() {
        when(dbStackService.findById(STACK_ID)).thenReturn(Optional.empty());
        underTest.updateSslConfig(STACK_ID);
        verify(dbStackService, never()).save(dbStack);
    }

    @Test
    public void testUpdateSslConfigWhenSslConfigIsNull() {
        when(dbStackService.findById(STACK_ID)).thenReturn(Optional.of(dbStack));
        when(dbStack.getSslConfig()).thenReturn(null);
        underTest.updateSslConfig(STACK_ID);
        verify(dbStackService, never()).save(dbStack);
    }

    @Test
    public void testUpdateSslConfigWhenSslDisabled() {
        SslConfig sslConfig = new SslConfig();
        sslConfig.setSslCertificateType(SslCertificateType.NONE);
        when(dbStackService.findById(STACK_ID)).thenReturn(Optional.of(dbStack));
        when(dbStack.getSslConfig()).thenReturn(sslConfig);
        underTest.updateSslConfig(STACK_ID);
        verify(dbStackService, never()).save(dbStack);
    }
}
