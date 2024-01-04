package com.sequenceiq.periscope.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class PeriscopeJob {

    @Id
    private String jobName;

    @Column(name = "lastExecuted")
    private Long lastExecuted;

    public PeriscopeJob() {

    }

    public PeriscopeJob(String jobName, Long lastExecuted) {
        this.jobName = jobName;
        this.lastExecuted = lastExecuted;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public Long getLastExecuted() {
        return lastExecuted;
    }

    public void setLastExecuted(Long lastExecuted) {
        this.lastExecuted = lastExecuted;
    }
}
