package com.sequenceiq.redbeams.service.sslcertificate;

import java.util.Set;
import java.util.stream.Collectors;

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
import com.sequenceiq.cloudbreak.cloud.model.database.CloudDatabaseServerSslCertificates;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslCertificateType;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.SslConfig;

@Service
public class DatabaseServerSslCertificatePrescriptionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseServerSslCertificatePrescriptionService.class);

    private final CloudPlatformConnectors cloudPlatformConnectors;

    public DatabaseServerSslCertificatePrescriptionService(CloudPlatformConnectors cloudPlatformConnectors) {
        this.cloudPlatformConnectors = cloudPlatformConnectors;
    }

    public void prescribeSslCertificateIfNeeded(CloudContext cloudContext, CloudCredential cloudCredential, DBStack dbStack, DatabaseStack databaseStack) {
        SslConfig sslConfig = dbStack.getSslConfig();
        String cloudPlatform = dbStack.getCloudPlatform();
        if (sslConfig != null && SslCertificateType.CLOUD_PROVIDER_OWNED.equals(sslConfig.getSslCertificateType())
                && CloudPlatform.AWS.name().equals(cloudPlatform)) {
            prescribeSslCertificateIfNeededAws(cloudContext, cloudCredential, dbStack, databaseStack.getDatabaseServer());
        } else {
            LOGGER.info(
                    "SSL not enabled or unsupported cloud platform \"{}\": SslConfig={}. " +
                            "Skipping SSL certificate CloudProviderIdentifier prescription for database stack {}",
                    cloudPlatform, sslConfig, cloudContext);
        }
    }

    private void prescribeSslCertificateIfNeededAws(CloudContext cloudContext, CloudCredential cloudCredential, DBStack dbStack,
            DatabaseServer databaseServer) {
        String cloudPlatform = dbStack.getCloudPlatform();
        String desiredSslCertificateIdentifier = dbStack.getSslConfig().getSslCertificateActiveCloudProviderIdentifier();
        if (desiredSslCertificateIdentifier != null) {
            prescribeSslCertificateAws(cloudContext, cloudCredential, cloudPlatform, desiredSslCertificateIdentifier, databaseServer);
        } else {
            LOGGER.info("No SSL certificate CloudProviderIdentifier to prescribe for cloud platform \"{}\". Using default setting for database stack {}.",
                    cloudPlatform, cloudContext);
        }
    }

    private void prescribeSslCertificateAws(CloudContext cloudContext, CloudCredential cloudCredential, String cloudPlatform,
            String desiredSslCertificateIdentifier, DatabaseServer databaseServer) {
        Set<String> availableSslCertificateIdentifiers = getAvailableSslCertificateIdentifiers(cloudContext, cloudCredential, cloudPlatform);
        if (availableSslCertificateIdentifiers.contains(desiredSslCertificateIdentifier)) {
            // A more proper condition would be to check if desiredSslCertificateIdentifier was the default among availableSslCertificateIdentifiers,
            // but this cannot be directly decided without launching a DB server and inspecting its associated SSL root cert.
            if (availableSslCertificateIdentifiers.size() == 1) {
                LOGGER.info("Desired SSL certificate CloudProviderIdentifier is the default for cloud platform \"{}\": \"{}\". " +
                        "Skipping prescription for database stack {}.", cloudPlatform, desiredSslCertificateIdentifier, cloudContext);
            } else {
                databaseServer.putParameter(DatabaseServer.SSL_CERTIFICATE_IDENTIFIER, desiredSslCertificateIdentifier);
                LOGGER.info("Prescribing SSL certificate CloudProviderIdentifier for cloud platform \"{}\": \"{}\", database stack {}", cloudPlatform,
                        desiredSslCertificateIdentifier, cloudContext);
            }
        } else {
            // desiredSslCertificateIdentifier is not available at the cloud provider side, so provision DB server using the default SSL cert
            LOGGER.warn("Unsupported SSL certificate CloudProviderIdentifier for cloud platform \"{}\": \"{}\". " +
                    "Using default setting for database stack {}.", cloudPlatform, desiredSslCertificateIdentifier, cloudContext);
        }
    }

    private Set<String> getAvailableSslCertificateIdentifiers(CloudContext cloudContext, CloudCredential cloudCredential, String cloudPlatform) {
        CloudConnector<Object> connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
        CloudDatabaseServerSslCertificates availableSslCertificates =
                connector.platformResources().databaseServerGeneralSslRootCertificates(cloudCredential, cloudContext.getLocation().getRegion());
        Set<String> availableSslCertificateIdentifiers = availableSslCertificates.getSslCertificates().stream()
                .map(CloudDatabaseServerSslCertificate::getCertificateIdentifier)
                .collect(Collectors.toSet());
        LOGGER.info("Available SSL certificate CloudProviderIdentifiers for cloud platform \"{}\": \"{}\", database stack {}", cloudPlatform,
                availableSslCertificateIdentifiers, cloudContext);
        return availableSslCertificateIdentifiers;
    }

}
