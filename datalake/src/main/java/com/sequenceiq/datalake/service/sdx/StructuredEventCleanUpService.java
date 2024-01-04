package com.sequenceiq.datalake.service.sdx;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.structuredevent.service.db.CDPStructuredEventDBService;
import com.sequenceiq.cloudbreak.util.TimeUtil;

@Service
public class StructuredEventCleanUpService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StructuredEventCleanUpService.class);

    private static final int THREE_MONTHS = 3;

    @Inject
    private SdxService service;

    @Inject
    private TimeUtil timeUtil;

    @Inject
    private CDPStructuredEventDBService structuredEventDBService;

    public void cleanUpStructuredEvents(Long sdxId) {
        Optional<String> resourceCrn = service.findResourceCrnById(sdxId);
        if (resourceCrn.isPresent()) {
            long timestampsThreeMonthsAgo = timeUtil.getTimestampThatMonthsBeforeNow(THREE_MONTHS);
            LOGGER.debug("About to request deletion for Data Lake structured events where the timestamp is smaller than {} and the resource CRN is the " +
                    "following: {}", timestampsThreeMonthsAgo, resourceCrn.get());
            structuredEventDBService.deleteStructuredEventByResourceCrnThatIsOlderThan(resourceCrn.get(), timestampsThreeMonthsAgo)
                    .ifPresent(e -> LOGGER.info("Unable to delete structured events for Data Lake [CRN: " + resourceCrn.get() + "] due to: "
                            + e.getMessage(), e));
        } else {
            LOGGER.debug("No stack has been found for the following stack id, therefore ne structured event cleanup happened: {}", sdxId);
        }
    }

}
