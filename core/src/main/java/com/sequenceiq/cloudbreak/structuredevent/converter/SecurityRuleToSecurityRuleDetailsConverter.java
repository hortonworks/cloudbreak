package com.sequenceiq.cloudbreak.structuredevent.converter;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.SecurityRule;
import com.sequenceiq.cloudbreak.structuredevent.event.SecurityRuleDetails;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class SecurityRuleToSecurityRuleDetailsConverter extends AbstractConversionServiceAwareConverter<SecurityRule, SecurityRuleDetails> {
    @Inject
    private ConversionService conversionService;

    @Override
    public SecurityRuleDetails convert(SecurityRule source) {
        SecurityRuleDetails securityRuleDetails = new SecurityRuleDetails();
        securityRuleDetails.setCidr(source.getCidr());
        securityRuleDetails.setProtocol(source.getProtocol());
        securityRuleDetails.setPorts(Arrays.stream(source.getPorts()).collect(Collectors.joining(",")));
        return securityRuleDetails;
    }
}
