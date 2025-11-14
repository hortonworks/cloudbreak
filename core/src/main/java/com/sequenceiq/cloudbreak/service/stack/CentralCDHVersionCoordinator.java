package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.common.type.ComponentType.cdhClusterComponentName;
import static com.sequenceiq.cloudbreak.common.type.ComponentType.cdhProductDetails;
import static org.apache.commons.lang3.StringUtils.substringBefore;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.component.StackType;
import com.sequenceiq.cloudbreak.cluster.service.ClouderaManagerProductsProvider;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.view.ClusterComponentView;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;

@Service
public class CentralCDHVersionCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CentralCDHVersionCoordinator.class);

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private ClouderaManagerProductsProvider clouderaManagerProductsProvider;

    public Component createFromClouderaManagerProduct(ClouderaManagerProduct clouderaManagerProduct, Stack stack) {
        if (StackType.CDH.name().equals(clouderaManagerProduct.getName())) {
            return new Component(cdhProductDetails(), cdhProductDetails().name(), new Json(clouderaManagerProduct), stack);
        }
        return new Component(cdhProductDetails(), clouderaManagerProduct.getName(), new Json(clouderaManagerProduct), stack);
    }

    public Set<ClouderaManagerProduct> getClouderaManagerProductsFromComponents(Set<ClusterComponentView> components) {
        return components.stream()
                .filter(clusterComponent -> isCdhProductDetails(clusterComponent))
                .map(clouderaManagerProductsProvider::getClouderaManagerProduct)
                .collect(Collectors.toSet());
    }

    public Set<ClusterComponentView> getClouderaManagerProductsFromComponents(Long clusterId) {
        return clusterComponentConfigProvider.getComponentListByType(clusterId, cdhProductDetails());
    }

    public boolean requestedComponentTypesContainsCdhComponentType(EnumSet<ComponentType> requestedComponents) {
        return requestedComponents.contains(cdhProductDetails());
    }

    public boolean isCdhProductDetails(ClusterComponent clusterComponent) {
        return isCdhProductDetails(clusterComponent.getComponentType());
    }

    public boolean isCdhProductDetails(ClusterComponentView componentView) {
        return isCdhProductDetails(componentView.getComponentType());
    }

    public boolean isCdhProductDetails(Component component) {
        return isCdhProductDetails(component.getComponentType());
    }

    public boolean isCdhProductDetails(ComponentType componentType) {
        return cdhProductDetails().equals(componentType);
    }

    public List<ClusterComponent> convertClouderaManagerProductsToClusterComponents(Cluster cluster, Set<ClouderaManagerProduct> filteredProducts) {
        return filteredProducts.stream()
                .map(product -> new ClusterComponent(cdhProductDetails(), product.getName(), new Json(product), cluster))
                .collect(Collectors.toList());
    }

    public String calculateStackVersionFromComponents(Collection<Component> components) {
        ClouderaManagerProduct runtime = componentConfigProviderService.getComponent(components, ClouderaManagerProduct.class, cdhProductDetails());
        return getRuntimeVersionFromClouderaManagerProduct(runtime);
    }

    public String calculateStackVersionFromClusterComponents(Set<ClusterComponent> components) {
        ClouderaManagerProduct runtime = clusterComponentConfigProvider.getComponent(
                components,
                ClouderaManagerProduct.class,
                cdhProductDetails(),
                cdhClusterComponentName()
        );
        return getRuntimeVersionFromClouderaManagerProduct(runtime);
    }

    private String getRuntimeVersionFromClouderaManagerProduct(ClouderaManagerProduct runtime) {
        if (Objects.nonNull(runtime)) {
            String stackVersion = substringBefore(runtime.getVersion(), "-");
            LOGGER.debug("Setting runtime version {} for stack", stackVersion);
            return stackVersion;
        } else {
            LOGGER.warn("Product component is not present amongst components, runtime could not be set! This is normal in case of base images");
            return null;
        }
    }
}
