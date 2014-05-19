package com.sequenceiq.provisioning.service;

import com.sequenceiq.provisioning.controller.json.ProvisionRequest;
import com.sequenceiq.provisioning.controller.json.ProvisionResult;
import com.sequenceiq.provisioning.domain.CloudPlatform;
import com.sequenceiq.provisioning.domain.User;

public interface ProvisionService {

    ProvisionResult provisionCluster(User user, ProvisionRequest provisionRequest);

    CloudPlatform getCloudPlatform();

}
