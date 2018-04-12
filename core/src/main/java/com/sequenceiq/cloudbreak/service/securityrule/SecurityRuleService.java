package com.sequenceiq.cloudbreak.service.securityrule;

import com.sequenceiq.cloudbreak.api.model.SecurityRuleResponse;
import com.sequenceiq.cloudbreak.api.model.SecurityRulesResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class SecurityRuleService {

    private static final String TCP_PROTOCOL = "tcp";

    @Value("${cb.nginx.port:9443}")
    private String gatewayPort;

    @Value("${cb.ssh.port:22}")
    private String sshPort;

    @Value("#{'${cb.default.gateway.cidr:0.0.0.0/0}'.split(',')}")
    private Set<String> defaultGatewayCidr;

    public SecurityRulesResponse getDefaultSecurityRules() {

        SecurityRulesResponse ret = new SecurityRulesResponse();

        Set<String> defaultGatewayCidrs = defaultGatewayCidr.stream().filter(StringUtils::isNotBlank).collect(Collectors.toSet());

        if (!defaultGatewayCidrs.isEmpty()) {
            defaultGatewayCidr.forEach(cidr -> addCidrAndPort(ret, cidr));
        }

        return ret;
    }

    private void addCidrAndPort(SecurityRulesResponse ret, String cidr) {
        ret.getGateway().addAll(createSecurityRuleResponse(cidr, sshPort, gatewayPort));
    }

    private Collection<SecurityRuleResponse> createSecurityRuleResponse(String cidr, String... ports) {
        Collection<SecurityRuleResponse> rules = new ArrayList<>();
        for (String port : ports) {
            if (StringUtils.isNotBlank(port)) {
                SecurityRuleResponse securityRuleResponse = new SecurityRuleResponse();
                securityRuleResponse.setPorts(port);
                securityRuleResponse.setProtocol(TCP_PROTOCOL);
                securityRuleResponse.setSubnet(cidr);
                rules.add(securityRuleResponse);
            }
        }
        return rules;
    }

}
