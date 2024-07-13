package com.sequenceiq.redbeams.controller.v4.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.BadRequestException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslCertificateEntryResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.support.CertificateSwapV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.support.CertificateSwapV4Response;
import com.sequenceiq.redbeams.configuration.DatabaseServerSslCertificateConfig;
import com.sequenceiq.redbeams.configuration.SslCertificateEntry;
import com.sequenceiq.redbeams.converter.v4.SslCertificateEntryToSslCertificateEntryResponseConverter;

@ExtendWith(MockitoExtension.class)
class SupportControllerTest {

    @Mock
    private DatabaseServerSslCertificateConfig databaseServerSslCertificateConfig;

    @Mock
    private SslCertificateEntryToSslCertificateEntryResponseConverter sslCertificateEntryToSslCertificateEntryResponseConverter;

    @InjectMocks
    private SupportController supportController;

    @Test
    public void testSwapCertificate() {
        CertificateSwapV4Request request = new CertificateSwapV4Request();
        request.setFirstCert(true);
        request.setSecondCert(true);

        SslCertificateEntry certV0 = mock(SslCertificateEntry.class);
        SslCertificateEntry certV1 = mock(SslCertificateEntry.class);
        when(certV0.version()).thenReturn(0);
        when(certV1.version()).thenReturn(1);

        Set<SslCertificateEntry> certs = new HashSet<>();
        certs.add(certV0);
        certs.add(certV1);

        when(databaseServerSslCertificateConfig.getCertsByCloudPlatformAndRegion("mock", "nodnol")).thenReturn(certs);

        CertificateSwapV4Response response = supportController.swapCertificate(request);

        assertNull(response);
        verify(databaseServerSslCertificateConfig).modifyMockProviderCertCache(any(), any());
    }

    @Test
    public void testGetLatestCertificate() {
        String cloudPlatform = "mock";
        String region = "london";

        SslCertificateEntry certV1 = mock(SslCertificateEntry.class);

        when(databaseServerSslCertificateConfig.getMaxVersionByCloudPlatformAndRegion(cloudPlatform, region)).thenReturn(1);
        when(databaseServerSslCertificateConfig.getCertByCloudPlatformAndRegionAndVersion(any(), any(), anyInt())).thenReturn(certV1);
        SslCertificateEntryResponse responseMock = mock(SslCertificateEntryResponse.class);
        when(sslCertificateEntryToSslCertificateEntryResponseConverter.convert(certV1)).thenReturn(responseMock);

        SslCertificateEntryResponse response = supportController.getLatestCertificate(cloudPlatform, region);

        assertEquals(responseMock, response);
    }

    @Test
    public void testGetLatestCertificateNotFound() {
        String cloudPlatform = "mock";
        String region = "london";
        X509Certificate x509Certificate = mock(X509Certificate.class);

        SslCertificateEntry certs = new SslCertificateEntry(1, "", "", "", "", x509Certificate);

        when(databaseServerSslCertificateConfig.getMaxVersionByCloudPlatformAndRegion(cloudPlatform, region)).thenReturn(1);
        when(databaseServerSslCertificateConfig.getCertByCloudPlatformAndRegionAndVersion(any(), any(), anyInt())).thenReturn(null);

        assertThrows(BadRequestException.class, () -> supportController.getLatestCertificate(cloudPlatform, region));
    }
}