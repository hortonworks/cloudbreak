package com.sequenceiq.cloudbreak.converter.v4.stacks.cli;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.SecurityRuleV4Request;
import com.sequenceiq.cloudbreak.domain.SecurityRule;

@Component
public class SecurityRuleToSecurityRuleV4RequestConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityRuleToSecurityRuleV4RequestConverter.class);

    public SecurityRuleV4Request convert(SecurityRule source) {
        SecurityRuleV4Request securityRuleRequest = new SecurityRuleV4Request();
        securityRuleRequest.setPorts(Arrays.asList(source.getPorts()));
        securityRuleRequest.setProtocol(source.getProtocol());
        securityRuleRequest.setSubnet(source.getCidr());
        return securityRuleRequest;
    }

}
