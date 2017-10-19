package com.sequenceiq.cloudbreak.api.model.imagecatalog;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModel;

@ApiModel("BaseImageResponse")
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseImageResponse extends ImageResponse {

    private List<StackDetailsJson> hdpStacks;

    private List<StackDetailsJson> hdfStacks;

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
