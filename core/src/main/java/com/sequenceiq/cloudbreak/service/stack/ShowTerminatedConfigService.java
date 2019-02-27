package com.sequenceiq.cloudbreak.service.stack;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.service.stack.TerminatedInstancesParameterService.TimeoutUnit;
import com.sequenceiq.cloudbreak.util.TimeService;

@Service
public class ShowTerminatedConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShowTerminatedConfigService.class);

    @Inject
    private TerminatedInstancesParameterService terminatedInstancesParameterService;

    @Inject
    private TimeService timeService;

    private Duration showTerminatedTimeout;

    @PostConstruct
    protected void init() {
        showTerminatedTimeout = Duration.of(
                terminatedInstancesParameterService.getTimeoutAmount(),
                convertTimeoutToChronoUnit(terminatedInstancesParameterService.getTimeoutUnit())
        );
    }

    public Boolean isActive() {
        return terminatedInstancesParameterService.getShowTerminatedOn();
    }

    public Long showAfter() {
        return timeService.nowMinus(showTerminatedTimeout);
    }

    private ChronoUnit convertTimeoutToChronoUnit(TimeoutUnit timeoutUnit) {
        switch (timeoutUnit) {
            case SECONDS:
                return ChronoUnit.SECONDS;
            case HOURS:
                return ChronoUnit.HOURS;
            case DAYS:
                return ChronoUnit.DAYS;
            default:
                return ChronoUnit.SECONDS;
        }
    }
}
