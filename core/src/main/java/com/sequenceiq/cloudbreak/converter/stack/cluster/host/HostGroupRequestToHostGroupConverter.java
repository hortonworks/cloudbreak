package com.sequenceiq.cloudbreak.converter.stack.cluster.host;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostGroupRequest;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;

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
