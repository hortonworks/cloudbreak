package com.sequenceiq.redbeams.service.dbserverconfig;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
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
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.workspace.resource.WorkspaceResource;
import com.sequenceiq.redbeams.domain.DatabaseConfig;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.repository.DatabaseServerConfigRepository;
import com.sequenceiq.redbeams.service.crn.CrnService;
import com.sequenceiq.redbeams.service.dbconfig.DatabaseConfigService;
import com.sequenceiq.redbeams.service.drivers.DriverFunctions;
import com.sequenceiq.redbeams.service.validation.DatabaseServerConnectionValidator;

@Service
public class DatabaseServerConfigService extends AbstractArchivistService<DatabaseServerConfig> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseServerConfigService.class);

    private static final Pattern VALID_DATABASE_NAME = Pattern.compile("^[\\p{Alnum}_][\\p{Alnum}_-]*$");

    @Inject
    private DatabaseServerConfigRepository repository;

    @Inject
    private DatabaseConfigService databaseConfigService;

    @Inject
    private DriverFunctions driverFunctions;

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

        return repository.findAllByWorkspaceIdAndEnvironmentId(workspaceId, environmentId);
    }

    public DatabaseServerConfig create(DatabaseServerConfig resource, Long workspaceId) {
        // FIXME? Currently no checks if logged-in user has access to workspace
        // Compare with AbstractWorkspaceAwareResourceService
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

    public DatabaseServerConfig getByName(Long workspaceId, String environmentId, String name) {
        Optional<DatabaseServerConfig> resourceOpt =
                repository.findByNameAndWorkspaceIdAndEnvironmentId(name, workspaceId, environmentId);
        if (resourceOpt.isEmpty()) {
            throw new NotFoundException(String.format("No %s found with name '%s' in environment '%s'",
                    resource().getShortName(), name, environmentId));
        }
        MDCBuilder.buildMdcContext(resourceOpt.get());
        return resourceOpt.get();
    }

    public DatabaseServerConfig deleteByName(Long workspaceId, String environemntId, String name) {
        DatabaseServerConfig resource = getByName(workspaceId, environemntId, name);
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
        return testConnection(getByName(workspaceId, environmentId, name));
    }

    public String testConnection(DatabaseServerConfig resource) {
        MapBindingResult errors = new MapBindingResult(new HashMap(), "databaseServer");
        connectionValidator.validate(resource, errors);
        if (!errors.hasErrors()) {
            return "success";
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

        DatabaseServerConfig databaseServerConfig = getByName(workspaceId, environmentId, serverName);
        StringBuilder createResult = new StringBuilder();

        driverFunctions.execWithDatabaseDriver(databaseServerConfig, driver -> {
            try (Connection conn = driver.connect(databaseServerConfig); Statement statement = conn.createStatement()) {

                LOGGER.info("Creating database {}", databaseName);
                if (!statement.execute("CREATE DATABASE " + databaseName)) {
                    createResult.append("created");

                    DatabaseConfig newDatabaseConfig =
                            databaseServerConfig.createDatabaseConfig(databaseName, databaseType);
                    databaseConfigService.register(newDatabaseConfig);

                } else {
                    createResult.append("failed");
                }

            } catch (SQLException e) {
                createResult.append("error when creating database: ").append(e.getMessage());
            }
        });

        return createResult.toString();
    }

    public WorkspaceResource resource() {
        return WorkspaceResource.DATABASE_SERVER;
    }

    @VisibleForTesting
    boolean validateDatabaseName(String databaseName) {
        return VALID_DATABASE_NAME.matcher(databaseName).matches();
    }
}
