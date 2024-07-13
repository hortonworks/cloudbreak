package com.sequenceiq.redbeams.service.sslcertificate;

import static com.sequenceiq.cloudbreak.cloud.model.DatabaseServer.SSL_CERTIFICATE_IDENTIFIER;
import static com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslCertificateType.CLOUD_PROVIDER_OWNED;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.database.CloudDatabaseServerSslCertificate;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.SslConfig;

@Service
public class DatabaseServerSslCertificatePrescriptionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseServerSslCertificatePrescriptionService.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private SslConfigService sslConfigService;

    public Optional<String> prescribeSslCertificateIfNeeded(CloudContext cloudContext, CloudCredential cloudCredential,
            DBStack dbStack, DatabaseStack databaseStack, Optional<CloudDatabaseServerSslCertificate> sslCertificateOnCloudSide) throws Exception {
        Optional<SslConfig> sslConfig = sslConfigService.fetchById(dbStack.getSslConfig());
        if (sslConfig.isPresent()) {
            return prescribeSslCertificateIfNeeded(
                    cloudContext,
                    cloudCredential,
                    dbStack,
                    sslConfig.get().getSslCertificateActiveCloudProviderIdentifier(),
                    databaseStack,
                    sslCertificateOnCloudSide);
        }
        return Optional.empty();
    }

    public Optional<String> prescribeSslCertificateIfNeeded(CloudContext cloudContext, CloudCredential cloudCredential,
            DBStack dbStack, String sslCertificateActiveCloudProviderIdentifier,
            DatabaseStack databaseStack, Optional<CloudDatabaseServerSslCertificate> certificateOnProvider) throws Exception {
        Optional<SslConfig> sslConfig = sslConfigService.fetchById(dbStack.getSslConfig());
        String cloudPlatform = dbStack.getCloudPlatform();
        if (sslConfig.isPresent() && CLOUD_PROVIDER_OWNED.equals(sslConfig.get().getSslCertificateType())
                && CloudPlatform.AWS.name().equals(cloudPlatform)) {
            return prescribeSslCertificateIfNeededAws(
                    cloudContext,
                    cloudCredential,
                    dbStack.getCloudPlatform(),
                    sslCertificateActiveCloudProviderIdentifier,
                    databaseStack.getDatabaseServer(),
                    certificateOnProvider);
        } else {
            LOGGER.info(
                    "SSL not enabled or unsupported cloud platform \"{}\": SslConfig={}. " +
                            "Skipping SSL certificate CloudProviderIdentifier prescription for database stack {}",
                    cloudPlatform, sslConfig, cloudContext);
        }
        return Optional.empty();
    }

    private Optional<String> prescribeSslCertificateIfNeededAws(CloudContext cloudContext, CloudCredential cloudCredential, String cloudPlatform,
            String desiredSslCertificateIdentifier, DatabaseServer databaseServer,
            Optional<CloudDatabaseServerSslCertificate> certificateOnProvider) throws Exception {
        if (desiredSslCertificateIdentifier != null) {
            return prescribeSslCertificateAws(
                    cloudContext,
                    cloudCredential,
                    cloudPlatform,
                    desiredSslCertificateIdentifier,
                    databaseServer,
                    certificateOnProvider);
        } else {
            LOGGER.info("No SSL certificate CloudProviderIdentifier to prescribe for cloud platform \"{}\". Using default setting for database stack {}.",
                    cloudPlatform, cloudContext);
        }
        return Optional.empty();
    }

    private Optional<String> prescribeSslCertificateAws(CloudContext cloudContext, CloudCredential cloudCredential, String cloudPlatform,
            String desiredSslCertificateIdentifier, DatabaseServer databaseServer,
            Optional<CloudDatabaseServerSslCertificate> certificateOnProvider) throws Exception {
        Set<CloudDatabaseServerSslCertificate> availableSslCertificates = getAvailableSslCertificates(cloudContext, cloudCredential, cloudPlatform);
        Optional<String> overriddenSslCertificateIdentifierOpt = getOverriddenSslCertificateIdentifier(cloudContext, cloudPlatform, availableSslCertificates);
        Set<String> availableSslCertificateIdentifiers = getAvailableSslCertificateIdentifiers(cloudContext, cloudPlatform, availableSslCertificates);
        if (overriddenSslCertificateIdentifierOpt.isPresent()) {
            String overriddenSslCertificateIdentifier = overriddenSslCertificateIdentifierOpt.get();
            LOGGER.info("Found overridden SSL certificate CloudProviderIdentifier for cloud platform \"{}\": \"{}\". ",
                    overriddenSslCertificateIdentifier, cloudContext);
            if (overriddenSslCertificateIdentifier.equals(desiredSslCertificateIdentifier)) {
                if (certificateOnProvider.isPresent() && !overriddenSslCertificateIdentifier.equals(certificateOnProvider.get().certificateIdentifier())) {
                    databaseServer.putParameter(SSL_CERTIFICATE_IDENTIFIER, desiredSslCertificateIdentifier);
                    LOGGER.info("Overridden SSL certificate identifier differs from RDS instance's SSL certificate identifier. " +
                                    "Prescribing SSL certificate CloudProviderIdentifier for cloud platform \"{}\": \"{}\", database stack {}", cloudPlatform,
                            desiredSslCertificateIdentifier, cloudContext);
                    return Optional.of(desiredSslCertificateIdentifier);
                }
                LOGGER.info("Desired SSL certificate CloudProviderIdentifier matches the overridden one. Skipping prescription for database stack {}.",
                        cloudContext);
            } else {
                LOGGER.info("Ignoring desired SSL certificate CloudProviderIdentifier that is different from the overridden one: \"{}\". " +
                                "The latter will be later synced back to SslConfig in DatabaseServerSslCertificateSyncService.syncSslCertificateAws(). " +
                                "Skipping prescription for database stack {}.", desiredSslCertificateIdentifier, cloudContext);
            }
        } else if (availableSslCertificateIdentifiers.contains(desiredSslCertificateIdentifier)) {
            if (availableSslCertificateIdentifiers.size() == 1) {
                LOGGER.info("Desired SSL certificate CloudProviderIdentifier is the default for cloud platform \"{}\": \"{}\". " +
                        "Skipping prescription for database stack {}.", cloudPlatform, desiredSslCertificateIdentifier, cloudContext);
            } else {
                databaseServer.putParameter(SSL_CERTIFICATE_IDENTIFIER, desiredSslCertificateIdentifier);
                LOGGER.info("Prescribing SSL certificate CloudProviderIdentifier for cloud platform \"{}\": \"{}\", database stack {}", cloudPlatform,
                        desiredSslCertificateIdentifier, cloudContext);
                return Optional.of(desiredSslCertificateIdentifier);
            }
        } else {
            LOGGER.warn("Unsupported SSL certificate CloudProviderIdentifier for cloud platform \"{}\": \"{}\". " +
                            "Using default certificate setting for database stack {}. The details of the actual SSL certificate will be later synced back to " +
                            "SslConfig in DatabaseServerSslCertificateSyncService.syncSslCertificateAws().", cloudPlatform, desiredSslCertificateIdentifier,
                    cloudContext);
        }
        return Optional.empty();
    }

    private Set<CloudDatabaseServerSslCertificate> getAvailableSslCertificates(CloudContext cloudContext, CloudCredential cloudCredential,
            String cloudPlatform) throws Exception {
        CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
        Set<CloudDatabaseServerSslCertificate> availableSslCertificates = connector.platformResources()
                .databaseServerGeneralSslRootCertificates(cloudCredential, cloudContext.getLocation().getRegion())
                .sslCertificates();
        LOGGER.info("Available SSL certificates for cloud platform \"{}\": \"{}\", database stack {}", cloudPlatform, availableSslCertificates, cloudContext);
        return availableSslCertificates;
    }

    private Optional<String> getOverriddenSslCertificateIdentifier(CloudContext cloudContext, String cloudPlatform,
            Set<CloudDatabaseServerSslCertificate> availableSslCertificates) {
        Optional<String> overriddenSslCertificateIdentifier = availableSslCertificates.stream()
                .filter(CloudDatabaseServerSslCertificate::overridden)
                .map(CloudDatabaseServerSslCertificate::certificateIdentifier)
                .findFirst();
        LOGGER.info("Overridden SSL certificate CloudProviderIdentifier for cloud platform \"{}\": \"{}\", database stack {}", cloudPlatform,
                overriddenSslCertificateIdentifier, cloudContext);
        return overriddenSslCertificateIdentifier;
    }

    private Set<String> getAvailableSslCertificateIdentifiers(CloudContext cloudContext, String cloudPlatform,
            Set<CloudDatabaseServerSslCertificate> availableSslCertificates) {
        Set<String> availableSslCertificateIdentifiers = availableSslCertificates.stream()
                .map(CloudDatabaseServerSslCertificate::certificateIdentifier)
                .collect(Collectors.toSet());
        LOGGER.info("Available SSL certificate CloudProviderIdentifiers for cloud platform \"{}\": \"{}\", database stack {}",
                cloudPlatform,
                availableSslCertificateIdentifiers,
                cloudContext);
        return availableSslCertificateIdentifiers;
    }

}
