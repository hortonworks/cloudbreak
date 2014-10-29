package com.sequenceiq.periscope.domain;

import javax.persistence.Entity;

@Entity
public class TimeAlarm extends BaseAlarm {

    private String timeZone;
    private String startTime;
    private String endTime;
    private Recurrence recurrence;

    @Override
    public void reset() {
        throw new UnsupportedOperationException();
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public Recurrence getRecurrence() {
        return recurrence;
    }

    public void setRecurrence(Recurrence recurrence) {
        this.recurrence = recurrence;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }
}
