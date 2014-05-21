package com.sequenceiq.provisioning.service;

import java.util.Set;

import com.sequenceiq.provisioning.controller.json.InfraJson;
import com.sequenceiq.provisioning.domain.User;

public interface InfraService {

    Set<InfraJson> getAll(User user);

    InfraJson get(Long id);

    void create(User user, InfraJson infraRequest);

}
