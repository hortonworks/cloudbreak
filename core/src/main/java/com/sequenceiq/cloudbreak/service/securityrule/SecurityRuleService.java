package com.sequenceiq.cloudbreak.service.securityrule;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.SecurityRuleResponse;
import com.sequenceiq.cloudbreak.api.model.SecurityRulesResponse;

@Component
public class SecurityRuleService {

    private static final String TCP_PROTOCOL = "tcp";

    @Value("${cb.nginx.port:9443}")
    private String gatewayPort;

    @Value("${cb.ssh.port:22}")
    private String sshPort;

    @Value("#{'${cb.default.gateway.cidr:}'.split(',')}")
    private Set<String> defaultGatewayCidr;

    public SecurityRulesResponse getPublicIps() {

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

    private List<SecurityRuleResponse> createSecurityRuleResponse(String cidr, String... ports) {
        List<SecurityRuleResponse> rules = new ArrayList<>();
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
