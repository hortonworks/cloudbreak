package com.sequenceiq.provisioning.service;

import com.sequenceiq.provisioning.controller.json.CloudInstanceResult;
import com.sequenceiq.provisioning.controller.json.InfraRequest;
import com.sequenceiq.provisioning.domain.CloudPlatform;
import com.sequenceiq.provisioning.domain.User;

public interface InfraService {

    CloudInstanceResult createInfra(User user, InfraRequest infraRequest);

    CloudPlatform getCloudPlatform();

}
