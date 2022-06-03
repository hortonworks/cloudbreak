package com.sequenceiq.redbeams.service.stack;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.redbeams.domain.stack.DBStack;

@Service
public class RedbeamsUpgradeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsUpgradeService.class);

    @Inject
    private DBStackService dbStackService;

    public void upgradeDatabaseServer(String crn, String targetMajorVersion) {
        DBStack dbStack = dbStackService.getByCrn(crn);
        MDCBuilder.addEnvironmentCrn(dbStack.getEnvironmentId());

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Upgrade called for: {}, with target version: {}", dbStack, targetMajorVersion);
        }

        LOGGER.debug("Not implemented yet");
    }

}
