package com.sequenceiq.cloudbreak.service.stack;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TerminatedInstancesParameterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TerminatedInstancesParameterService.class);

    private static final TimeoutUnit DEFAULT_UNIT = TimeoutUnit.SECONDS;

    enum TimeoutUnit {
        SECONDS,
        HOURS,
        DAYS
    }

    @Value("${show.terminated.active:false}")
    private Boolean showTerminatedOn;

    @Value("${show.terminated.timeout.value:3600}")
    private Long timeoutAmount;

    @Value("${show.terminated.timeout.unit}")
    private String timeoutUnitString;

    private TimeoutUnit timeoutUnit;

    @PostConstruct
    private void init(){
        timeoutUnit = parseTimeoutUnit();
    }

    private TimeoutUnit parseTimeoutUnit() {
        if (StringUtils.isNotEmpty(timeoutUnitString)) {
            try {
                return TimeoutUnit.valueOf(timeoutUnitString);
            } catch (IllegalArgumentException e) {
                LOGGER.debug("timeout unit {} could not be converted, assuming {}", timeoutUnitString, DEFAULT_UNIT);
                return DEFAULT_UNIT;
            }
        }
        return DEFAULT_UNIT;
    }

    public Boolean getShowTerminatedOn() {
        return showTerminatedOn;
    }

    public Long getTimeoutAmount() {
        return timeoutAmount;
    }

    public TimeoutUnit getTimeoutUnit() {
        return timeoutUnit;
    }
}
