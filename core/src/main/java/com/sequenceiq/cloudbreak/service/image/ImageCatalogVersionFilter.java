package com.sequenceiq.cloudbreak.service.image;


import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.VersionComparator;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakVersion;

@Component
public class ImageCatalogVersionFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogVersionFilter.class);

    private static final String RELEASED_VERSION_PATTERN = "^\\d+\\.\\d+\\.\\d+";

    private static final String UNRELEASED_VERSION_PATTERN = "^\\d+\\.\\d+\\.\\d+-[d,r][c,e][v]?";

    private static final String EXTENDED_UNRELEASED_VERSION_PATTERN = "^\\d+\\.\\d+\\.\\d+-[d,r,b][c,e]?[v]?\\.\\d+";

    private static final String UNSPECIFIED_VERSION = "unspecified";

    public Set<CloudbreakVersion> filterReleasedVersions(Collection<CloudbreakVersion> cloudbreakVersions, String releasedVersion) {
        Map<Boolean, List<CloudbreakVersion>> partitionedVersions = cloudbreakVersions.stream()
                .collect(Collectors.partitioningBy(containsReleaseVersion(releasedVersion)));
        if (hasFiltered(partitionedVersions)) {
            LOGGER.debug("Used filter releaseVersion: | {} | Versions filtered: {}",
                    releasedVersion,
                    partitionedVersions.get(false).stream()
                            .map(CloudbreakVersion::getVersions).flatMap(List::stream)
                            .collect(Collectors.joining(", ")));
        }
        return new HashSet<>(partitionedVersions.get(true));
    }

    public Set<CloudbreakVersion> filterUnreleasedVersions(Collection<CloudbreakVersion> cloudbreakVersions, String unReleasedVersion) {
        Map<Boolean, List<CloudbreakVersion>> partitionedVersions = cloudbreakVersions.stream()
                .collect(Collectors.partitioningBy(containsNonReleaseVersion(unReleasedVersion)));
        if (hasFiltered(partitionedVersions)) {
            LOGGER.debug("Used filter unReleasedVersion: | {} | Versions filtered: {}",
                    unReleasedVersion,
                    partitionedVersions.get(false).stream()
                            .map(CloudbreakVersion::getVersions).flatMap(List::stream)
                            .collect(Collectors.joining(", ")));
        }
        return new HashSet<>(partitionedVersions.get(true));
    }

    private boolean hasFiltered(Map<Boolean, List<CloudbreakVersion>> partitionedVersions) {
        return !partitionedVersions.get(false).isEmpty();
    }

    public String latestCloudbreakVersion(Iterable<CloudbreakVersion> cloudbreakVersions) {
        SortedMap<Versioned, CloudbreakVersion> sortedCloudbreakVersions = new TreeMap<>(new VersionComparator());
        for (CloudbreakVersion cbv : cloudbreakVersions) {
            cbv.getVersions().forEach(cbvs -> sortedCloudbreakVersions.put(() -> cbvs, cbv));
        }
        return sortedCloudbreakVersions.lastKey().getVersion();
    }

    public boolean isVersionUnspecified(String cbVersion) {
        return UNSPECIFIED_VERSION.equals(cbVersion);
    }

    public String extractUnreleasedVersion(String cbVersion) {
        return extractCbVersion(UNRELEASED_VERSION_PATTERN, cbVersion);
    }

    public String extractReleasedVersion(String cbVersion) {
        return extractCbVersion(RELEASED_VERSION_PATTERN, cbVersion);
    }

    public String extractExtendedUnreleasedVersion(String cbVersion) {
        return extractCbVersion(EXTENDED_UNRELEASED_VERSION_PATTERN, cbVersion);
    }

    private static Predicate<CloudbreakVersion> isExactVersionMatch(String cbv) {
        return cloudbreakVersion -> cloudbreakVersion.getVersions().contains(cbv);
    }

    private static Predicate<CloudbreakVersion> containsReleaseVersion(String releasedVersion) {
        return cloudbreakVersion -> cloudbreakVersion.getVersions().contains(releasedVersion);
    }

    private static Predicate<CloudbreakVersion> containsNonReleaseVersion(String unReleasedVersion) {
        return cbVersion -> cbVersion.getVersions().stream().anyMatch(aVersion -> aVersion.startsWith(unReleasedVersion));
    }

    private String extractCbVersion(String pattern, String cbVersion) {
        Matcher matcher = Pattern.compile(pattern).matcher(cbVersion);
        if (matcher.find()) {
            return matcher.group(0);
        }
        return cbVersion;
    }
}
