package com.sequenceiq.cloudbreak.service.stack;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ShowTerminatedClusterParameterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShowTerminatedClusterParameterService.class);

    private static final TimeUnit DEFAULT_UNIT = TimeUnit.SECONDS;

    private enum TimeUnit {
        SECONDS,
        MINUTES,
        HOURS,
        DAYS
    }

    @Value("${show.terminated.active:false}")
    private Boolean showTerminatedOn;

    @Value("${show.terminated.timeout.value:3600}")
    private Long timeoutAmount;

    @Value("${show.terminated.timeout.unit}")
    private String timeoutUnitString;

    private Duration timeout;

    @PostConstruct
    private void init() {
        timeout = Duration.of(
                timeoutAmount,
                convertTimeoutToChronoUnit(parseTimeoutUnit())
        );
    }

    public Boolean getShowTerminatedOn() {
        return showTerminatedOn;
    }

    public Duration getTimeout() {
        return timeout;
    }

    private ChronoUnit convertTimeoutToChronoUnit(TimeUnit timeUnit) {
        switch (timeUnit) {
            case SECONDS:
                return ChronoUnit.SECONDS;
            case MINUTES:
                return ChronoUnit.MINUTES;
            case HOURS:
                return ChronoUnit.HOURS;
            case DAYS:
                return ChronoUnit.DAYS;
            default:
                return ChronoUnit.SECONDS;
        }
    }

    private TimeUnit parseTimeoutUnit() {
        if (StringUtils.isNotEmpty(timeoutUnitString)) {
            try {
                return TimeUnit.valueOf(timeoutUnitString);
            } catch (IllegalArgumentException e) {
                LOGGER.debug("timeout unit {} could not be converted, assuming {}", timeoutUnitString, DEFAULT_UNIT);
                return DEFAULT_UNIT;
            }
        }
        return DEFAULT_UNIT;
    }
}
