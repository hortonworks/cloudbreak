package com.sequenceiq.redbeams.service.stack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.security.cert.X509Certificate;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslCertificateType;
import com.sequenceiq.redbeams.configuration.DatabaseServerSslCertificateConfig;
import com.sequenceiq.redbeams.configuration.SslCertificateEntry;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.SslConfig;
import com.sequenceiq.redbeams.service.sslcertificate.DatabaseServerSslCertificateSyncService;
import com.sequenceiq.redbeams.service.sslcertificate.SslConfigService;

@ExtendWith(MockitoExtension.class)
public class DBStackUpdaterTest {

    private static final Long STACK_ID = 1L;

    private static final X509Certificate X_509_CERT = mock(X509Certificate.class);

    @InjectMocks
    private DBStackUpdater underTest;

    @Mock
    private DatabaseServerSslCertificateConfig databaseServerSslCertificateConfig;

    @Mock
    private DBStackService dbStackService;

    @Mock
    private DBStack dbStack;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private DatabaseStack databaseStack;

    @Mock
    private DatabaseServerSslCertificateSyncService databaseServerSslCertificateSyncService;

    @Mock
    private SslConfigService sslConfigService;

    @Captor
    private ArgumentCaptor<SslConfig> sslConfigArgumentCaptor;

    @Test
    public void testUpdateSslConfigWhenDBStackExistsEnabledAndActiveCertFound() throws Exception {
        SslConfig sslConfig = new SslConfig();
        sslConfig.setSslCertificateType(SslCertificateType.CLOUD_PROVIDER_OWNED);
        Set<SslCertificateEntry> certificateEntries = new LinkedHashSet<>();
        certificateEntries.add(new SslCertificateEntry(0,
                "cloudkey",
                "cloudPlatform",
                "cloudPlatform",
                "certPem0",
                X_509_CERT));
        certificateEntries.add(new SslCertificateEntry(1,
                "cloudkey",
                "cloudPlatform",
                "cloudPlatform",
                "certPem1",
                X_509_CERT));
        Set<String> certPems = new LinkedHashSet<>();
        certPems.add("certPem0");
        certPems.add("certPem1");
        when(dbStackService.findById(STACK_ID)).thenReturn(Optional.of(dbStack));
        when(dbStack.getSslConfig()).thenReturn(1L);
        when(sslConfigService.fetchById(1L)).thenReturn(Optional.of(sslConfig));
        when(dbStack.getCloudPlatform()).thenReturn("cloudPlatform");
        when(dbStack.getRegion()).thenReturn("region");
        when(databaseServerSslCertificateConfig.isCloudPlatformAndRegionSupportedForCerts(any(), any())).thenReturn(true);
        when(databaseServerSslCertificateConfig.getCertsByCloudPlatformAndRegion("cloudPlatform", "region")).thenReturn(certificateEntries);
        when(databaseServerSslCertificateConfig.getMaxVersionByCloudPlatformAndRegion("cloudPlatform", "region")).thenReturn(1);

        underTest.updateSslConfig(STACK_ID, cloudContext, cloudCredential, databaseStack);
        assertEquals(1, sslConfig.getSslCertificateActiveVersion());
        assertEquals(certPems, sslConfig.getSslCertificates());
        assertEquals("cloudPlatform", sslConfig.getSslCertificateActiveCloudProviderIdentifier());
        assertEquals(SslCertificateType.CLOUD_PROVIDER_OWNED, sslConfig.getSslCertificateType());

        verify(sslConfigService).save(sslConfigArgumentCaptor.capture());
        verify(databaseServerSslCertificateSyncService).syncSslCertificateIfNeeded(any(), any(), any(), any());
        assertThat(sslConfigArgumentCaptor.getValue().getSslCertificateExpirationDate())
                .isEqualTo(certificateEntries.stream()
                        .filter(e -> e.version() == sslConfigArgumentCaptor.getValue().getSslCertificateActiveVersion())
                        .findFirst()
                        .get()
                        .expirationDate());
    }

    @Test
    public void testUpdateSslConfigWhenDBStackSyncDropRuntimeException() throws Exception {
        SslConfig sslConfig = new SslConfig();
        sslConfig.setSslCertificateType(SslCertificateType.CLOUD_PROVIDER_OWNED);
        Set<SslCertificateEntry> certificateEntries = new LinkedHashSet<>();
        certificateEntries.add(new SslCertificateEntry(0,
                "cloudkey",
                "cloudPlatform",
                "cloudPlatform",
                "certPem0",
                X_509_CERT));
        certificateEntries.add(new SslCertificateEntry(1,
                "cloudkey",
                "cloudPlatform",
                "cloudPlatform",
                "certPem1",
                X_509_CERT));
        Set<String> certPems = new LinkedHashSet<>();
        certPems.add("certPem0");
        certPems.add("certPem1");
        when(dbStackService.findById(STACK_ID)).thenReturn(Optional.of(dbStack));
        when(dbStack.getSslConfig()).thenReturn(1L);
        when(sslConfigService.fetchById(1L)).thenReturn(Optional.of(sslConfig));
        when(dbStack.getCloudPlatform()).thenReturn("cloudPlatform");
        when(dbStack.getRegion()).thenReturn("region");
        when(databaseServerSslCertificateConfig.isCloudPlatformAndRegionSupportedForCerts(any(), any())).thenReturn(true);
        when(databaseServerSslCertificateConfig.getCertsByCloudPlatformAndRegion("cloudPlatform", "region")).thenReturn(certificateEntries);
        when(databaseServerSslCertificateConfig.getMaxVersionByCloudPlatformAndRegion("cloudPlatform", "region")).thenReturn(1);
        doThrow(new RuntimeException("test")).when(databaseServerSslCertificateSyncService)
                .syncSslCertificateIfNeeded(any(), any(), any(), any());

        RuntimeException actualException = assertThrows(RuntimeException.class, () ->
                underTest.updateSslConfig(STACK_ID, cloudContext, cloudCredential, databaseStack));

        assertEquals(1, sslConfig.getSslCertificateActiveVersion());
        assertNotNull(actualException);
        assertEquals(certPems, sslConfig.getSslCertificates());
        assertEquals("cloudPlatform", sslConfig.getSslCertificateActiveCloudProviderIdentifier());
        assertEquals(SslCertificateType.CLOUD_PROVIDER_OWNED, sslConfig.getSslCertificateType());

        verify(databaseServerSslCertificateSyncService).syncSslCertificateIfNeeded(any(), any(), any(), any());
    }

    @Test
    public void testUpdateSslConfigWhenDBStackExistsAndActiveCertNotFound() {
        SslConfig sslConfig = new SslConfig();
        sslConfig.setSslCertificateType(SslCertificateType.CLOUD_PROVIDER_OWNED);
        Set<SslCertificateEntry> certificateEntries = new LinkedHashSet<>();
        certificateEntries.add(new SslCertificateEntry(0,
                "cloudkey",
                "cloudPlatform",
                "cloudPlatform",
                "certPem0",
                X_509_CERT));
        when(dbStackService.findById(STACK_ID)).thenReturn(Optional.of(dbStack));
        when(dbStack.getSslConfig()).thenReturn(1L);
        when(sslConfigService.fetchById(1L)).thenReturn(Optional.of(sslConfig));
        when(dbStack.getCloudPlatform()).thenReturn("cloudPlatform");
        when(dbStack.getRegion()).thenReturn("region");
        when(dbStack.getName()).thenReturn("name");
        when(databaseServerSslCertificateConfig.isCloudPlatformAndRegionSupportedForCerts(any(), any())).thenReturn(true);
        when(databaseServerSslCertificateConfig.getCertsByCloudPlatformAndRegion("cloudPlatform", "region")).thenReturn(certificateEntries);
        when(databaseServerSslCertificateConfig.getMaxVersionByCloudPlatformAndRegion("cloudPlatform", "region")).thenReturn(1);

        NotFoundException actual = assertThrows(NotFoundException.class, () ->
                underTest.updateSslConfig(STACK_ID, cloudContext, cloudCredential, databaseStack));
        assertEquals("Active SSL cert cannot be found for name", actual.getMessage());

        verify(sslConfigService, never()).save(any(SslConfig.class));
    }

    @Test
    public void testUpdateSslConfigWhenDBStackExistsButCloudProviderNotSupported() {
        SslConfig sslConfig = new SslConfig();
        sslConfig.setSslCertificateType(SslCertificateType.CLOUD_PROVIDER_OWNED);
        when(dbStackService.findById(STACK_ID)).thenReturn(Optional.of(dbStack));
        when(dbStack.getSslConfig()).thenReturn(1L);
        when(sslConfigService.fetchById(1L)).thenReturn(Optional.of(sslConfig));
        when(dbStack.getCloudPlatform()).thenReturn("cloudPlatform");
        when(dbStack.getRegion()).thenReturn("region");
        when(databaseServerSslCertificateConfig.isCloudPlatformAndRegionSupportedForCerts(any(), any())).thenReturn(false);

        underTest.updateSslConfig(STACK_ID, cloudContext, cloudCredential, databaseStack);

        verify(sslConfigService, times(0)).save(any(SslConfig.class));
    }

    @Test
    public void testUpdateSslConfigWhenDBStackNotExists() {
        when(dbStackService.findById(STACK_ID)).thenReturn(Optional.empty());
        underTest.updateSslConfig(STACK_ID, cloudContext, cloudCredential, databaseStack);
        verifyNoInteractions(sslConfigService);
    }

    @Test
    public void testUpdateSslConfigWhenSslConfigIsNull() {
        when(dbStackService.findById(STACK_ID)).thenReturn(Optional.of(dbStack));
        when(dbStack.getSslConfig()).thenReturn(null);
        when(sslConfigService.fetchById(any())).thenReturn(Optional.empty());
        underTest.updateSslConfig(STACK_ID, cloudContext, cloudCredential, databaseStack);
        verify(sslConfigService, never()).save(any(SslConfig.class));
    }

    @Test
    public void testUpdateSslConfigWhenSslDisabled() {
        SslConfig sslConfig = new SslConfig();
        sslConfig.setSslCertificateType(SslCertificateType.NONE);
        when(dbStackService.findById(STACK_ID)).thenReturn(Optional.of(dbStack));
        when(dbStack.getSslConfig()).thenReturn(1L);
        when(sslConfigService.fetchById(1L)).thenReturn(Optional.of(sslConfig));
        underTest.updateSslConfig(STACK_ID, cloudContext, cloudCredential, databaseStack);
        verify(sslConfigService, never()).save(any(SslConfig.class));
    }
}
