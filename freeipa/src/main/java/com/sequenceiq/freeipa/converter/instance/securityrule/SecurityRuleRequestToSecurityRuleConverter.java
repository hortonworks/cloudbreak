package com.sequenceiq.freeipa.converter.instance.securityrule;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.SecurityRuleRequest;
import com.sequenceiq.freeipa.entity.SecurityRule;

@Component
public class SecurityRuleRequestToSecurityRuleConverter implements Converter<SecurityRuleRequest, SecurityRule> {

    private static final String PORT_REGEX = "^([1-9][0-9]{0,3}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])"
            + "(-([1-9][0-9]{0,3}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5]))?$";

    private static final int MIN_RANGE = 1;

    private static final int MAX_RANGE = 65535;

    @Override
    public SecurityRule convert(SecurityRuleRequest source) {
        SecurityRule entity = new SecurityRule();
        entity.setCidr(source.getSubnet());
        List<String> ports = source.getPorts();
        validatePorts(ports);
        entity.setPorts(String.join(",", ports));
        entity.setProtocol(source.getProtocol());
        entity.setModifiable(source.isModifiable() == null ? false : source.isModifiable());
        return entity;
    }

    private void validatePorts(List<String> ports) {
        if (!ports.stream().allMatch(port -> Pattern.matches(PORT_REGEX, port))) {
            throw new BadRequestException(String.format("Ports must be in range of %d-%d", MIN_RANGE, MAX_RANGE));
        }
    }

}
