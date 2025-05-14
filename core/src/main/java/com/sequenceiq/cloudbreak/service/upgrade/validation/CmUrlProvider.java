package com.sequenceiq.cloudbreak.service.upgrade.validation;

import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.CM;
import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.CM_BUILD_NUMBER;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.PaywallCredentialPopulator;
import com.sequenceiq.cloudbreak.client.RestClientFactory;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.image.CustomImageProvider;
import com.sequenceiq.common.model.OsType;

@Component
public class CmUrlProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(CmUrlProvider.class);

    private static final String CM_PUBLIC = "cm-public";

    private static final String RELEASE_MANIFEST_JSON = "release_manifest.json";

    @Inject
    private RestClientFactory restClientFactory;

    @Inject
    private PaywallCredentialPopulator paywallCredentialPopulator;

    @Cacheable(CmUrlCache.CM_URL_CACHE)
    public String getCmRpmUrl(Image image) {
        LOGGER.debug("Retrieving CM RPM package URL from image {}", image.getUuid());
        return fetchUrlFromManifest(image).orElseGet(() -> concatRpmUrlLegacyWay(image));
    }

    private String concatRpmUrlLegacyWay(Image image) {
        LOGGER.info("Creating the CM rpm URL the legacy way for {}", image);
        return image.getRepo().get(image.getOsType())
                .concat("RPMS/x86_64/cloudera-manager-server-")
                .concat(image.getPackageVersions().get(CM.getKey()))
                .concat("-")
                .concat(image.getPackageVersions().get(CM_BUILD_NUMBER.getKey()))
                .concat(".")
                .concat(OsType.getByOsTypeString(image.getOsType()).getParcelPostfix())
                .concat(".x86_64.rpm");
    }

    private Optional<String> fetchUrlFromManifest(Image image) {
        String cmRepoUrlForOs = image.getRepo().get(image.getOsType());
        if (cmRepoUrlForOs.startsWith(CustomImageProvider.INTERNAL_BASE_URL) && cmRepoUrlForOs.contains(CM_PUBLIC)) {
            try {
                String manifestUrl = constructManifestUrl(cmRepoUrlForOs);
                CmManifestFile response = getManifestFile(manifestUrl);
                LOGGER.debug("Manifest file {} for image {}", response, image);
                Optional<String> cmServerRpmUrlFromManifest = selectCmServerRpmUrl(image, response)
                        .map(cmServerRelativeUrl -> StringUtils.removeEnd(manifestUrl, RELEASE_MANIFEST_JSON) + cmServerRelativeUrl);
                LOGGER.info("CM server RPM URL using manifest: {}", cmServerRpmUrlFromManifest);
                return cmServerRpmUrlFromManifest;
            } catch (Exception e) {
                LOGGER.warn("Fetching CM RPM URL from manifest file failed unexpectedly. Falling back to legacy mode", e);
                return Optional.empty();
            }
        } else {
            LOGGER.info("CM repo URL [{}] is not suitable for manifest file", cmRepoUrlForOs);
            return Optional.empty();
        }
    }

    private Optional<String> selectCmServerRpmUrl(Image image, CmManifestFile response) {
        Set<String> cmPackages = response.getFiles().stream()
                .filter(file -> file.contains("cloudera-manager-server-" + image.getPackageVersions().get(CM.getKey())))
                .filter(file -> file.contains(image.getPackageVersions().get(CM_BUILD_NUMBER.getKey())))
                .filter(file -> file.contains("x86_64.rpm"))
                .filter(file -> file.contains(image.getOsType()))
                .collect(Collectors.toSet());
        LOGGER.info("Package candidate: {}, selecting first", cmPackages);
        return cmPackages.stream().findFirst();
    }

    private CmManifestFile getManifestFile(String manifestUrl) {
        Client client = restClientFactory.getOrCreateDefault();
        WebTarget target = client.target(manifestUrl);
        paywallCredentialPopulator.populateWebTarget(manifestUrl, target);
        return target.request().get(CmManifestFile.class);
    }

    /**
     * @param cmRepoUrlForOs eg: https://archive.cloudera.com/p/cm7/7.2.6/redhat7/yum/
     * @return eg: https://archive.cloudera.com/p/cm7/7.2.6/release_manifest.json
     */
    private String constructManifestUrl(String cmRepoUrlForOs) {
        String[] splitBySlash = StringUtils.splitPreserveAllTokens(cmRepoUrlForOs, "/");
        int indexOfCmPublicPart = Arrays.asList(splitBySlash).indexOf(CM_PUBLIC);
        String cmRepoUrlWithVersion = Arrays.stream(splitBySlash).limit(indexOfCmPublicPart + 2L).collect(Collectors.joining("/"));
        String manifestUrl = StringUtils.appendIfMissing(cmRepoUrlWithVersion, "/") + RELEASE_MANIFEST_JSON;
        LOGGER.debug("Manifest URL: {} from {}", manifestUrl, cmRepoUrlForOs);
        return manifestUrl;
    }
}