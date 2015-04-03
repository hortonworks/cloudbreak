package com.sequenceiq.cloudbreak.core.flow;

import java.util.Set;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;

public interface ClusterSetupService {

    void preSetup(Long stackId, InstanceGroup gateway, Set<InstanceGroup> hostGroupTypeGroups) throws CloudbreakException;

    void gatewaySetup(Long stackId, InstanceGroup gateway) throws CloudbreakException;

    void hostgroupsSetup(Long stackId, Set<InstanceGroup> instanceGroups) throws CloudbreakException;

    FlowContext postSetup(Long stackId) throws CloudbreakException;

    ClusterSetupTool clusterSetupTool();

    void preSetupNewNode(Long stackId, InstanceGroup gateway, Set<String> instanceIds) throws CloudbreakException;

    void newHostgroupNodesSetup(Long stackId, Set<String> instanceIds, String hostGroup) throws CloudbreakException;

}
