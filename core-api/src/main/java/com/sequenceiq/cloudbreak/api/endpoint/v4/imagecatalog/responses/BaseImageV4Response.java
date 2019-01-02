package com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseImageV4Response extends ImageV4Response {

    private List<StackDetailsV4Response> hdpStacks;

    private List<StackDetailsV4Response> hdfStacks;

    private String ambariRepoGpgKey;

    public String getAmbariRepoGpgKey() {
        return ambariRepoGpgKey;
    }

    public void setAmbariRepoGpgKey(String ambariRepoGpgKey) {
        this.ambariRepoGpgKey = ambariRepoGpgKey;
    }

    public List<StackDetailsV4Response> getHdpStacks() {
        return hdpStacks;
    }

    public void setHdpStacks(List<StackDetailsV4Response> hdpStacks) {
        this.hdpStacks = hdpStacks;
    }

    public List<StackDetailsV4Response> getHdfStacks() {
        return hdfStacks;
    }

    public void setHdfStacks(List<StackDetailsV4Response> hdfStacks) {
        this.hdfStacks = hdfStacks;
    }
}
