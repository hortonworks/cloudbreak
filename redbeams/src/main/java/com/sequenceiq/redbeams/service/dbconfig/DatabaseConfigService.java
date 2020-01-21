package com.sequenceiq.redbeams.service.dbconfig;

import static com.sequenceiq.redbeams.service.RedbeamsConstants.DATABASE_TEST_RESULT_SUCCESS;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.springframework.stereotype.Service;
import org.springframework.validation.FieldError;
import org.springframework.validation.MapBindingResult;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.common.archive.AbstractArchivistService;
import com.sequenceiq.cloudbreak.common.database.DatabaseCommon;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.core.ResourceIdProvider;
import com.sequenceiq.redbeams.api.endpoint.v4.ResourceStatus;
import com.sequenceiq.redbeams.domain.DatabaseConfig;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.exception.BadRequestException;
import com.sequenceiq.redbeams.exception.NotFoundException;
import com.sequenceiq.redbeams.exception.RedbeamsException;
import com.sequenceiq.redbeams.repository.DatabaseConfigRepository;
import com.sequenceiq.redbeams.service.crn.CrnService;
import com.sequenceiq.redbeams.service.drivers.DriverFunctions;
import com.sequenceiq.redbeams.service.validation.DatabaseConnectionValidator;

@Service
public class DatabaseConfigService extends AbstractArchivistService<DatabaseConfig> implements ResourceIdProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseConfigService.class);

    @Inject
    private DatabaseConfigRepository repository;

    @Inject
    private DriverFunctions driverFunctions;

    @Inject
    private DatabaseCommon databaseCommon;

    @Inject
    private Clock clock;

    @Inject
    private CrnService crnService;

    @Inject
    private DatabaseConnectionValidator connectionValidator;

    public Set<DatabaseConfig> findAll(String environmentCrn) {
        return repository.findByEnvironmentId(environmentCrn);
    }

    public DatabaseConfig register(DatabaseConfig configToSave, boolean test) {

        if (configToSave.getConnectionDriver() == null) {
            configToSave.setConnectionDriver(configToSave.getDatabaseVendor().connectionDriver());
            LOGGER.info("Database configuration lacked a connection driver; defaulting to {}",
                configToSave.getConnectionDriver());
        }

        if (test) {
            String testResults = testConnection(configToSave);

            if (!testResults.equals(DATABASE_TEST_RESULT_SUCCESS)) {
                throw new IllegalArgumentException(testResults);
            }
        }

        try {
            MDCBuilder.buildMdcContext(configToSave);
            // prepareCreation(configToSave);
            configToSave.setCreationDate(clock.getCurrentTimeMillis());
            Crn crn = crnService.createCrn(configToSave);
            configToSave.setResourceCrn(crn);
            configToSave.setAccountId(crn.getAccountId());
            return repository.save(configToSave);
        } catch (ConstraintViolationException | DataIntegrityViolationException e) {
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

    public DatabaseConfig getByCrn(String resourceCrn) {
        Crn crn = Crn.safeFromString(resourceCrn);
        Optional<DatabaseConfig> resourceOpt = repository.findByResourceCrn(crn);
        if (resourceOpt.isEmpty()) {
            throw new NotFoundException(String.format("No database found with crn '%s'", resourceCrn));
        }
        MDCBuilder.buildMdcContext(resourceOpt.get());
        return resourceOpt.get();
    }

    public DatabaseConfig getByName(String name, String environmentCrn) {
        Optional<DatabaseConfig> resourceOpt =
                repository.findByEnvironmentIdAndName(environmentCrn, name);
        if (resourceOpt.isEmpty()) {
            throw new NotFoundException(String.format("No database found with name '%s' in environment '%s'",
                    name, environmentCrn));
        }
        MDCBuilder.buildMdcContext(resourceOpt.get());
        return resourceOpt.get();
    }

    public Set<DatabaseConfig> deleteMultipleByCrn(Set<String> resourceCrns) {
        // TODO return a MUTLI-STATUS if some of the deletes don't succeed.
        Set<Crn> parsedCrns = resourceCrns.stream()
                .map(Crn::safeFromString)
                .collect(Collectors.toSet());
        Set<DatabaseConfig> foundDatabaseConfigs = repository.findByResourceCrnIn(parsedCrns);
        if (resourceCrns.size() != foundDatabaseConfigs.size()) {
            Set<String> notFoundDatabaseConfigs = Sets.difference(resourceCrns,
                foundDatabaseConfigs.stream()
                    .map(DatabaseConfig::getResourceCrn)
                    .map(Object::toString)
                    .collect(Collectors.toSet()));
            throw new NotFoundException(
                    String.format("Database(s) not found: %s", String.join(", ", notFoundDatabaseConfigs)));
        }
        return foundDatabaseConfigs.stream()
                .map(this::delete)
                .collect(Collectors.toSet());
    }

    public DatabaseConfig deleteByCrn(String resourceCrn) {
        DatabaseConfig resource = getByCrn(resourceCrn);
        return delete(resource);
    }

    public DatabaseConfig deleteByName(String name, String environmentCrn) {
        DatabaseConfig resource = getByName(name, environmentCrn);
        return delete(resource);
    }

    public String testConnection(String databaseConfigName, String environmentCrn) {
        DatabaseConfig databaseConfig = getByName(databaseConfigName, environmentCrn);
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

    @Override
    public DatabaseConfig delete(DatabaseConfig databaseConfig) {
        return delete(databaseConfig, false, false);
    }

    /**
     * Deletes a database configuration. If the database is service-managed,
     * then also deletes the database from its home server unless
     * <code>skipDeletionOnServer</code> is true. If deletion from the server
     * fails, deletion fails unless <code>force</code> is true. (Currently,
     * deletion from the server is the only action that <code>force</code>
     * applies to.)
     *
     * @param  databaseConfig       database to delete
     * @param  force                whether to force deletion
     * @param  skipDeletionOnServer whether to skip deleting the database on its server
     * @return                      deleted database
     */
    public DatabaseConfig delete(DatabaseConfig databaseConfig, boolean force, boolean skipDeletionOnServer) {
        LOGGER.info("Deleting database with name: {}", databaseConfig.getName());

        if (databaseConfig.getStatus() == ResourceStatus.SERVICE_MANAGED) {
            if (!skipDeletionOnServer) {
                // Only delete database from redbeams once successfully removed from server
                try {
                    deleteServiceManagedDatabase(databaseConfig);
                } catch (RuntimeException e) {
                    if (force) {
                        LOGGER.warn("Deletion for database '{}' failed, continuing because termination is forced",
                                databaseConfig.getName(), e);
                    } else {
                        throw e;
                    }
                }
            } else {
                LOGGER.debug("Skipping deletion on server");
            }
        }

        return super.delete(databaseConfig);
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

        // For now, do not use a transaction (PostgreSQL forbids it).
        boolean dropDatabaseInsideTransaction = false;

        driverFunctions.execWithDatabaseDriver(databaseServerConfig, driver -> {
            try (Connection conn = driver.connect(databaseServerConfig)) {
                databaseCommon.executeUpdates(conn, sqlStrings, dropDatabaseInsideTransaction);
            } catch (SQLException e) {
                throw new RedbeamsException("Failed to drop database " + databaseName, e);
            }
        });
    }

    @Override
    public JpaRepository repository() {
        return repository;
    }
}
