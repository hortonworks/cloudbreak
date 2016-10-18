package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.SecurityRuleRequest;
import com.sequenceiq.cloudbreak.domain.SecurityRule;

@Component
public class JsonToSecurityRuleConverter extends AbstractConversionServiceAwareConverter<SecurityRuleRequest, SecurityRule> {
    @Override
    public SecurityRule convert(SecurityRuleRequest json) {
        SecurityRule entity = new SecurityRule();
        entity.setCidr(json.getSubnet());
        entity.setPorts(json.getPorts());
        entity.setProtocol(json.getProtocol());
        entity.setModifiable(json.isModifiable());
        return entity;
    }
}
