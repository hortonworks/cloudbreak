package com.sequenceiq.distrox.v1.distrox.converter;

import static java.util.stream.Collectors.toSet;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.RootVolumeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.VolumeV4Request;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.volume.RootVolumeV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.volume.VolumeV1Request;

@Component
public class VolumeConverter {

    public RootVolumeV4Request convert(RootVolumeV1Request source) {
        RootVolumeV4Request response = new RootVolumeV4Request();
        response.setSize(source.getSize());
        return response;
    }

    public VolumeV4Request convert(VolumeV1Request source) {
        VolumeV4Request response = new VolumeV4Request();
        response.setSize(source.getSize());
        response.setCount(source.getCount());
        response.setType(source.getType());
        return response;
    }

    public Set<VolumeV4Request> convert(Set<VolumeV1Request> attachedVolumes) {
        return attachedVolumes.stream().map(this::convert).collect(toSet());
    }
}
