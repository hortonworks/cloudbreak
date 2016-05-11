package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

public class JobId {

    private final String jobId;

    private JobId(String jobId) {
        this.jobId = jobId;
    }

    public String getJobId() {
        return jobId;
    }

    public static JobId jobId(String id) {
        return new JobId(id);
    }
}
