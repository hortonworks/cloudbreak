package com.sequenceiq.redbeams.service.dbconfig;

import javax.inject.Inject;

import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.redbeams.domain.DatabaseConfig;
import com.sequenceiq.redbeams.repository.DatabaseConfigRepository;
import com.sequenceiq.redbeams.service.crn.CrnService;

@Service
public class DatabaseConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseConfigService.class);

    @Inject
    private DatabaseConfigRepository databaseConfigRepository;

    @Inject
    private Clock clock;

    @Inject
    private CrnService crnService;

    public DatabaseConfig register(DatabaseConfig configToSave) {
        try {
            MDCBuilder.buildMdcContext(configToSave);
            // prepareCreation(configToSave);
            configToSave.setStatus(ResourceStatus.USER_MANAGED);
            configToSave.setCreationDate(clock.getCurrentTimeMillis());
            configToSave.setCrn(crnService.createCrn(configToSave));
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
}
