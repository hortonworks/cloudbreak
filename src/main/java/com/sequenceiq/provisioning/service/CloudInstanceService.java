package com.sequenceiq.provisioning.service;

import java.util.Set;

import com.sequenceiq.provisioning.controller.json.CloudInstanceRequest;
import com.sequenceiq.provisioning.controller.json.CloudInstanceResult;
import com.sequenceiq.provisioning.domain.User;

public interface CloudInstanceService {

    CloudInstanceRequest get(Long id);

    Set<CloudInstanceRequest> getAll(User user);

    CloudInstanceResult create(User user, CloudInstanceRequest cloudInstanceRequest);

}
