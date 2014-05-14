package com.sequenceiq.provisioning.service.azure;

import org.springframework.stereotype.Component;

import com.sequenceiq.provisioning.controller.json.AzureProvisionResult;
import com.sequenceiq.provisioning.controller.json.ProvisionRequest;
import com.sequenceiq.provisioning.controller.json.ProvisionResult;
import com.sequenceiq.provisioning.domain.CloudPlatform;
import com.sequenceiq.provisioning.service.ProvisionService;

@Component
public class AzureProvisionService implements ProvisionService {

    private static final String OK_STATUS = "ok";

    @Override
    public ProvisionResult provisionCluster(ProvisionRequest provisionRequest) {

        return new AzureProvisionResult(OK_STATUS);
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }

}
