package com.sequenceiq.cloudbreak.service.stack;

import java.io.IOException;
import java.util.Set;

import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackDescription;
import com.sequenceiq.cloudbreak.domain.User;

public interface StackService {

    Stack create(User user, Stack stack);

    Stack get(User user, Long id);

    Set<Stack> getAll(User user);

    void delete(User user, Long id);

    Boolean startAll(User user, Long stackId);

    Boolean stopAll(User user, Long stackId);

    void addNode(User user, Stack stack, String hostgroup);

    Set<InstanceMetaData> getMetaData(String hash);

    StackDescription getStackDescription(User user, Stack stack);

    Boolean assignableHostgroup(Stack stack, String hostgroup) throws IOException;

}
