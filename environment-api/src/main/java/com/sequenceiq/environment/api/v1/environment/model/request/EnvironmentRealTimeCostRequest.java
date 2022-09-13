package com.sequenceiq.environment.api.v1.environment.model.request;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModel;

@ApiModel(value = "EnvironmentRealTimeCostV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnvironmentRealTimeCostRequest implements Serializable {

    private List<String> environmentCrns;

    private List<String> datalakeCrns;

    private List<String> datahubCrns;

    public List<String> getEnvironmentCrns() {
        return environmentCrns;
    }

    public void setEnvironmentCrns(List<String> environmentCrns) {
        this.environmentCrns = environmentCrns;
    }

    public List<String> getDatalakeCrns() {
        return datalakeCrns;
    }

    public void setDatalakeCrns(List<String> datalakeCrns) {
        this.datalakeCrns = datalakeCrns;
    }

    public List<String> getDatahubCrns() {
        return datahubCrns;
    }

    public void setDatahubCrns(List<String> datahubCrns) {
        this.datahubCrns = datahubCrns;
    }

    @Override
    public String toString() {
        return "EnvironmentRealTimeCostRequest{" +
                "environmentCrns=" + environmentCrns +
                ", datalakeCrns=" + datalakeCrns +
                ", datahubCrns=" + datahubCrns +
                '}';
    }
}
