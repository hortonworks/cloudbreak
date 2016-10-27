package com.sequenceiq.cloudbreak.converter;

import java.util.List;
import java.util.Set;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.SecurityGroupResponse;
import com.sequenceiq.cloudbreak.api.model.SecurityRuleResponse;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.SecurityRule;

@Component
public class SecurityGroupToJsonConverter extends AbstractConversionServiceAwareConverter<SecurityGroup, SecurityGroupResponse> {

    @Override
    public SecurityGroupResponse convert(SecurityGroup source) {
        SecurityGroupResponse json = new SecurityGroupResponse();
        json.setId(source.getId());
        json.setName(source.getName());
        json.setDescription(source.getDescription());
        json.setAccount(source.getAccount());
        json.setOwner(source.getOwner());
        json.setPublicInAccount(source.isPublicInAccount());
        json.setSecurityRules(convertSecurityRules(source.getSecurityRules()));
        return json;
    }

    private List<SecurityRuleResponse> convertSecurityRules(Set<SecurityRule> securityRules) {
        return (List<SecurityRuleResponse>) getConversionService().convert(securityRules, TypeDescriptor.forObject(securityRules),
                TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(SecurityRuleResponse.class)));
    }
}
