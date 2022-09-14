package com.sequenceiq.cloudbreak.cost.banzai;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BanzaiResponse {

    private Set<BanzaiProductResponse> products;

    public Set<BanzaiProductResponse> getProducts() {
        return products;
    }

    public void setProducts(Set<BanzaiProductResponse> products) {
        this.products = products;
    }
}
