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

    private static final String CIDR_IP_FORMAT = "%s/32";

    @Value("#{'${cb.publicip:}'.split(',')}")
    private Set<String> cloudbreakPublicIps;

    @Value("${cb.nginx.port:9443}")
    private String gatewayPort;

    @Value("${cb.knox.port:8443}")
    private String knoxPort;

    @Value("${cb.https.port:443}")
    private String httpsPort;

    @Value("${cb.ssh.port:22}")
    private String sshPort;

    @Value("${cb.default.gateway.cidr:}")
    private String defaultGatewayCidr;

    public SecurityRulesResponse getPublicIps() {

        SecurityRulesResponse ret = new SecurityRulesResponse();

        Set<String> defaultPublicIps = cloudbreakPublicIps.stream().filter(StringUtils::isNotBlank).collect(Collectors.toSet());

        if (!defaultPublicIps.isEmpty()) {
            defaultPublicIps.forEach(ip -> addCidrAndPort(ret, String.format(CIDR_IP_FORMAT, ip)));
        }

        if (StringUtils.isNotBlank(defaultGatewayCidr)) {
            addCidrAndPort(ret, defaultGatewayCidr);
        }

        return ret;
    }

    private void addCidrAndPort(SecurityRulesResponse ret, String cidr) {
        ret.getCore().addAll(createSecurityRuleResponse(cidr, sshPort));
        ret.getGateway().addAll(createSecurityRuleResponse(cidr, sshPort, gatewayPort, knoxPort, httpsPort));
    }

    private List<SecurityRuleResponse> createSecurityRuleResponse(String cidr, String... ports) {
        List<SecurityRuleResponse> rules = new ArrayList<>();
        for (String port : ports) {
            SecurityRuleResponse securityRuleResponse = new SecurityRuleResponse();
            securityRuleResponse.setPorts(port);
            securityRuleResponse.setProtocol(TCP_PROTOCOL);
            securityRuleResponse.setSubnet(cidr);
            rules.add(securityRuleResponse);
        }
        return rules;
    }

}
