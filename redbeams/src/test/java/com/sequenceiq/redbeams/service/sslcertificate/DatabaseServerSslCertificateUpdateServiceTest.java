package com.sequenceiq.redbeams.service.sslcertificate;

import static com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslCertificateType.CLOUD_PROVIDER_OWNED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.redbeams.configuration.DatabaseServerSslCertificateConfig;
import com.sequenceiq.redbeams.configuration.SslCertificateEntry;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.SslConfig;

@ExtendWith(MockitoExtension.class)
class DatabaseServerSslCertificateUpdateServiceTest {

    @Mock
    private DatabaseServerSslCertificateConfig databaseServerSslCertificateConfig;

    @Mock
    private SslConfigService sslConfigService;

    @InjectMocks
    private DatabaseServerSslCertificateUpdateService databaseServerSslCertificateUpdateService;

    @Test
    public void testUpdateSslCertificateIfNeededUpdatePossible() {
        DBStack dbStack = new DBStack();
        dbStack.setRegion("us-west-2");
        dbStack.setSslConfig(1L);
        dbStack.setCloudPlatform("AWS");
        SslConfig sslConfig = mock(SslConfig.class);
        SslCertificateEntry sslCertificateEntry = mock(SslCertificateEntry.class);
        long time = new Date().getTime();

        when(sslConfigService.fetchById(1L)).thenReturn(Optional.of(sslConfig));
        when(sslConfig.getSslCertificateType()).thenReturn(CLOUD_PROVIDER_OWNED);
        when(sslCertificateEntry.cloudProviderIdentifier()).thenReturn("desiredCertificateId");
        when(sslCertificateEntry.version()).thenReturn(1);
        when(sslCertificateEntry.certPem()).thenReturn("certPem");
        when(sslCertificateEntry.expirationDate()).thenReturn(time);
        when(databaseServerSslCertificateConfig.getCertByCloudPlatformAndRegionAndCloudProviderIdentifier(
                eq("AWS"), eq("us-west-2"), eq("desiredCertificateId")))
                .thenReturn(sslCertificateEntry);

        databaseServerSslCertificateUpdateService.updateSslCertificateIfNeeded(dbStack, Optional.of("desiredCertificateId"));

        verify(sslConfig).setSslCertificateActiveCloudProviderIdentifier("desiredCertificateId");
        verify(sslConfig, times(2)).setSslCertificateActiveVersion(1);
        verify(sslConfig).setSslCertificateExpirationDate(time);
        verify(sslConfigService).save(sslConfig);
    }

    @Test
    public void testUpdateSslCertificateIfNeededUpdateNotPossible() {
        DBStack dbStack = mock(DBStack.class);

        when(dbStack.getSslConfig()).thenReturn(1L);
        when(dbStack.getCloudPlatform()).thenReturn("AWS");
        when(sslConfigService.fetchById(1L)).thenReturn(Optional.empty());

        databaseServerSslCertificateUpdateService.updateSslCertificateIfNeeded(dbStack, Optional.of("desiredCertificateId"));

        verify(sslConfigService, never()).save(any(SslConfig.class));
    }
}