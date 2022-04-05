package com.sequenceiq.cloudbreak.service.parcel;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateGeneratorService;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain.SupportedService;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.service.upgrade.sync.component.ImageReaderService;

@Service
public class ParcelFilterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParcelFilterService.class);

    @Inject
    private CmTemplateGeneratorService clusterTemplateGeneratorService;

    @Inject
    private ImageReaderService imageReaderService;

    @Inject
    private ManifestRetrieverService manifestRetrieverService;

    public Set<ClouderaManagerProduct> filterParcelsByBlueprint(Long stackId, Set<ClouderaManagerProduct> parcels, Blueprint blueprint) {
        LOGGER.debug("Filtering the following parcels based on the blueprint {}", parcels);
        Set<String> serviceNamesInBlueprint = getServiceNamesInBlueprint(blueprint);
        LOGGER.debug("The following services are found in the blueprint: {}", serviceNamesInBlueprint);
        if (serviceNamesInBlueprint.contains(null)) {
            LOGGER.debug("We can not identify one of the service from the blueprint so to stay on the safe side we will add every parcel");
            return parcels;
        }
        Set<ClouderaManagerProduct> requiredParcels = filterParcels(stackId, parcels, serviceNamesInBlueprint);
        LOGGER.debug("The following parcels are used in CM based on blueprint: {}", requiredParcels);
        return requiredParcels;
    }

    private Set<ClouderaManagerProduct> filterParcels(Long stackId, Set<ClouderaManagerProduct> parcels, Set<String> requiredServicesInBlueprint) {
        Set<ClouderaManagerProduct> requiredParcels = new HashSet<>();
        Set<ClouderaManagerProduct> notAccessibleParcels = new HashSet<>();
        Iterator<ClouderaManagerProduct> parcelIterator = parcels.iterator();
        while (!requiredServicesInBlueprint.isEmpty() && parcelIterator.hasNext()) {
            ClouderaManagerProduct parcel = parcelIterator.next();
            ImmutablePair<ManifestStatus, Manifest> manifest = manifestRetrieverService.readRepoManifest(parcel.getParcel());
            if (manifestAvailable(manifest)) {
                Set<String> servicesInParcel = getAllServiceNameInParcel(manifest.right);
                LOGGER.debug("The {} parcel contains the following services: {}", parcel.getName(), servicesInParcel);
                if (servicesArePresentInTheBlueprint(requiredServicesInBlueprint, servicesInParcel, parcel) || isCustomParcel(stackId, parcel)) {
                    requiredParcels.add(parcel);
                    LOGGER.debug("Removing {} from the remaining required services because these services are found in {} parcel.", servicesInParcel, parcel);
                    requiredServicesInBlueprint.removeAll(servicesInParcel);
                } else {
                    LOGGER.info("Skip parcel '{}' as there isn't any service both in the manifest and in the blueprint.", parcel);
                }
            } else {
                LOGGER.info("Add parcel '{}' as we were unable to check parcel's manifest.", parcel);
                notAccessibleParcels.add(parcel);
            }
        }
        if (!requiredServicesInBlueprint.isEmpty()) {
            LOGGER.debug("Add {} not accessible parcels to the required parcel list because the {} required services are not found in the accessible parcels.",
                    notAccessibleParcels, requiredServicesInBlueprint);
            requiredParcels.addAll(notAccessibleParcels);
        }
        return requiredParcels;
    }

    private boolean manifestAvailable(ImmutablePair<ManifestStatus, Manifest> manifest) {
        return manifest.right != null && ManifestStatus.SUCCESS.equals(manifest.left);
    }

    private boolean servicesArePresentInTheBlueprint(Set<String> serviceNamesInBlueprint, Set<String> componentNamesInParcel, ClouderaManagerProduct parcel) {
        if (componentNamesInParcel.stream().anyMatch(serviceNamesInBlueprint::contains)) {
            LOGGER.debug("Add parcel '{}' as there is at least one service both in the manifest and in the blueprint.", parcel);
            return true;
        } else {
            LOGGER.debug("The following services in the parcel {} are not present in the blueprint {}", componentNamesInParcel, serviceNamesInBlueprint);
            return false;
        }
    }

    private boolean isCustomParcel(Long stackId, ClouderaManagerProduct parcel) {
        Set<String> parcelNamesFromImage = imageReaderService.getParcelNames(stackId, false);
        if (parcelNamesFromImage.stream().noneMatch(preWarmParcel -> preWarmParcel.equals(parcel.getName()))) {
            LOGGER.debug("Add custom parcel {}", parcel);
            return true;
        } else {
            LOGGER.debug("The parcel {} is present on the image. It's not a custom parcel", parcel);
            return false;
        }
    }

    private Set<String> getServiceNamesInBlueprint(Blueprint blueprint) {
        String blueprintText = blueprint.getBlueprintText();
        Set<SupportedService> supportedServices = clusterTemplateGeneratorService.getServicesByBlueprint(blueprintText).getServices();
        return supportedServices.stream()
                .map(SupportedService::getComponentNameInParcel)
                .collect(Collectors.toSet());
    }

    private Set<String> getAllServiceNameInParcel(Manifest manifest) {
        return manifest.getParcels().stream()
                .flatMap(it -> it.getComponents().stream())
                .map(Component::getName)
                .map(String::trim)
                .collect(Collectors.toSet());
    }
}
