package com.sequenceiq.redbeams.service.sslcertificate;

import static com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslCertificateType.CLOUD_PROVIDER_OWNED;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.redbeams.configuration.DatabaseServerSslCertificateConfig;
import com.sequenceiq.redbeams.configuration.SslCertificateEntry;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.SslConfig;

@Service
public class DatabaseServerSslCertificateUpdateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseServerSslCertificateUpdateService.class);

    @Inject
    private DatabaseServerSslCertificateConfig databaseServerSslCertificateConfig;

    @Inject
    private SslConfigService sslConfigService;

    public void updateSslCertificateIfNeeded(DBStack dbStack, Optional<String> desiredCertificate) {
        Optional<SslConfig> sslConfigOptional = sslConfigService.fetchById(dbStack.getSslConfig());
        String cloudPlatform = dbStack.getCloudPlatform();
        if (udatePossible(sslConfigOptional, cloudPlatform)) {
            SslConfig sslConfig = sslConfigOptional.get();
            SslCertificateEntry desiredSslCertificateEntry =
                    databaseServerSslCertificateConfig.getCertByCloudPlatformAndRegionAndCloudProviderIdentifier(cloudPlatform, dbStack.getRegion(),
                            desiredCertificate.get());
            sslConfig.setSslCertificateActiveCloudProviderIdentifier(desiredSslCertificateEntry.cloudProviderIdentifier());
            updateSslConfigWithActiveSslCertificateEntry(sslConfig, desiredSslCertificateEntry);
            sslConfigService.save(sslConfig);
        }
    }

    private boolean udatePossible(Optional<SslConfig> sslConfig, String cloudPlatform) {
        return sslConfig.isPresent() && CLOUD_PROVIDER_OWNED.equals(sslConfig.get().getSslCertificateType())
                && (CloudPlatform.AWS.name().equals(cloudPlatform));
    }

    private void updateSslConfigWithActiveSslCertificateEntry(SslConfig sslConfig, SslCertificateEntry desiredCertificateEntry) {
        sslConfig.setSslCertificateActiveVersion(desiredCertificateEntry.version());

        String activeSslCertificatePem = desiredCertificateEntry.certPem();
        Set<String> sslCertificates = Optional.ofNullable(sslConfig.getSslCertificates()).orElse(new HashSet<>());
        if (!sslCertificates.contains(activeSslCertificatePem)) {
            sslCertificates = new HashSet<>(sslCertificates);
            sslCertificates.add(activeSslCertificatePem);
        }
        sslConfig.setSslCertificateActiveVersion(desiredCertificateEntry.version());
        sslConfig.setSslCertificateExpirationDate(desiredCertificateEntry.expirationDate());
        sslConfig.setSslCertificates(sslCertificates);
    }

}
