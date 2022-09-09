package com.sequenceiq.consumption.api.doc;

public class ConsumptionOpDescription {
    public static final String SCHEDULE_STORAGE = "Schedules a quartz job to report the cloud storage consumption for a given location of a given environment";
    public static final String UNSCHEDULE_STORAGE = "Unschedules the quartz job that reports the cloud storage consumption for a given storage location " +
            "of a given environment";
    public static final String STORAGE_EXISTS = "Checks if cloud storage consumption collection is scheduled for a given storage location and monitored " +
            "resource CRN";
    public static final String SCHEDULE_CLOUD_RESOURCE = "Schedules a quartz job to report the cloud resource consumption for a given " +
            "cloud resource of a given environment";
    public static final String UNSCHEDULE_CLOUD_RESOURCE = "Unschedules the quartz job that reports the cloud resource consumption for a given cloud resource " +
            "of a given environment";
    public static final String CLOUD_RESOURCE_EXISTS = "Checks if cloud resource consumption collection is scheduled for a given cloud resource ID " +
            "and monitored resource CRN";

    private ConsumptionOpDescription() {
    }
}
