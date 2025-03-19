package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.validation;

import static com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.ConnectionNameFormat.USERNAME_ONLY;
import static com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.ConnectionNameFormat.USERNAME_WITH_HOSTNAME;

import java.util.Objects;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseConnectionProperties;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.converter.TargetMajorVersionToUpgradeTargetVersionConverter;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeOnCloudProviderRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeOnCloudProviderResult;
import com.sequenceiq.cloudbreak.service.rdsconfig.DbUsernameConverterService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsClientService;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.UpgradeDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.ConnectionNameFormat;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabasePropertiesV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.UpgradeDatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackBase;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;

@Component
public class ValidateRdsUpgradeOnProviderHandler extends ExceptionCatcherEventHandler<ValidateRdsUpgradeOnCloudProviderRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidateRdsUpgradeOnProviderHandler.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private RedbeamsClientService redbeamsClientService;

    @Inject
    private TargetMajorVersionToUpgradeTargetVersionConverter targetMajorVersionToUpgradeTargetVersionConverter;

    @Inject
    private ExternalDatabaseService externalDatabaseService;

    @Inject
    private SecretService secretService;

    @Inject
    private DbUsernameConverterService dbUsernameConverterService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ValidateRdsUpgradeOnCloudProviderRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ValidateRdsUpgradeOnCloudProviderRequest> event) {
        LOGGER.error("Validating RDS upgrade on cloud provider side has failed", e);
        return new ValidateRdsUpgradeFailedEvent(resourceId, e, DetailedStackStatus.EXTERNAL_DATABASE_UPGRADE_VALIDATION_FAILED);
    }

    @Override
    public Selectable doAccept(HandlerEvent<ValidateRdsUpgradeOnCloudProviderRequest> event) {
        ValidateRdsUpgradeOnCloudProviderRequest request = event.getData();
        Long stackId = request.getResourceId();
        StackDto stack = stackDtoService.getById(stackId);
        try {
            LOGGER.info("Validating RDS upgrade on cloud provider side");
            ClusterView cluster = stack.getCluster();
            UpgradeDatabaseServerV4Request upgradeRequest = getUpgradeDatabaseServerV4Request(request.getVersion(), stack, cluster);
            UpgradeDatabaseServerV4Response response = redbeamsClientService.validateUpgrade(cluster.getDatabaseServerCrn(), upgradeRequest);
            return new ValidateRdsUpgradeOnCloudProviderResult(stackId, request.getVersion(), response.getReason(), response.getFlowIdentifier(),
                    getDatabaseConnectionProperties(upgradeRequest));
        } catch (Exception e) {
            LOGGER.error("Database 'upgrade validation' on cloud provider side has failed for stack: {}", stack.getName(), e);
            return upgradeValidationFailedEvent(stackId, e);
        }
    }

    private DatabaseConnectionProperties getDatabaseConnectionProperties(UpgradeDatabaseServerV4Request upgradeRequest) {
        String userName = Optional.ofNullable(upgradeRequest)
                .map(UpgradeDatabaseServerV4Request::getUpgradedDatabaseSettings)
                .map(DatabaseServerV4StackBase::getRootUserName)
                .orElse(null);
        DatabaseConnectionProperties databaseConnectionProperties = new DatabaseConnectionProperties();
        databaseConnectionProperties.setUsername(userName);
        return databaseConnectionProperties;
    }

    private UpgradeDatabaseServerV4Request getUpgradeDatabaseServerV4Request(TargetMajorVersion targetMajorVersion, StackDto stack,
            ClusterView cluster) {
        UpgradeDatabaseServerV4Request upgradeRequest = new UpgradeDatabaseServerV4Request();
        upgradeRequest.setUpgradeTargetMajorVersion(targetMajorVersionToUpgradeTargetVersionConverter.convert(targetMajorVersion));
        DatabaseServerV4StackRequest migratedRequest = externalDatabaseService.migrateDatabaseSettingsIfNeeded(stack, targetMajorVersion);
        if (Objects.nonNull(migratedRequest)) {
            DatabaseServerV4Response existingDbResponse = ThreadBasedUserCrnProvider.doAsInternalActor(
                    () ->  redbeamsClientService.getByCrn(cluster.getDatabaseServerCrn()));
            ConnectionNameFormat connectionNameFormat = Optional.ofNullable(existingDbResponse.getDatabasePropertiesV4Response())
                    .map(DatabasePropertiesV4Response::getConnectionNameFormat).orElse(USERNAME_ONLY);
            String userName = (connectionNameFormat == USERNAME_WITH_HOSTNAME) ?
                    dbUsernameConverterService.toDatabaseUsername(secretService.getByResponse(existingDbResponse.getConnectionUserName())) :
                    secretService.getByResponse(existingDbResponse.getConnectionUserName());
            migratedRequest.setRootUserName(userName);
            migratedRequest.setRootUserPassword(secretService.getByResponse(existingDbResponse.getConnectionPassword()));
            upgradeRequest.setUpgradedDatabaseSettings(migratedRequest);
        }
        return upgradeRequest;
    }

    private StackEvent upgradeValidationFailedEvent(Long stackId, Exception e) {
        return new ValidateRdsUpgradeFailedEvent(stackId, e, DetailedStackStatus.EXTERNAL_DATABASE_UPGRADE_VALIDATION_FAILED);
    }

}