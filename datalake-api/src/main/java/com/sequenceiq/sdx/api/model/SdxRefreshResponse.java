package com.sequenceiq.sdx.api.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxRefreshResponse {

    @ApiModelProperty(ModelDescriptions.DATAHUB_CRNS_REFRESHED)
    private List<String> dataHubsRefreshed;

    public SdxRefreshResponse() {
    }

    public SdxRefreshResponse(List<String> dataHubsRefreshed) {
        this.dataHubsRefreshed = dataHubsRefreshed;
    }

    public List<String> getDataHubsRefreshed() {
        return dataHubsRefreshed;
    }

    public void setDataHubsRefreshed(List<String> dataHubsRefreshed) {
        this.dataHubsRefreshed = dataHubsRefreshed;
    }

    @Override
    public String toString() {
        return "SdxRecoveryResponse{" +
                "dataHubRefreshed=" + dataHubsRefreshed +
                '}';
    }
}
