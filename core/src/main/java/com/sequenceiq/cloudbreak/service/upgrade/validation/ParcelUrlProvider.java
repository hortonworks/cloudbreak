package com.sequenceiq.cloudbreak.service.upgrade.validation;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cluster.model.ParcelInfo;
import com.sequenceiq.cloudbreak.cluster.model.ParcelStatus;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.parcel.ParcelService;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.sync.component.CmServerQueryService;

@Component
public class ParcelUrlProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParcelUrlProvider.class);

    @Inject
    private ParcelService parcelService;

    @Inject
    private CmServerQueryService cmServerQueryService;

    public Set<String> getRequiredParcelsFromImage(ClusterUpgradeProperties clusterUpgradeProperties, StackDto stackDto) {
        LOGGER.debug("Retrieving parcel URLs from image {}", clusterUpgradeProperties.getTargetImageId());
        Set<String> requiredParcelNamesFromImage = parcelService.getComponentNamesByProducts(stackDto, clusterUpgradeProperties.getAllTargetProducts());
        Set<String> requiredParcelUrls = getRequiredParcelUrls(clusterUpgradeProperties, stackDto, requiredParcelNamesFromImage);
        LOGGER.debug("Required parcel URLs: {}", requiredParcelUrls);
        return requiredParcelUrls;
    }

    private Set<String> getRequiredParcelUrls(ClusterUpgradeProperties clusterUpgradeProperties, StackDto stackDto, Set<String> requiredParcelNamesFromImage) {
        Set<ParcelInfo> activeAndDistributedParcels = getActiveAndDistributedParcels(stackDto);
        return clusterUpgradeProperties.getAllTargetProducts().stream()
                .filter(product -> isRequiredProduct(requiredParcelNamesFromImage, product) && isNotActiveProduct(activeAndDistributedParcels, product))
                .map(this::getParcelAndCsdUrlsFromProduct)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    private Set<ParcelInfo> getActiveAndDistributedParcels(StackDto stackDto) {
        return cmServerQueryService.queryAllParcels(stackDto).stream()
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
}
