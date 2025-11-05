package com.sequenceiq.datalake.service.sdx;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.securitygroup.SecurityGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.SecurityRuleV4Request;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@Service
public class SecurityConfigService {

    public void prepareDefaultSecurityConfigs(SdxClusterShape shape, StackV4Request stackV4Request, CloudPlatform cloudPlatform) {
        if (!SdxClusterShape.CUSTOM.equals(shape) && !List.of(CloudPlatform.MOCK, CloudPlatform.YARN).contains(cloudPlatform)) {
            stackV4Request.getInstanceGroups().forEach(instance -> {
                SecurityGroupV4Request groupRequest = new SecurityGroupV4Request();
                if (InstanceGroupType.CORE.equals(instance.getType())) {
                    groupRequest.setSecurityRules(rulesWithPorts("22"));
                } else if (InstanceGroupType.GATEWAY.equals(instance.getType())) {
                    groupRequest.setSecurityRules(rulesWithPorts("443", "22"));
                } else {
                    throw new IllegalStateException("Unknown instance group type " + instance.getType());
                }
                instance.setSecurityGroup(groupRequest);
            });
        }
    }

    private List<SecurityRuleV4Request> rulesWithPorts(String... ports) {
        return Stream.of(ports)
                .map(port -> {
                    SecurityRuleV4Request ruleRequest = new SecurityRuleV4Request();
                    ruleRequest.setSubnet("0.0.0.0/0");
                    ruleRequest.setPorts(List.of(port));
                    ruleRequest.setProtocol("tcp");
                    return ruleRequest;
                })
                .collect(Collectors.toList());
    }
}
