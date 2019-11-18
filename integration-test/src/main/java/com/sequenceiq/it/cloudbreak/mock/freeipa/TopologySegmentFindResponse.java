package com.sequenceiq.it.cloudbreak.mock.freeipa;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.client.model.TopologySegment;

import spark.Request;
import spark.Response;

@Component
public class TopologySegmentFindResponse extends AbstractFreeIpaResponse<Set<TopologySegment>> {
    @Override
    public String method() {
        return "topologysegment_find";
    }

    @Override
    protected Set<TopologySegment> handleInternal(Request request, Response response) {
        return Set.of();
    }
}
