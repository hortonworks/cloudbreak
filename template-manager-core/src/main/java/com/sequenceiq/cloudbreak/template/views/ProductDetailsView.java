package com.sequenceiq.cloudbreak.template.views;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;

public class ProductDetailsView {

    private ClouderaManagerRepo cm;

    private List<ClouderaManagerProduct> products;

    public ProductDetailsView(ClouderaManagerRepo cm, List<ClouderaManagerProduct> products) {
        this.cm = cm;
        this.products = products;
    }

    public ClouderaManagerRepo getCm() {
        return cm;
    }

    public void setCm(ClouderaManagerRepo cm) {
        this.cm = cm;
    }

    public List<ClouderaManagerProduct> getProducts() {
        return products;
    }

    public void setProducts(List<ClouderaManagerProduct> products) {
        this.products = products;
    }
}
