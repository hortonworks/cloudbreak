package com.sequenceiq.cloudbreak.service.image;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.VersionComparator;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakVersion;

@Component
public class PrefixMatcherService {

    @Inject
    private ImageCatalogVersionFilter versionFilter;

    public PrefixMatchImages prefixMatchForCBVersion(String cbVersion, Collection<CloudbreakVersion> cloudbreakVersions) {
        Set<String> supportedVersions = new HashSet<>();
        Set<String> vMImageUUIDs = new HashSet<>();
        Set<String> defaultVMImageUUIDs = new HashSet<>();
        String unReleasedVersion = versionFilter.extractUnreleasedVersion(cbVersion);
        boolean versionIsReleased = unReleasedVersion.equals(cbVersion);

        if (!versionIsReleased) {
            Set<CloudbreakVersion> unReleasedCbVersions = versionFilter.filterUnreleasedVersions(cloudbreakVersions, unReleasedVersion);
            supportedVersions = getSupportedVersions(vMImageUUIDs, defaultVMImageUUIDs, unReleasedCbVersions);
        }

        if (versionIsReleased || vMImageUUIDs.isEmpty()) {
            String releasedVersion = versionFilter.extractReleasedVersion(cbVersion);
            Set<CloudbreakVersion> releasedCbVersions = versionFilter.filterReleasedVersions(cloudbreakVersions, releasedVersion);

            Integer accumulatedImageCount = accumulateImageCount(releasedCbVersions);
            if (releasedCbVersions.isEmpty() || accumulatedImageCount == 0) {
                releasedCbVersions = previousCbVersion(releasedVersion, cloudbreakVersions);
            }
            supportedVersions = getSupportedVersions(vMImageUUIDs, defaultVMImageUUIDs, releasedCbVersions);
        }
        return new PrefixMatchImages(vMImageUUIDs, defaultVMImageUUIDs, supportedVersions);
    }

    private Set<String> getSupportedVersions(Set<String> vMImageUUIDs, Set<String> defaultVMImageUUIDs, Set<CloudbreakVersion> unReleasedCbVersions) {
        for (CloudbreakVersion unReleasedCbVersion : unReleasedCbVersions) {
            vMImageUUIDs.addAll(unReleasedCbVersion.getImageIds());
            defaultVMImageUUIDs.addAll(unReleasedCbVersion.getDefaults());
        }
        return unReleasedCbVersions.stream().map(CloudbreakVersion::getVersions).flatMap(List::stream).collect(Collectors.toSet());
    }

    private Integer accumulateImageCount(Collection<CloudbreakVersion> cloudbreakVersions) {
        return cloudbreakVersions.stream()
                .map(CloudbreakVersion::getImageIds)
                .map(List::size)
                .reduce(Integer::sum)
                .orElse(0);
    }

    private Set<CloudbreakVersion> previousCbVersion(String releasedVersion, Collection<CloudbreakVersion> cloudbreakVersions) {
        List<String> versions = cloudbreakVersions.stream()
                .map(CloudbreakVersion::getVersions)
                .flatMap(List::stream)
                .distinct()
                .collect(toList());

        versions = versions.stream().sorted((o1, o2) -> new VersionComparator().compare(() -> o2, () -> o1)).collect(toList());

        Predicate<String> ealierVersionPredicate = ver -> new VersionComparator().compare(() -> ver, () -> releasedVersion) < 0;
        Predicate<String> releaseVersionPredicate = ver -> versionFilter.extractExtendedUnreleasedVersion(ver).equals(ver);
        Predicate<String> versionHasImagesPredicate = ver -> accumulateImageCount(cloudbreakVersions) > 0;
        Optional<String> applicableVersion = versions.stream()
                .filter(ealierVersionPredicate)
                .filter(releaseVersionPredicate)
                .filter(versionHasImagesPredicate)
                .findAny();

        return applicableVersion
                .map(ver -> cloudbreakVersions.stream().filter(cbVer -> cbVer.getVersions().contains(ver)).collect(Collectors.toSet()))
                .orElse(emptySet());
    }

}
