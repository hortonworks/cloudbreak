package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.HostGroupRequest;
import com.sequenceiq.cloudbreak.domain.HostGroup;

@Component
public class JsonToHostGroupConverter extends AbstractConversionServiceAwareConverter<HostGroupRequest, HostGroup> {
    @Override
    public HostGroup convert(HostGroupRequest source) {
        HostGroup hostGroup = new HostGroup();
        hostGroup.setName(source.getName());
        return hostGroup;
    }
}
