package com.sequenceiq.cloudbreak.converter.v2;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.v2.SecurityGroupV2Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.SecurityRule;
import com.sequenceiq.cloudbreak.util.ConverterUtil;

@Component
public class SecurityGroupV2RequestToSecurityGroupConverter
        extends AbstractConversionServiceAwareConverter<SecurityGroupV2Request, SecurityGroup> {

    @Inject
    private ConverterUtil converterUtil;

    @Override
    public SecurityGroup convert(@Nonnull SecurityGroupV2Request source) {
        SecurityGroup entity = new SecurityGroup();
        entity.setSecurityGroupIds(source.getSecurityGroupIds());
        if (source.getSecurityRules() != null) {
            entity.setSecurityRules(converterUtil.convertAllAsSet(source.getSecurityRules(), SecurityRule.class));
        }
        return entity;
    }
}
