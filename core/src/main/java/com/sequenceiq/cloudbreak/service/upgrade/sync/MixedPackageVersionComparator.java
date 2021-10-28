package com.sequenceiq.cloudbreak.service.upgrade.sync;

import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.CM;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.cloud.VersionComparator;
import com.sequenceiq.cloudbreak.service.upgrade.sync.common.ParcelInfo;

@Service
public class MixedPackageVersionComparator {

    public boolean matchParcelVersions(Set<ParcelInfo> activeParcels, Map<String, String> productsFromImage) {
        return !CollectionUtils.isEmpty(activeParcels) && !CollectionUtils.isEmpty(productsFromImage)
                && activeParcels.stream().allMatch(parcelInfo -> parcelInfo.getVersion().equals(productsFromImage.get(parcelInfo.getName())));
    }

    public Map<String, String> filterTargetPackageVersionsByNewerPackageVersions(Map<String, String> targetProducts, String targetCmVersion,
            Map<String, String> newerComponentVersions) {
        Map<String, String> targetVersions = targetProducts.entrySet().stream()
                .filter(product -> newerComponentVersions.containsKey(product.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Optional.ofNullable(newerComponentVersions.get(CM.getDisplayName())).ifPresent(version -> targetVersions.put(CM.getDisplayName(), targetCmVersion));
        return targetVersions;
    }

    public Map<String, String> getComponentsWithNewerVersionThanTheTarget(Map<String, String> targetProducts, String targetCmVersion,
            Set<ParcelInfo> activeParcels, String activeCmVersion) {
        VersionComparator versionComparator = new VersionComparator();
        Map<String, String> componentsWithNewerVersion = activeParcels.stream()
                .filter(parcelInfo -> compareParcelVersions(parcelInfo, targetProducts, versionComparator))
                .collect(Collectors.toMap(ParcelInfo::getName, ParcelInfo::getVersion));
        if (versionComparator.compare(() -> activeCmVersion, () -> targetCmVersion) > 0) {
            componentsWithNewerVersion.put(CM.getDisplayName(), activeCmVersion);
        }
        return componentsWithNewerVersion;
    }

    public boolean areAllComponentVersionsMatchingWithImage(String cmVersionFomImage, Map<String, String> productsFromImage, String activeCmVersion,
            Set<ParcelInfo> activeParcels) {
        return activeCmVersion.equals(cmVersionFomImage) && matchParcelVersions(activeParcels, productsFromImage);
    }

    private boolean compareParcelVersions(ParcelInfo parcelInfo, Map<String, String> productsFromImage, VersionComparator versionComparator) {
        return productsFromImage.entrySet().stream().filter(product -> parcelInfo.getName().equals(product.getKey())).findFirst()
                .map(product -> versionComparator.compare(parcelInfo::getVersion, product::getValue) > 0)
                .orElse(false);
    }

}
