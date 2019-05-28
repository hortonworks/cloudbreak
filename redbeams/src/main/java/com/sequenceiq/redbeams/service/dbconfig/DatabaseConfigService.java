package com.sequenceiq.redbeams.service.dbconfig;

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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.common.archive.AbstractArchivistService;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.redbeams.domain.DatabaseConfig;
import com.sequenceiq.redbeams.repository.DatabaseConfigRepository;
import com.sequenceiq.redbeams.service.crn.CrnService;

@Service
public class DatabaseConfigService extends AbstractArchivistService<DatabaseConfig> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseConfigService.class);

    @Inject
    private DatabaseConfigRepository databaseConfigRepository;

    @Inject
    private Clock clock;

    @Inject
    private CrnService crnService;

    @Inject
    private TransactionService transactionService;

    public List<DatabaseConfig> list(String environmentId) {
        return Lists.newArrayList(databaseConfigRepository.findByEnvironmentId(environmentId));
    }

    public DatabaseConfig register(DatabaseConfig configToSave) {
        try {
            MDCBuilder.buildMdcContext(configToSave);
            // prepareCreation(configToSave);
            configToSave.setStatus(ResourceStatus.USER_MANAGED);
            configToSave.setCreationDate(clock.getCurrentTimeMillis());
            Crn crn = crnService.createCrn(configToSave);
            configToSave.setResourceCrn(crn);
            configToSave.setAccountId(crn.getAccountId());
            return databaseConfigRepository.save(configToSave);
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

    public Set<DatabaseConfig> delete(Set<String> names, String environmentCrn) {
        // TODO return a MUTLI-STATUS if some of the deletes won't succeed.
        // TODO crn validation, maybe as a validator
        Set<DatabaseConfig> foundDatabaseConfigs = databaseConfigRepository.findAllByEnvironmentIdAndNameIn(environmentCrn, names);
        if (names.size() != foundDatabaseConfigs.size()) {
            Set<String> notFoundDatabaseConfigs = Sets.difference(names, foundDatabaseConfigs.stream().map(DatabaseConfig::getName).collect(Collectors.toSet()));
            throw new NotFoundException(
                    String.format("Database(s) for %s not found in environment %s", String.join(", ", notFoundDatabaseConfigs), environmentCrn));
        }
        foundDatabaseConfigs.forEach(this::deleteOne);
        return foundDatabaseConfigs;
    }

    public DatabaseConfig delete(String databaseName, String environmentCrnString) {
        Optional<DatabaseConfig> foundDatabaseConfig = databaseConfigRepository.findByEnvironmentIdAndName(environmentCrnString, databaseName);
        DatabaseConfig databaseConfig = foundDatabaseConfig
                .orElseThrow(() -> new NotFoundException(
                        String.format("Database with name '%s' not found in environment %s", databaseName, environmentCrnString)));
        return deleteOne(databaseConfig);
    }

    private DatabaseConfig deleteOne(DatabaseConfig databaseConfig) {
        try {
            return transactionService.required(() -> {
                delete(databaseConfig);
                if (databaseConfig.isUserManaged()) {
                    deletePhysicalDatabase(databaseConfig);
                }
                return databaseConfig;
            });
        } catch (TransactionService.TransactionExecutionException e) {
            throw new RuntimeException("Transaction failed", e);
        }
    }

    private void deletePhysicalDatabase(DatabaseConfig databaseConfig) {
        // TODO add code to delete physical database
    }

    @Override
    public JpaRepository repository() {
        return databaseConfigRepository;
    }
}
