package com.sequenceiq.cloudbreak.service.stackpatch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("existingstackpatcher.active-patches.logging-agent-auto-restart")
public class LoggingAgentAutoRestartPatchConfig {

    private String affectedVersionFrom;

    private String dateAfter;

    private String dateBefore;

    public String getAffectedVersionFrom() {
        return affectedVersionFrom;
    }

    public void setAffectedVersionFrom(String affectedVersionFrom) {
        this.affectedVersionFrom = affectedVersionFrom;
    }

    public String getDateAfter() {
        return dateAfter;
    }

    public void setDateAfter(String dateAfter) {
        this.dateAfter = dateAfter;
    }

    public String getDateBefore() {
        return dateBefore;
    }

    public void setDateBefore(String dateBefore) {
        this.dateBefore = dateBefore;
    }
}
