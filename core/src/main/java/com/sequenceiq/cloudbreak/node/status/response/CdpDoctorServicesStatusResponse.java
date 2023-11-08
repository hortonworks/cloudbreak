package com.sequenceiq.cloudbreak.node.status.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.api.client.util.Lists;
import com.google.common.base.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CdpDoctorServicesStatusResponse {

    private List<CdpDoctorServiceStatus> infraServices = Lists.newArrayList();

    private List<CdpDoctorServiceStatus> cmServices = Lists.newArrayList();

    public List<CdpDoctorServiceStatus> getInfraServices() {
        return infraServices;
    }

    public void setInfraServices(List<CdpDoctorServiceStatus> infraServices) {
        if (infraServices != null) {
            this.infraServices = infraServices;
        }
    }

    public List<CdpDoctorServiceStatus> getCmServices() {
        return cmServices;
    }

    public void setCmServices(List<CdpDoctorServiceStatus> cmServices) {
        if (cmServices != null) {
            this.cmServices = cmServices;
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
        CdpDoctorServicesStatusResponse that = (CdpDoctorServicesStatusResponse) o;
        return Objects.equal(getInfraServices(), that.getInfraServices()) && Objects.equal(getCmServices(), that.getCmServices());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getInfraServices(), getCmServices());
    }

    @Override
    public String toString() {
        return "CdpDoctorServicesStatusResponse{" +
                "infraServices=" + infraServices +
                ", cmServices=" + cmServices +
                '}';
    }
}
