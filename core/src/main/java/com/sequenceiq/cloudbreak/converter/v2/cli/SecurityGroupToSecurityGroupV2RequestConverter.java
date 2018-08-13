package com.sequenceiq.cloudbreak.converter.v2.cli;

import java.util.ArrayList;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.SecurityRuleRequest;
import com.sequenceiq.cloudbreak.api.model.v2.SecurityGroupV2Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.SecurityRule;

@Component
public class SecurityGroupToSecurityGroupV2RequestConverter extends AbstractConversionServiceAwareConverter<SecurityGroup, SecurityGroupV2Request> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityGroupToSecurityGroupV2RequestConverter.class);

    @Override
    public SecurityGroupV2Request convert(@Nonnull SecurityGroup source) {
        SecurityGroupV2Request securityGroupV2Request = new SecurityGroupV2Request();
        //because of backward compability
        securityGroupV2Request.setSecurityGroupId(source.getFirstSecurityGroupId());
        securityGroupV2Request.setSecurityGroupIds(source.getSecurityGroupIds());
        securityGroupV2Request.setSecurityRules(new ArrayList<>());
        for (SecurityRule securityRule : source.getSecurityRules()) {
            securityGroupV2Request.getSecurityRules().add(getConversionService().convert(securityRule, SecurityRuleRequest.class));
        }
        return securityGroupV2Request;
    }

}
