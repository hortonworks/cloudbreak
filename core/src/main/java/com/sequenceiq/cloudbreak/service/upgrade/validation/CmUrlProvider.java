package com.sequenceiq.cloudbreak.service.upgrade.validation;

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
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.service.image.CustomImageProvider;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.common.model.Architecture;

@Component
public class CmUrlProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(CmUrlProvider.class);

    private static final String CM_PUBLIC = "cm-public";

    private static final String RELEASE_MANIFEST_JSON = "release_manifest.json";

    @Inject
    private RestClientFactory restClientFactory;

    @Inject
    private PaywallCredentialPopulator paywallCredentialPopulator;

    @Cacheable(value = CmUrlCache.CM_URL_CACHE, key = "{#clusterUpgradeProperties.clouderaManagerRepo.baseUrl, "
            + "#clusterUpgradeProperties.clouderaManagerRepo.version, #clusterUpgradeProperties.clouderaManagerRepo.buildNumber, "
            + "#clusterUpgradeProperties.targetOsType, #clusterUpgradeProperties.targetImage.architecture}")
    public String getCmRpmUrl(ClusterUpgradeProperties clusterUpgradeProperties) {
        LOGGER.debug("Retrieving CM RPM package URL from image {}", clusterUpgradeProperties.targetImage().imageId());
        return fetchUrlFromManifest(clusterUpgradeProperties).orElseGet(() -> concatRpmUrlLegacyWay(clusterUpgradeProperties));
    }

    private Optional<String> fetchUrlFromManifest(ClusterUpgradeProperties clusterUpgradeProperties) {
        String cmRepoUrlForOs = clusterUpgradeProperties.targetImage().clouderaManagerRepo().getBaseUrl();
        if (cmRepoUrlForOs.startsWith(CustomImageProvider.INTERNAL_BASE_URL) && cmRepoUrlForOs.contains(CM_PUBLIC)) {
            try {
                String manifestUrl = constructManifestUrl(cmRepoUrlForOs);
                CmManifestFile response = getManifestFile(manifestUrl);
                LOGGER.debug("Manifest file {} for URL {}", response, manifestUrl);
                Optional<String> cmServerRpmUrlFromManifest = selectCmServerRpmUrl(clusterUpgradeProperties, response)
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

    private Optional<String> selectCmServerRpmUrl(ClusterUpgradeProperties clusterUpgradeProperties, CmManifestFile response) {
        ClusterUpgradeProperties.TargetImageUpgradeContext targetImageProperties = clusterUpgradeProperties.targetImage();
        String architecture = Architecture.fromStringWithFallback(targetImageProperties.architecture()).getRpmName();
        Set<String> cmPackages = response.getFiles().stream()
                .filter(file -> file.contains("cloudera-manager-server-" + targetImageProperties.clouderaManagerRepo().getVersion()))
                .filter(file -> file.contains(targetImageProperties.clouderaManagerRepo().getBuildNumber()))
                .filter(file -> file.contains(architecture + ".rpm"))
                .filter(file -> file.contains(targetImageProperties.osType().getOsType()))
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

    private String concatRpmUrlLegacyWay(ClusterUpgradeProperties clusterUpgradeProperties) {
        ClusterUpgradeProperties.TargetImageUpgradeContext targetImageUpgradeContext = clusterUpgradeProperties.targetImage();
        LOGGER.info("Creating the CM rpm URL the legacy way for {}", targetImageUpgradeContext.imageId());
        String architecture = Architecture.fromStringWithFallback(targetImageUpgradeContext.architecture()).getRpmName();
        ClouderaManagerRepo clouderaManagerRepo = targetImageUpgradeContext.clouderaManagerRepo();
        return clouderaManagerRepo.getBaseUrl()
                .concat("RPMS/")
                .concat(architecture)
                .concat("/cloudera-manager-server-")
                .concat(clouderaManagerRepo.getVersion())
                .concat("-")
                .concat(clouderaManagerRepo.getBuildNumber())
                .concat(".")
                .concat(targetImageUpgradeContext.osType().getParcelPostfix())
                .concat(".")
                .concat(architecture)
                .concat(".rpm");
    }
}