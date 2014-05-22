package com.sequenceiq.provisioning.service;

import java.util.Set;

import com.sequenceiq.provisioning.controller.json.CloudInstanceJson;
import com.sequenceiq.provisioning.controller.json.CloudInstanceResult;
import com.sequenceiq.provisioning.domain.User;

public interface CloudInstanceService {

    CloudInstanceJson get(User user, Long id);

    Set<CloudInstanceJson> getAll(User user);

    CloudInstanceResult create(User user, CloudInstanceJson cloudInstanceRequest);

    void delete(Long id);

}
