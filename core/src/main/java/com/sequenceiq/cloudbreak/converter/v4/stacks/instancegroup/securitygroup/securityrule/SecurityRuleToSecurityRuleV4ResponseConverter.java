package com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.securitygroup.securityrule;

import java.util.Arrays;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.SecurityRuleV4Response;
import com.sequenceiq.cloudbreak.domain.SecurityRule;

@Component
public class SecurityRuleToSecurityRuleV4ResponseConverter {

    public SecurityRuleV4Response convert(SecurityRule source) {
        SecurityRuleV4Response entity = new SecurityRuleV4Response();
        entity.setSubnet(source.getCidr());
        entity.setPorts(Arrays.asList(source.getPorts()));
        entity.setProtocol(source.getProtocol());
        entity.setModifiable(source.isModifiable());
        return entity;
    }

}
