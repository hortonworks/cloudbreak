package com.sequenceiq.periscope.utils;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceMetaDataJson;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceStatus;

@Service
public class StackResponseUtils {

    public Optional<InstanceMetaDataJson> getNotTerminatedPrimaryGateways(StackResponse stackResponse) {
        return stackResponse.getInstanceGroups().stream().flatMap(ig -> ig.getMetadata().stream()).filter(
                im -> im.getInstanceType() == InstanceMetadataType.GATEWAY_PRIMARY
                        && im.getInstanceStatus() != InstanceStatus.TERMINATED
        ).findFirst();
    }
}
