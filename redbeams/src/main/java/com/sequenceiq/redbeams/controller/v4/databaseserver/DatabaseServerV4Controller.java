package com.sequenceiq.redbeams.controller.v4.databaseserver;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.CREATE_DATABASE;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_ENVIRONMENT;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.CRN;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.slf4j.Logger;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.AccountIdNotNeeded;
import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByRequestProperty;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrnList;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.annotation.ResourceCrnList;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.InitiatorUserCrn;
import com.sequenceiq.cloudbreak.auth.security.internal.RequestObject;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.api.UsedSubnetsByEnvironmentResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.RetryableFlowResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.database.request.CreateDatabaseV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.database.responses.CreateDatabaseV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.AllocateDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.ClusterDatabaseServerCertificateStatusV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.DatabaseServerCertificateStatusV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.DatabaseServerTestV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.DatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.RotateDatabaseServerSecretV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.UpgradeDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.ClusterDatabaseServerCertificateStatusV4Responses;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerCertificateStatusV4Responses;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerStatusV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerTestV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Responses;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.UpgradeDatabaseServerV4Response;
import com.sequenceiq.redbeams.converter.stack.AllocateDatabaseServerV4RequestToDBStackConverter;
import com.sequenceiq.redbeams.converter.upgrade.UpgradeDatabaseResponseToUpgradeDatabaseServerV4ResponseConverter;
import com.sequenceiq.redbeams.converter.upgrade.UpgradeDatabaseServerV4RequestToUpgradeDatabaseServerRequestConverter;
import com.sequenceiq.redbeams.converter.v4.databaseserver.DBStackToDatabaseServerStatusV4ResponseConverter;
import com.sequenceiq.redbeams.converter.v4.databaseserver.DatabaseServerConfigToDatabaseServerV4ResponseConverter;
import com.sequenceiq.redbeams.converter.v4.databaseserver.DatabaseServerV4RequestToDatabaseServerConfigConverter;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.upgrade.UpgradeDatabaseRequest;
import com.sequenceiq.redbeams.service.RedBeamsRetryService;
import com.sequenceiq.redbeams.service.dbserverconfig.DatabaseServerConfigService;
import com.sequenceiq.redbeams.service.dbserverconfig.DatabaseServerSslCertificateConfigService;
import com.sequenceiq.redbeams.service.rotation.RedbeamsRotationService;
import com.sequenceiq.redbeams.service.stack.RedbeamsCreationService;
import com.sequenceiq.redbeams.service.stack.RedbeamsRotateSslService;
import com.sequenceiq.redbeams.service.stack.RedbeamsStartService;
import com.sequenceiq.redbeams.service.stack.RedbeamsStopService;
import com.sequenceiq.redbeams.service.stack.RedbeamsTerminationService;
import com.sequenceiq.redbeams.service.stack.RedbeamsUpgradeService;
import com.sequenceiq.redbeams.service.validation.RedBeamsTagValidator;

@Controller
@Transactional(TxType.NEVER)
public class DatabaseServerV4Controller implements DatabaseServerV4Endpoint {

    static final Long DEFAULT_WORKSPACE = 0L;

    private static final Logger LOGGER = getLogger(DatabaseServerV4Controller.class);

    @Inject
    private RedbeamsCreationService redbeamsCreationService;

    @Inject
    private RedbeamsTerminationService redbeamsTerminationService;

    @Inject
    private RedbeamsStartService redbeamsStartService;

    @Inject
    private RedbeamsStopService redbeamsStopService;

    @Inject
    private RedbeamsRotateSslService redbeamsRotateSslService;

    @Inject
    private RedbeamsUpgradeService redbeamsUpgradeService;

    @Inject
    private RedBeamsTagValidator redBeamsTagValidator;

    @Inject
    private DatabaseServerConfigService databaseServerConfigService;

    @Inject
    private DatabaseServerSslCertificateConfigService databaseServerSslCertificateConfigService;

    @Inject
    private AllocateDatabaseServerV4RequestToDBStackConverter dbStackConverter;

    @Inject
    private DatabaseServerConfigToDatabaseServerV4ResponseConverter databaseServerConfigToDatabaseServerV4ResponseConverter;

    @Inject
    private DBStackToDatabaseServerStatusV4ResponseConverter dbStackToDatabaseServerStatusV4ResponseConverter;

    @Inject
    private DatabaseServerV4RequestToDatabaseServerConfigConverter databaseServerV4RequestToDatabaseServerConfigConverter;

    @Inject
    private UpgradeDatabaseServerV4RequestToUpgradeDatabaseServerRequestConverter upgradeDatabaseServerV4RequestConverter;

    @Inject
    private UpgradeDatabaseResponseToUpgradeDatabaseServerV4ResponseConverter upgradeDatabaseServerV4ResponseConverter;

    @Inject
    private RedbeamsRotationService redbeamsRotationService;

    @Inject
    private RedBeamsRetryService retryService;

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_ENVIRONMENT)
    public DatabaseServerV4Responses list(@ResourceCrn String environmentCrn) {
        Set<DatabaseServerConfig> all = databaseServerConfigService.findAll(DEFAULT_WORKSPACE, environmentCrn);
        return new DatabaseServerV4Responses(all.stream()
                .map(d -> databaseServerConfigToDatabaseServerV4ResponseConverter.convert(d))
                .collect(Collectors.toSet())
        );
    }

    @Override
    @InternalOnly
    public DatabaseServerCertificateStatusV4Responses listDatabaseServersCertificateStatus(DatabaseServerCertificateStatusV4Request request,
            @InitiatorUserCrn String initiatorUserCrn) {
        return databaseServerSslCertificateConfigService.listDatabaseServersCertificateStatus(request, ThreadBasedUserCrnProvider.getAccountId());
    }

    @Override
    @InternalOnly
    public ClusterDatabaseServerCertificateStatusV4Responses listDatabaseServersCertificateStatusByStackCrns(
            ClusterDatabaseServerCertificateStatusV4Request request, @InitiatorUserCrn String initiatorUserCrn) {
        return databaseServerSslCertificateConfigService.listDatabaseServersCertificateStatus(request, ThreadBasedUserCrnProvider.getAccountId());
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_DATABASE_SERVER)
    public DatabaseServerV4Response getByName(@ResourceCrn String environmentCrn, @ResourceName String name) {
        DatabaseServerConfig server = databaseServerConfigService.getByName(DEFAULT_WORKSPACE, environmentCrn, name);
        return databaseServerConfigToDatabaseServerV4ResponseConverter.convert(server);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_DATABASE_SERVER)
    public DatabaseServerV4Response getByCrn(@ResourceCrn String crn) {
        DatabaseServerConfig server = databaseServerConfigService.getByCrn(crn);
        return databaseServerConfigToDatabaseServerV4ResponseConverter.convert(server);
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_ENVIRONMENT)
    public DatabaseServerV4Response getByClusterCrn(@ResourceCrn String environmentCrn, String clusterCrn) {
        DatabaseServerConfig server = databaseServerConfigService.findByClusterCrn(environmentCrn, clusterCrn).orElseThrow(() ->
                new NotFoundException(String.format("No database server config found with cluster CRN '%s' in environment '%s'", clusterCrn, environmentCrn)));
        return databaseServerConfigToDatabaseServerV4ResponseConverter.convert(server);
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_ENVIRONMENT)
    public DatabaseServerV4Responses listByClusterCrn(@ResourceCrn String environmentCrn, String clusterCrn) {
        List<DatabaseServerConfig> servers = databaseServerConfigService.listByClusterCrn(environmentCrn, clusterCrn);
        return new DatabaseServerV4Responses(servers.stream()
                .map(server -> databaseServerConfigToDatabaseServerV4ResponseConverter.convert(server))
                .collect(Collectors.toSet())
        );
    }

    @Override
    @InternalOnly
    public DatabaseServerStatusV4Response createInternal(AllocateDatabaseServerV4Request request, @InitiatorUserCrn String initiatorUserCrn) {
        return create(request);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_DATABASE_SERVER)
    public DatabaseServerStatusV4Response create(AllocateDatabaseServerV4Request request) {
        return createDatabaseServer(request, false);
    }

    @Override
    @InternalOnly
    public DatabaseServerStatusV4Response migrateDatabaseToSslByCrnInternal(@ResourceCrn String crn, @InitiatorUserCrn String initiatorUserCrn) {
        return null;
    }

    @Override
    @InternalOnly
    public void enforceSslOnDatabaseByCrnInternal(@ResourceCrn String crn, @InitiatorUserCrn String initiatorUserCrn) {
    }

    @Override
    @InternalOnly
    public DatabaseServerStatusV4Response createNonUniqueInternal(AllocateDatabaseServerV4Request request, @InitiatorUserCrn String initiatorUserCrn) {
        return createDatabaseServer(request, true);
    }

    private DatabaseServerStatusV4Response createDatabaseServer(AllocateDatabaseServerV4Request request, boolean enableMultipleDatabaseServers) {
        MDCBuilder.addEnvironmentCrn(request.getEnvironmentCrn());
        DBStack dbStack = dbStackConverter.convert(request, ThreadBasedUserCrnProvider.getUserCrn());
        ValidationResult validationResult = redBeamsTagValidator.validateTags(dbStack.getCloudPlatform(), request.getTags());
        if (validationResult.hasError()) {
            throw new IllegalArgumentException(validationResult.getFormattedErrors());
        }
        DBStack savedDBStack;
        LOGGER.debug("Database server creation called with parameters: {}", request);
        if (enableMultipleDatabaseServers) {
            savedDBStack = redbeamsCreationService.launchMultiDatabaseServer(dbStack, request.getClusterCrn(), request.getNetwork());
        } else {
            savedDBStack = redbeamsCreationService.launchDatabaseServer(dbStack, request.getClusterCrn(), request.getNetwork());
        }
        return dbStackToDatabaseServerStatusV4ResponseConverter.convert(savedDBStack);
    }

    @Override
    @InternalOnly
    public void updateClusterCrn(String environmentCrn, String currentClusterCrn, String newClusterCrn, @InitiatorUserCrn String initiatorUserCrn) {
        List<DatabaseServerConfig> databaseServerConfigs = databaseServerConfigService.listByClusterCrn(environmentCrn, currentClusterCrn);
        databaseServerConfigs.forEach(databaseServerConfig -> databaseServerConfig.setClusterCrn(newClusterCrn));
        databaseServerConfigService.updateAll(databaseServerConfigs);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DELETE_DATABASE_SERVER)
    public DatabaseServerV4Response release(@ResourceCrn String crn) {
        DatabaseServerConfig server = databaseServerConfigService.release(crn);
        return databaseServerConfigToDatabaseServerV4ResponseConverter.convert(server);
    }

    @CheckPermissionByAccount(action = AuthorizationResourceAction.REGISTER_DATABASE_SERVER)
    public DatabaseServerV4Response register(DatabaseServerV4Request request) {
        MDCBuilder.addEnvironmentCrn(request.getEnvironmentCrn());
        DatabaseServerConfig server = databaseServerConfigService.create(
                databaseServerV4RequestToDatabaseServerConfigConverter.convert(request), DEFAULT_WORKSPACE);
        return databaseServerConfigToDatabaseServerV4ResponseConverter.convert(server);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DELETE_DATABASE_SERVER)
    public DatabaseServerV4Response deleteByCrn(@ResourceCrn String crn, boolean force) {
        // RedbeamsTerminationService handles both service-managed and user-managed database servers
        DatabaseServerConfig deleted = redbeamsTerminationService.terminateByCrn(crn, force);
        return databaseServerConfigToDatabaseServerV4ResponseConverter.convert(deleted);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DELETE_DATABASE_SERVER)
    public DatabaseServerV4Response deleteByName(@ResourceCrn String environmentCrn, @ResourceName String name, boolean force) {
        // RedbeamsTerminationService handles both service-managed and user-managed database servers
        DatabaseServerConfig deleted = redbeamsTerminationService.terminateByName(environmentCrn, name, force);
        return databaseServerConfigToDatabaseServerV4ResponseConverter.convert(deleted);
    }

    @Override
    @CheckPermissionByResourceCrnList(action = AuthorizationResourceAction.DELETE_DATABASE_SERVER)
    public DatabaseServerV4Responses deleteMultiple(@ResourceCrnList Set<String> crns, boolean force) {
        // RedbeamsTerminationService handles both service-managed and user-managed database servers
        Set<DatabaseServerConfig> deleted = redbeamsTerminationService.terminateMultipleByCrn(crns, force);
        return new DatabaseServerV4Responses(deleted.stream()
                .map(d -> databaseServerConfigToDatabaseServerV4ResponseConverter.convert(d))
                .collect(Collectors.toSet()));
    }

    @Override
    @CheckPermissionByRequestProperty(path = "existingDatabaseServerCrn", type = CRN, action = AuthorizationResourceAction.DESCRIBE_DATABASE_SERVER)
    public DatabaseServerTestV4Response test(@RequestObject DatabaseServerTestV4Request request) {
        throw new UnsupportedOperationException("Connection testing is disabled for security reasons until further notice");
    }

    @Override
    @CheckPermissionByRequestProperty(path = "existingDatabaseServerCrn", type = CRN, action = CREATE_DATABASE)
    public CreateDatabaseV4Response createDatabase(@RequestObject CreateDatabaseV4Request request) {
        String result = databaseServerConfigService.createDatabaseOnServer(
                request.getExistingDatabaseServerCrn(),
                request.getDatabaseName(),
                request.getType(),
                Optional.ofNullable(request.getDatabaseDescription()));
        return new CreateDatabaseV4Response(result);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.START_DATABASE_SERVER)
    public void start(@ResourceCrn String crn) {
        redbeamsStartService.startDatabaseServer(crn);
    }

    @Override
    @InternalOnly
    @AccountIdNotNeeded
    public FlowIdentifier rotateSslCert(@ResourceCrn String crn) {
        return redbeamsRotateSslService.rotateDatabaseServerSslCert(crn);
    }

    @Override
    @InternalOnly
    @AccountIdNotNeeded
    public FlowIdentifier updateToLatestSslCert(@ResourceCrn String crn) {
        return redbeamsRotateSslService.updateToLatestDatabaseServerSslCert(crn);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.STOP_DATABASE_SERVER)
    public void stop(@ResourceCrn String crn) {
        redbeamsStopService.stopDatabaseServer(crn);
    }

    @Override
    @InternalOnly
    public UpgradeDatabaseServerV4Response upgrade(@ResourceCrn String databaseServerCrn, UpgradeDatabaseServerV4Request request) {
        UpgradeDatabaseRequest upgradeDatabaseRequest = upgradeDatabaseServerV4RequestConverter.convert(request);
        return upgradeDatabaseServerV4ResponseConverter.convert(redbeamsUpgradeService.upgradeDatabaseServer(databaseServerCrn, upgradeDatabaseRequest));
    }

    @Override
    @InternalOnly
    public UsedSubnetsByEnvironmentResponse getUsedSubnetsByEnvironment(@ResourceCrn String environmentCrn) {
        LOGGER.info("We don't store the used subnet id so we don't give it back");
        return new UsedSubnetsByEnvironmentResponse(Collections.emptyList());
    }

    @Override
    @InternalOnly
    public UpgradeDatabaseServerV4Response validateUpgrade(@ResourceCrn String crn, UpgradeDatabaseServerV4Request request) {
        UpgradeDatabaseRequest upgradeDatabaseRequest = upgradeDatabaseServerV4RequestConverter.convert(request);
        return upgradeDatabaseServerV4ResponseConverter.convert(redbeamsUpgradeService.validateUpgradeDatabaseServer(crn, upgradeDatabaseRequest));
    }

    @Override
    @InternalOnly
    public UpgradeDatabaseServerV4Response validateUpgradeCleanup(@ResourceCrn String crn) {
        return upgradeDatabaseServerV4ResponseConverter.convert(redbeamsUpgradeService.validateUpgradeDatabaseServerCleanup(crn));
    }

    @Override
    @InternalOnly
    public FlowIdentifier rotateSecret(RotateDatabaseServerSecretV4Request request, @InitiatorUserCrn String initiatorUserCrn) {
        return redbeamsRotationService.rotateSecrets(request.getCrn(), List.of(request.getSecret()), request.getExecutionType(),
                request.getAdditionalProperties());
    }

    @Override
    @InternalOnly
    @AccountIdNotNeeded
    public FlowIdentifier retry(@ResourceCrn String databaseCrn) {
        return retryService.retry(databaseCrn);
    }

    @Override
    @InternalOnly
    @AccountIdNotNeeded
    public List<RetryableFlowResponse> listRetryableFlows(@ResourceCrn String databaseCrn) {
        return retryService.getRetryableFlows(databaseCrn);
    }
}