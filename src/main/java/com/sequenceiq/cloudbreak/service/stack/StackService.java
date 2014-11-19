package com.sequenceiq.cloudbreak.service.stack;

import java.util.Set;

import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackDescription;
import com.sequenceiq.cloudbreak.domain.StatusRequest;

public interface StackService {

    Set<Stack> retrievePrivateStacks(CbUser user);

    Set<Stack> retrieveAccountStacks(CbUser user);

    Stack get(Long id);

    Stack get(String name);

    Stack getByAmbariAdress(String ambariAddress);

    Stack create(CbUser user, Stack stack);

    void delete(Long id);

    void delete(String name);

    Set<InstanceMetaData> getMetaData(String hash);

    StackDescription getStackDescription(Stack stack);

    void updateStatus(Long stackId, StatusRequest status);

    void updateStatus(String name, StatusRequest status);

    void updateNodeCount(Long stackId, Integer nodeCount);

    void updateNodeCount(String name, Integer nodeCount);
}
