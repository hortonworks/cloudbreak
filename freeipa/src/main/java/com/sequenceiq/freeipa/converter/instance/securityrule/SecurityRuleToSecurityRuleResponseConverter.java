package com.sequenceiq.freeipa.converter.instance.securityrule;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.SecurityRuleResponse;
import com.sequenceiq.freeipa.entity.SecurityRule;

@Component
public class SecurityRuleToSecurityRuleResponseConverter implements Converter<SecurityRule, SecurityRuleResponse> {

    @Override
    public SecurityRuleResponse convert(SecurityRule source) {
        SecurityRuleResponse entity = new SecurityRuleResponse();
        entity.setSubnet(source.getCidr());
        entity.setPorts(Arrays.asList(source.getPorts()));
        entity.setProtocol(source.getProtocol());
        entity.setModifiable(source.isModifiable());
        return entity;
    }

    public List<SecurityRuleResponse> convert(Iterable<SecurityRule> source) {
        return StreamSupport.stream(source.spliterator(), false).map(this::convert).collect(Collectors.toList());
    }

}
