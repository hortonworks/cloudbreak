package com.sequenceiq.environment.environment.service;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.core.AttemptMaker;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

@Service
public class FreeipaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeipaService.class);

    @Value("${env.stop.polling.attempt:90}")
    private Integer attempt;

    @Value("${env.stop.polling.sleep.time:5}")
    private Integer sleeptime;

    private final FreeIpaV1Endpoint freeIpaV1Endpoint;

    private final FreeipaPollerCollection freeipaPollerCollection;

    public FreeipaService(FreeIpaV1Endpoint freeIpaV1Endpoint, FreeipaPollerCollection freeipaPollerCollection) {
        this.freeIpaV1Endpoint = freeIpaV1Endpoint;
        this.freeipaPollerCollection = freeipaPollerCollection;
    }

    public void stopAttachedFreeipa(Long envId, String envCrn) {
        pollingFreeipa(envCrn, freeIpaV1Endpoint::stop, freeipaPollerCollection.stopPoller(envId, envCrn));
    }

    public void startAttachedFreeipa(Long envId, String envCrn) {
        pollingFreeipa(envCrn, freeIpaV1Endpoint::start, freeipaPollerCollection.startPoller(envId, envCrn));
    }

    private void pollingFreeipa(String envCrn, Consumer<String> fn, AttemptMaker<Void> attemptMaker) {
        DescribeFreeIpaResponse describeFreeipa = freeipaPollerCollection.describe(envCrn);
        if (describeFreeipa != null && !describeFreeipa.getStatus().isAvailable()) {
            fn.accept(envCrn);
        }
        Polling.stopAfterAttempt(attempt)
                .stopIfException(true)
                .waitPeriodly(sleeptime, TimeUnit.SECONDS)
                .run(attemptMaker);
    }
}
