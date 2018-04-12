package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.cloudbreak.api.model.HostGroupRequest;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import org.springframework.stereotype.Component;

@Component
public class HostGroupRequestToHostGroupConverter extends AbstractConversionServiceAwareConverter<HostGroupRequest, HostGroup> {
    @Override
    public HostGroup convert(HostGroupRequest source) {
        HostGroup hostGroup = new HostGroup();
        hostGroup.setName(source.getName());
        hostGroup.setRecoveryMode(source.getRecoveryMode());
        return hostGroup;
    }
}
