package com.sequenceiq.periscope.rest.json;

public class TimeAlarmJson extends AbstractAlarmJson {

    private String timeZone;
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
