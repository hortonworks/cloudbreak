package com.sequenceiq.cloudbreak.cm.model;

import com.google.common.base.Objects;

public class ParcelResource {

    private String clusterName;

    private String product;

    private String version;

    public ParcelResource(String clusterName, String product, String version) {
        this.clusterName = clusterName;
        this.product = product;
        this.version = version;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ParcelResource that = (ParcelResource) o;
        return Objects.equal(clusterName, that.clusterName) &&
                Objects.equal(product, that.product) &&
                Objects.equal(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(clusterName, product, version);
    }
}
