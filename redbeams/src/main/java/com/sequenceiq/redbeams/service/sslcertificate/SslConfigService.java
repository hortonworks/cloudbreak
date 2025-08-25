package com.sequenceiq.redbeams.service.sslcertificate;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.AllocateDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslMode;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslCertificateType;
import com.sequenceiq.redbeams.configuration.DatabaseServerSslCertificateConfig;
import com.sequenceiq.redbeams.configuration.SslCertificateEntry;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.SslConfig;
import com.sequenceiq.redbeams.repository.SslConfigRepository;

@Service
public class SslConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SslConfigService.class);

    @Value("${redbeams.ssl.enabled:}")
    private boolean sslEnabled;

    @Inject
    private DatabaseServerSslCertificateConfig databaseServerSslCertificateConfig;

    @Inject
    private SslConfigRepository repository;

    public SslConfig createSslConfig(AllocateDatabaseServerV4Request source, DBStack dbStack) {
        return getSslConfig(source.getSslConfig() == null ? SslMode.DISABLED : source.getSslConfig().getSslMode(), dbStack, Optional.empty());
    }

    public void createSslConfig(SslMode sslMode, DBStack dbStack) {
        if (dbStack.getSslConfig() == null || emptySslCertificates(dbStack)) {
            getSslConfig(sslMode, dbStack, repository.findById(dbStack.getSslConfig()));
        }
    }

    private boolean emptySslCertificates(DBStack dbStack) {
        return fetchById(dbStack.getSslConfig())
                .map(sslConfig -> sslConfig.getSslCertificates().isEmpty())
                .orElse(true);
    }

    private SslConfig getSslConfig(SslMode sslMode, DBStack dbStack, Optional<SslConfig> sslConfigOptional) {
        SslConfig sslConfig = sslConfigOptional.orElse(new SslConfig());
        if (sslEnabled && SslMode.isEnabled(sslMode)) {
            LOGGER.info("SSL is enabled and has been requested. Setting up SslConfig for DBStack.");
            String cloudPlatform = dbStack.getCloudPlatform();
            String region = dbStack.getRegion();
            int maxVersion = databaseServerSslCertificateConfig.getMaxVersionByCloudPlatformAndRegion(cloudPlatform, region);
            sslConfig.setSslCertificateActiveVersion(maxVersion);
            int numberOfCerts = databaseServerSslCertificateConfig.getNumberOfCertsByCloudPlatformAndRegion(cloudPlatform, region);
            if (numberOfCerts != 0) {
                Set<SslCertificateEntry> certsTemp =
                        databaseServerSslCertificateConfig.getCertsByCloudPlatformAndRegion(cloudPlatform, region)
                                .stream()
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet());
                validateNonNullCertsCount(cloudPlatform, region, numberOfCerts, certsTemp);
                for (int i = maxVersion - numberOfCerts + 1; i < maxVersion; i++) {
                    findAndValidateCertByVersion(cloudPlatform, region, i, certsTemp);
                }
                Set<String> certs = certsTemp
                        .stream()
                        .map(SslCertificateEntry::certPem)
                        .collect(Collectors.toSet());
                validateUniqueCertsCount(cloudPlatform, region, numberOfCerts, certs);
                sslConfig.setSslCertificates(certs);
                String cloudProviderIdentifier = findAndValidateCertByVersion(cloudPlatform, region, maxVersion, certsTemp).cloudProviderIdentifier();
                sslConfig.setSslCertificateActiveCloudProviderIdentifier(cloudProviderIdentifier);

                SslCertificateEntry cert = databaseServerSslCertificateConfig.getCertByCloudPlatformAndRegionAndVersion(
                    dbStack.getCloudPlatform(),
                    dbStack.getRegion(),
                    sslConfig.getSslCertificateActiveVersion()
                );
                sslConfig.setSslCertificateExpirationDate(cert.expirationDate());
            } else {
                sslConfig.setSslCertificates(Collections.emptySet());
            }
            sslConfig.setSslCertificateType(SslCertificateType.CLOUD_PROVIDER_OWNED);
            LOGGER.info("Finished setting up SslConfig: {}", sslConfig);
        } else {
            LOGGER.info("SSL is not enabled or has not been requested. Skipping SslConfig setup for DBStack.");
        }
        return repository.save(sslConfig);
    }

    public Optional<SslConfig> fetchById(Long id) {
        if (id == null) {
            return Optional.empty();
        } else {
            Optional<SslConfig> sslConfig = repository.findById(id);
            if (sslConfig.isEmpty()) {
                throw new NotFoundException(String.format("SslConfig [%s] not found", id));
            } else {
                return sslConfig;
            }
        }
    }

    public SslConfig save(SslConfig sslConfig) {
        return repository.save(sslConfig);
    }

    public void delete(Long id) {
        Optional.ofNullable(id).ifPresent(repository::deleteById);
    }

    private void validateNonNullCertsCount(String cloudPlatform, String region, int numberOfCertsExpected, Set<SslCertificateEntry> certs) {
        validateCountInternal(cloudPlatform, region, numberOfCertsExpected, certs.size(),
                "SSL certificate count mismatch for cloud platform \"%s\" and region \"%s\": expected=%d, actual=%d");
    }

    private SslCertificateEntry findAndValidateCertByVersion(String cloudPlatform, String region, int version, Set<SslCertificateEntry> certs) {
        SslCertificateEntry result = certs.stream()
                .filter(c -> c.version() == version)
                .findFirst()
                .orElse(null);
        validateCert(cloudPlatform, region, version, result);
        return result;
    }

    private void validateUniqueCertsCount(String cloudPlatform, String region, int numberOfCertsExpected, Set<String> certs) {
        validateCountInternal(cloudPlatform, region, numberOfCertsExpected, certs.size(),
                "Duplicated SSL certificate PEM for cloud platform \"%s\" and region \"%s\". Unique count: expected=%d, actual=%d");
    }

    private void validateCountInternal(String cloudPlatform, String region, int countExpected, int countActual, String errorMsg) {
        if (countActual != countExpected) {
            throw new IllegalStateException(String.format(errorMsg, cloudPlatform, region, countExpected, countActual));
        }
    }

    private void validateCert(String cloudPlatform, String region, int versionExpected, SslCertificateEntry cert) {
        if (cert == null) {
            throw new IllegalStateException(
                    String.format("Could not find SSL certificate version %d for cloud platform \"%s\" and region \"%s\"", versionExpected, cloudPlatform,
                            region));
        }

        if (Strings.isNullOrEmpty(cert.cloudProviderIdentifier())) {
            throw new IllegalStateException(
                    String.format("Blank CloudProviderIdentifier in SSL certificate version %d for cloud platform \"%s\" and region \"%s\"", versionExpected,
                            cloudPlatform, region));
        }

        if (Strings.isNullOrEmpty(cert.certPem())) {
            throw new IllegalStateException(String.format("Blank PEM in SSL certificate version %d for cloud platform \"%s\" and region \"%s\"", versionExpected,
                    cloudPlatform, region));
        }
    }
}
