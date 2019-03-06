package com.sequenceiq.cloudbreak.structuredevent.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.SecurityRule;
import com.sequenceiq.cloudbreak.structuredevent.event.SecurityRuleDetails;

@Component
public class SecurityRuleToSecurityRuleDetailsConverter extends AbstractConversionServiceAwareConverter<SecurityRule, SecurityRuleDetails> {

    @Override
    public SecurityRuleDetails convert(SecurityRule source) {
        SecurityRuleDetails securityRuleDetails = new SecurityRuleDetails();
        securityRuleDetails.setCidr(source.getCidr());
        securityRuleDetails.setProtocol(source.getProtocol());
        if (!SecurityRule.ICMP.equalsIgnoreCase(source.getProtocol())) {
            securityRuleDetails.setPorts(String.join(",", source.getPorts()));
        }
        return securityRuleDetails;
    }
}
