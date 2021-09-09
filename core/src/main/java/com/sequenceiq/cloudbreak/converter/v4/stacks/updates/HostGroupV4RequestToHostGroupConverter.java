package com.sequenceiq.cloudbreak.converter.v4.stacks.updates;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupV4Request;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;

@Component
public class HostGroupV4RequestToHostGroupConverter {

    public HostGroup convert(HostGroupV4Request source) {
        HostGroup hostGroup = new HostGroup();
        hostGroup.setName(source.getName().toLowerCase());
        hostGroup.setRecoveryMode(source.getRecoveryMode());
        return hostGroup;
    }
}
