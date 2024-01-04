package com.sequenceiq.cloudbreak.service.parcel;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

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

    private static final String CDH = "CDH";

    @Inject
    private CmTemplateGeneratorService clusterTemplateGeneratorService;

    @Inject
    private ImageReaderService imageReaderService;

    @Inject
    private ManifestRetrieverService manifestRetrieverService;

    public Set<ClouderaManagerProduct> filterParcelsByBlueprint(Long workspaceId, Long stackId, Set<ClouderaManagerProduct> parcels, Blueprint blueprint) {
        LOGGER.debug("Filtering the following parcels based on the blueprint {}", parcels);
        Set<String> serviceNamesInBlueprint = getServiceNamesInBlueprint(blueprint);
        LOGGER.debug("The following services are found in the blueprint: {}", serviceNamesInBlueprint);
        if (serviceNamesInBlueprint.contains(null)) {
            LOGGER.debug("We can not identify one of the service from the blueprint so to stay on the safe side we will add every parcel");
            return parcels;
        }
        Set<String> availableParcelNamesFromImage = imageReaderService.getParcelNames(workspaceId, stackId);
        if (availableParcelNamesFromImage.isEmpty()) {
            LOGGER.debug("There is no pre warmed parcel on the image therefore we assume that every parcel is required. Parcels: {}", parcels);
            return parcels;
        } else {
            LOGGER.debug("The following parcels are available on the pre-warm image: {}", availableParcelNamesFromImage);
        }
        Set<ClouderaManagerProduct> requiredParcels = filterParcels(parcels, serviceNamesInBlueprint, availableParcelNamesFromImage);
        LOGGER.debug("The following parcels are used in CM based on blueprint: {}", requiredParcels);
        return requiredParcels;
    }

    private Set<ClouderaManagerProduct> filterParcels(Set<ClouderaManagerProduct> parcels, Set<String> requiredServicesInBlueprint,
            Set<String> availableParcelNamesFromImage) {
        Set<ClouderaManagerProduct> requiredParcels = new HashSet<>();
        Set<ClouderaManagerProduct> notAccessibleParcels = new HashSet<>();
        Iterator<ClouderaManagerProduct> parcelIterator = parcels.iterator();
        requiredParcels.addAll(collectCustomParcelsIfPresent(availableParcelNamesFromImage, parcels));
        requiredParcels.addAll(collectCdhParcel(availableParcelNamesFromImage, parcels));
        while (!requiredServicesInBlueprint.isEmpty() && parcelIterator.hasNext()) {
            ClouderaManagerProduct parcel = parcelIterator.next();
            ImmutablePair<ManifestStatus, Manifest> manifest = manifestRetrieverService.readRepoManifest(parcel.getParcel());
            if (manifestAvailable(manifest)) {
                Set<String> servicesInParcel = getAllServiceNameInParcel(manifest.right);
                LOGGER.debug("The {} parcel contains the following services: {}", parcel.getName(), servicesInParcel);
                if (servicesArePresentInTheBlueprint(requiredServicesInBlueprint, servicesInParcel, parcel) || CDH.equals(parcel.getName())) {
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

    private Set<ClouderaManagerProduct> collectCustomParcelsIfPresent(Set<String> availableParcelNamesFromImage, Set<ClouderaManagerProduct> parcels) {
        Set<ClouderaManagerProduct> customParcels = parcels.stream()
                .filter(parcel -> !availableParcelNamesFromImage.contains(parcel.getName()))
                .collect(Collectors.toSet());
        if (customParcels.isEmpty()) {
            LOGGER.debug("There is no custom parcel found in the provided parcel list");
        } else {
            LOGGER.debug("The following custom parcels are required: {}", customParcels);
        }
        return customParcels;
    }

    private Collection<ClouderaManagerProduct> collectCdhParcel(Set<String> availableParcelNamesFromImage, Set<ClouderaManagerProduct> parcels) {
        return parcels.stream()
                .filter(p -> CDH.equals(p.getName()))
                .collect(Collectors.toSet());
    }

    private Set<String> getServiceNamesInBlueprint(Blueprint blueprint) {
        String blueprintText = blueprint.getBlueprintJsonText();
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
