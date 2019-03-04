package com.sequenceiq.periscope.utils;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;

@Service
public class StackResponseUtils {

    public Optional<InstanceMetaDataV4Response> getNotTerminatedPrimaryGateways(StackV4Response stackResponse) {
        return stackResponse.getInstanceGroups().stream().flatMap(ig -> ig.getMetadata().stream()).filter(
                im -> im.getInstanceType() == InstanceMetadataType.GATEWAY_PRIMARY
                        && im.getInstanceStatus() != InstanceStatus.TERMINATED
        ).findFirst();
    }
}
