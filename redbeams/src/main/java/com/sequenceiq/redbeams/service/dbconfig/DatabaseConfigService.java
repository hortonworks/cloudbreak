package com.sequenceiq.redbeams.service.dbconfig;

import static com.sequenceiq.redbeams.service.RedbeamsConstants.DATABASE_TEST_RESULT_SUCCESS;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.validation.FieldError;
import org.springframework.validation.MapBindingResult;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.common.archive.AbstractArchivistService;
import com.sequenceiq.cloudbreak.common.database.DatabaseCommon;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.redbeams.api.endpoint.v4.ResourceStatus;
import com.sequenceiq.redbeams.domain.DatabaseConfig;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.exception.RedbeamsException;
import com.sequenceiq.redbeams.repository.DatabaseConfigRepository;
import com.sequenceiq.redbeams.repository.DatabaseServerConfigRepository;
import com.sequenceiq.redbeams.service.crn.CrnService;
import com.sequenceiq.redbeams.service.drivers.DriverFunctions;
import com.sequenceiq.redbeams.service.validation.DatabaseConnectionValidator;

@Service
public class DatabaseConfigService extends AbstractArchivistService<DatabaseConfig> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseConfigService.class);

    @Inject
    private DatabaseConfigRepository repository;

    @Inject
    private DatabaseServerConfigRepository serverRepository;

    @Inject
    private DriverFunctions driverFunctions;

    @Inject
    private DatabaseCommon databaseCommon;

    @Inject
    private Clock clock;

    @Inject
    private CrnService crnService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private DatabaseConnectionValidator connectionValidator;

    public Set<DatabaseConfig> findAll(String environmentId) {
        return repository.findByEnvironmentId(environmentId);
    }

    public DatabaseConfig register(DatabaseConfig configToSave) {
        try {
            MDCBuilder.buildMdcContext(configToSave);
            // prepareCreation(configToSave);
            configToSave.setCreationDate(clock.getCurrentTimeMillis());
            Crn crn = crnService.createCrn(configToSave);
            configToSave.setResourceCrn(crn);
            configToSave.setAccountId(crn.getAccountId());
            return repository.save(configToSave);
        } catch (AccessDeniedException | DataIntegrityViolationException e) {
            ConstraintViolationException cve = null;
            for (Throwable t = e.getCause(); t != null; t = t.getCause()) {
                if (t instanceof ConstraintViolationException) {
                    cve = (ConstraintViolationException) t;
                    break;
                }
            }
            if (cve != null) {
                String message = String.format("database config already exists with name '%s'", configToSave.getName());
                throw new BadRequestException(message, cve);
            }
            throw e;
        }
    }

    public void archive(DatabaseConfig databaseConfig) {
        databaseConfig.setArchived(true);
        repository.save(databaseConfig);
    }

    public DatabaseConfig get(String name, String environmentId) {
        Optional<DatabaseConfig> resourceOpt =
                repository.findByEnvironmentIdAndName(environmentId, name);
        if (resourceOpt.isEmpty()) {
            throw new NotFoundException(String.format("No database found with name '%s' in environment '%s'",
                    name, environmentId));
        }
        MDCBuilder.buildMdcContext(resourceOpt.get());
        return resourceOpt.get();
    }

    public Set<DatabaseConfig> delete(Set<String> names, String environmentId) {
        // TODO return a MUTLI-STATUS if some of the deletes won't succeed.
        // TODO crn validation, maybe as a validator
        Set<DatabaseConfig> foundDatabaseConfigs = repository.findAllByEnvironmentIdAndNameIn(environmentId, names);
        if (names.size() != foundDatabaseConfigs.size()) {
            Set<String> notFoundDatabaseConfigs = Sets.difference(names, foundDatabaseConfigs.stream().map(DatabaseConfig::getName).collect(Collectors.toSet()));
            throw new NotFoundException(
                    String.format("Database(s) for %s not found in environment %s", String.join(", ", notFoundDatabaseConfigs), environmentId));
        }
        return foundDatabaseConfigs.stream()
                .map(this::deleteOne)
                .collect(Collectors.toSet());
    }

    public DatabaseConfig delete(String name, String environmentId) {
        DatabaseConfig resource = get(name, environmentId);
        return deleteOne(resource);
    }

    public String testConnection(String databaseConfigName, String environmentId) {
        DatabaseConfig databaseConfig = get(databaseConfigName, environmentId);
        return testConnection(databaseConfig);
    }

    public String testConnection(DatabaseConfig config) {
        MapBindingResult errors = new MapBindingResult(new HashMap(), "database");
        connectionValidator.validate(config, errors);
        if (!errors.hasErrors()) {
            return DATABASE_TEST_RESULT_SUCCESS;
        }
        return errors.getAllErrors().stream()
                .map(e -> (e instanceof FieldError ? ((FieldError) e).getField() + ": " : "") + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
    }

    private DatabaseConfig deleteOne(DatabaseConfig databaseConfig) {
        LOGGER.info("Deleting database with name: {}", databaseConfig.getName());

        DatabaseConfig deletedConfig = delete(databaseConfig);

        // Only delete database from server if successfully removed from redbeams
        if (databaseConfig.getStatus() == ResourceStatus.SERVICE_MANAGED) {
            deleteServiceManagedDatabase(databaseConfig);
        }

        return deletedConfig;
    }

    private void deleteServiceManagedDatabase(DatabaseConfig databaseConfig) {
        String databaseName = databaseConfig.getName();
        String databaseUserName = databaseConfig.getConnectionUserName().getRaw();
        DatabaseServerConfig databaseServerConfig = databaseConfig.getServer();
        if (databaseServerConfig == null) {
            LOGGER.error("Cannot delete database " + databaseName + " from server, server not known");
            return;
        }

        List<String> sqlStrings = new ArrayList<>();
        boolean distinctDbUser = !databaseUserName.equals(databaseServerConfig.getConnectionUserName());
        if (distinctDbUser) {
            sqlStrings.add("REVOKE ALL PRIVILEGES ON DATABASE " + databaseName + " FROM " + databaseUserName);
        }
        sqlStrings.add("DROP DATABASE " + databaseName);
        if (distinctDbUser) {
            sqlStrings.add("DROP USER " + databaseUserName);
        }

        try {
            transactionService.required(() -> {
                driverFunctions.execWithDatabaseDriver(databaseServerConfig, driver -> {
                    try (Connection conn = driver.connect(databaseServerConfig); Statement statement = conn.createStatement()) {
                        databaseCommon.executeUpdates(statement, sqlStrings);
                    } catch (SQLException e) {
                        throw new RedbeamsException("Failed to drop database " + databaseName, e);
                    }
                });
                return true;
            });
        } catch (TransactionExecutionException e) {
            LOGGER.error("Error / transaction failure while deleting database from server", e);
        }
    }

    @Override
    public JpaRepository repository() {
        return repository;
    }
}
