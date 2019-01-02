package com.sequenceiq.cloudbreak.converter;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.SecurityRuleV4Response;
import com.sequenceiq.cloudbreak.domain.SecurityRule;

@Component
public class SecurityRuleToSecurityRuleResponseConverter extends AbstractConversionServiceAwareConverter<SecurityRule, SecurityRuleV4Response> {

    @Override
    public SecurityRuleV4Response convert(SecurityRule entity) {
        SecurityRuleV4Response json = new SecurityRuleV4Response(entity.getCidr());
        json.setId(entity.getId());
        json.setPorts(StringUtils.join(entity.getPorts(), ","));
        json.setProtocol(entity.getProtocol());
        json.setModifiable(entity.isModifiable());
        return json;
    }
}
