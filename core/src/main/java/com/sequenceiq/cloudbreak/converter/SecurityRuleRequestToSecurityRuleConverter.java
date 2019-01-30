package com.sequenceiq.cloudbreak.converter;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.SecurityRuleRequest;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.SecurityRule;

@Component
public class SecurityRuleRequestToSecurityRuleConverter extends AbstractConversionServiceAwareConverter<SecurityRuleRequest, SecurityRule> {

    private static final String PORT_REGEX = "^([1-9][0-9]{0,3}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])"
            + "(-([1-9][0-9]{0,3}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5]))?$";

    private static final int MIN_RANGE = 1;

    private static final int MAX_RANGE = 65535;

    @Override
    public SecurityRule convert(SecurityRuleRequest json) {
        SecurityRule entity = new SecurityRule();
        entity.setCidr(json.getSubnet());
        String ports = json.getPorts();
        validatePorts(ports, json.getProtocol());
        entity.setPorts(ports);
        entity.setProtocol(json.getProtocol());
        entity.setModifiable(json.isModifiable() == null ? false : json.isModifiable());
        return entity;
    }

    private void validatePorts(String ports, String protocol) {
        if (!SecurityRule.ICMP.equalsIgnoreCase(protocol)) {
            for (String portString : ports.split(",")) {
                if (!Pattern.matches(PORT_REGEX, portString)) {
                    throw new BadRequestException(String.format("Ports must be in range of %d-%d", MIN_RANGE, MAX_RANGE));
                }
            }
        }
    }

}
