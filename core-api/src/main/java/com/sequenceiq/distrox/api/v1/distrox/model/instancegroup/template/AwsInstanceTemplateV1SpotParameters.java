package com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template;

import java.io.Serializable;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AwsInstanceTemplateV1SpotParameters implements Serializable {

    @ApiModelProperty(TemplateModelDescription.SPOT_PERCENTAGE)
    @Min(value = 0, message = "Spot percentage must be between 0 and 100 percent")
    @Max(value = 100, message = "Spot percentage must be between 0 and 100 percent")
    @Digits(fraction = 0, integer = 3, message = "Spot percentage has to be a number")
    private int percentage;

    @ApiModelProperty(TemplateModelDescription.SPOT_MAX_PRICE)
    @DecimalMin(value = "0.001", message = "Spot max price must be between 0.001 and 255 with maximum 4 fraction digits")
    @Max(value = 255, message = "Spot max price must be between 0.001 and 255 with maximum 4 fraction digits")
    @Digits(fraction = 4, integer = 3, message = "Spot max price must be between 0.001 and 255 with maximum 4 fraction digits")
    private Double maxPrice;

    public int getPercentage() {
        return percentage;
    }

    public void setPercentage(int percentage) {
        this.percentage = percentage;
    }

    public Double getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(Double maxPrice) {
        this.maxPrice = maxPrice;
    }
}
