package com.sequenceiq.cloudbreak.service.upgrade;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.util.CdhVersionProvider;

@Component
public class BaseImagePackageVersionsEnricher {

    private static final String CDH_PRODUCT_NAME = "CDH";

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseImagePackageVersionsEnricher.class);

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    public Image enrich(Image image, Stack stack) {
        if (!BaseImageUtils.isBaseImage(image)) {
            return image;
        }
        Map<String, String> resolvedPackageVersions = resolvePackageVersions(stack);
        if (resolvedPackageVersions.isEmpty()) {
            LOGGER.debug("Could not resolve package versions for base image cluster {}", stack.getName());
            return image;
        }
        LOGGER.debug("Enriching base image package versions for cluster {} with {}", stack.getName(), resolvedPackageVersions);
        return withPackageVersions(image, resolvedPackageVersions);
    }

    private Map<String, String> resolvePackageVersions(Stack stack) {
        Map<String, String> fromInstances = getPackageVersionsFromInstances(stack.getId());
        if (!fromInstances.isEmpty()) {
            return fromInstances;
        }
        return getPackageVersionsFromClusterComponents(stack);
    }

    private Map<String, String> getPackageVersionsFromInstances(Long stackId) {
        return instanceMetaDataService.getNotDeletedAndNotZombieInstanceMetadataByStackId(stackId).stream()
                .map(this::getPackageVersionsFromInstance)
                .filter(this::hasComponentVersions)
                .findFirst()
                .orElseGet(HashMap::new);
    }

    private Map<String, String> getPackageVersionsFromInstance(InstanceMetaData instanceMetaData) {
        if (instanceMetaData.getImage() == null || StringUtils.isBlank(instanceMetaData.getImage().getValue())) {
            return new HashMap<>();
        }
        try {
            Image image = instanceMetaData.getImage().get(Image.class);
            if (image == null || image.getPackageVersions() == null) {
                return new HashMap<>();
            }
            return new HashMap<>(image.getPackageVersions());
        } catch (IOException e) {
            LOGGER.warn("Failed to read package versions from instance {}", instanceMetaData.getInstanceId(), e);
            return new HashMap<>();
        }
    }

    private boolean hasComponentVersions(Map<String, String> packageVersions) {
        return StringUtils.isNotBlank(packageVersions.get(ImagePackageVersion.CM.getKey()))
                || StringUtils.isNotBlank(packageVersions.get(ImagePackageVersion.STACK.getKey()));
    }

    private Map<String, String> getPackageVersionsFromClusterComponents(Stack stack) {
        if (stack.getCluster() == null) {
            return Map.of();
        }
        Long clusterId = stack.getCluster().getId();
        Map<String, String> packageVersions = new HashMap<>();
        addCmPackageVersions(packageVersions, clusterId);
        addCdhPackageVersions(packageVersions, clusterId);
        addParcelPackageVersions(packageVersions, clusterId);
        return packageVersions;
    }

    private void addCmPackageVersions(Map<String, String> packageVersions, Long clusterId) {
        ClouderaManagerRepo clouderaManagerRepo = clusterComponentConfigProvider.getClouderaManagerRepoDetails(clusterId);
        if (clouderaManagerRepo == null) {
            LOGGER.debug("CM repo details are not available for cluster {}", clusterId);
            return;
        }
        if (StringUtils.isNotBlank(clouderaManagerRepo.getVersion())) {
            packageVersions.put(ImagePackageVersion.CM.getKey(), clouderaManagerRepo.getVersion());
        }
        if (StringUtils.isNotBlank(clouderaManagerRepo.getBuildNumber())) {
            packageVersions.put(ImagePackageVersion.CM_BUILD_NUMBER.getKey(), clouderaManagerRepo.getBuildNumber());
        }
    }

    private void addCdhPackageVersions(Map<String, String> packageVersions, Long clusterId) {
        clusterComponentConfigProvider.getCdhProduct(clusterId).ifPresent(cdhProduct -> {
            String version = cdhProduct.getVersion();
            if (StringUtils.isBlank(version)) {
                return;
            }
            Optional<String> stackVersion = CdhVersionProvider.getCdhStackVersionFromVersionString(version);
            stackVersion.ifPresent(parsedStackVersion -> packageVersions.put(ImagePackageVersion.STACK.getKey(), parsedStackVersion));
            Optional<Integer> buildNumber = CdhVersionProvider.getCdhBuildNumberFromVersionString(version);
            if (buildNumber.isPresent()) {
                packageVersions.put(ImagePackageVersion.CDH_BUILD_NUMBER.getKey(), String.valueOf(buildNumber.get()));
            } else if (stackVersion.isPresent() && version.contains("-")) {
                packageVersions.put(ImagePackageVersion.CDH_BUILD_NUMBER.getKey(), version.substring(version.indexOf('-') + 1));
            } else if (stackVersion.isEmpty()) {
                putVersionWithBuildNumber(packageVersions, ImagePackageVersion.STACK.getKey(), ImagePackageVersion.CDH_BUILD_NUMBER.getKey(), version);
            }
        });
    }

    private void addParcelPackageVersions(Map<String, String> packageVersions, Long clusterId) {
        clusterComponentConfigProvider.getClouderaManagerProductDetails(clusterId).stream()
                .filter(product -> !CDH_PRODUCT_NAME.equals(product.getName()))
                .forEach(product -> toPackageVersionKey(product).ifPresent(key -> {
                    if (StringUtils.isNotBlank(product.getVersion())) {
                        packageVersions.put(key, product.getVersion());
                    }
                }));
    }

    private Optional<String> toPackageVersionKey(ClouderaManagerProduct product) {
        if (CDH_PRODUCT_NAME.equals(product.getName())) {
            return Optional.of(ImagePackageVersion.STACK.getKey());
        }
        return Arrays.stream(ImagePackageVersion.values())
                .filter(imagePackageVersion -> imagePackageVersion.getKey().equalsIgnoreCase(product.getName())
                        || imagePackageVersion.getDisplayName().equalsIgnoreCase(product.getName()))
                .map(ImagePackageVersion::getKey)
                .findFirst();
    }

    private void putVersionWithBuildNumber(Map<String, String> packageVersions, String versionKey, String buildNumberKey, String fullVersion) {
        int dashIndex = fullVersion.indexOf('-');
        if (dashIndex > 0) {
            packageVersions.put(versionKey, fullVersion.substring(0, dashIndex));
            packageVersions.put(buildNumberKey, fullVersion.substring(dashIndex + 1));
        } else {
            packageVersions.put(versionKey, fullVersion);
        }
    }

    private Image withPackageVersions(Image image, Map<String, String> packageVersions) {
        Map<String, String> mergedPackageVersions = new HashMap<>(image.getPackageVersions());
        mergedPackageVersions.putAll(packageVersions);
        return Image.builder()
                .withImageName(image.getImageName())
                .withUserdata(image.getUserdata())
                .withOs(image.getOs())
                .withOsType(image.getOsType())
                .withArchitecture(image.getArchitecture())
                .withImageCatalogUrl(image.getImageCatalogUrl())
                .withImageCatalogName(image.getImageCatalogName())
                .withImageId(image.getImageId())
                .withPackageVersions(mergedPackageVersions)
                .withDate(image.getDate())
                .withCreated(image.getCreated())
                .withTags(image.getTags())
                .build();
    }
}
