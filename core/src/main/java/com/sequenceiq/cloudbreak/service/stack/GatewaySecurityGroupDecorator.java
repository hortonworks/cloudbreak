package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.service.securityrule.SecurityRuleService.TCP_PROTOCOL;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.SecurityRule;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.Tunnel;

@Component
public class GatewaySecurityGroupDecorator {
    private static final Logger LOGGER = LoggerFactory.getLogger(GatewaySecurityGroupDecorator.class);

    @Value("#{'${cb.default.gateway.cidr}'.split(',')}")
    private Set<String> defaultGatewayCidr;

    @Value("${cb.nginx.port}")
    private Integer nginxPort;

    @Value("${cb.https.port}")
    private Integer httpsPort;

    public void extendGatewaySecurityGroupWithDefaultGatewayCidrs(Stack stack, Tunnel tunnel) {
        Set<InstanceGroup> gateways = filterForGateways(stack);
        Set<String> defaultGatewayCidrs = filterEmptyCidrs();
        LOGGER.debug("Extending security group for gateways: [{}] DefaultGatewayCidrs: [{}] Tunnel: [{}]",
                gateways.stream().map(InstanceGroup::getGroupName).collect(Collectors.joining(", ")), defaultGatewayCidrs, tunnel);
        if (!defaultGatewayCidrs.isEmpty() && !tunnel.useCcm()) {
            extendGatewaysSecurityGroupsWithRules(stack, gateways, defaultGatewayCidrs);
        }
    }

    private void extendGatewaysSecurityGroupsWithRules(Stack stack, Set<InstanceGroup> gateways, Set<String> defaultGatewayCidrs) {
        for (InstanceGroup gateway : gateways) {
            SecurityGroup securityGroup = createNewSecurityGroupIfNotPresent(gateway);
            LOGGER.debug("InstanceGroupName: [{}] SecurityGoupIds: [{}]", gateway.getGroupName(), securityGroup.getSecurityGroupIds());
            if (CollectionUtils.isEmpty(securityGroup.getSecurityGroupIds())) {
                addSecurityRuleToSecurityGroup(stack, defaultGatewayCidrs, gateway);
            }
        }
    }

    private SecurityGroup createNewSecurityGroupIfNotPresent(InstanceGroup gateway) {
        SecurityGroup securityGroup = Optional.ofNullable(gateway.getSecurityGroup()).orElse(new SecurityGroup());
        gateway.setSecurityGroup(securityGroup);
        return securityGroup;
    }

    private void addSecurityRuleToSecurityGroup(Stack stack, Set<String> defaultGatewayCidrs, InstanceGroup gateway) {
        SecurityGroup securityGroup = gateway.getSecurityGroup();
        Set<SecurityRule> rules = securityGroup.getSecurityRules();
        String ports = collectPorts(stack);
        defaultGatewayCidrs.forEach(cloudbreakCidr -> rules.add(createSecurityRule(securityGroup, cloudbreakCidr, ports)));
        LOGGER.info("The control plane cidrs {} are added to the {} gateway group for the {} port.", defaultGatewayCidrs, gateway.getGroupName(),
                stack.getGatewayPort());
    }

    private String collectPorts(Stack stack) {
        return (stack.getGatewayPort() == null ? nginxPort.toString() : stack.getGatewayPort().toString()) + ',' + httpsPort;
    }

    private Set<String> filterEmptyCidrs() {
        return defaultGatewayCidr.stream().filter(StringUtils::isNotBlank).collect(Collectors.toSet());
    }

    private Set<InstanceGroup> filterForGateways(Stack stack) {
        return stack.getInstanceGroups().stream().filter(ig -> InstanceGroupType.isGateway(ig.getInstanceGroupType())).collect(Collectors.toSet());
    }

    private SecurityRule createSecurityRule(SecurityGroup securityGroup, String cidr, String ports) {
        SecurityRule securityRule = new SecurityRule();
        securityRule.setPorts(ports);
        securityRule.setProtocol(TCP_PROTOCOL);
        securityRule.setCidr(cidr);
        securityRule.setSecurityGroup(securityGroup);
        return securityRule;
    }
}
