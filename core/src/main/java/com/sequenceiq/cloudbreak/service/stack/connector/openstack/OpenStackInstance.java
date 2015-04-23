package com.sequenceiq.cloudbreak.service.stack.connector.openstack;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.domain.InstanceGroupType;

public final class OpenStackInstance {

    private final String flavor;
    private final List<OpenStackVolume> volumes;
    private final Map<String, String> metadata;
    private InstanceGroupType type;

    public OpenStackInstance(String flavor, List<OpenStackVolume> volumes, Map<String, String> metadata, InstanceGroupType type) {
        this.flavor = flavor;
        this.volumes = volumes;
        this.metadata = metadata;
        this.type = type;
    }

    public String getFlavor() {
        return flavor;
    }

    public List<OpenStackVolume> getVolumes() {
        return volumes;
    }

    public Map<String, String> getMetadataAsMap() {
        return metadata;
    }

    public InstanceGroupType getType() {
        return type;
    }

    public String getMetadata() {
        try {
            return new ObjectMapper().writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            return metadata.toString();
        }
    }

}
