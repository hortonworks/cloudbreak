package com.sequenceiq.cloudbreak.service.runtimes;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.common.runtime.RuntimeVersions;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.util.VersionComparator;

@Component
public class SupportedRuntimes {

    private static final Logger LOGGER = LoggerFactory.getLogger(SupportedRuntimes.class);

    @Inject
    private RuntimeVersions runtimeVersions;

    @Inject
    private ImageCatalogService imageCatalogService;

    private final VersionComparator versionComparator = new VersionComparator();

    public boolean isSupported(String runtime) {
        String latestSupportedRuntime = getLatestSupportedRuntime();
        boolean ret;
        if (Strings.isNullOrEmpty(latestSupportedRuntime)) {
            ret = true;
        } else {
            ret = versionComparator.compare(() -> runtime, () -> latestSupportedRuntime) <= 0;
        }
        return ret;
    }

    /**
     * @return the latest version that is supported - every former version shall just work. When the configured policy
     *         leaves it blank, it is derived from the default image catalog instead.
     */
    private String getLatestSupportedRuntime() {
        String latestSupportedRuntime = runtimeVersions.latest();
        try {
            if (Strings.isNullOrEmpty(latestSupportedRuntime)) {
                List<String> defaultImageCatalogRuntimeVersions = imageCatalogService.getRuntimeVersionsFromDefault();
                LOGGER.debug("Define latest supported runtime by available runtimes in the default image catalog ('{}').",
                        String.join(",", defaultImageCatalogRuntimeVersions));

                Optional<String> latestRuntime = defaultImageCatalogRuntimeVersions.stream().findFirst();
                if (latestRuntime.isEmpty()) {
                    LOGGER.error("Runtime not found in the default image catalog");
                }
                return latestRuntime.orElse(null);
            }
        } catch (CloudbreakImageCatalogException ex) {
            LOGGER.error("Failed to get runtimes from the default image catalog", ex);
        }
        return latestSupportedRuntime;
    }
}
