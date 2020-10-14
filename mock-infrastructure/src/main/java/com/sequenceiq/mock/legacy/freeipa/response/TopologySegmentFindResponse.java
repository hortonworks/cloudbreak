package com.sequenceiq.mock.legacy.freeipa.response;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.client.model.TopologySegment;

@Component
public class TopologySegmentFindResponse extends AbstractFreeIpaResponse<Set<TopologySegment>> {
    @Override
    public String method() {
        return "topologysegment_find";
    }

    @Override
    protected Set<TopologySegment> handleInternal(String body) {
        return Set.of();
    }
}
