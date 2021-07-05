package com.sequenceiq.environment.api.v1.environment.model.request.aws;

import java.io.Serializable;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "AttachedFreeIpaRequestAwsSpotParameters")
public class AwsFreeIpaSpotParameters implements Serializable {

    @ApiModelProperty(EnvironmentModelDescription.FREEIPA_AWS_SPOT_PERCENTAGE)
    @Min(value = 0, message = "Spot percentage must be between 0 and 100 percent")
    @Max(value = 100, message = "Spot percentage must be between 0 and 100 percent")
    @Digits(fraction = 0, integer = 3, message = "Spot percentage has to be a number")
    private int percentage;

    @ApiModelProperty(EnvironmentModelDescription.FREEIPA_AWS_SPOT_MAX_PRICE)
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

    @Override
    public String toString() {
        return "AwsFreeIpaSpotParameters{" +
                "percentage=" + percentage +
                ", maxPrice=" + maxPrice +
                '}';
    }
}
