package com.sequenceiq.cloudbreak.api.model.v2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.CustomContainerRequest;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class ByosV2Request implements JsonEntity {

    @ApiModelProperty(ModelDescriptions.ClusterModelDescription.CUSTOM_CONTAINERS)
    private CustomContainerRequest customContainer;

    @ApiModelProperty(ModelDescriptions.ClusterModelDescription.CUSTOM_QUEUE)
    private String customQueue;

    public CustomContainerRequest getCustomContainer() {
        return customContainer;
    }

    public void setCustomContainer(CustomContainerRequest customContainer) {
        this.customContainer = customContainer;
    }

    public String getCustomQueue() {
        return customQueue;
    }

    public void setCustomQueue(String customQueue) {
        this.customQueue = customQueue;
    }
}
