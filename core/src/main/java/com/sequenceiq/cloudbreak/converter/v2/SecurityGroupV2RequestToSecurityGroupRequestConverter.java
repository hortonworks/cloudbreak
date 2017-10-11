package com.sequenceiq.cloudbreak.converter.v2;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.SecurityGroupRequest;
import com.sequenceiq.cloudbreak.api.model.v2.SecurityGroupV2Request;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.service.MissingResourceNameGenerator;

@Component
public class SecurityGroupV2RequestToSecurityGroupRequestConverter
        extends AbstractConversionServiceAwareConverter<SecurityGroupV2Request, SecurityGroupRequest> {

    @Inject
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Override
    public SecurityGroupRequest convert(SecurityGroupV2Request source) {
        SecurityGroupRequest entity = new SecurityGroupRequest();
        entity.setName(missingResourceNameGenerator.generateName(APIResourceType.SECURITY_GROUP));
        entity.setSecurityGroupId(source.getSecurityGroupId());
        entity.setSecurityRules(source.getSecurityRules());
        entity.setSecurityGroupId(source.getSecurityGroupId());
        return entity;
    }
}
