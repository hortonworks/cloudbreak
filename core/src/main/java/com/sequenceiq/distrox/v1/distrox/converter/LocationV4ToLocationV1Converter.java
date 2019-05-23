package com.sequenceiq.distrox.v1.distrox.converter;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.location.StorageLocationV4Request;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.storage.location.StorageLocationV1Request;

@Component
public class LocationV4ToLocationV1Converter {

    public Set<StorageLocationV4Request> convert(Set<StorageLocationV1Request> source) {
        return source.stream().map(this::convert).collect(Collectors.toSet());
    }

    public StorageLocationV4Request convert(StorageLocationV1Request source) {
        StorageLocationV4Request response = new StorageLocationV4Request();
        response.setPropertyFile(source.getPropertyFile());
        response.setPropertyName(source.getPropertyName());
        response.setValue(source.getValue());
        return response;
    }
}
