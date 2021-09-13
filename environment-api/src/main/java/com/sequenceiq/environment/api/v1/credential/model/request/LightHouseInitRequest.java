package com.sequenceiq.environment.api.v1.credential.model.request;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("LightHouseInitV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LightHouseInitRequest implements Serializable {

    @NotNull
    @ApiModelProperty(required = true, example = "a8d4457d-310v-41p6-sc53-14g8d733e514")
    private String subscriptionId;

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    @Override
    public String toString() {
        return "LightHouseInitRequest{" +
                "subscriptionId='" + subscriptionId + '\'' +
                '}';
    }
}
