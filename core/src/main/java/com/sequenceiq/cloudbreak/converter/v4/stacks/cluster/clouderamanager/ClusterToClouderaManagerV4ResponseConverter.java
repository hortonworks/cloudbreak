package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.clouderamanager;

import java.io.IOException;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerV4Response;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cli.cm.ClouderaManagerProductToClouderaManagerProductV4Response;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cli.cm.ClouderaManagerRepoToClouderaManagerRepositoryV4Response;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;

public final class ClusterToClouderaManagerV4ResponseConverter {

    private ClusterToClouderaManagerV4ResponseConverter() {
    }

    public static ClouderaManagerV4Response convert(Cluster cluster) {
        Predicate<ClusterComponent> cmRepoFilter = component -> ComponentType.CM_REPO_DETAILS.equals(component.getComponentType());

        Set<ClusterComponent> components = cluster.getComponents();
        if (components.stream().noneMatch(cmRepoFilter)) {
            return null;
        }

        return new ClouderaManagerV4Response()
                .withRepository(components.stream()
                        .filter(cmRepoFilter)
                        .map(ClusterComponent::getAttributes)
                        .map(toAttributeClass(ClouderaManagerRepo.class))
                        .map(ClouderaManagerRepoToClouderaManagerRepositoryV4Response::convert)
                        .findFirst()
                        .orElse(null))
                .withProducts(components.stream()
                        .filter(component -> ComponentType.CDH_PRODUCT_DETAILS.equals(component.getComponentType()))
                        .map(ClusterComponent::getAttributes)
                        .map(toAttributeClass(ClouderaManagerProduct.class))
                        .map(ClouderaManagerProductToClouderaManagerProductV4Response::convert)
                        .collect(Collectors.toList()));
    }

    private static <T> Function<Json, T> toAttributeClass(Class<T> attributeClass) {
        return attribute -> {
            try {
                return attribute.get(attributeClass);
            } catch (IOException e) {
                throw new BadRequestException("Cannot deserialize the compnent: " + attributeClass, e);
            }
        };
    }
}

