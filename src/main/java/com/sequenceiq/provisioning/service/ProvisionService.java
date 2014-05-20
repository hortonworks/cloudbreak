package com.sequenceiq.provisioning.service;

import com.sequenceiq.provisioning.controller.json.CloudInstanceRequest;
import com.sequenceiq.provisioning.controller.json.CloudInstanceResult;
import com.sequenceiq.provisioning.domain.CloudPlatform;
import com.sequenceiq.provisioning.domain.User;

public interface ProvisionService {

    CloudInstanceResult createCloudInstance(User user, CloudInstanceRequest cloudInstanceRequest);

    CloudPlatform getCloudPlatform();

}
