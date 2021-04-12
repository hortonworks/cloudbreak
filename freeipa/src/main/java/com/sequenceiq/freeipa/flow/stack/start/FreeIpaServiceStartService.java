package com.sequenceiq.freeipa.flow.stack.start;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;

@Service
public class FreeIpaServiceStartService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaServiceStartService.class);

    @Value("${freeipa.start.polling.attempt:90}")
    private Integer attempt;

    @Value("${freeipa.start.polling.sleeping.time:5}")
    private Integer sleepingTime;

    private AttemptMakerFactory attemptMakerFactory;

    public FreeIpaServiceStartService(AttemptMakerFactory attemptMakerFactory) {
        this.attemptMakerFactory = attemptMakerFactory;
    }

    public void pollFreeIpaHealth(Stack stack) {
        Set<InstanceMetaData> notTerminatedStackInstances = stack.getAllInstanceMetaDataList().stream()
                .filter(Predicate.not(InstanceMetaData::isTerminated))
                .collect(Collectors.toSet());
        pollUntilFreeIpaIsAvailable(stack, notTerminatedStackInstances);
    }

    private void pollUntilFreeIpaIsAvailable(Stack stack, Set<InstanceMetaData> instanceMetaDataSet) {
        try {
            Polling.stopAfterAttempt(attempt)
                    .stopIfException(true)
                    .waitPeriodly(sleepingTime, TimeUnit.SECONDS)
                    .run(attemptMakerFactory.create(stack, instanceMetaDataSet));
        } catch (Exception e) {
            LOGGER.info("freeipa health check poller failed, cause: {}", e.getMessage());
            throw new OperationException("Failed to start freeipa services");
        }
        LOGGER.debug("freeipa health check poller finished, freeipa is up and running.");

    }
}
