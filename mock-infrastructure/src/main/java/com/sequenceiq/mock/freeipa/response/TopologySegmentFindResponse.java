package com.sequenceiq.mock.freeipa.response;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.freeipa.client.model.TopologySegment;

@Component
public class TopologySegmentFindResponse extends AbstractFreeIpaResponse<Set<TopologySegment>> {
    @Override
    public String method() {
        return "topologysegment_find";
    }

    @Override
    protected Set<TopologySegment> handleInternal(List<CloudVmMetaDataStatus> metadatas, String body) {
        return Set.of();
    }
}
