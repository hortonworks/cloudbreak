package com.sequenceiq.cloudbreak.service.stack;

import java.util.List;
import java.util.Set;

import com.sequenceiq.cloudbreak.domain.InstanceStatus;
import com.sequenceiq.cloudbreak.domain.StackValidation;
import com.sequenceiq.cloudbreak.controller.json.InstanceGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StatusRequest;
import com.sequenceiq.cloudbreak.domain.Subnet;

public interface StackService {

    Set<Stack> retrievePrivateStacks(CbUser user);

    Set<Stack> retrieveAccountStacks(CbUser user);

    Stack get(Long id);

    Stack getById(Long id);

    Stack get(String ambariAddress);

    Stack create(CbUser user, Stack stack);

    void delete(Long id, CbUser cbUser);

    Set<InstanceMetaData> getMetaData(String hash);

    InstanceMetaData updateMetaDataStatus(Long id, String hostName, InstanceStatus status);

    void updateStatus(Long stackId, StatusRequest status);

    Stack getPrivateStack(String name, CbUser cbUser);

    Stack getPublicStack(String name, CbUser cbUser);

    void delete(String name, CbUser cbUser);

    void updateNodeCount(Long stackId, InstanceGroupAdjustmentJson instanceGroupAdjustmentJson);

    void updateAllowedSubnets(Long stackId, List<Subnet> subnetList);

    void validateStack(StackValidation stackValidation);
}
