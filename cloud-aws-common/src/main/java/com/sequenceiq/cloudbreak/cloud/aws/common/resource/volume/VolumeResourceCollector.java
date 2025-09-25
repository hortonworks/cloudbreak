package com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume;

import static com.sequenceiq.cloudbreak.util.NullUtil.throwIfNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.common.api.type.ResourceType;

@Component
public class VolumeResourceCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(VolumeResourceCollector.class);

    public Pair<List<String>, List<CloudResource>> getVolumeIdsByVolumeResources(Iterable<CloudResource> resources, ResourceType resourceType,
            Function<CloudResource, VolumeSetAttributes> setVolumeAttribute) {
        throwIfNull(resources, () -> new IllegalArgumentException("The collection of " + CloudResource.class.getSimpleName() + " should not be null!"));
        throwIfNull(setVolumeAttribute, () -> new IllegalArgumentException(String.format("%s function for %s should not be null!",
                VolumeSetAttributes.class.getSimpleName(), CloudResource.class.getSimpleName())));
        List<CloudResource> volumeResources = collectCloudResourcesBasedOnType(resources, resourceType);
        List<String> volumeIds = volumeResources
                .stream()
                .map(setVolumeAttribute::apply)
                .map(VolumeSetAttributes::getVolumes)
                .flatMap(List::stream)
                .map(VolumeSetAttributes.Volume::getId)
                .filter(Objects::nonNull)
                .collect(toList());
        return Pair.of(volumeIds, volumeResources);
    }

    private List<CloudResource> collectCloudResourcesBasedOnType(Iterable<CloudResource> resources, ResourceType resourceType) {
        List<CloudResource> cloudResources = stream(resources.spliterator(), false)
                .filter(r -> r.getType().equals(resourceType))
                .collect(toList());
        LOGGER.debug("The following cloud resource(s) has been collected based on the requested type ({}): [{}]",
                resourceType != null ? resourceType.name() : "null",
                cloudResources.stream().map(CloudResource::toString).collect(Collectors.joining(",")));
        return cloudResources;
    }

}
