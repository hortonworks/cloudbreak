package com.sequenceiq.provisioning.service;

import java.util.Set;

import com.sequenceiq.provisioning.controller.json.StackJson;
import com.sequenceiq.provisioning.controller.json.StackResult;
import com.sequenceiq.provisioning.domain.User;

public interface StackService {

    StackJson get(User user, Long id);

    Set<StackJson> getAll(User user);

    StackResult create(User user, StackJson stackRequest);

    void delete(Long id);

}
