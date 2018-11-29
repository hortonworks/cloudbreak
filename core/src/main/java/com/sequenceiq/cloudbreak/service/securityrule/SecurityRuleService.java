package com.sequenceiq.cloudbreak.service.securityrule;

import java.util.ArrayList;
import java.util.Collection;
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

    @Value("${cb.nginx.port}")
    private String gatewayPort;

    @Value("${cb.knox.port}")
    private String knoxPort;

    @Value("${cb.ssh.port}")
    private String sshPort;

    @Value("${cb.https.port}")
    private String httpsPort;

    @Value("#{'${cb.default.gateway.cidr:0.0.0.0/0}'.split(',')}")
    private Set<String> defaultGatewayCidr;

    public SecurityRulesResponse getDefaultSecurityRules(Boolean knoxEnabled) {
        SecurityRulesResponse ret = new SecurityRulesResponse();
        Set<String> defaultGatewayCidrs = defaultGatewayCidr
                .stream()
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
        if (!defaultGatewayCidrs.isEmpty()) {
            defaultGatewayCidr.forEach(cloudbreakCidr -> addCidrAndPort(ret, cloudbreakCidr, knoxEnabled));
        }
        return ret;
    }

    private void addCidrAndPort(SecurityRulesResponse ret, String cloudbreakCidr, Boolean knoxEnabled) {
        if (knoxEnabled) {
            ret.getGateway().addAll(createSecurityRuleResponse(cloudbreakCidr, gatewayPort, knoxPort));
        } else {
            ret.getGateway().addAll(createSecurityRuleResponse(cloudbreakCidr, gatewayPort, httpsPort));
        }
        ret.getGateway().addAll(createSecurityRuleResponse(cloudbreakCidr, sshPort));
        ret.getCore().addAll(createSecurityRuleResponse(cloudbreakCidr, sshPort));
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
