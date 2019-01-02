package com.sequenceiq.cloudbreak.converter.v4.stacks.cli;

import java.util.ArrayList;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.securitygroup.SecurityGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.SecurityRuleV4Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.SecurityRule;

@Component
public class SecurityGroupToSecurityGroupV4RequestConverter extends AbstractConversionServiceAwareConverter<SecurityGroup, SecurityGroupV4Request> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityGroupToSecurityGroupV4RequestConverter.class);

    @Override
    public SecurityGroupV4Request convert(@Nonnull SecurityGroup source) {
        SecurityGroupV4Request securityGroupV2Request = new SecurityGroupV4Request();
        securityGroupV2Request.setSecurityGroupIds(source.getSecurityGroupIds());
        securityGroupV2Request.setSecurityRules(new ArrayList<>());
        for (SecurityRule securityRule : source.getSecurityRules()) {
            securityGroupV2Request.getSecurityRules().add(getConversionService().convert(securityRule, SecurityRuleV4Request.class));
        }
        return securityGroupV2Request;
    }

}
