package com.sequenceiq.cloudbreak.service.upgrade;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.InternalUpgradeSettings;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cluster.service.ClouderaManagerProductsProvider;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.service.parcel.ParcelService;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;

@Component
public class ImageFilterParamsFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageFilterParamsFactory.class);

    @Inject
    private ParcelService parcelService;

    @Inject
    private ClouderaManagerProductsProvider clouderaManagerProductsProvider;

    public ImageFilterParams create(Image image, boolean lockComponents, Stack stack, InternalUpgradeSettings internalUpgradeSettings) {
        return new ImageFilterParams(image, lockComponents, getStackRelatedParcels(stack), stack.getType(),
                getBlueprint(stack), stack.getId(), internalUpgradeSettings, stack.cloudPlatform());
    }

    public Map<String, String> getStackRelatedParcels(Stack stack) {
        Set<ClusterComponent> componentsByBlueprint = parcelService.getParcelComponentsByBlueprint(stack);
        if (stack.isDatalake()) {
            ClouderaManagerProduct stackProduct = getCdhProduct(componentsByBlueprint);
            LOGGER.debug("For datalake clusters only the CDH parcel is related in CM: {}", stackProduct);
            return Map.of(stackProduct.getName(), stackProduct.getVersion());
        } else {
            Set<ClouderaManagerProduct> products = clouderaManagerProductsProvider.getProducts(componentsByBlueprint);
            LOGGER.debug("The following parcels are related for this datahub cluster: {}", products);
            return products.stream().collect(Collectors.toMap(ClouderaManagerProduct::getName, ClouderaManagerProduct::getVersion));
        }
    }

    private ClouderaManagerProduct getCdhProduct(Set<ClusterComponent> componentsByBlueprint) {
        return clouderaManagerProductsProvider.findCdhProduct(componentsByBlueprint)
                .orElseThrow(() -> new NotFoundException("Runtime component not found!"));
    }

    private Blueprint getBlueprint(Stack stack) {
        return stack.getCluster().getBlueprint();
    }
}
