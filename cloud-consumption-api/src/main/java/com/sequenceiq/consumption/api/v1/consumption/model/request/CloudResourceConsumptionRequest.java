package com.sequenceiq.consumption.api.v1.consumption.model.request;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.consumption.api.doc.ConsumptionModelDescription;
import com.sequenceiq.consumption.api.v1.consumption.model.common.ConsumptionType;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudResourceConsumptionRequest extends ConsumptionBaseRequest {

    @NotEmpty
    @ApiModelProperty(value = ConsumptionModelDescription.CLOUD_RESOURCE_ID, required = true)
    private String cloudResourceId;

    @NotNull
    @ApiModelProperty(value = ConsumptionModelDescription.CONSUMPTION_TYPE, allowableValues = "UNKNOWN,STORAGE,EBS,ELASTIC_FILESYSTEM", required = true)
    private ConsumptionType consumptionType;

    public ConsumptionType getConsumptionType() {
        return consumptionType;
    }

    public void setConsumptionType(ConsumptionType consumptionType) {
        this.consumptionType = consumptionType;
    }

    public String getCloudResourceId() {
        return cloudResourceId;
    }

    public void setCloudResourceId(String cloudResourceId) {
        this.cloudResourceId = cloudResourceId;
    }

    @Override
    public String toString() {
        return super.toString() +
                ", CloudResourceConsumptionRequest{" +
                "cloudResourceId='" + cloudResourceId + "' " +
                "consumptionType='" + consumptionType + '\'' +
                '}';
    }

}
