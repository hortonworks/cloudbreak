package com.sequenceiq.consumption.api.doc;

public class ConsumptionOpDescription {
    public static final String SCHEDULE_STORAGE = "Schedules a quartz job to report the cloud storage consumption for a given location of a given environment";
    public static final String UNSCHEDULE_STORAGE = "Unschedules the quartz job that reports the cloud storage consumption for a given storage location " +
            "of a given environment";
    public static final String STORAGE_EXISTS = "Checks if cloud storage consumption collection is scheduled for a given storage location and monitored " +
            "resource CRN";

    private ConsumptionOpDescription() {
    }
}
