package com.sequenceiq.mock.freeipa.response;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.freeipa.client.model.TopologySegment;

@Component
public class TopologySegmentAddResponse extends AbstractFreeIpaResponse<TopologySegment> {
    @Override
    public String method() {
        return "topologysegment_add";
    }

    @Override
    protected TopologySegment handleInternal(List<CloudVmMetaDataStatus> metadatas, String body) {
        return new TopologySegment();
    }
}
