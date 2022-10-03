package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response;

import java.util.List;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralCollectionV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;

public class StackInstancesV4Responses extends GeneralCollectionV4Response<InstanceMetaDataV4Response> {

    public StackInstancesV4Responses(Set<InstanceMetaDataV4Response> responses) {
        super(responses);
    }

    public StackInstancesV4Responses() {
        super(List.of());
    }
}
