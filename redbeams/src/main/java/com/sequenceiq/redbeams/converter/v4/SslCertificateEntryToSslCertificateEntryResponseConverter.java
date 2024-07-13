package com.sequenceiq.redbeams.converter.v4;

import org.springframework.stereotype.Component;

import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslCertificateEntryResponse;
import com.sequenceiq.redbeams.configuration.SslCertificateEntry;

@Component
public class SslCertificateEntryToSslCertificateEntryResponseConverter {

    public SslCertificateEntryResponse convert(SslCertificateEntry entry) {
        SslCertificateEntryResponse sslCertificateEntryResponse = new SslCertificateEntryResponse();
        sslCertificateEntryResponse.setCertPem(entry.certPem());
        sslCertificateEntryResponse.setCloudKey(entry.cloudKey());
        sslCertificateEntryResponse.setCloudPlatform(entry.cloudPlatform());
        sslCertificateEntryResponse.setDeprecated(entry.deprecated());
        sslCertificateEntryResponse.setVersion(entry.version());
        sslCertificateEntryResponse.setCloudProviderIdentifier(entry.cloudProviderIdentifier());
        sslCertificateEntryResponse.setFingerprint(entry.fingerprint());
        sslCertificateEntryResponse.setExpirationDate(entry.expirationDate());
        return sslCertificateEntryResponse;
    }
}
