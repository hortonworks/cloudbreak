package com.sequenceiq.cloudbreak.job;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StackStatusCheckerConfig {

    // Minutes to skip status checks after the last flow finished
    @Value("${cb.statuschecker.skip.window.minutes:2}")
    private Integer skipWindow;

    @Value("${cb.statuschecker.salt.check.enabled:false}")
    private boolean saltCheckEnabled;

    @Value("${cb.statuschecker.salt.statuschange.enabled:false}")
    private boolean saltCheckStatusChangeEnabled;

    public Integer getSkipWindow() {
        return skipWindow;
    }

    public boolean isSaltCheckEnabled() {
        return saltCheckEnabled;
    }

    public boolean isSaltCheckStatusChangeEnabled() {
        return saltCheckStatusChangeEnabled;
    }
}
