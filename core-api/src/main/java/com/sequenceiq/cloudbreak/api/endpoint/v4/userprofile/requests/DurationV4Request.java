package com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.requests;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;

import io.swagger.annotations.ApiModel;

@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel
@NotNull
public class DurationV4Request  implements JsonEntity {

    @NotNull
    @Min(0)
    @Max(59)
    private Integer seconds;

    @NotNull
    @Min(0)
    @Max(59)
    private Integer minutes;

    @NotNull
    @Min(0)
    @Max(23)
    private Integer hours;

    @NotNull
    @Min(0)
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
