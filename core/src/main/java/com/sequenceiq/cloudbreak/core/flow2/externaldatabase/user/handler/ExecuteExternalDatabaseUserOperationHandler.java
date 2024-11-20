package com.sequenceiq.cloudbreak.core.flow2.externaldatabase.user.handler;

import static com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.PostgresConfigService.POSTGRESQL_USER_SLS;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.user.config.ExternalDatabaseUserEvent.EXTERNAL_DATABASE_USER_OPERATION_FAILED_EVENT;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.common.database.DatabaseCommon;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.PostgresConfigService;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseSelectableEvent;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.user.ExternalDatabaseUserOperation;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.UserOperationExternalDatabaseFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.UserOperationExternalDatabaseRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.UserOperationExternalDatabaseResult;
import com.sequenceiq.cloudbreak.rotation.SecretRotationSaltService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsDbServerConfigurer;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;

@Component
public class ExecuteExternalDatabaseUserOperationHandler extends ExceptionCatcherEventHandler<UserOperationExternalDatabaseRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteExternalDatabaseUserOperationHandler.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private RedbeamsDbServerConfigurer redbeamsDbServerConfigurer;

    @Inject
    private PostgresConfigService postgresConfigService;

    @Inject
    private SecretRotationSaltService secretRotationSaltService;

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private DatabaseCommon dbCommon;

    @Override
    public String selector() {
        return ExternalDatabaseSelectableEvent.selector(UserOperationExternalDatabaseRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UserOperationExternalDatabaseRequest> event) {
        return new UserOperationExternalDatabaseFailed(resourceId, EXTERNAL_DATABASE_USER_OPERATION_FAILED_EVENT.event(), event.getData().getResourceName(),
                event.getData().getResourceCrn(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<UserOperationExternalDatabaseRequest> event) {
        UserOperationExternalDatabaseRequest data = event.getData();
        StackDto stackDto = stackDtoService.getByCrn(data.getResourceCrn());
        if (!RedbeamsDbServerConfigurer.isRemoteDatabaseRequested(stackDto.getCluster().getDatabaseServerCrn())) {
            throw new CloudbreakServiceException("Database user operation is supported for external databases only!");
        }
        DatabaseType databaseType = data.getDatabaseType();
        String databaseNameByType = postgresConfigService.getDatabaseNameByType(databaseType);
        Optional<RDSConfig> rdsConfig = getRdsConfig(data, stackDto, databaseNameByType);
        executeSaltState(stackDto, rdsConfig, data);
        handleRdsConfig(rdsConfig, data.getOperation());
        return new UserOperationExternalDatabaseResult(data.getResourceId(),
                ExternalDatabaseSelectableEvent.selector(UserOperationExternalDatabaseResult.class), data.getResourceName(), data.getResourceCrn());
    }

    private Optional<RDSConfig> getRdsConfig(UserOperationExternalDatabaseRequest data, StackDto stackDto, String dbName) {
        DatabaseServerV4Response dbServer = redbeamsDbServerConfigurer.getDatabaseServer(stackDto.getCluster().getDatabaseServerCrn());
        String connectionUrl = dbCommon.getJdbcConnectionUrl(dbServer.getDatabaseVendor(), dbServer.getHost(), dbServer.getPort(), Optional.of(dbName));
        String dbUser = data.getDatabaseUser();
        Optional<RDSConfig> relatedRdsConfig = rdsConfigService.findAllByConnectionUrlAndType(connectionUrl)
                .stream().filter(rdsConfig -> StringUtils.equals(rdsConfig.getConnectionUserName(), dbUser)).findFirst();
        switch (data.getOperation()) {
            case CREATION -> {
                if (relatedRdsConfig.isPresent()) {
                    LOGGER.warn("Database entry already exists for connection URL {} and username {} for {} database.", connectionUrl, dbUser, dbName);
                } else {
                    return Optional.of(createNewRdsConfigObject(data, stackDto, dbName, dbServer));
                }
            }
            case DELETION -> {
                if (relatedRdsConfig.isEmpty()) {
                    LOGGER.warn("Database entry already deleted for connection URL {} and username {} for {} database.", connectionUrl, dbUser, dbName);
                    return Optional.empty();
                }
            }
            default -> LOGGER.warn("Uncovered operation, falling back to default RdsConfig based on parameters.");
        }
        return relatedRdsConfig;
    }

    private RDSConfig createNewRdsConfigObject(UserOperationExternalDatabaseRequest data, StackDto stackDto, String dbName, DatabaseServerV4Response dbServer) {
        DatabaseType databaseType = data.getDatabaseType();
        RDSConfig rdsConfig = redbeamsDbServerConfigurer.createNewRdsConfigForNewUser(data.getResourceName(), data.getResourceId(),
                dbServer.getCrn(), stackDto.getCluster().getId(), dbName, data.getDatabaseUser(), databaseType);
        rdsConfig.setWorkspace(stackDto.getWorkspace());
        return rdsConfig;
    }

    private void executeSaltState(StackDto stackDto, Optional<RDSConfig> rdsConfig, UserOperationExternalDatabaseRequest data) {
        try {
            SaltPillarProperties pillarProperties = getPillarPropertiesForUserOperation(stackDto, rdsConfig, data);
            secretRotationSaltService.updateSaltPillar(stackDto, Map.of(PostgresConfigService.POSTGRES_USER, pillarProperties));
            secretRotationSaltService.executeSaltStateOnPrimaryGateway(stackDto, getUserOperationState(data.getOperation()));
        } catch (CloudbreakOrchestratorFailedException e) {
            throw new RuntimeException(e);
        }
    }

    private SaltPillarProperties getPillarPropertiesForUserOperation(StackDto stackDto, Optional<RDSConfig> rdsConfig,
            UserOperationExternalDatabaseRequest data) {
        return switch (data.getOperation()) {
            case CREATION -> postgresConfigService.getPillarPropertiesForUserCreation(stackDto,
                    rdsConfig.orElseThrow(() -> new RuntimeException("No database entry present!")), data.getDatabaseType());
            case DELETION -> postgresConfigService.getPillarPropertiesForUserDeletion(stackDto, data.getDatabaseUser(), data.getDatabaseType());
            default -> new SaltPillarProperties(POSTGRESQL_USER_SLS, Map.of());
        };
    }

    private List<String> getUserOperationState(ExternalDatabaseUserOperation operation) {
        return switch (operation) {
            case CREATION -> List.of("postgresql.newuser.init");
            case DELETION -> List.of("postgresql.deleteuser.init");
            default -> List.of();
        };
    }

    private void handleRdsConfig(Optional<RDSConfig> rdsConfig, ExternalDatabaseUserOperation operation) {
        switch (operation) {
            case CREATION -> rdsConfigService.pureSave(rdsConfig.orElseThrow(() -> new RuntimeException("No database entry present!")));
            case DELETION -> rdsConfig.ifPresent(rds -> rdsConfigService.delete(rds));
            default -> LOGGER.error("Uncovered database operation, nothing to do with RdsConfig.");
        }
    }
}
