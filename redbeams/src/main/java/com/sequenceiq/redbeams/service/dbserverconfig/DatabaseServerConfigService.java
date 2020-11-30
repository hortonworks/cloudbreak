package com.sequenceiq.redbeams.service.dbserverconfig;

import static com.sequenceiq.redbeams.service.RedbeamsConstants.DATABASE_TEST_RESULT_SUCCESS;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.InternalServerErrorException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.validation.FieldError;
import org.springframework.validation.MapBindingResult;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.ResourceBasedCrnProvider;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.common.archive.AbstractArchivistService;
import com.sequenceiq.cloudbreak.common.database.DatabaseCommon;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.logger.MDCUtils;
import com.sequenceiq.redbeams.api.endpoint.v4.ResourceStatus;
import com.sequenceiq.redbeams.domain.DatabaseConfig;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.exception.ConflictException;
import com.sequenceiq.redbeams.exception.NotFoundException;
import com.sequenceiq.redbeams.exception.RedbeamsException;
import com.sequenceiq.redbeams.repository.DatabaseServerConfigRepository;
import com.sequenceiq.redbeams.service.PasswordGeneratorService;
import com.sequenceiq.redbeams.service.UserGeneratorService;
import com.sequenceiq.redbeams.service.crn.CrnService;
import com.sequenceiq.redbeams.service.dbconfig.DatabaseConfigService;
import com.sequenceiq.redbeams.service.drivers.DriverFunctions;
import com.sequenceiq.redbeams.service.stack.DBStackService;
import com.sequenceiq.redbeams.service.validation.DatabaseServerConnectionValidator;

@Service
public class DatabaseServerConfigService extends AbstractArchivistService<DatabaseServerConfig> implements ResourceBasedCrnProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseServerConfigService.class);

    private static final Pattern VALID_DATABASE_NAME = Pattern.compile("^[\\p{Alnum}_][\\p{Alnum}_-]*$");

    @Inject
    private DatabaseServerConfigRepository repository;

    @Inject
    private DatabaseConfigService databaseConfigService;

    @Inject
    private DBStackService dbStackService;

    @Inject
    private DriverFunctions driverFunctions;

    @Inject
    private DatabaseCommon databaseCommon;

    @Inject
    private DatabaseServerConnectionValidator connectionValidator;

    @Inject
    private Clock clock;

    @Inject
    private TransactionService transactionService;

    @Inject
    private CrnService crnService;

    @Inject
    private UserGeneratorService userGeneratorService;

    @Inject
    private PasswordGeneratorService passwordGeneratorService;

    @Inject
    private GrpcUmsClient grpcUmsClient;

    public Set<DatabaseServerConfig> findAll(Long workspaceId, String environmentCrn) {
        if (environmentCrn == null) {
            throw new IllegalArgumentException("No environment CRN supplied.");
        }

        return repository.findByWorkspaceIdAndEnvironmentId(workspaceId, environmentCrn);
    }

    public DatabaseServerConfig create(DatabaseServerConfig resource, Long workspaceId, boolean test) {

        if (repository.findByName(resource.getName()).isPresent()) {
            throw new BadRequestException(String.format("%s already exists with name '%s' in workspace %d",
                    DatabaseServerConfig.class.getSimpleName(), resource.getName(), resource.getWorkspaceId()));
        }
        if (resource.getConnectionDriver() == null) {
            resource.setConnectionDriver(resource.getDatabaseVendor().connectionDriver());
            LOGGER.info("Database server configuration lacked a connection driver; defaulting to {}",
                    resource.getConnectionDriver());
        }

        if (test) {
            // FIXME? Currently no checks if logged-in user has access to workspace
            // Compare with AbstractWorkspaceAwareResourceService
            String testResults = testConnection(resource);

            if (!testResults.equals(DATABASE_TEST_RESULT_SUCCESS)) {
                throw new IllegalArgumentException(testResults);
            }
        }

        try {
            MDCBuilder.buildMdcContext(resource);
            // prepareCreation(resource);
            resource.setCreationDate(clock.getCurrentTimeMillis());
            if (resource.getResourceCrn() == null) {
                Crn crn = crnService.createCrn(resource);
                resource.setResourceCrn(crn);
                resource.setAccountId(crn.getAccountId());
            }
            resource.setWorkspaceId(workspaceId);
            return transactionService.required(() -> {
                DatabaseServerConfig saved = repository.save(resource);
                grpcUmsClient.assignResourceOwnerRoleIfEntitled(ThreadBasedUserCrnProvider.getUserCrn(),
                        saved.getResourceCrn().toString(), ThreadBasedUserCrnProvider.getAccountId());
                return saved;
            });
        } catch (TransactionService.TransactionExecutionException e) {
            LOGGER.error("Error happened during database server creation: ", e);
            throw new InternalServerErrorException(e);
        }
    }

    public DatabaseServerConfig update(DatabaseServerConfig resource) {
        MDCBuilder.buildMdcContext(resource);
        return repository.save(resource);
    }

    public DatabaseServerConfig release(String resourceCrn) {
        try {
            return transactionService.required(() -> {
                DatabaseServerConfig resource = getByCrn(resourceCrn);
                if (!resource.getResourceStatus().isReleasable()) {
                    throw new ConflictException(String.format("Database server configuration has unreleasable resource "
                        + "status %s: releasable statuses are %s", resource.getResourceStatus(), ResourceStatus.getReleasableValues()));
                }

                Optional<DBStack> dbStack = resource.getDbStack();
                if (dbStack.isPresent()) {
                    dbStackService.delete(dbStack.get());
                    resource.setDbStack(null);
                } else {
                    LOGGER.info("Database stack missing for crn: '{}', continuing anyway", resourceCrn);
                }

                resource.setResourceStatus(ResourceStatus.USER_MANAGED);
                return repository.save(resource);
            });
        } catch (TransactionService.TransactionExecutionException e) {
            throw e.getCause() != null ? e.getCause() : new RedbeamsException("Failed to release management of " + resourceCrn);
        }
    }

    @Override
    public DatabaseServerConfig delete(DatabaseServerConfig resource) {
        if (resource.getDatabases() != null) {
            for (DatabaseConfig db : resource.getDatabases()) {
                databaseConfigService.delete(db, true, true);
            }
        }

        // Reload the entity so that we start without any referenced databases in it
        // Otherwise, JPA/Hibernate pitches a fit
        DatabaseServerConfig resourceToDelete = getByCrn(resource.getResourceCrn()).get();
        DatabaseServerConfig archived = super.delete(resourceToDelete);
        grpcUmsClient.notifyResourceDeleted(archived.getResourceCrn().toString(), MDCUtils.getRequestId());
        return archived;
    }

    public DatabaseServerConfig getByName(Long workspaceId, String environmentCrn, String name) {
        Optional<DatabaseServerConfig> resourceOpt = repository.findByNameAndWorkspaceIdAndEnvironmentId(name, workspaceId, environmentCrn);
        if (resourceOpt.isEmpty()) {
            throw new NotFoundException(String.format("No %s found with name '%s' in environment '%s'",
                    DatabaseServerConfig.class.getSimpleName(), name, environmentCrn));
        }
        MDCBuilder.buildMdcContext(resourceOpt.get());
        return resourceOpt.get();
    }

    public DatabaseServerConfig getByCrn(String resourceCrn) {
        Crn crn = Crn.safeFromString(resourceCrn);
        Optional<DatabaseServerConfig> resourceOpt = repository.findByResourceCrn(crn);
        if (resourceOpt.isEmpty()) {
            throw new NotFoundException(String.format("No %s found with crn '%s'", DatabaseServerConfig.class.getSimpleName(), resourceCrn));
        }
        MDCBuilder.buildMdcContext(resourceOpt.get());
        return resourceOpt.get();
    }

    public Optional<DatabaseServerConfig> getByCrn(Crn resourceCrn) {
        return repository.findByResourceCrn(resourceCrn);
    }

    public Optional<DatabaseServerConfig> getByClusterCrn(String clusterCrn) {
        return repository.findByClusterCrn(clusterCrn);
    }

    public DatabaseServerConfig deleteByCrn(String crn) {
        DatabaseServerConfig resource = getByCrn(crn);
        if (resource.getResourceStatus() == ResourceStatus.SERVICE_MANAGED) {
            throw new IllegalStateException("deleteByCrn called with service-managed server. This indicates an error in redbeams code");
        }
        return delete(resource);
    }

    @Override
    public JpaRepository repository() {
        return repository;
    }

    private Set<DatabaseServerConfig> getByNames(Long workspaceId, String environmentCrn, Set<String> names) {
        Set<DatabaseServerConfig> resources =
                repository.findByNameInAndWorkspaceIdAndEnvironmentId(names, workspaceId, environmentCrn);
        Set<String> notFound = Sets.difference(names,
                resources.stream().map(DatabaseServerConfig::getName).collect(Collectors.toSet()));

        if (!notFound.isEmpty()) {
            throw new NotFoundException(String.format("No %s(s) found with name(s) %s in environment '%s'", DatabaseServerConfig.class.getSimpleName(),
                    String.join(", ", notFound), environmentCrn));
        }

        return resources;
    }

    public Set<DatabaseServerConfig> getByCrns(Set<String> crns) {
        Set<Crn> parsedCrns = crns.stream()
                .map(Crn::safeFromString)
                .collect(Collectors.toSet());
        Set<DatabaseServerConfig> resources = repository.findByResourceCrnIn(parsedCrns);
        Set<String> notFound = Sets.difference(crns,
                resources.stream().map(dsc -> dsc.getResourceCrn().toString()).collect(Collectors.toSet()));

        if (!notFound.isEmpty()) {
            throw new NotFoundException(String.format("No %s(s) found with crn(s) %s ",
                    DatabaseServerConfig.class.getSimpleName(), String.join(", ", notFound)));
        }

        return resources;
    }

    public String testConnection(String crn) {
        return testConnection(getByCrn(crn));
    }

    public String testConnection(DatabaseServerConfig resource) {
        MapBindingResult errors = new MapBindingResult(new HashMap(), "databaseServer");
        connectionValidator.validate(resource, errors);
        if (!errors.hasErrors()) {
            return DATABASE_TEST_RESULT_SUCCESS;
        }
        return errors.getAllErrors().stream()
                .map(e -> (e instanceof FieldError ? ((FieldError) e).getField() + ": " : "") + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
    }

    public String createDatabaseOnServer(String serverCrn, String databaseName, String databaseType, Optional<String> databaseDescription) {
        // Prepared statements cannot be used for DDL statements, so we have to scrub the databaseName ourselves.
        // This is a subset of valid SQL identifiers, but I believe it's a sane constraint to put on database name
        // identifiers that protects us from SQL injections
        if (!validateDatabaseName(databaseName)) {
            throw new IllegalArgumentException("The database must contain only alphanumeric characters or underscores");
        }

        LOGGER.info("Creating database with name: {}", databaseName);

        DatabaseServerConfig databaseServerConfig = getByCrn(serverCrn);
        // A database password does not necessarily need to follow cloud provider rules for the root
        // password of a database server, but we can try to follow them anyway. A user-managed
        // database server will not have a known cloud platform, however.
        Optional<CloudPlatform> cloudPlatform = databaseServerConfig.getDbStack()
            .map(DBStack::getCloudPlatform)
            .map(CloudPlatform::valueOf);

        String databaseUserName = userGeneratorService.generateUserName();
        String databasePassword = passwordGeneratorService.generatePassword(cloudPlatform);
        List<String> sqlStrings = List.of(
                "CREATE DATABASE " + databaseName,
                "CREATE USER " + databaseUserName + " WITH ENCRYPTED PASSWORD '" + databasePassword + "'",
                "GRANT ALL PRIVILEGES ON DATABASE " + databaseName + " TO " + databaseUserName
        );

        // For now, do not use a transaction (PostgreSQL forbids it).
        boolean createDatabaseInsideTransaction = false;

        driverFunctions.execWithDatabaseDriver(databaseServerConfig, driver -> {
            try (Connection conn = driver.connect(databaseServerConfig)) {
                databaseCommon.executeUpdates(conn, sqlStrings, createDatabaseInsideTransaction);
            } catch (SQLException e) {
                throw new RedbeamsException("Failed to create database " + databaseName, e);
            }
        });

        // Only record database on server if successfully created on server
        DatabaseConfig newDatabaseConfig =
                databaseServerConfig.createDatabaseConfig(databaseName, databaseType, databaseDescription, ResourceStatus.SERVICE_MANAGED,
                        databaseUserName, databasePassword);
        databaseConfigService.register(newDatabaseConfig, false);

        return "created";
    }

    @VisibleForTesting
    boolean validateDatabaseName(String databaseName) {
        return VALID_DATABASE_NAME.matcher(databaseName).matches();
    }

    @Override
    public AuthorizationResourceType getResourceType() {
        return AuthorizationResourceType.DATABASE_SERVER;
    }

    @Override
    public String getResourceCrnByResourceName(String resourceName) {
        return repository.findResourceCrnByName(resourceName).orElseThrow(NotFoundException.notFound("databaseserver", resourceName)).toString();
    }

    @Override
    public List<String> getResourceCrnListByResourceNameList(List<String> resourceNames) {
        return repository.findResourceCrnsByNames(resourceNames)
                .stream().map(Crn::toString).collect(Collectors.toList());
    }

    @Override
    public List<String> getResourceCrnsInAccount() {
        return repository.findAllResourceCrnsByAccountId(ThreadBasedUserCrnProvider.getAccountId())
                .stream().map(Crn::toString).collect(Collectors.toList());
    }
}
