package com.sequenceiq.cloudbreak.converter.v4.stacks.cli.cm;

import java.io.IOException;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.service.stack.CentralCDHVersionCoordinator;

@Component
public class ClusterToClouderaManagerV4RequestConverter {

    @Inject
    private CentralCDHVersionCoordinator centralCDHVersionCoordinator;

    public ClouderaManagerV4Request convert(Cluster cluster) {
        Predicate<ClusterComponent> cmRepoFilter = component -> ComponentType.CM_REPO_DETAILS.equals(component.getComponentType());

        Set<ClusterComponent> components = cluster.getComponents();
        if (components.stream().noneMatch(cmRepoFilter)) {
            return null;
        }

        return new ClouderaManagerV4Request()
                .withRepository(components.stream()
                        .filter(cmRepoFilter)
                        .map(ClusterComponent::getAttributes)
                        .map(toAttributeClass(ClouderaManagerRepo.class))
                        .map(ClouderaManagerRepoToClouderaManagerRepositoryV4Request::convert)
                        .findFirst()
                        .orElse(null))
                .withProducts(components.stream()
                        .filter(component -> centralCDHVersionCoordinator.isCdhProductDetails(component))
                        .map(ClusterComponent::getAttributes)
                        .map(toAttributeClass(ClouderaManagerProduct.class))
                        .map(ClouderaManagerProductToClouderaManagerProductV4Request::convert)
                        .collect(Collectors.toList()))
                .withEnableAutoTls(cluster.isAutoTlsEnabled());
    }

    private <T> Function<Json, T> toAttributeClass(Class<T> attributeClass) {
        return attribute -> {
            try {
                return attribute.get(attributeClass);
            } catch (IOException e) {
                throw new BadRequestException("Cannot deserialize the component: " + attributeClass, e);
            }
        };
    }
}
