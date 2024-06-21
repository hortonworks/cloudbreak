package com.sequenceiq.redbeams.service.sslcertificate;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.database.CloudDatabaseServerSslCertificate;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslCertificateType;
import com.sequenceiq.redbeams.configuration.DatabaseServerSslCertificateConfig;
import com.sequenceiq.redbeams.configuration.SslCertificateEntry;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.SslConfig;

@Service
public class DatabaseServerSslCertificateSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseServerSslCertificateSyncService.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private DatabaseServerSslCertificateConfig databaseServerSslCertificateConfig;

    @Inject
    private SslConfigService sslConfigService;

    public void syncSslCertificateIfNeeded(CloudContext cloudContext, CloudCredential cloudCredential, DBStack dbStack, DatabaseStack databaseStack)
            throws Exception {
        Optional<SslConfig> sslConfig = sslConfigService.fetchById(dbStack.getSslConfig());
        String cloudPlatform = dbStack.getCloudPlatform();
        if (sslConfig.isPresent() && SslCertificateType.CLOUD_PROVIDER_OWNED.equals(sslConfig.get().getSslCertificateType())
                && (CloudPlatform.AWS.name().equals(cloudPlatform) || CloudPlatform.GCP.name().equals(cloudPlatform))) {
            CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, cloudCredential);
            CloudDatabaseServerSslCertificate activeSslRootCertificate = connector.resources().getDatabaseServerActiveSslRootCertificate(ac, databaseStack);
            if (activeSslRootCertificate == null) {
                LOGGER.warn("Database server or its SSL certificate does not exist in cloud platform \"{}\" for {}. Skipping synchronization.", cloudPlatform,
                        cloudContext);
            } else {
                if (CloudPlatform.AWS.name().equals(cloudPlatform)) {
                    syncSslCertificateAws(cloudContext, dbStack, activeSslRootCertificate, sslConfig.get());
                } else {
                    syncSslCertificateGcp(activeSslRootCertificate, sslConfig.get());
                }
            }
        } else {
            LOGGER.info("SSL not enabled or unsupported cloud platform \"{}\": SslConfig={}. Skipping SSL certificate synchronization for database stack {}",
                    cloudPlatform, sslConfig, cloudContext);
        }
    }

    public Optional<CloudDatabaseServerSslCertificate> getCertificateFromProvider(CloudContext cloudContext,
            CloudCredential cloudCredential, DBStack dbStack, DatabaseStack databaseStack) throws Exception {
        CloudDatabaseServerSslCertificate result = null;
        Optional<SslConfig> sslConfig = sslConfigService.fetchById(dbStack.getSslConfig());
        String cloudPlatform = dbStack.getCloudPlatform();
        if (sslConfig.isPresent() && SslCertificateType.CLOUD_PROVIDER_OWNED.equals(sslConfig.get().getSslCertificateType())
                && (CloudPlatform.AWS.name().equals(cloudPlatform) || CloudPlatform.GCP.name().equals(cloudPlatform))) {
            CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, cloudCredential);
            result = connector.resources().getDatabaseServerActiveSslRootCertificate(ac, databaseStack);
        } else {
            LOGGER.info("SSL not enabled or unsupported cloud platform \"{}\": SslConfig={}. Skipping SSL certificate synchronization for database stack {}",
                    cloudPlatform, sslConfig, cloudContext);
        }
        return Optional.ofNullable(result);
    }

    private void syncSslCertificateGcp(CloudDatabaseServerSslCertificate activeSslRootCertificate, SslConfig sslConfig) {
        sslConfig.setSslCertificateActiveVersion(0);
        sslConfig.setSslCertificates(Collections.singleton(activeSslRootCertificate.certificate()));
        sslConfig.setSslCertificateActiveCloudProviderIdentifier(activeSslRootCertificate.certificateIdentifier());
        sslConfig.setSslCertificateExpirationDate(activeSslRootCertificate.expirationDate());
        sslConfigService.save(sslConfig);
    }

    private void syncSslCertificateAws(CloudContext cloudContext, DBStack dbStack, CloudDatabaseServerSslCertificate activeSslRootCertificate,
            SslConfig sslConfig) {
        String cloudPlatform = dbStack.getCloudPlatform();
        String desiredSslCertificateIdentifier = sslConfig.getSslCertificateActiveCloudProviderIdentifier();
        String activeSslCertificateIdentifier = activeSslRootCertificate.certificateIdentifier();
        if (activeSslCertificateIdentifier.equals(desiredSslCertificateIdentifier)) {
            LOGGER.info("Active SSL certificate CloudProviderIdentifier for cloud platform \"{}\" matches the desired one: \"{}\", database stack {}",
                    cloudPlatform, activeSslCertificateIdentifier, cloudContext);
        } else {
            LOGGER.info("Mismatching SSL certificate CloudProviderIdentifier for cloud platform \"{}\": desired=\"{}\", actual=\"{}\", " +
                            "database stack {}. Syncing it to the actual value; this may result in an \"SSL certificate outdated\" status for the DBStack.",
                    cloudPlatform, Optional.ofNullable(desiredSslCertificateIdentifier).orElse("undefined (legacy DBStack)"), activeSslCertificateIdentifier,
                    cloudContext);
            sslConfig.setSslCertificateActiveCloudProviderIdentifier(activeSslCertificateIdentifier);
            SslCertificateEntry activeSslCertificateEntry =
                    databaseServerSslCertificateConfig.getCertByCloudPlatformAndRegionAndCloudProviderIdentifier(cloudPlatform, dbStack.getRegion(),
                            activeSslCertificateIdentifier);
            if (activeSslCertificateEntry == null) {
                LOGGER.warn("Unable to determine version & PEM for the actual CloudProviderIdentifier, leaving database server registration unchanged. " +
                        "The DBStack uses an SSL root certificate that is unknown to redbeams, because it is either super-recent, " +
                        "or too old so that it is treated as deprecated. This will result in an \"SSL certificate outdated\" status for the DBStack.");
            } else {
                LOGGER.info("Updating database server registration with the version & PEM of the actual CloudProviderIdentifier. " +
                        "Associated SslCertificateEntry: {}. This may still result in an \"SSL certificate outdated\" status for the DBStack if the AWS side " +
                        "SSL certificate lags behind the highest version supported by redbeams.", activeSslCertificateEntry);
                validateCert(cloudPlatform, activeSslCertificateIdentifier, activeSslCertificateEntry);
                updateSslConfigWithActiveSslCertificateEntry(sslConfig, activeSslCertificateEntry);
            }
            sslConfigService.save(sslConfig);
        }
    }

    private void updateSslConfigWithActiveSslCertificateEntry(SslConfig sslConfig, SslCertificateEntry activeSslCertificateEntry) {
        sslConfig.setSslCertificateActiveVersion(activeSslCertificateEntry.version());

        String activeSslCertificatePem = activeSslCertificateEntry.certPem();
        Set<String> sslCertificates = Optional.ofNullable(sslConfig.getSslCertificates()).orElse(new HashSet<>());
        if (!sslCertificates.contains(activeSslCertificatePem)) {
            sslCertificates = new HashSet<>(sslCertificates);
            sslCertificates.add(activeSslCertificatePem);
        }
        sslConfig.setSslCertificateExpirationDate(activeSslCertificateEntry.expirationDate());
        sslConfig.setSslCertificates(sslCertificates);
    }

    private void validateCert(String cloudPlatform, String cloudProviderIdentifierExpected, SslCertificateEntry cert) {
        String cloudProviderIdentifier = cert.cloudProviderIdentifier();
        if (!cloudProviderIdentifierExpected.equals(cloudProviderIdentifier)) {
            throw new IllegalStateException(
                    String.format("SSL certificate CloudProviderIdentifier mismatch for cloud platform \"%s\": expected=\"%s\", actual=\"%s\"", cloudPlatform,
                            cloudProviderIdentifierExpected, cloudProviderIdentifier));
        }

        if (Strings.isNullOrEmpty(cert.certPem())) {
            throw new IllegalStateException(String.format("Blank PEM in SSL certificate with CloudProviderIdentifier \"%s\" for cloud platform \"%s\"",
                    cloudProviderIdentifierExpected, cloudPlatform));
        }
    }

}
