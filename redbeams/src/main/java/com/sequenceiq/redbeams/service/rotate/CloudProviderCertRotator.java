package com.sequenceiq.redbeams.service.rotate;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import com.sequenceiq.redbeams.configuration.DatabaseServerSslCertificateConfig;
import com.sequenceiq.redbeams.configuration.SslCertificateEntry;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.service.sslcertificate.DatabaseServerSslCertificatePrescriptionService;
import com.sequenceiq.redbeams.service.sslcertificate.DatabaseServerSslCertificateSyncService;
import com.sequenceiq.redbeams.service.stack.DBStackService;

@Service
public class CloudProviderCertRotator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudProviderCertRotator.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private DBStackService dbStackService;

    @Inject
    private DatabaseServerSslCertificateSyncService databaseServerSslCertificateSyncService;

    @Inject
    private DatabaseServerSslCertificatePrescriptionService databaseServerSslCertificatePrescriptionService;

    @Inject
    private DatabaseServerSslCertificateConfig databaseServerSslCertificateConfig;

    public void rotate(Long dbStackId, CloudContext cloudContext, CloudCredential cloudCredential, DatabaseStack databaseStack) throws Exception {
        DBStack dbStack = dbStackService.getById(dbStackId);

        CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
        AuthenticatedContext ac = connector.authentication().authenticate(
                cloudContext,
                cloudCredential);

        ResourceConnector resources = connector.resources();

        ExternalDatabaseStatus status = resources.getDatabaseServerStatus(ac, databaseStack);
        if (status != null && status == ExternalDatabaseStatus.STARTED) {
            int maxVersion = databaseServerSslCertificateConfig.getMaxVersionByCloudPlatformAndRegion(
                    cloudContext.getPlatform().getValue(),
                    cloudContext.getLocation().getRegion().getRegionName());
            SslCertificateEntry cert = databaseServerSslCertificateConfig.getCertByCloudPlatformAndRegionAndVersion(
                    dbStack.getCloudPlatform(),
                    dbStack.getRegion(),
                    maxVersion);
            Optional<String> desiredCertificate = databaseServerSslCertificatePrescriptionService.prescribeSslCertificateIfNeeded(
                    cloudContext,
                    cloudCredential,
                    dbStack,
                    cert.cloudProviderIdentifier(),
                    databaseStack);
            if (desiredCertificate.isPresent()) {
                resources.updateDatabaseServerActiveSslRootCertificate(
                        ac,
                        databaseStack,
                        desiredCertificate.get());
                databaseServerSslCertificateSyncService.syncSslCertificateIfNeeded(
                        cloudContext,
                        cloudCredential,
                        dbStack,
                        databaseStack);
            } else {
                LOGGER.warn("The desired certificate {} not found.", desiredCertificate);
            }
        } else {
            LOGGER.debug("Database server {} must be in started status.", dbStack);
        }
    }
}
