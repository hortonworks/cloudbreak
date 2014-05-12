package com.sequenceiq.provisioning.service;

import com.sequenceiq.provisioning.controller.json.ProvisionRequest;
import com.sequenceiq.provisioning.controller.json.ProvisionResult;

public interface ProvisionService {

    ProvisionResult provisionCluster(ProvisionRequest provisionRequest);

}
