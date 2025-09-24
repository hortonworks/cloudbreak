package com.sequenceiq.cloudbreak.service.parcel;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.component.StackType;
import com.sequenceiq.cloudbreak.cluster.model.ParcelOperationStatus;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.view.ClusterComponentView;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.CentralCDHVersionCoordinator;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterComponentUpdater;
import com.sequenceiq.cloudbreak.service.upgrade.sync.component.ImageReaderService;
import com.sequenceiq.cloudbreak.view.StackView;

@Service
@EnableRetry
public class ParcelService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParcelService.class);

    @Inject
    private ClouderaManagerProductTransformer clouderaManagerProductTransformer;

    @Inject
    private ParcelFilterService parcelFilterService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private ClusterComponentUpdater clusterComponentUpdater;

    @Inject
    private ImageReaderService imageReaderService;

    @Inject
    private CentralCDHVersionCoordinator centralCDHVersionCoordinator;

    public Set<ClusterComponentView> getParcelComponentsByBlueprint(StackDtoDelegate stack) {
        Set<ClusterComponentView> components = getComponents(stack.getCluster().getId());
        LOGGER.debug("The following components are available in the cluster {}", components);
        if (stack.getStack().isDatalake()) {
            return getDataLakeClusterComponents(components);
        } else {
            Map<String, ClusterComponentView> cmProductMap = new HashMap<>();
            Set<ClouderaManagerProduct> cmProducts = new HashSet<>();
            for (ClusterComponentView clusterComponent : components) {
                ClouderaManagerProduct product = clusterComponent.getAttributes().getUnchecked(ClouderaManagerProduct.class);
                cmProductMap.put(product.getName(), clusterComponent);
                cmProducts.add(product);
            }
            cmProducts = filterParcelsByBlueprint(stack.getWorkspaceId(), stack.getId(), cmProducts, stack.getBlueprint());
            Set<ClusterComponentView> componentsByRequiredProducts = getComponentsByRequiredProducts(cmProductMap, cmProducts);
            LOGGER.debug("The following components are required for cluster {}", componentsByRequiredProducts);
            return componentsByRequiredProducts;
        }
    }

    public Set<String> getComponentNamesByImage(StackDtoDelegate stack, Image image) {
        return getComponentsByImage(stack.getStack(), stack.getCluster().getId(), stack.getBlueprint(), image)
                .stream().map(ClusterComponentView::getName)
                .collect(Collectors.toSet());
    }

    private Set<ClusterComponentView> getComponentsByImage(StackView stack, Long clusterId, Blueprint blueprint, Image image) {
        Set<ClusterComponentView> components = getComponents(clusterId);
        if (stack.isDatalake()) {
            return getDataLakeClusterComponents(components);
        } else {
            Map<String, ClusterComponentView> cmProductMap = collectClusterComponentsByName(components);
            Set<ClouderaManagerProduct> cmProducts = clouderaManagerProductTransformer.transform(image, true, true);
            cmProducts = filterParcelsByBlueprint(stack.getWorkspaceId(), stack.getId(), cmProducts, blueprint);
            LOGGER.debug("The following parcels are used in CM based on blueprint: {}", cmProducts);
            return getComponentsByRequiredProducts(cmProductMap, cmProducts);
        }
    }

    public ParcelOperationStatus removeUnusedParcelComponents(StackDto stackDto) throws CloudbreakException {
        Set<ClusterComponentView> clusterComponentsByBlueprint = getParcelComponentsByBlueprint(stackDto);
        return removeUnusedParcelComponents(stackDto, clusterComponentsByBlueprint);
    }

    public ParcelOperationStatus removeUnusedParcelComponents(StackDtoDelegate stackDto, Set<ClusterComponentView> clusterComponentsByBlueprint)
            throws CloudbreakException {
        LOGGER.debug("Starting to remove unused parcels from the cluster.");
        Set<String> parcelsFromImage = imageReaderService.getParcelNames(stackDto.getStack().getWorkspaceId(), stackDto.getId());
        ParcelOperationStatus removalStatus = clusterApiConnectors.getConnector(stackDto).removeUnusedParcels(clusterComponentsByBlueprint, parcelsFromImage);
        clusterComponentUpdater.removeUnusedCdhProductsFromClusterComponents(stackDto.getCluster().getId(), clusterComponentsByBlueprint, removalStatus);
        return removalStatus;
    }

    public Set<ClouderaManagerProduct> getRequiredProductsFromImage(StackDtoDelegate stackDto, Image image) {
        Set<String> requiredParcelNames = getComponentNamesByImage(stackDto, image);
        return clouderaManagerProductTransformer.transform(image, true, !stackDto.getStack().isDatalake())
                .stream()
                .filter(products -> requiredParcelNames.contains(products.getName()))
                .collect(Collectors.toSet());
    }

    private Map<String, ClusterComponentView> collectClusterComponentsByName(Set<ClusterComponentView> components) {
        return components.stream().collect(Collectors.toMap(ClusterComponentView::getName, component -> component));
    }

    private Set<ClusterComponentView> getComponents(Long clusterId) {
        return centralCDHVersionCoordinator.getClouderaManagerProductsFromComponents(clusterId);
    }

    private Set<ClouderaManagerProduct> filterParcelsByBlueprint(Long workspaceId, Long stackId, Set<ClouderaManagerProduct> cmProducts, Blueprint blueprint) {
        return parcelFilterService.filterParcelsByBlueprint(workspaceId, stackId, cmProducts, blueprint);
    }

    private Set<ClusterComponentView> getDataLakeClusterComponents(Set<ClusterComponentView> components) {
        ClusterComponentView stackComponent = getCdhComponent(components);
        LOGGER.debug("For datalake clusters only the CDH parcel is used in CM: {}", stackComponent);
        return Collections.singleton(stackComponent);
    }

    private ClusterComponentView getCdhComponent(Set<ClusterComponentView> components) {
        return components.stream()
                .filter(clusterComponent -> clusterComponent.getName().equals(StackType.CDH.name()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Runtime component not found!"));
    }

    private Set<ClusterComponentView> getComponentsByRequiredProducts(Map<String, ClusterComponentView> cmProductMap, Set<ClouderaManagerProduct> cmProducts) {
        return cmProducts.stream()
                .map(cmp -> cmProductMap.get(cmp.getName()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
