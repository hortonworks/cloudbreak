package com.sequenceiq.cloudbreak.service.parcel;

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
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.service.upgrade.sync.component.ImageReaderService;

/**
 * Filters the set of candidate parcels down to only those required by a given cluster blueprint,
 * using each parcel's remote manifest to determine which services it provides.
 * <p>
 * Filtering only applies to pre-warmed images. For base images (no parcels baked in), all
 * candidate parcels are returned as-is because there is no image-based baseline to compare against.
 * Similarly, if any service in the blueprint cannot be identified, all parcels are returned to
 * avoid accidentally excluding a required one.
 * <p>
 * Parcels that are not present on the pre-warmed image are treated as custom parcels and are
 * always included regardless of blueprint content. CDH is always included because it is a
 * mandatory runtime dependency (e.g. Knox is always added to clusters and is shipped in CDH),
 * even when no CDH-resident service appears explicitly in the blueprint.
 */
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
            LOGGER.debug("We cannot identify all services from the blueprint so to stay on the safe side we will add every parcel");
            return parcels;
        }
        Set<String> availableParcelNamesFromImage = imageReaderService.getParcelNames(workspaceId, stackId);
        if (availableParcelNamesFromImage.isEmpty()) {
            LOGGER.debug("There is no pre-warmed parcel on the image therefore we assume that every parcel is required. Parcels: {}", parcels);
            return parcels;
        } else {
            LOGGER.debug("The following parcels are available on the pre-warmed image: {}", availableParcelNamesFromImage);
        }
        Set<ClouderaManagerProduct> customParcels = collectCustomParcels(availableParcelNamesFromImage, parcels);
        Set<ClouderaManagerProduct> cdhParcels = collectCdhParcels(parcels);
        Set<ClouderaManagerProduct> requiredParcels = filterParcelsByManifest(parcels, serviceNamesInBlueprint, customParcels, cdhParcels);
        LOGGER.debug("The following parcels are used in CM based on blueprint: {}", requiredParcels);
        return requiredParcels;
    }

    private Set<String> getServiceNamesInBlueprint(Blueprint blueprint) {
        String blueprintText = blueprint.getBlueprintJsonText();
        Set<SupportedService> supportedServices = clusterTemplateGeneratorService.getServicesByBlueprint(blueprintText).getServices();
        return supportedServices.stream()
                .map(SupportedService::getName)
                .collect(Collectors.toSet());
    }

    private Set<ClouderaManagerProduct> collectCustomParcels(Set<String> availableParcelNamesFromImage, Set<ClouderaManagerProduct> parcels) {
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

    private Set<ClouderaManagerProduct> collectCdhParcels(Set<ClouderaManagerProduct> parcels) {
        return parcels.stream()
                .filter(parcel -> CDH.equals(parcel.getName()))
                .collect(Collectors.toSet());
    }

    /**
     * Iterates over parcels, fetching each one's remote manifest to determine which services it
     * provides, and includes it in the result if any of those services are required by the blueprint.
     * The loop exits early once all required services have been matched.
     * <p>
     * Parcels whose manifest cannot be retrieved or parsed are collected separately as
     * {@code notAccessibleParcels}. These are only added to the result at the end if there are
     * still unmatched required services — on the assumption that one of the inaccessible parcels
     * may provide them. If all required services were already matched by accessible parcels,
     * the inaccessible ones are silently dropped.
     */
    private Set<ClouderaManagerProduct> filterParcelsByManifest(Set<ClouderaManagerProduct> parcels, Set<String> requiredServicesInBlueprint,
            Set<ClouderaManagerProduct> customParcels, Set<ClouderaManagerProduct> cdhParcels) {
        Set<ClouderaManagerProduct> requiredParcels = new HashSet<>();
        Set<ClouderaManagerProduct> notAccessibleParcels = new HashSet<>();
        Iterator<ClouderaManagerProduct> parcelIterator = parcels.iterator();
        requiredParcels.addAll(customParcels);
        requiredParcels.addAll(cdhParcels);
        while (!requiredServicesInBlueprint.isEmpty() && parcelIterator.hasNext()) {
            ClouderaManagerProduct parcel = parcelIterator.next();
            ImmutablePair<ManifestStatus, Manifest> manifest;
            try {
                manifest = manifestRetrieverService.readRepoManifest(parcel.getParcel());
            } catch (CloudbreakRuntimeException e) {
                LOGGER.info("Adding parcel '{}' as retrieving its manifest has failed.", parcel);
                notAccessibleParcels.add(parcel);
                continue;
            }
            if (manifestAvailable(manifest)) {
                Set<String> servicesInParcel = getAllServiceNamesInParcel(manifest.right);
                LOGGER.debug("Parcel '{}' contains the following services: {}", parcel.getName(), servicesInParcel);
                if (CDH.equals(parcel.getName()) || parcelContainsServicesRequiredByBlueprint(requiredServicesInBlueprint, servicesInParcel, parcel)) {
                    requiredParcels.add(parcel);
                    LOGGER.debug("Removing '{}' from the remaining required services because these services are found in '{}' parcel.",
                        servicesInParcel, parcel);
                    requiredServicesInBlueprint.removeAll(servicesInParcel);
                } else {
                    LOGGER.info("Skipping parcel '{}' as none of its manifest services are required by the blueprint.", parcel);
                }
            } else {
                LOGGER.info("Adding parcel '{}' as its manifest was not available (status: {}).", parcel, manifest.left);
                notAccessibleParcels.add(parcel);
            }
        }
        if (!requiredServicesInBlueprint.isEmpty()) {
            LOGGER.debug("Adding {} not accessible parcels to required parcels as {} required services are not found in the accessible parcels.",
                    notAccessibleParcels, requiredServicesInBlueprint);
            requiredParcels.addAll(notAccessibleParcels);
        }
        return requiredParcels;
    }

    private boolean manifestAvailable(ImmutablePair<ManifestStatus, Manifest> manifest) {
        return manifest.right != null && ManifestStatus.SUCCESS.equals(manifest.left);
    }

    private Set<String> getAllServiceNamesInParcel(Manifest manifest) {
        return manifest.getParcels().stream()
                .flatMap(parcel -> parcel.getComponents().stream())
                .map(Component::getName)
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    private boolean parcelContainsServicesRequiredByBlueprint(Set<String> serviceNamesInBlueprint, Set<String> serviceNamesInParcel,
        ClouderaManagerProduct parcel) {
        if (serviceNamesInParcel.stream()
                .anyMatch(serviceInParcel -> serviceNamesInBlueprint.stream()
                        .anyMatch(serviceInBlueprint -> serviceInBlueprint.equalsIgnoreCase(serviceInParcel)))) {
            LOGGER.debug("Adding parcel '{}' as there is at least one service both in the manifest and in the blueprint.", parcel);
            return true;
        } else {
            LOGGER.debug("The following services in the parcel '{}' are not present in the blueprint {}", serviceNamesInParcel, serviceNamesInBlueprint);
            return false;
        }
    }
}
