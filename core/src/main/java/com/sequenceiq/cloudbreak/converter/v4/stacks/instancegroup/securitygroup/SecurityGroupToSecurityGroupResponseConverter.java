package com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.securitygroup;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.securitygroup.SecurityGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.SecurityRuleV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;

@Component
public class SecurityGroupToSecurityGroupResponseConverter extends AbstractConversionServiceAwareConverter<SecurityGroup, SecurityGroupV4Response> {

    @Inject
    private ConverterUtil converterUtil;

    @Override
    public SecurityGroupV4Response convert(SecurityGroup source) {
        SecurityGroupV4Response json = new SecurityGroupV4Response();
        json.setSecurityRules(converterUtil.convertAll(source.getSecurityRules(), SecurityRuleV4Response.class));
        json.setSecurityGroupIds(source.getSecurityGroupIds());
        return json;
    }

}
