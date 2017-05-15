package com.sequenceiq.cloudbreak.api.model.flex;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;

import io.swagger.annotations.ApiModel;

@ApiModel("CloudbreakFlexUsage")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudbreakFlexUsageJson  implements JsonEntity {

    private FlexUsageControllerJson controller;

    private List<FlexUsageProductJson> products;

    public FlexUsageControllerJson getController() {
        return controller;
    }

    public void setController(FlexUsageControllerJson controller) {
        this.controller = controller;
    }

    public List<FlexUsageProductJson> getProducts() {
        return products;
    }

    public void setProducts(List<FlexUsageProductJson> products) {
        this.products = products;
    }
}
