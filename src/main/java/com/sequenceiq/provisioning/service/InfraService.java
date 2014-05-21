package com.sequenceiq.provisioning.service;

import java.util.Set;

import com.sequenceiq.provisioning.controller.json.InfraRequest;
import com.sequenceiq.provisioning.domain.User;

public interface InfraService {

    Set<InfraRequest> getAll(User user);

    InfraRequest get(Long id);

    void create(User user, InfraRequest infraRequest);

}
