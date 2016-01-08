package com.sequenceiq.cloudbreak.converter;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.google.api.client.util.Maps;
import com.sequenceiq.cloudbreak.controller.json.TopologyResponse;
import com.sequenceiq.cloudbreak.domain.Topology;
import com.sequenceiq.cloudbreak.domain.TopologyRecord;

@Component
public class TopologyToJsonConverter extends AbstractConversionServiceAwareConverter<Topology, TopologyResponse> {

    @Override
    public TopologyResponse convert(Topology source) {
        TopologyResponse json = new TopologyResponse();
        json.setId(source.getId());
        json.setName(source.getName());
        json.setDescription(source.getDescription());
        json.setCloudPlatform(source.getCloudPlatform());
        json.setEndpoint(source.getEndpoint());
        json.setNodes(convertNodes(source.getRecords()));
        return json;
    }

    private Map<String, String> convertNodes(List<TopologyRecord> records) {
        Map<String, String> result = Maps.newHashMap();
        for (TopologyRecord record : records) {
            result.put(record.getHypervisor(), record.getRack());
        }
        return result;
    }
}
