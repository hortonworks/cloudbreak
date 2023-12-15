package com.sequenceiq.distrox.v1.distrox.converter;

import static java.util.stream.Collectors.toSet;

import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.RootVolumeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.VolumeV4Request;
import com.sequenceiq.cloudbreak.service.stack.DefaultRootVolumeSizeProvider;
import com.sequenceiq.common.RootVolumeRequest;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.volume.RootVolumeV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.volume.VolumeV1Request;

@Component
public class VolumeConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(VolumeConverter.class);

    @Inject
    private DefaultRootVolumeSizeProvider rootVolumeSizeProvider;

    public RootVolumeV4Request convert(RootVolumeV1Request source, String cloudPlatform) {
        overrideSizeIfNecessary(source, cloudPlatform);
        RootVolumeV4Request response = new RootVolumeV4Request();
        response.setSize(source.getSize());
        return response;
    }

    public RootVolumeV1Request convert(RootVolumeV4Request source, String cloudPlatform) {
        overrideSizeIfNecessary(source, cloudPlatform);
        RootVolumeV1Request response = new RootVolumeV1Request();
        response.setSize(source.getSize());
        return response;
    }

    public VolumeV4Request convert(VolumeV1Request source) {
        VolumeV4Request response = new VolumeV4Request();
        response.setSize(source.getSize());
        response.setCount(source.getCount() == null ? Integer.valueOf(0) : source.getCount());
        response.setType(source.getType());
        return response;
    }

    public Set<VolumeV4Request> convertTo(Set<VolumeV1Request> attachedVolumes) {
        return attachedVolumes.stream().map(this::convert).collect(toSet());
    }

    public VolumeV1Request convert(VolumeV4Request source) {
        VolumeV1Request response = new VolumeV1Request();
        response.setSize(source.getSize());
        response.setCount(source.getCount());
        response.setType(source.getType());
        return response;
    }

    public Set<VolumeV1Request> convertFrom(Set<VolumeV4Request> attachedVolumes) {
        return attachedVolumes.stream().map(this::convert).collect(toSet());
    }

    private void overrideSizeIfNecessary(RootVolumeRequest source, String cloudPlatform) {
        int defaultRootVolumeSize = rootVolumeSizeProvider.getForPlatform(cloudPlatform);
        if (source.getSize() < defaultRootVolumeSize) {
            LOGGER.warn("Root volume size {} is smaller than the minimum {} for platform {}, so it is increased",
                    source.getSize(), defaultRootVolumeSize, cloudPlatform);
            source.setSize(defaultRootVolumeSize);
        }
    }

}
