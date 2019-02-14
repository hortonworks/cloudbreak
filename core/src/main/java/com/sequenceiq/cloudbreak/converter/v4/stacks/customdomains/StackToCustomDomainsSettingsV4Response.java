package com.sequenceiq.cloudbreak.converter.v4.stacks.customdomains;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.customdomain.CustomDomainSettingsV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@Component
public class StackToCustomDomainsSettingsV4Response extends AbstractConversionServiceAwareConverter<Stack, CustomDomainSettingsV4Response> {
    @Override
    public CustomDomainSettingsV4Response convert(Stack source) {
        CustomDomainSettingsV4Response response = new CustomDomainSettingsV4Response();
        response.setDomainName(source.getCustomDomain());
        response.setHostname(source.getCustomHostname());
        response.setClusterNameAsSubdomain(source.isClusterNameAsSubdomain());
        response.setHostgroupNameAsHostname(source.isHostgroupNameAsHostname());
        return response;
    }
}
