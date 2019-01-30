package com.sequenceiq.cloudbreak.converter.v2.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.SecurityRuleRequest;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.SecurityRule;

@Component
public class SecurityRuleToSecurityRuleRequestConverter extends AbstractConversionServiceAwareConverter<SecurityRule, SecurityRuleRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityRuleToSecurityRuleRequestConverter.class);

    @Override
    public SecurityRuleRequest convert(SecurityRule source) {
        SecurityRuleRequest securityRuleRequest = new SecurityRuleRequest();
        StringBuilder sb = new StringBuilder();
        for (String portDefinition : source.getPorts()) {
            sb.append(String.format("%s,", portDefinition));
        }
        if (!SecurityRule.ICMP.equalsIgnoreCase(source.getProtocol())) {
            securityRuleRequest.setPorts(sb.toString().substring(0, sb.toString().length() - 1));
        }
        securityRuleRequest.setProtocol(source.getProtocol());
        securityRuleRequest.setSubnet(source.getCidr());
        return securityRuleRequest;
    }

}
