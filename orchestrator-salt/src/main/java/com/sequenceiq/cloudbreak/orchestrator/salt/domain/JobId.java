package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

public class JobId {

    private final String jobId;

    private JobId(String jobId) {
        this.jobId = jobId;
    }

    public static JobId jobId(String id) {
        return new JobId(id);
    }

    public String getJobId() {
        return jobId;
    }

    @Override
    public String toString() {
        return "JobId{"
                + "jobId='" + jobId + '\''
                + '}';
    }
}
