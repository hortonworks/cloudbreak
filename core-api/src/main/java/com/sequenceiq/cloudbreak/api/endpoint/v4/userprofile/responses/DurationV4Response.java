package com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.responses;

import io.swagger.annotations.ApiModel;

@ApiModel
public class DurationV4Response {

    private Integer seconds;

    private Integer minutes;

    private Integer hours;

    private Integer days;

    public Integer getSeconds() {
        return seconds;
    }

    public Integer getMinutes() {
        return minutes;
    }

    public Integer getHours() {
        return hours;
    }

    public Integer getDays() {
        return days;
    }

    public void setSeconds(Integer seconds) {
        this.seconds = seconds;
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
