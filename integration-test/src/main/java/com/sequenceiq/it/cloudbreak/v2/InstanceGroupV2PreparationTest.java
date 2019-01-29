package com.sequenceiq.it.cloudbreak.v2;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.google.api.client.util.Lists;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.securitygroup.SecurityGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.VolumeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.SecurityRuleV4Request;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.TemplateAdditionHelper;

public class InstanceGroupV2PreparationTest extends AbstractCloudbreakIntegrationTest {
    @Inject
    private TemplateAdditionHelper templateAdditionHelper;

    @BeforeMethod(groups = "igRequestCreation")
    @Parameters({"group", "nodeCount", "groupType", "recoveryMode"})
    public void createInstanceGroupRequest(String group, int nodeCount, String groupType, @Optional("MANUAL") String recoveryMode) {
        InstanceGroupV4Request instanceGroupV2Request = new InstanceGroupV4Request();
        instanceGroupV2Request.setName(group);
        instanceGroupV2Request.setNodeCount(nodeCount);
        instanceGroupV2Request.setType(InstanceGroupType.valueOf(groupType));

        IntegrationTestContext itContext = getItContext();
        Map<String, InstanceGroupV4Request> igMap;
        synchronized (itContext) {
            igMap = itContext.getContextParam(CloudbreakV2Constants.INSTANCEGROUP_MAP, Map.class);
            if (igMap == null) {
                igMap = Maps.newConcurrentMap();
                itContext.putContextParam(CloudbreakV2Constants.INSTANCEGROUP_MAP, igMap);
            }
        }
        igMap.put(group, instanceGroupV2Request);
    }

    @BeforeMethod(dependsOnGroups = "igRequestCreation")
    @Parameters({"group", "instanceType", "volumeType", "volumeSize", "volumeCount"})
    public void createTemplateRequest(String group, String instanceType, String volumeType, int volumeSize, int volumeCount) {
        InstanceTemplateV4Request templateV2Request = new InstanceTemplateV4Request();
        templateV2Request.setInstanceType(instanceType);
        VolumeV4Request volume = new VolumeV4Request();
        volume.setType(volumeType);
        volume.setSize(volumeSize);
        volume.setCount(volumeCount);
        templateV2Request.setAttachedVolumes(Set.of(volume));
        IntegrationTestContext itContext = getItContext();
        Map<String, InstanceGroupV4Request> igMap = itContext.getContextParam(CloudbreakV2Constants.INSTANCEGROUP_MAP, Map.class);
        InstanceGroupV4Request instanceGroupV2Request = igMap.get(group);
        instanceGroupV2Request.setTemplate(templateV2Request);
    }

    @BeforeMethod(dependsOnGroups = "igRequestCreation")
    @Parameters({"group", "securityRules"})
    public void createSecurityRequest(String group, String securityRules) {
        IntegrationTestContext itContext = getItContext();
        Map<String, InstanceGroupV4Request> igMap = itContext.getContextParam(CloudbreakV2Constants.INSTANCEGROUP_MAP, Map.class);
        InstanceGroupV4Request instanceGroupV2Request = igMap.get(group);
        SecurityGroupV4Request securityGroupV2Request = new SecurityGroupV4Request();
        List<String[]> secRules = templateAdditionHelper.parseCommaSeparatedRows(securityRules);
        List<SecurityRuleV4Request> secRulesRequests = Lists.newArrayList();
        for (String[] secRule : secRules) {
            SecurityRuleV4Request securityRuleRequest = new SecurityRuleV4Request();
            securityRuleRequest.setProtocol(secRule[0]);
            securityRuleRequest.setSubnet(secRule[1]);
            securityRuleRequest.setPorts(List.of(secRule[2]));
            secRulesRequests.add(securityRuleRequest);
        }
        securityGroupV2Request.setSecurityRules(secRulesRequests);
        instanceGroupV2Request.setSecurityGroup(securityGroupV2Request);
    }

    @Test
    public void prepareInstanceGroup() {
        // Currently no test is needed, only the requests are created in the methods annotated with @BeforeMthod and stored in the context
    }
}
