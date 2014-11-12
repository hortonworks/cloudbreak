package com.sequenceiq.periscope.domain;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Entity
@DiscriminatorValue("TIME")
public class TimeAlarm extends BaseAlarm {

    private String timeZone;
    private String cron;

    @Override
    public void reset() {
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o, "notificationSent", "notifications", "scalingPolicy");
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, "notificationSent", "notifications", "scalingPolicy");
    }
}
