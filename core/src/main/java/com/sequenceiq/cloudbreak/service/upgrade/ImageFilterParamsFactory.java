package com.sequenceiq.cloudbreak.service.upgrade;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.InternalUpgradeSettings;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cluster.service.ClouderaManagerProductsProvider;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.PlatformStringTransformer;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.ClusterComponentView;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.parcel.ParcelService;
import com.sequenceiq.cloudbreak.service.stack.CentralCDHVersionCoordinator;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;

@Component
public class ImageFilterParamsFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageFilterParamsFactory.class);

    @Inject
    private ParcelService parcelService;

    @Inject
    private ClouderaManagerProductsProvider clouderaManagerProductsProvider;

    @Inject
    private CentralCDHVersionCoordinator centralCDHVersionCoordinator;

    @Inject
    private PlatformStringTransformer platformStringTransformer;

    public ImageFilterParams create(String targetImageId, Image image, boolean lockComponents, boolean replaceVms, Stack stack,
            InternalUpgradeSettings internalUpgradeSettings, boolean getAllImages) {
        return new ImageFilterParams(targetImageId, image, image.getImageCatalogName(), lockComponents, replaceVms, getStackRelatedParcels(stack),
                stack.getType(), getBlueprint(stack), stack.getId(), internalUpgradeSettings,
                platformStringTransformer.getPlatformStringForImageCatalog(stack.cloudPlatform(), stack.getPlatformVariant()),
                stack.cloudPlatform(), stack.getRegion(), getAllImages);
    }

    public Map<String, String> getStackRelatedParcels(StackDtoDelegate stack) {
        Set<ClusterComponentView> componentsByBlueprint = parcelService.getParcelComponentsByBlueprint(stack);
        if (stack.getStack().isDatalake()) {
            ClouderaManagerProduct stackProduct = getCdhProduct(componentsByBlueprint);
            LOGGER.debug("For datalake clusters only the CDH parcel is related in CM: {}", stackProduct);
            return Map.of(stackProduct.getName(), stackProduct.getVersion());
        } else {
            Set<ClouderaManagerProduct> products = centralCDHVersionCoordinator.getClouderaManagerProductsFromComponents(componentsByBlueprint);
            LOGGER.debug("The following parcels are related for this datahub cluster: {}", products);
            return products.stream().collect(Collectors.toMap(ClouderaManagerProduct::getName, ClouderaManagerProduct::getVersion));
        }
    }

    private ClouderaManagerProduct getCdhProduct(Set<ClusterComponentView> componentsByBlueprint) {
        return clouderaManagerProductsProvider.findCdhProduct(componentsByBlueprint)
                .orElseThrow(() -> new NotFoundException("Runtime component not found!"));
    }

    private Blueprint getBlueprint(Stack stack) {
        return stack.getCluster().getBlueprint();
    }
}
