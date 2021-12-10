package com.sequenceiq.redbeams.service.sslcertificate;

import java.util.Collections;

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
import com.sequenceiq.redbeams.service.stack.DBStackService;

@Service
public class DatabaseServerSslCertificateSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseServerSslCertificateSyncService.class);

    private final DBStackService dbStackService;

    private final CloudPlatformConnectors cloudPlatformConnectors;

    private final DatabaseServerSslCertificateConfig databaseServerSslCertificateConfig;

    public DatabaseServerSslCertificateSyncService(DBStackService dbStackService, CloudPlatformConnectors cloudPlatformConnectors,
            DatabaseServerSslCertificateConfig databaseServerSslCertificateConfig) {
        this.dbStackService = dbStackService;
        this.cloudPlatformConnectors = cloudPlatformConnectors;
        this.databaseServerSslCertificateConfig = databaseServerSslCertificateConfig;
    }

    public void syncSslCertificateIfNeeded(CloudContext cloudContext, CloudCredential cloudCredential, DBStack dbStack, DatabaseStack databaseStack)
            throws Exception {
        SslConfig sslConfig = dbStack.getSslConfig();
        String cloudPlatform = dbStack.getCloudPlatform();
        if (sslConfig != null && SslCertificateType.CLOUD_PROVIDER_OWNED.equals(sslConfig.getSslCertificateType())
                && CloudPlatform.AWS.name().equals(cloudPlatform)) {
            CloudConnector<Object> connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, cloudCredential);
            CloudDatabaseServerSslCertificate activeSslRootCertificate = connector.resources().getDatabaseServerActiveSslRootCertificate(ac, databaseStack);
            if (activeSslRootCertificate == null) {
                LOGGER.warn("Database server or its SSL certificate does not exist in cloud platform \"{}\" for {}. Skipping synchronization.", cloudPlatform,
                        cloudContext);
            } else {
                syncSslCertificateAws(cloudContext, dbStack, activeSslRootCertificate);
            }
        } else {
            LOGGER.info("SSL not enabled or unsupported cloud platform \"{}\": SslConfig={}. Skipping SSL certificate synchronization for database stack {}",
                    cloudPlatform, sslConfig, cloudContext);
        }
    }

    private void syncSslCertificateAws(CloudContext cloudContext, DBStack dbStack, CloudDatabaseServerSslCertificate activeSslRootCertificate) {
        SslConfig sslConfig = dbStack.getSslConfig();
        String cloudPlatform = dbStack.getCloudPlatform();
        String desiredSslCertificateIdentifier = sslConfig.getSslCertificateActiveCloudProviderIdentifier();
        String activeSslCertificateIdentifier = activeSslRootCertificate.getCertificateIdentifier();
        // Note that while activeSslCertificateIdentifier can never be null, desiredSslCertificateIdentifier == null is indeed possible for legacy DB servers.
        // The latter case is not, however, handled specially, so that the DBStack of the legacy DB server can be also updated to get rid of legacy null values.
        if (activeSslCertificateIdentifier.equals(desiredSslCertificateIdentifier)) {
            LOGGER.info("Active SSL certificate CloudProviderIdentifier for cloud platform \"{}\" matches the desired one: \"{}\", database stack {}",
                    cloudPlatform, activeSslCertificateIdentifier, cloudContext);
        } else {
            // Always sync CloudProviderIdentifier; this may result in an "SSL certificate outdated" status for the DB server registration.
            sslConfig.setSslCertificateActiveCloudProviderIdentifier(activeSslCertificateIdentifier);
            SslCertificateEntry activeSslCertificateEntry =
                    databaseServerSslCertificateConfig.getCertByCloudPlatformAndRegionAndCloudProviderIdentifier(cloudPlatform, dbStack.getRegion(),
                            activeSslCertificateIdentifier);
            if (activeSslCertificateEntry == null) {
                // This is only possible if the newly launched DB server uses a super-recent SSL root certificate that is yet unknown to CB,
                // or if the DB server SSL root certificate is too old and has already been removed from CB.
                // Neither is a typical scenario, but they will always result in an "SSL certificate outdated" status for the DB server registration.
                LOGGER.warn("Mismatching SSL certificate CloudProviderIdentifier for cloud platform \"{}\": desired=\"{}\", actual=\"{}\", " +
                        "database stack {}. Unable to determine version & PEM for the actual CloudProviderIdentifier, leaving database server " +
                        "registration unchanged.", cloudPlatform, desiredSslCertificateIdentifier, activeSslCertificateIdentifier, cloudContext);
            } else {
                // This makes the DB server registration in sync with the cloud provider DB server instance,
                // but it may also result in an "SSL certificate outdated" status for the DB server registration
                // if the cloud provider side SSL certificate lags behind the highest version supported by CB.
                LOGGER.info("Mismatching SSL certificate CloudProviderIdentifier for cloud platform \"{}\": desired=\"{}\", actual=\"{}\", " +
                                "database stack {}. Updating database server registration with the version & PEM of the actual CloudProviderIdentifier.",
                        cloudPlatform, desiredSslCertificateIdentifier, activeSslCertificateIdentifier, cloudContext);
                validateCert(cloudPlatform, activeSslCertificateIdentifier, activeSslCertificateEntry);
                sslConfig.setSslCertificateActiveVersion(activeSslCertificateEntry.getVersion());
                sslConfig.setSslCertificates(Collections.singleton(activeSslCertificateEntry.getCertPem()));
            }
            dbStackService.save(dbStack);
        }
    }

    private void validateCert(String cloudPlatform, String cloudProviderIdentifierExpected, SslCertificateEntry cert) {
        String cloudProviderIdentifier = cert.getCloudProviderIdentifier();
        if (!cloudProviderIdentifierExpected.equals(cloudProviderIdentifier)) {
            throw new IllegalStateException(
                    String.format("SSL certificate CloudProviderIdentifier mismatch for cloud platform \"%s\": expected=\"%s\", actual=\"%s\"", cloudPlatform,
                            cloudProviderIdentifierExpected, cloudProviderIdentifier));
        }

        if (Strings.isNullOrEmpty(cert.getCertPem())) {
            throw new IllegalStateException(String.format("Blank PEM in SSL certificate with CloudProviderIdentifier \"%s\" for cloud platform \"%s\"",
                            cloudProviderIdentifierExpected, cloudPlatform));
        }
    }

}
