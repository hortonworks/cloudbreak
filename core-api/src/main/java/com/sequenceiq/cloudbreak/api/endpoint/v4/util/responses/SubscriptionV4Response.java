package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class SubscriptionV4Response implements JsonEntity {

    @ApiModelProperty(value = ModelDescriptions.ID, required = true)
    private Long id;

    public SubscriptionV4Response() {

    }

    public SubscriptionV4Response(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
