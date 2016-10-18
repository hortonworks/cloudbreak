package com.sequenceiq.cloudbreak.converter;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.SecurityRuleResponse;
import com.sequenceiq.cloudbreak.domain.SecurityRule;

@Component
public class SecurityRuleToJsonConverter extends AbstractConversionServiceAwareConverter<SecurityRule, SecurityRuleResponse> {

    @Override
    public SecurityRuleResponse convert(SecurityRule entity) {
        SecurityRuleResponse json = new SecurityRuleResponse(entity.getCidr());
        json.setId(entity.getId());
        json.setPorts(StringUtils.join(entity.getPorts(), ","));
        json.setProtocol(entity.getProtocol());
        json.setModifiable(entity.isModifiable());
        return json;
    }
}
