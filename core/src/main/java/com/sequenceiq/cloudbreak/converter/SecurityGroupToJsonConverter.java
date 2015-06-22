package com.sequenceiq.cloudbreak.converter;

import java.util.List;
import java.util.Set;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.SecurityGroupJson;
import com.sequenceiq.cloudbreak.controller.json.SecurityRuleJson;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.SecurityRule;

@Component
public class SecurityGroupToJsonConverter extends AbstractConversionServiceAwareConverter<SecurityGroup, SecurityGroupJson> {

    @Override
    public SecurityGroupJson convert(SecurityGroup source) {
        SecurityGroupJson json = new SecurityGroupJson();
        json.setId(source.getId());
        json.setName(source.getName());
        json.setDescription(source.getDescription());
        json.setAccount(source.getAccount());
        json.setOwner(source.getOwner());
        json.setPublicInAccount(source.isPublicInAccount());
        json.setSecurityRules(convertSecurityRules(source.getSecurityRules()));
        return json;
    }

    private List<SecurityRuleJson> convertSecurityRules(Set<SecurityRule> securityRules) {
        return (List<SecurityRuleJson>) getConversionService().convert(securityRules, TypeDescriptor.forObject(securityRules),
                TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(SecurityRuleJson.class)));
    }
}
