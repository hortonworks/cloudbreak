package com.sequenceiq.it.cloudbreak.newway.cloud;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.model.SecurityRuleRequest;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.model.v2.InstanceGroupV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.SecurityGroupV2Request;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;

public enum HostGroupType {
    MASTER("master", InstanceGroupType.GATEWAY, InstanceCountParameter.MASTER_INSTANCE_COUNT.getName()),
    WORKER("worker", InstanceGroupType.CORE, InstanceCountParameter.WORKER_INSTANCE_COUNT.getName()),
    COMPUTE("compute", InstanceGroupType.CORE, InstanceCountParameter.COMPUTE_INSTANCE_COUNT.getName()),
    SERVICES("Services", InstanceGroupType.GATEWAY, InstanceCountParameter.SERVICE_INSTANCE_COUNT.getName()),
    NIFI("NiFi", InstanceGroupType.CORE, InstanceCountParameter.NIFI_INSTANCE_COUNT.getName()),
    ZOOKEEPER("ZooKeeper", InstanceGroupType.CORE, InstanceCountParameter.ZOOKEEPER_INSTANCE_COUNT.getName());

    private final String name;

    private final String countParameterName;

    private final InstanceGroupType instanceGroupType;

    HostGroupType(String name, InstanceGroupType instanceGroupType, String countParameterName) {
        this.name = name;
        this.instanceGroupType = instanceGroupType;
        this.countParameterName = countParameterName;
    }

    public String getCountParameterName() {
        return countParameterName;
    }

    public String getName() {
        return name;
    }

    public InstanceGroupType getInstanceGroupType() {
        return instanceGroupType;
    }

    public InstanceGroupV2Request hostgroupRequest(CloudProvider cloudProvider, TestParameter testParameter) {
        return hostgroup(cloudProvider, name, instanceGroupType, determineInstanceCount(testParameter), getSecurityGroupV2Request());
    }

    public InstanceGroupV2Request hostgroupRequest(CloudProvider cloudProvider, TestParameter testParameter, Set<String> recipes) {
        return hostgroup(cloudProvider, name, instanceGroupType, determineInstanceCount(testParameter), recipes);
    }

    public InstanceGroupV2Request hostgroupRequest(CloudProvider cloudProvider, TestParameter testParameter, String securityGroupId) {
        return hostgroup(cloudProvider, name, instanceGroupType, determineInstanceCount(testParameter), securityGroupId);
    }

    public int determineInstanceCount(TestParameter testParameter) {
        String instanceCount = testParameter.get(countParameterName);
        int instanceCountInt;
        try {
            instanceCountInt = Integer.parseInt(instanceCount);
        } catch (NumberFormatException e) {
            instanceCountInt = 1;
        }
        return instanceCountInt;
    }

    private InstanceGroupV2Request hostgroup(CloudProvider cloudProvider, String groupName, InstanceGroupType groupType, int nodeCount,
            SecurityGroupV2Request securityGroupV2Request) {
        InstanceGroupV2Request r = new InstanceGroupV2Request();
        r.setNodeCount(nodeCount);
        r.setGroup(groupName);
        r.setType(groupType);
        r.setSecurityGroup(securityGroupV2Request);
        r.setTemplate(cloudProvider.template());
        r.setRecoveryMode(cloudProvider.getRecoveryModeParam(getName()));
        return r;
    }

    private InstanceGroupV2Request hostgroup(CloudProvider cloudProvider, String groupName, InstanceGroupType groupType, int nodeCount, String securityGroupId) {
        InstanceGroupV2Request r = hostgroup(cloudProvider, groupName, groupType, nodeCount, getSecurityGroupV2Request(securityGroupId));
        return r;
    }

    private InstanceGroupV2Request hostgroup(CloudProvider cloudProvider, String groupName, InstanceGroupType groupType, int nodeCount, Set<String> recipes) {
        InstanceGroupV2Request r = hostgroup(cloudProvider, groupName, groupType, nodeCount, getSecurityGroupV2Request());
        r.setRecipeNames(recipes);
        return r;
    }

    private SecurityGroupV2Request getSecurityGroupV2Request() {
        SecurityGroupV2Request s = new SecurityGroupV2Request();
        s.setSecurityRules(rules());
        return s;
    }

    private SecurityGroupV2Request getSecurityGroupV2Request(String securityGroupId) {
        SecurityGroupV2Request s = new SecurityGroupV2Request();
        s.setSecurityGroupId(securityGroupId);
        return s;
    }

    private List<SecurityRuleRequest> rules() {
        List<SecurityRuleRequest> rules = new ArrayList<>();
        SecurityRuleRequest a = new SecurityRuleRequest();
        a.setSubnet("0.0.0.0/0");
        a.setProtocol("tcp");
        a.setPorts("22,443,8443,9443,8080");
        rules.add(a);

        return rules;
    }
}
