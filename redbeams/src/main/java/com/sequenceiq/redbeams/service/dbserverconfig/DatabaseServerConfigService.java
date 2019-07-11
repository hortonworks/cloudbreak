package com.sequenceiq.redbeams.service.dbserverconfig;

import static com.sequenceiq.redbeams.service.RedbeamsConstants.DATABASE_TEST_RESULT_SUCCESS;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.FieldError;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.common.archive.AbstractArchivistService;
import com.sequenceiq.cloudbreak.common.database.DatabaseCommon;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.workspace.resource.WorkspaceResource;
import com.sequenceiq.redbeams.api.endpoint.v4.ResourceStatus;
import com.sequenceiq.redbeams.domain.DatabaseConfig;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.exception.RedbeamsException;
import com.sequenceiq.redbeams.repository.DatabaseServerConfigRepository;
import com.sequenceiq.redbeams.service.crn.CrnService;
import com.sequenceiq.redbeams.service.dbconfig.DatabaseConfigService;
import com.sequenceiq.redbeams.service.drivers.DriverFunctions;
import com.sequenceiq.redbeams.service.validation.DatabaseServerConnectionValidator;

@Service
public class DatabaseServerConfigService extends AbstractArchivistService<DatabaseServerConfig> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseServerConfigService.class);

    private static final Pattern VALID_DATABASE_NAME = Pattern.compile("^[\\p{Alnum}_][\\p{Alnum}_-]*$");

    private static final int MAX_RANDOM_INT_FOR_CHARACTER = 26;

    private static final int USER_NAME_LENGTH = 10;

    @Inject
    private DatabaseServerConfigRepository repository;

    @Inject
    private DatabaseConfigService databaseConfigService;

    @Inject
    private DriverFunctions driverFunctions;

    @Inject
    private DatabaseCommon databaseCommon;

    @Inject
    private DatabaseServerConnectionValidator connectionValidator;

    @Inject
    private Clock clock;

    @Inject
    private CrnService crnService;

    public Set<DatabaseServerConfig> findAll(Long workspaceId, String environmentId, Boolean attachGlobal) {
        if (environmentId == null) {
            throw new IllegalArgumentException("No environmentId supplied.");
        }

        return repository.findByWorkspaceIdAndEnvironmentId(workspaceId, environmentId);
    }

    public DatabaseServerConfig create(DatabaseServerConfig resource, Long workspaceId) {
        // FIXME? Currently no checks if logged-in user has access to workspace
        // Compare with AbstractWorkspaceAwareResourceService
        String testResults = testConnection(resource);

        if (!testResults.equals(DATABASE_TEST_RESULT_SUCCESS)) {
            throw new IllegalArgumentException(testResults);
        }

        try {
            MDCBuilder.buildMdcContext(resource);
            // prepareCreation(resource);
            resource.setCreationDate(clock.getCurrentTimeMillis());
            Crn crn = crnService.createCrn(resource);
            resource.setResourceCrn(crn);
            resource.setAccountId(crn.getAccountId());
            resource.setWorkspaceId(workspaceId);
            return repository.save(resource);
        } catch (AccessDeniedException | DataIntegrityViolationException e) {
            Optional<Throwable> cve = Throwables.getCausalChain(e).stream()
                    .filter(c -> c instanceof ConstraintViolationException)
                    .findFirst();
            if (cve.isPresent()) {
                String message = String.format("%s already exists with name '%s' in workspace %d",
                        resource().getShortName(), resource.getName(), resource.getWorkspaceId());
                throw new BadRequestException(message, cve.get());
            }
            throw e;
        }
    }

    public void archive(DatabaseServerConfig resource) {
        for (DatabaseConfig dbConfig : resource.getDatabases()) {
            databaseConfigService.archive(dbConfig);
        }
        resource.setArchived(true);
        repository.save(resource);
    }

    public DatabaseServerConfig getByNameOrCrn(Long workspaceId, String environmentId, String name) {
        Optional<DatabaseServerConfig> resourceOpt =
                repository.findByNameAndWorkspaceIdAndEnvironmentId(name, workspaceId, environmentId);
        if (resourceOpt.isEmpty()) {
            throw new NotFoundException(String.format("No %s found with name '%s' in environment '%s'",
                    resource().getShortName(), name, environmentId));
        }
        MDCBuilder.buildMdcContext(resourceOpt.get());
        return resourceOpt.get();
    }

    public DatabaseServerConfig getByCrn(String resourceCrn) {
        Crn crn = Crn.safeFromString(resourceCrn);
        Optional<DatabaseServerConfig> resourceOpt = repository.findByResourceCrn(crn);
        if (resourceOpt.isEmpty()) {
            throw new NotFoundException(String.format("No %s found with crn '%s'", resource().getShortName(), resourceCrn));
        }
        MDCBuilder.buildMdcContext(resourceOpt.get());
        return resourceOpt.get();
    }

    public DatabaseServerConfig deleteByName(Long workspaceId, String environemntId, String name) {
        DatabaseServerConfig resource = getByNameOrCrn(workspaceId, environemntId, name);
        return delete(resource);
    }

    @Override
    public JpaRepository repository() {
        return repository;
    }

    public Set<DatabaseServerConfig> deleteMultipleByName(Long workspaceId, String environmentId, Set<String> names) {
        Set<DatabaseServerConfig> resources = getByNames(workspaceId, environmentId, names);
        return resources.stream()
                .map(this::delete)
                .collect(Collectors.toSet());
    }

    Set<DatabaseServerConfig> getByNames(Long workspaceId, String environmentId, Set<String> names) {
        Set<DatabaseServerConfig> resources =
                repository.findByNameInAndWorkspaceIdAndEnvironmentId(names, workspaceId, environmentId);
        Set<String> notFound = Sets.difference(names,
                resources.stream().map(DatabaseServerConfig::getName).collect(Collectors.toSet()));

        if (!notFound.isEmpty()) {
            throw new NotFoundException(String.format("No %s(s) found with name(s) %s in environment %s", resource().getShortName(),
                    String.join(", ", notFound), environmentId));
        }

        return resources;
    }

    public String testConnection(Long workspaceId, String environmentId, String name) {
        return testConnection(getByNameOrCrn(workspaceId, environmentId, name));
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

    public String createDatabaseOnServer(Long workspaceId, String environmentId, String serverName, String databaseName,
            String databaseType) {
        // Prepared statements cannot be used for DDL statements, so we have to scrub the databaseName ourselves.
        // This is a subset of valid SQL identifiers, but I believe it's a sane constraint to put on database name
        // identifiers that protects us from SQL injections
        if (!validateDatabaseName(databaseName)) {
            throw new IllegalArgumentException("The database must contain only alphanumeric characters or underscores");
        }

        LOGGER.info("Creating database with name: {}", databaseName);

        DatabaseServerConfig databaseServerConfig = getByNameOrCrn(workspaceId, environmentId, serverName);

        String databaseUserName = generateDatabaseUserName();
        String databasePassword = generateDatabasePassword();
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
                databaseServerConfig.createDatabaseConfig(databaseName, databaseType, ResourceStatus.SERVICE_MANAGED,
                        databaseUserName, databasePassword);
        databaseConfigService.register(newDatabaseConfig);

        return "created";
    }

    private String generateDatabaseUserName() {
        return ThreadLocalRandom.current().ints(0, MAX_RANDOM_INT_FOR_CHARACTER)
                .limit(USER_NAME_LENGTH).boxed()
                .map(i -> Character.toString((char) ('a' + i)))
                .collect(Collectors.joining());
    }

    private String generateDatabasePassword() {
        return UUID.randomUUID().toString();
    }

    public WorkspaceResource resource() {
        return WorkspaceResource.DATABASE_SERVER;
    }

    @VisibleForTesting
    boolean validateDatabaseName(String databaseName) {
        return VALID_DATABASE_NAME.matcher(databaseName).matches();
    }
}
