package com.sequenceiq.provisioning.service;

import com.sequenceiq.provisioning.controller.json.ProvisionRequestJson;
import com.sequenceiq.provisioning.controller.json.ProvisionResultJson;

public interface ProvisionService {

    ProvisionResultJson provisionCluster(ProvisionRequestJson provisionRequestJson);

}
