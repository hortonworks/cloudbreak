package com.sequenceiq.cloudbreak.service.parcel;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.component.StackType;
import com.sequenceiq.cloudbreak.cluster.model.ParcelOperationStatus;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterComponentUpdater;

@Service
public class ParcelService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParcelService.class);

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private ClouderaManagerProductTransformer clouderaManagerProductTransformer;

    @Inject
    private ParcelFilterService parcelFilterService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private ClusterComponentUpdater clusterComponentUpdater;

    public Set<ClusterComponent> getParcelComponentsByBlueprint(Stack stack) {
        Set<ClusterComponent> components = getComponents(stack);
        LOGGER.debug("The following components are available in the cluster {}", components);
        if (stack.isDatalake()) {
            return getDataLakeClusterComponents(components);
        } else {
            Map<String, ClusterComponent> cmProductMap = new HashMap<>();
            Set<ClouderaManagerProduct> cmProducts = new HashSet<>();
            for (ClusterComponent clusterComponent : components) {
                ClouderaManagerProduct product = clusterComponent.getAttributes().getSilent(ClouderaManagerProduct.class);
                cmProductMap.put(product.getName(), clusterComponent);
                cmProducts.add(product);
            }
            cmProducts = filterParcelsByBlueprint(cmProducts, stack.getCluster().getBlueprint());
            Set<ClusterComponent> componentsByRequiredProducts = getComponentsByRequiredProducts(cmProductMap, cmProducts);
            LOGGER.debug("The following components are required for cluster {}", componentsByRequiredProducts);
            return componentsByRequiredProducts;
        }
    }

    public Set<ClusterComponent> getComponentsByImage(Stack stack, Image image) {
        Set<ClusterComponent> components = getComponents(stack);
        if (stack.isDatalake()) {
            return getDataLakeClusterComponents(components);
        } else {
            Map<String, ClusterComponent> cmProductMap = collectClusterComponentsByName(components);
            Set<ClouderaManagerProduct> cmProducts = clouderaManagerProductTransformer
                    .transform(image, true, true);
            cmProducts = filterParcelsByBlueprint(cmProducts, stack.getCluster().getBlueprint());
            LOGGER.debug("The following parcels are used in CM based on blueprint: {}", cmProducts);
            return getComponentsByRequiredProducts(cmProductMap, cmProducts);
        }
    }

    public ParcelOperationStatus removeUnusedParcelComponents(Stack stack) throws CloudbreakException {
        Set<ClusterComponent> clusterComponentsByBlueprint = getParcelComponentsByBlueprint(stack);
        return removeUnusedParcelComponents(stack, clusterComponentsByBlueprint);
    }

    public ParcelOperationStatus removeUnusedParcelComponents(Stack stack, Set<ClusterComponent> clusterComponentsByBlueprint) throws CloudbreakException {
        LOGGER.debug("Starting to remove unused parcels from the cluster.");
        ParcelOperationStatus removalStatus = clusterApiConnectors.getConnector(stack).removeUnusedParcels(clusterComponentsByBlueprint);
        clusterComponentUpdater.removeUnusedCdhProductsFromClusterComponents(stack.getCluster().getId(), clusterComponentsByBlueprint, removalStatus);
        return removalStatus;
    }

    private Map<String, ClusterComponent> collectClusterComponentsByName(Set<ClusterComponent> components) {
        return components.stream().collect(Collectors.toMap(ClusterComponent::getName, component -> component));
    }

    private Set<ClusterComponent> getComponents(Stack stack) {
        return clusterComponentConfigProvider.getComponentsByClusterId(stack.getCluster().getId()).stream()
                .filter(clusterComponent -> ComponentType.CDH_PRODUCT_DETAILS == clusterComponent.getComponentType())
                .collect(Collectors.toSet());
    }

    private Set<ClouderaManagerProduct> filterParcelsByBlueprint(Set<ClouderaManagerProduct> cmProducts, Blueprint blueprint) {
        return parcelFilterService.filterParcelsByBlueprint(cmProducts, blueprint);
    }

    private Set<ClusterComponent> getDataLakeClusterComponents(Set<ClusterComponent> components) {
        ClusterComponent stackComponent = getCdhComponent(components);
        LOGGER.debug("For datalake clusters only the CDH parcel is used in CM: {}", stackComponent);
        return Collections.singleton(stackComponent);
    }

    private ClusterComponent getCdhComponent(Set<ClusterComponent> components) {
        return components.stream()
                .filter(clusterComponent -> clusterComponent.getName().equals(StackType.CDH.name()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Runtime component not found!"));
    }

    private Set<ClusterComponent> getComponentsByRequiredProducts(Map<String, ClusterComponent> cmProductMap, Set<ClouderaManagerProduct> cmProducts) {
        return cmProducts.stream()
                .map(cmp -> cmProductMap.get(cmp.getName()))
                .collect(Collectors.toSet());
    }
}
