package com.sequenceiq.consumption.service;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.structuredevent.service.db.CDPStructuredEventDBService;
import com.sequenceiq.cloudbreak.util.TimeUtil;

@Service
public class ConsumptionStructuredEventCleanupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumptionStructuredEventCleanupService.class);

    private static final int THREE_MONTHS = 3;

    @Inject
    private TimeUtil timeUtil;

    @Inject
    private CDPStructuredEventDBService structuredEventDBService;

    public void cleanUpStructuredEvents(String resourceCrn) {
        if (isNotEmpty(resourceCrn)) {
            long timestampsThreeMonthsAgo = timeUtil.getTimestampThatMonthsBeforeNow(THREE_MONTHS);
            LOGGER.debug("About to request deletion for Consumption service related structured events where the timestamp is smaller than {} and its " +
                    "resourceCrn is the following: {}", timestampsThreeMonthsAgo, resourceCrn);
            structuredEventDBService.deleteStructuredEventByResourceCrnThatIsOlderThan(resourceCrn, timestampsThreeMonthsAgo)
                    .ifPresent(exception -> LOGGER.warn("Cleaning up structured events for consumption entry [CRN: " + resourceCrn + "] has failed due to: "
                            + exception.getMessage(), exception));
        } else {
            LOGGER.debug("No resource CRN has been provided, therefore no structured event cleanup has happened.");
        }
    }

}
