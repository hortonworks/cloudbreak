package com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.StackDetailsJson;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseImageV4Response extends ImageV4Response {

    private List<StackDetailsJson> hdpStacks;

    private List<StackDetailsJson> hdfStacks;

    private String ambariRepoGpgKey;

    public String getAmbariRepoGpgKey() {
        return ambariRepoGpgKey;
    }

    public void setAmbariRepoGpgKey(String ambariRepoGpgKey) {
        this.ambariRepoGpgKey = ambariRepoGpgKey;
    }

    public List<StackDetailsJson> getHdpStacks() {
        return hdpStacks;
    }

    public void setHdpStacks(List<StackDetailsJson> hdpStacks) {
        this.hdpStacks = hdpStacks;
    }

    public List<StackDetailsJson> getHdfStacks() {
        return hdfStacks;
    }

    public void setHdfStacks(List<StackDetailsJson> hdfStacks) {
        this.hdfStacks = hdfStacks;
    }
}
