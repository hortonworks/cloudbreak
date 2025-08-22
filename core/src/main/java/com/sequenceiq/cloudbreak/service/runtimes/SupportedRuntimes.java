package com.sequenceiq.cloudbreak.service.runtimes;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.util.VersionComparator;

@Component
public class SupportedRuntimes {

    private static final Logger LOGGER = LoggerFactory.getLogger(SupportedRuntimes.class);

    // Defines what is the latest version what is supported, every former version shall just work
    @Value("${cb.runtimes.latest}")
    private String latestSupportedRuntime;

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

    private String getLatestSupportedRuntime() {
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
