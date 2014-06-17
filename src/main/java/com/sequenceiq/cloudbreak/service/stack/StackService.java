package com.sequenceiq.cloudbreak.service.stack;

import java.util.Set;

import com.sequenceiq.cloudbreak.controller.json.IdJson;
import com.sequenceiq.cloudbreak.controller.json.StackJson;
import com.sequenceiq.cloudbreak.domain.MetaData;
import com.sequenceiq.cloudbreak.domain.User;

public interface StackService {

    StackJson get(User user, Long id);

    Set<StackJson> getAll(User user);

    IdJson create(User user, StackJson stackRequest);

    void delete(User user, Long id);

    Boolean startAll(User user, Long stackId);

    Boolean stopAll(User user, Long stackId);

    Set<MetaData> getMetaData(User one, String hash);
}
