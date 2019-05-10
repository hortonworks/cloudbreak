package com.sequenceiq.redbeams.service.dbconfig;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.common.service.Clock;
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
        configToSave.setStatus(ResourceStatus.USER_MANAGED);
        configToSave.setCreationDate(clock.getCurrentTimeMillis());
        configToSave.setCrn(crnService.createDatabaseCrn());
        return databaseConfigRepository.save(configToSave);
    }
}
