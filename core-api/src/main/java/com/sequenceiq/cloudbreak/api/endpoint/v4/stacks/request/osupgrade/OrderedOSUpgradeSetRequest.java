package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.osupgrade;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderedOSUpgradeSetRequest {

    private String imageId;

    private List<OrderedOSUpgradeSet> orderedOsUpgradeSets;

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public List<OrderedOSUpgradeSet> getOrderedOsUpgradeSets() {
        return orderedOsUpgradeSets;
    }

    public void setOrderedOsUpgradeSets(List<OrderedOSUpgradeSet> orderedOsUpgradeSets) {
        this.orderedOsUpgradeSets = orderedOsUpgradeSets;
    }

    @Override
    public String toString() {
        return "OrderedOsUpgradeSetRequest{" +
                "imageId='" + imageId + '\'' +
                ", numberedOsUpgradeSets=" + orderedOsUpgradeSets +
                '}';
    }

}
