package com.sequenceiq.cloudbreak.service.upgrade.validation;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cluster.model.ParcelInfo;
import com.sequenceiq.cloudbreak.cluster.model.ParcelStatus;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.parcel.ClouderaManagerProductTransformer;
import com.sequenceiq.cloudbreak.service.parcel.ParcelService;
import com.sequenceiq.cloudbreak.service.upgrade.sync.component.CmServerQueryService;

@Component
public class ParcelUrlProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParcelUrlProvider.class);

    @Inject
    private ParcelService parcelService;

    @Inject
    private CmServerQueryService cmServerQueryService;

    @Inject
    private ClouderaManagerProductTransformer clouderaManagerProductTransformer;

    public Set<String> getRequiredParcelsFromImage(Image image, Stack stack) {
        LOGGER.debug("Retrieving parcel URLs from image {}", image.getUuid());
        Set<String> requiredParcelNamesFromImage = parcelService.getComponentNamesByImage(stack, image);
        Set<ParcelInfo> activeAndDistributedParcels = getActiveAndDistributedParcels(stack);
        Set<String> requiredParcelUrls = getRequiredParcelUrls(image, stack, requiredParcelNamesFromImage, activeAndDistributedParcels);
        LOGGER.debug("Required parcel URLs: {}", requiredParcelUrls);
        return requiredParcelUrls;
    }

    private Set<String> getRequiredParcelUrls(Image image, Stack stack, Set<String> requiredParcelNamesFromImage, Set<ParcelInfo> activeAndDistributedParcels) {
        return transformProducts(image, !stack.isDatalake()).stream()
                .filter(product -> isRequiredProduct(requiredParcelNamesFromImage, product) && isNotActiveProduct(activeAndDistributedParcels, product))
                .map(this::getParcelAndCsdUrlsFromProduct)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    private Set<ParcelInfo> getActiveAndDistributedParcels(Stack stack) {
        return cmServerQueryService.queryAllParcels(stack).stream()
                .filter(parcelInfo -> ParcelStatus.DISTRIBUTED.equals(parcelInfo.getStatus()) || ParcelStatus.ACTIVATED.equals(parcelInfo.getStatus()))
                .collect(Collectors.toSet());
    }

    private boolean isRequiredProduct(Set<String> requiredParcelNamesFromImage, ClouderaManagerProduct product) {
        return requiredParcelNamesFromImage.contains(product.getName());
    }

    private boolean isNotActiveProduct(Set<ParcelInfo> activeParcels, ClouderaManagerProduct product) {
        return activeParcels.stream()
                .noneMatch(activeParcel -> product.getName().equals(activeParcel.getName()) &&
                        product.getVersion().equals(activeParcel.getVersion()));
    }

    private Set<String> getParcelAndCsdUrlsFromProduct(ClouderaManagerProduct product) {
        Set<String> parcelAndCsdUrls = new HashSet<>();
        Optional.of(product).map(ClouderaManagerProduct::getCsd).ifPresent(parcelAndCsdUrls::addAll);
        parcelAndCsdUrls.add(product.getParcelFileUrl());
        return parcelAndCsdUrls;
    }

    private Set<ClouderaManagerProduct> transformProducts(Image image, boolean getPreWarmParcels) {
        return clouderaManagerProductTransformer.transform(image, true, getPreWarmParcels);
    }
}
