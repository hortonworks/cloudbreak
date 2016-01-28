package com.sequenceiq.cloudbreak.converter;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.google.api.client.util.Lists;
import com.sequenceiq.cloudbreak.api.model.TopologyRequest;
import com.sequenceiq.cloudbreak.domain.Topology;
import com.sequenceiq.cloudbreak.domain.TopologyRecord;

@Component
public class JsonToTopologyConverter extends AbstractConversionServiceAwareConverter<TopologyRequest, Topology> {
    @Override
    public Topology convert(TopologyRequest source) {
        Topology result = new Topology();
        result.setId(source.getId());
        result.setName(source.getName());
        result.setDescription(source.getDescription());
        result.setCloudPlatform(source.getCloudPlatform());
        result.setEndpoint(source.getEndpoint());
        result.setRecords(convertNodes(source.getNodes()));
        return result;
    }

    private List<TopologyRecord> convertNodes(Map<String, String> nodes) {
        List<TopologyRecord> result = Lists.newArrayList();
        if (nodes != null) {
            for (Map.Entry<String, String> node : nodes.entrySet()) {
                result.add(new TopologyRecord(node.getKey(), node.getValue()));
            }
        }
        return result;
    }
}
