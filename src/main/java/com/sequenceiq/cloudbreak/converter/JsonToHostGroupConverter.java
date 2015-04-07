package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.HostGroupJson;
import com.sequenceiq.cloudbreak.domain.HostGroup;

@Component
public class JsonToHostGroupConverter extends AbstractConversionServiceAwareConverter<HostGroupJson, HostGroup> {
    @Override
    public HostGroup convert(HostGroupJson source) {
        HostGroup hostGroup = new HostGroup();
        hostGroup.setName(source.getName());
        return hostGroup;
    }
}
