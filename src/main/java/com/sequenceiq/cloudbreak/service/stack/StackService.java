package com.sequenceiq.cloudbreak.service.stack;

import java.util.Set;

import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackDescription;
import com.sequenceiq.cloudbreak.domain.StatusRequest;
import com.sequenceiq.cloudbreak.domain.User;

public interface StackService {

    Stack create(User user, Stack stack);

    Stack get(User user, Long id);

    Set<Stack> getAll(User user);

    void delete(User user, Long id);

    Set<InstanceMetaData> getMetaData(String hash);

    StackDescription getStackDescription(User user, Stack stack);

    void updateStatus(User user, Long stackId, StatusRequest status);

    void updateNodeCount(User user, Long stackId, Integer nodeCount);

}
