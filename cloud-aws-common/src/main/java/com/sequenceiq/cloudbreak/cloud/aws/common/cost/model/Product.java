package com.sequenceiq.cloudbreak.cloud.aws.common.cost.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Product {

    private String productFamily;

    private Attributes attributes;

    private String sku;

    public Product(String productFamily, Attributes attributes, String sku) {
        this.productFamily = productFamily;
        this.attributes = attributes;
        this.sku = sku;
    }

    public String getProductFamily() {
        return productFamily;
    }

    public Attributes getAttributes() {
        return attributes;
    }

    public String getSku() {
        return sku;
    }
}
