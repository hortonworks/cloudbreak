package com.sequenceiq.environment.experience.common.responses;

import java.util.Objects;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CpInternalCluster {

    private String crn;

    private String name;

    private String status;

    private String statusReason;

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CpInternalCluster.class.getSimpleName() + "[", "]")
                .add("crn='" + crn + "'")
                .add("name='" + name + "'")
                .add("status='" + status + "'")
                .add("statusReason='" + statusReason + "'")
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !getClass().equals(o.getClass())) {
            return false;
        }
        CpInternalCluster that = (CpInternalCluster) o;
        return Objects.equals(crn, that.crn)
                && Objects.equals(name, that.name)
                && Objects.equals(status, that.status)
                && Objects.equals(statusReason, that.statusReason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(crn, name, status, statusReason);
    }
}
