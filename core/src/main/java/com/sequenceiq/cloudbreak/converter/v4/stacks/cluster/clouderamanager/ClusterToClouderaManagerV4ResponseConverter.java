package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.clouderamanager;

import java.io.IOException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerV4Response;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cli.cm.ClouderaManagerProductToClouderaManagerProductV4Response;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cli.cm.ClouderaManagerRepoToClouderaManagerRepositoryV4Response;
import com.sequenceiq.cloudbreak.domain.view.ClusterComponentView;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.stack.CentralCDHVersionCoordinator;

@Component
public class ClusterToClouderaManagerV4ResponseConverter {

    @Inject
    private CentralCDHVersionCoordinator centralCDHVersionCoordinator;

    private ClusterToClouderaManagerV4ResponseConverter() {
    }

    public ClouderaManagerV4Response convert(StackDtoDelegate stackDto) {
        Predicate<ClusterComponentView> cmRepoFilter = component -> ComponentType.CM_REPO_DETAILS.equals(component.getComponentType());

        if (stackDto.getClusterComponents().stream().noneMatch(cmRepoFilter)) {
            return null;
        }

        return new ClouderaManagerV4Response()
                .withRepository(stackDto.getClusterComponents().stream()
                        .filter(cmRepoFilter)
                        .map(ClusterComponentView::getAttributes)
                        .map(toAttributeClass(ClouderaManagerRepo.class))
                        .map(ClouderaManagerRepoToClouderaManagerRepositoryV4Response::convert)
                        .findFirst()
                        .orElse(null))
                .withProducts(stackDto.getClusterComponents().stream()
                        .filter(component -> centralCDHVersionCoordinator.isCdhProductDetails(component))
                        .map(ClusterComponentView::getAttributes)
                        .map(toAttributeClass(ClouderaManagerProduct.class))
                        .map(ClouderaManagerProductToClouderaManagerProductV4Response::convert)
                        .collect(Collectors.toList()))
                .withTlsEnabled(stackDto.getCluster().getAutoTlsEnabled());
    }

    private static <T> Function<Json, T> toAttributeClass(Class<T> attributeClass) {
        return attribute -> {
            try {
                return attribute.get(attributeClass);
            } catch (IOException e) {
                throw new BadRequestException("Cannot deserialize the component: " + attributeClass, e);
            }
        };
    }
}

