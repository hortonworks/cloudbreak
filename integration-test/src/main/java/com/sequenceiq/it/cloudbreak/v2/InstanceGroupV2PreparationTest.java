package com.sequenceiq.it.cloudbreak.v2;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.google.api.client.util.Lists;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.model.SecurityRuleRequest;
import com.sequenceiq.cloudbreak.api.model.v2.InstanceGroupV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.SecurityGroupV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.TemplateV2Request;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.TemplateAdditionHelper;

public class InstanceGroupV2PreparationTest extends AbstractCloudbreakIntegrationTest {
    @Inject
    private TemplateAdditionHelper templateAdditionHelper;

    @BeforeMethod(groups = "igRequestCreation")
    @Parameters({"group", "nodeCount", "groupType", "recoveryMode"})
    public void createInstanceGroupRequest(String group, int nodeCount, String groupType, @Optional("MANUAL") String recoveryMode) {
        InstanceGroupV2Request instanceGroupV2Request = new InstanceGroupV2Request();
        instanceGroupV2Request.setGroup(group);
        instanceGroupV2Request.setNodeCount(nodeCount);
        instanceGroupV2Request.setType(InstanceGroupType.valueOf(groupType));

        IntegrationTestContext itContext = getItContext();
        Map<String, InstanceGroupV2Request> igMap;
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
        TemplateV2Request templateV2Request = new TemplateV2Request();
        templateV2Request.setInstanceType(instanceType);
        templateV2Request.setVolumeType(volumeType);
        templateV2Request.setVolumeSize(volumeSize);
        templateV2Request.setVolumeCount(volumeCount);
        IntegrationTestContext itContext = getItContext();
        Map<String, InstanceGroupV2Request> igMap = itContext.getContextParam(CloudbreakV2Constants.INSTANCEGROUP_MAP, Map.class);
        InstanceGroupV2Request instanceGroupV2Request = igMap.get(group);
        instanceGroupV2Request.setTemplate(templateV2Request);
    }

    @BeforeMethod(dependsOnGroups = "igRequestCreation")
    @Parameters({"group", "securityRules"})
    public void createSecurityRequest(String group, String securityRules) {
        IntegrationTestContext itContext = getItContext();
        Map<String, InstanceGroupV2Request> igMap = itContext.getContextParam(CloudbreakV2Constants.INSTANCEGROUP_MAP, Map.class);
        InstanceGroupV2Request instanceGroupV2Request = igMap.get(group);
        SecurityGroupV2Request securityGroupV2Request = new SecurityGroupV2Request();
        List<String[]> secRules = templateAdditionHelper.parseCommaSeparatedRows(securityRules);
        List<SecurityRuleRequest> secRulesRequests = Lists.newArrayList();
        for (String[] secRule : secRules) {
            SecurityRuleRequest securityRuleRequest = new SecurityRuleRequest();
            securityRuleRequest.setProtocol(secRule[0]);
            securityRuleRequest.setSubnet(secRule[1]);
            securityRuleRequest.setPorts(secRule[2]);
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
