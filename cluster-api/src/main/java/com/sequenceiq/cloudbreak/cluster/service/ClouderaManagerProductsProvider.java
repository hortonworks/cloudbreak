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
import com.sequenceiq.cloudbreak.domain.view.ClusterComponentView;

@Component
public class ClouderaManagerProductsProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerProductsProvider.class);

    public Optional<ClouderaManagerProduct> findCdhProduct(Set<ClusterComponentView> components) {
        return components.stream()
                .filter(clusterComponent -> clusterComponent.getName().equals(StackType.CDH.name()))
                .map(this::getClouderaManagerProduct)
                .findFirst();
    }

    public Optional<ClouderaManagerProduct> getCdhProduct(Set<ClouderaManagerProduct> products) {
        return products.stream()
                .filter(service -> service.getName().equals(StackType.CDH.name()))
                .peek(clouderaManagerProduct -> LOGGER.debug("The following CDH parcels are available: {}", clouderaManagerProduct))
                .findFirst();
    }

    public Set<ClouderaManagerProduct> getNonCdhProducts(Set<ClouderaManagerProduct> products) {
        return products.stream()
                .filter(product -> !product.getName().equals(StackType.CDH.name()))
                .collect(Collectors.toSet());
    }

    public ClouderaManagerProduct getClouderaManagerProduct(ClusterComponentView clusterComponent) {
        try {
            return clusterComponent.getAttributes().get(ClouderaManagerProduct.class);
        } catch (IOException e) {
            LOGGER.error("Could not convert component {} into a ClouderaManagerProduct", clusterComponent.getName(), e);
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }
}
