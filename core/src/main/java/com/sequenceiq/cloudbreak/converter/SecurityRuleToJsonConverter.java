package com.sequenceiq.cloudbreak.converter;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.SecurityRuleJson;
import com.sequenceiq.cloudbreak.domain.SecurityRule;

@Component
public class SecurityRuleToJsonConverter extends AbstractConversionServiceAwareConverter<SecurityRule, SecurityRuleJson> {

    @Override
    public SecurityRuleJson convert(SecurityRule entity) {
        SecurityRuleJson json = new SecurityRuleJson(entity.getCidr());
        json.setId(entity.getId());
        json.setPorts(StringUtils.join(entity.getPorts(), ","));
        json.setProtocol(entity.getProtocol());
        json.setModifiable(entity.isModifiable());
        return json;
    }
}
