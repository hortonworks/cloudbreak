package com.sequenceiq.redbeams.service.dbconfig;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
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

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.common.archive.AbstractArchivistService;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
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
    private Clock clock;

    @Inject
    private CrnService crnService;

    @Inject
    private TransactionService transactionService;

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

    private DatabaseConfig deleteOne(DatabaseConfig databaseConfig) {
        LOGGER.debug("Deleting database with name: {}", databaseConfig.getName());
        try {
            // FIXME this can take a while and trigger logging that the transaction took too long
            return transactionService.required(() -> {
                if (databaseConfig.getStatus() == ResourceStatus.SERVICE_MANAGED) {
                    return deleteServiceManagedDatabase(databaseConfig);
                } else {
                    return delete(databaseConfig);
                }
            });
        } catch (TransactionService.TransactionExecutionException e) {
            throw new RuntimeException("Transaction failed", e);
        }
    }

    private DatabaseConfig deleteServiceManagedDatabase(DatabaseConfig databaseConfig) {
        String databaseName = databaseConfig.getName();
        DatabaseServerConfig databaseServerConfig = databaseConfig.getServer();
        if (databaseServerConfig == null) {
            throw new RedbeamsException("Cannot delete database " + databaseName + ", server not known");
        }

        driverFunctions.execWithDatabaseDriver(databaseServerConfig, driver -> {
            try (Connection conn = driver.connect(databaseServerConfig); Statement statement = conn.createStatement()) {
                LOGGER.info("Deleting database {}", databaseName);
                statement.executeUpdate("DROP DATABASE " + databaseName);
            } catch (SQLException e) {
                throw new RedbeamsException("Failed to drop database " + databaseName, e);
            }
        });

        return delete(databaseConfig);
    }

    @Override
    public JpaRepository repository() {
        return repository;
    }
}
