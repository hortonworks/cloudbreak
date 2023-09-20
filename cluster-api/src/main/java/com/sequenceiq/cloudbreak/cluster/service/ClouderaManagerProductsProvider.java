package com.sequenceiq.cloudbreak.cluster.service;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.component.StackType;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.view.ClusterComponentView;

@Component
public class ClouderaManagerProductsProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerProductsProvider.class);

    public Set<ClouderaManagerProduct> getProducts(Set<ClusterComponentView> components) {
        return components.stream()
                .filter(clusterComponent -> ComponentType.CDH_PRODUCT_DETAILS.equals(clusterComponent.getComponentType()))
                .map(this::getClouderaManagerProduct)
                .collect(Collectors.toSet());
    }

    public Optional<ClouderaManagerProduct> findCdhProduct(Set<ClusterComponentView> components) {
        return components.stream()
                .filter(clusterComponent -> clusterComponent.getName().equals(StackType.CDH.name()))
                .map(this::getClouderaManagerProduct)
                .findFirst();
    }

    public ClouderaManagerProduct getCdhProducts(Set<ClouderaManagerProduct> products) {
        return products.stream()
                .filter(service -> service.getName().equals(StackType.CDH.name()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Runtime component not found!"));
    }

    private ClouderaManagerProduct getClouderaManagerProduct(ClusterComponentView clusterComponent) {
        try {
            return clusterComponent.getAttributes().get(ClouderaManagerProduct.class);
        } catch (IOException e) {
            LOGGER.error("Could not convert component {} into a ClouderaManagerProduct", clusterComponent.getName(), e);
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }
}
