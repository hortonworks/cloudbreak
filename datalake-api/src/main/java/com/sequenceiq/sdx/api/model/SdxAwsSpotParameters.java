package com.sequenceiq.sdx.api.model;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.validation.Choice;

import io.swagger.annotations.ApiModelProperty;

public class SdxAwsSpotParameters {

    @Min(value = 0, message = "Spot percentage must be between 0 and 100 percent")
    @Max(value = 100, message = "Spot percentage must be between 0 and 100 percent")
    @Digits(fraction = 0, integer = 3, message = "Spot percentage has to be a number")
    @Choice(intValues = {0, 100}, message = "Spot percentage must be either 0 or 100")
    private int percentage;

    @ApiModelProperty(ModelDescriptions.TemplateModelDescription.SPOT_MAX_PRICE)
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
