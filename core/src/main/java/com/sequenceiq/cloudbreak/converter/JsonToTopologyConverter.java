package com.sequenceiq.cloudbreak.converter;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
        result.setName(source.getName());
        result.setDescription(source.getDescription());
        result.setCloudPlatform(source.getCloudPlatform());
        result.setRecords(convertNodes(source.getNodes()));
        return result;
    }

    private List<TopologyRecord> convertNodes(Map<String, String> nodes) {
        List<TopologyRecord> result = Lists.newArrayList();
        if (nodes != null) {
            for (Entry<String, String> node : nodes.entrySet()) {
                result.add(new TopologyRecord(node.getKey(), node.getValue()));
            }
        }
        return result;
    }
}
