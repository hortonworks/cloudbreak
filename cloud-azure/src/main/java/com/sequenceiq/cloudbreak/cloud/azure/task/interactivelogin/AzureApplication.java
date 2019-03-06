package com.sequenceiq.cloudbreak.cloud.azure.task.interactivelogin;

import com.sequenceiq.cloudbreak.cloud.azure.AzureApplicationCreationView;

public class AzureApplication {

    private String appId;

    private String objectId;

    private AzureApplicationCreationView azureApplicationCreationView;

    public AzureApplication(String appId, String objectId, AzureApplicationCreationView appCreationView) {
        this.appId = appId;
        this.objectId = objectId;
        azureApplicationCreationView = appCreationView;
    }

    public String getAppId() {
        return appId;
    }

    public String getObjectId() {
        return objectId;
    }

    public AzureApplicationCreationView getAzureApplicationCreationView() {
        return azureApplicationCreationView;
    }
}
