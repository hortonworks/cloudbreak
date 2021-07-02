package com.sequenceiq.cloudbreak.cloud.model.objectstorage;

import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;

public class AzureParameters extends DynamicModel {

    private String singleResourceGroupName;

    public AzureParameters() {
    }

    public AzureParameters(String singleResourceGroupName) {
        this.singleResourceGroupName = singleResourceGroupName;
    }

    public String getSingleResourceGroupName() {
        return singleResourceGroupName;
    }

    public void setSingleResourceGroupName(String singleResourceGroupName) {
        this.singleResourceGroupName = singleResourceGroupName;
    }

    @Override
    public String toString() {
        return "AzureParameters{" +
                "singleResourceGroupName='" + singleResourceGroupName +
                '}';
    }
}
