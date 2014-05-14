package com.sequenceiq.provisioning.service;

import com.sequenceiq.provisioning.controller.json.ProvisionRequest;
import com.sequenceiq.provisioning.controller.json.ProvisionResult;
import com.sequenceiq.provisioning.domain.CloudPlatform;

public interface ProvisionService {

    ProvisionResult provisionCluster(ProvisionRequest provisionRequest);

    CloudPlatform getCloudPlatform();

}
