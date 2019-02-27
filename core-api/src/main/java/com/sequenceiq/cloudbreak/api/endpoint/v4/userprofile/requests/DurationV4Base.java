package com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.requests;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

public class DurationV4Base {

    @Min(0)
    @Max(59)
    private Integer minutes;

    @Min(0)
    @Max(23)
    private Integer hours;

    @Min(0)
    private Integer days;

    public Integer getMinutes() {
        return minutes;
    }

    public Integer getHours() {
        return hours;
    }

    public Integer getDays() {
        return days;
    }

    public void setMinutes(Integer minutes) {
        this.minutes = minutes;
    }

    public void setHours(Integer hours) {
        this.hours = hours;
    }

    public void setDays(Integer days) {
        this.days = days;
    }
}
