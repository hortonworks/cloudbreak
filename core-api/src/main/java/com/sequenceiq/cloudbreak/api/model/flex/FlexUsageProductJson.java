package com.sequenceiq.cloudbreak.api.model.flex;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FlexUsageProductJson implements JsonEntity {

    private String productId;

    private List<FlexUsageComponentJson> components;

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public List<FlexUsageComponentJson> getComponents() {
        return components;
    }

    public void setComponents(List<FlexUsageComponentJson> components) {
        this.components = components;
    }
}
