package com.sequenceiq.periscope.api.model;

import com.sequenceiq.periscope.doc.ApiDescription.TimeAlertJsonProperties;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("MetricSchedule")
public class TimeAlertJson extends AbstractAlertJson {

    @ApiModelProperty(TimeAlertJsonProperties.TIMEZONE)
    private String timeZone;
    @ApiModelProperty(TimeAlertJsonProperties.CRON)
    private String cron;

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

}
