package com.sequenceiq.cloudbreak.common.imdupdate;

import static com.google.common.base.Predicates.notNull;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.MapUtils;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

public record InstanceMetadataUpdateTypeProperty(String supportedPlatforms, Map<CloudPlatform, InstanceMetadataUpdateTypeMetadata> metadata) {

    @Override
    public Map<CloudPlatform, InstanceMetadataUpdateTypeMetadata> metadata() {
        return MapUtils.emptyIfNull(metadata);
    }

    public Set<CloudPlatform> getSupportedPlatformsSet() {
        return Arrays.stream(supportedPlatforms.split(","))
                .map(CloudPlatform::valueOf)
                .filter(notNull())
                .collect(Collectors.toSet());
    }
}
