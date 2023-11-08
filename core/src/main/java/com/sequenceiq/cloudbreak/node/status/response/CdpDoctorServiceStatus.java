package com.sequenceiq.cloudbreak.node.status.response;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CdpDoctorServiceStatus {

    private String name;

    private CdpDoctorCheckStatus status = CdpDoctorCheckStatus.UNKNOWN;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name != null) {
            this.name = name;
        }
    }

    public CdpDoctorCheckStatus getStatus() {
        return status;
    }

    public void setStatus(CdpDoctorCheckStatus status) {
        if (status != null) {
            this.status = status;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CdpDoctorServiceStatus that = (CdpDoctorServiceStatus) o;
        return Objects.equals(getName(), that.getName()) && getStatus() == that.getStatus();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getStatus());
    }

    @Override
    public String toString() {
        return "CdpDoctorServiceStatus{" +
                "name='" + name + '\'' +
                ", status=" + status +
                '}';
    }
}
