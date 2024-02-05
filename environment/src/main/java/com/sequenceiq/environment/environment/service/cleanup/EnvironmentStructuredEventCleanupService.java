package com.sequenceiq.environment.environment.service.cleanup;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.structuredevent.service.db.CDPStructuredEventDBService;
import com.sequenceiq.cloudbreak.util.TimeUtil;

@Service
public class EnvironmentStructuredEventCleanupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentStructuredEventCleanupService.class);

    private static final int ONE_MONTH = 1;

    private final TimeUtil timeUtil;

    private final CDPStructuredEventDBService structuredEventService;

    public EnvironmentStructuredEventCleanupService(CDPStructuredEventDBService structuredEventService, TimeUtil timeUtil) {
        this.structuredEventService = structuredEventService;
        this.timeUtil = timeUtil;
    }

    public void cleanUpStructuredEvents(String resourceCrn) {
        if (isNotEmpty(resourceCrn)) {
            long timestampOneMonthAgo = timeUtil.getTimestampThatMonthsBeforeNow(ONE_MONTH);
            LOGGER.debug("About to request deletion for environment [CRN: {}] structured events where the timestamp is smaller than {}", resourceCrn,
                    timestampOneMonthAgo);
            structuredEventService.deleteStructuredEventByResourceCrnThatIsOlderThan(resourceCrn, timestampOneMonthAgo)
                    .ifPresent(exception -> LOGGER.warn("Cleaning up structured events for environment [CRN: " + resourceCrn + "] has failed due to: "
                            + exception.getMessage(), exception));
        } else {
            LOGGER.debug("No resource CRN has been provided, therefore no structured event cleanup has happened.");
        }
    }

}
