package com.sequenceiq.environment.api.v1.environment.model.request;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.api.client.util.Lists;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "EnvironmentRealTimeCO2V1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnvironmentRealTimeCO2Request implements Serializable {

    private List<String> environmentCrns = Lists.newArrayList();

    private List<String> datalakeCrns = Lists.newArrayList();

    private List<String> datahubCrns = Lists.newArrayList();

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
        return "EnvironmentRealTimeCO2Request{" +
                "environmentCrns=" + environmentCrns +
                ", datalakeCrns=" + datalakeCrns +
                ", datahubCrns=" + datahubCrns +
                '}';
    }
}
