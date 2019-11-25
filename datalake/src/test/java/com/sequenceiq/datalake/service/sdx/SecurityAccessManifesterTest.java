package com.sequenceiq.datalake.service.sdx;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.securitygroup.SecurityGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.SecurityRuleV4Request;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityAccessManifester service tests")
class SecurityAccessManifesterTest {

    @Mock
    private InternalApiCallCalculator internalApiCallCalculator;

    @InjectMocks
    private SecurityAccessManifester securityAccessManifester;

    @BeforeEach
    void initMocks() {
        when(internalApiCallCalculator.isInternalApiCall(any(SecurityGroupV4Request.class))).thenReturn(false);
    }

    @Test
    void overrideSecurityAccessWhenOneCidrRangeProvidedThenShouldUpdateTheCidrRange() {
        InstanceGroupType gateway = InstanceGroupType.GATEWAY;
        List<InstanceGroupV4Request> instanceGroups = getInstanceGroups();
        String theWholeWorld = "0.0.0.0/0";

        securityAccessManifester.overrideSecurityAccess(gateway, instanceGroups, null, theWholeWorld);

        assertEquals(1, instanceGroups.get(0).getSecurityGroup().getSecurityRules().size());
        assertEquals(List.of(theWholeWorld), collectSubnets(instanceGroups));
    }

    @Test
    void overrideSecurityAccessWhenMultipleCidrRangeProvidedThenShouldUpdateTheCidrRanges() {
        InstanceGroupType gateway = InstanceGroupType.GATEWAY;
        List<InstanceGroupV4Request> instanceGroups = getInstanceGroups();
        String theWholeWorldAndASimpleCidr = "0.0.0.0/0,172.16.0.0/16";

        securityAccessManifester.overrideSecurityAccess(gateway, instanceGroups, null, theWholeWorldAndASimpleCidr);

        assertEquals(2, instanceGroups.get(0).getSecurityGroup().getSecurityRules().size());
        assertEquals(List.of(theWholeWorldAndASimpleCidr.split(",")), collectSubnets(instanceGroups));
    }

    private List<String> collectSubnets(List<InstanceGroupV4Request> instanceGroups) {
        return instanceGroups
                .stream()
                .flatMap(a -> {
                    List<String> subnets = a.getSecurityGroup()
                            .getSecurityRules()
                            .stream()
                            .map(e -> e.getSubnet())
                            .collect(Collectors.toList());
                    return subnets.stream();
                })
                .collect(Collectors.toList());
    }

    private List<InstanceGroupV4Request> getInstanceGroups() {
        List<InstanceGroupV4Request> instanceGroupRequests = new ArrayList<>();
        instanceGroupRequests.add(instanceGroupRequest(1));
        return instanceGroupRequests;
    }

    private InstanceGroupV4Request instanceGroupRequest(int index) {
        InstanceGroupV4Request instanceGroup = new InstanceGroupV4Request();
        instanceGroup.setName("ig-" + index);
        instanceGroup.setNodeCount(1);
        instanceGroup.setType(InstanceGroupType.GATEWAY);

        SecurityGroupV4Request securityGroupV4Request = new SecurityGroupV4Request();

        SecurityRuleV4Request securityRuleV4Request = new SecurityRuleV4Request();
        securityRuleV4Request.setProtocol("tcp");
        securityRuleV4Request.setPorts(List.of("22", "443"));
        securityGroupV4Request.setSecurityRules(Lists.newArrayList(securityRuleV4Request));

        instanceGroup.setSecurityGroup(securityGroupV4Request);


        return instanceGroup;
    }

}
