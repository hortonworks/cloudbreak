package com.sequenceiq.redbeams.service.stack;

import static com.sequenceiq.redbeams.flow.redbeams.rotate.RedbeamsSslCertRotateEvent.REDBEAMS_SSL_CERT_ROTATE_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.sslmigration.RedbeamsSslMigrationEventSelectors.REDBEAMS_SSL_MIGRATION_EVENT;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslMode;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.exception.RedbeamsException;
import com.sequenceiq.redbeams.flow.RedbeamsFlowManager;
import com.sequenceiq.redbeams.flow.redbeams.rotate.event.SslCertRotateRedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.sslmigration.event.RedbeamsSslMigrationEvent;
import com.sequenceiq.redbeams.service.sslcertificate.SslConfigService;

@Service
public class RedbeamsRotateSslService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsRotateSslService.class);

    @Inject
    private DBStackService dbStackService;

    @Inject
    private RedbeamsFlowManager flowManager;

    @Inject
    private SslConfigService sslConfigService;

    public FlowIdentifier rotateDatabaseServerSslCert(String crn) {
        DBStack dbStack = dbStackService.getByCrn(crn);
        MDCBuilder.buildMdcContext(dbStack);
        LOGGER.debug("Rotate to latest ssl called for: {}", dbStack);
        return flowManager.notify(REDBEAMS_SSL_CERT_ROTATE_EVENT.selector(),
                new SslCertRotateRedbeamsEvent(REDBEAMS_SSL_CERT_ROTATE_EVENT.selector(), dbStack.getId(), false));
    }

    public FlowIdentifier updateToLatestDatabaseServerSslCert(String crn) {
        DBStack dbStack = dbStackService.getByCrn(crn);
        MDCBuilder.buildMdcContext(dbStack);
        LOGGER.debug("Update to latest ssl called for: {}", dbStack);
        return flowManager.notify(REDBEAMS_SSL_CERT_ROTATE_EVENT.selector(),
                new SslCertRotateRedbeamsEvent(REDBEAMS_SSL_CERT_ROTATE_EVENT.selector(), dbStack.getId(), true));
    }

    public DBStack migrateDatabaseServerSslCertFromNonSslToSsl(String crn) {
        DBStack dbStack = dbStackService.getByCrn(crn);
        MDCBuilder.addEnvironmentCrn(dbStack.getEnvironmentId());
        LOGGER.debug("Migrate to latest ssl called for: {}", dbStack);
        sslConfigService.createSslConfig(SslMode.ENABLED, dbStack);
        return dbStackService.getByCrn(crn);
    }

    public FlowIdentifier turnOnSsl(String crn) {
        DBStack dbStack = dbStackService.getByCrn(crn);
        MDCBuilder.addEnvironmentCrn(dbStack.getEnvironmentId());
        LOGGER.debug("Migrate to latest ssl called for: {}", dbStack);
        if (!dbStack.getCloudPlatform().equalsIgnoreCase(CloudPlatform.AWS.name())) {
            throw new RedbeamsException(String.format("SSL DB migration is not supported for the cloud platform: %s",
                dbStack.getCloudPlatform()));
        }
        sslConfigService.createSslConfig(SslMode.ENABLED, dbStack);
        return flowManager.notify(REDBEAMS_SSL_MIGRATION_EVENT.selector(),
                new RedbeamsSslMigrationEvent(REDBEAMS_SSL_MIGRATION_EVENT.selector(), dbStack.getId()));
    }
}
