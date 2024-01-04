package com.sequenceiq.freeipa.service.freeipa.cleanup;

import static com.sequenceiq.cloudbreak.util.Benchmark.measureAndWarnIfLong;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.structuredevent.repository.CDPStructuredEventRepository;
import com.sequenceiq.cloudbreak.util.TimeUtil;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class StructuredEventCleanupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StructuredEventCleanupService.class);

    private static final int THREE_MONTHS = 3;

    @Inject
    private StackService stackService;

    @Inject
    private TimeUtil timeUtil;

    @Inject
    private CDPStructuredEventRepository structuredEventRepository;

    public void cleanUpStructuredEvents(Long stackId) {
        Optional<String> accountId = stackService.findAccountById(stackId);
        if (accountId.isPresent()) {
            long timestampsThreeMonthsAgo = timeUtil.getTimestampThatMonthsBeforeNow(THREE_MONTHS);
            LOGGER.debug("About to request deletion for FreeIPA structured events where the timestamp is smaller than {}", timestampsThreeMonthsAgo);
            measureAndWarnIfLong(() -> structuredEventRepository.deleteByAccountIdOlderThan(accountId.get(), timestampsThreeMonthsAgo),
                    LOGGER, "Cleaning up FreeIPA structured event(s) that are older than " + THREE_MONTHS + " months for the given account: "
                            + accountId.get());
        } else {
            LOGGER.debug("Since no account ID has found for stack id [id: {}], no structured event cleanup has happened.", stackId);
        }
    }

}
