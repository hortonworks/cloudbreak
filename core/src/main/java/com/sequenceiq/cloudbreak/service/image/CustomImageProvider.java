package com.sequenceiq.cloudbreak.service.image;

import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImageStackDetails;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackRepoDetails;
import com.sequenceiq.cloudbreak.domain.CustomImage;
import com.sequenceiq.cloudbreak.domain.VmImage;

@Component
public class CustomImageProvider {

    public static final String INTERNAL_BASE_URL = "https://archive.cloudera.com/p/";

    public StatedImage mergeSourceImageAndCustomImageProperties(
            StatedImage statedImage, CustomImage customImage, String imageCatalogUrl, String catalogName) {
        Image image = statedImage.getImage();
        Image result = Image.builder()
                .withDate(image.getDate())
                .withCreated(customImage.getCreated())
                .withPublished(customImage.getCreated())
                .withDescription(customImage.getDescription())
                .withOs(image.getOs())
                .withOsType(image.getOsType())
                .withUuid(customImage.getName())
                .withVersion(image.getVersion())
                .withRepo(getRepoWithCustomBaseUrl(image.getRepo(), customImage.getBaseParcelUrl()))
                .withImageSetsByProvider(getImageSetsByProvider(image.getImageSetsByProvider(), customImage.getVmImage()))
                .withStackDetails(getStackDetailsWithCustomBaseUrl(image.getStackDetails(), customImage.getBaseParcelUrl()))
                .withPreWarmParcels(getPreWarmParcelsWithCustomBaseUrl(image.getPreWarmParcels(), customImage.getBaseParcelUrl()))
                .withPreWarmCsd(getPreWarmCsdWithCustomBaseUrl(image.getPreWarmCsd(), customImage.getBaseParcelUrl()))
                .withCmBuildNumber(image.getCmBuildNumber())
                .withAdvertised(image.isAdvertised())
                .withBaseParcelUrl(customImage.getBaseParcelUrl())
                .withSourceImageId(customImage.getCustomizedImageId())
                .withTags(image.getTags())
                .withPackageVersions(image.getPackageVersions())
                .build();
        return StatedImage.statedImage(result, imageCatalogUrl, catalogName);
    }

    private Map<String, Map<String, String>> getImageSetsByProvider(Map<String, Map<String, String>> imageSetsByProvider, Set<VmImage> vmImages) {
        if (vmImages == null || vmImages.isEmpty()) {
            return imageSetsByProvider;
        }
        Optional<String> provider = imageSetsByProvider.keySet().stream().findFirst();
        return provider.map(p -> Collections.singletonMap(p, vmImages.stream().collect(Collectors.toMap(VmImage::getRegion, VmImage::getImageReference))))
                .orElse(Collections.emptyMap());
    }

    private Map<String, String> getRepoWithCustomBaseUrl(Map<String, String> repo, String customBaseUrl) {
        return repo.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> changeBaseUrlToCustomUrl(e.getValue(), customBaseUrl)));
    }

    private ImageStackDetails getStackDetailsWithCustomBaseUrl(ImageStackDetails details, String customBaseUrl) {
        if (details != null) {
            Map<String, String> stack = details.getRepo().getStack().entrySet()
                    .stream().collect(toMap(Map.Entry::getKey, e -> changeBaseUrlToCustomUrl(e.getValue(), customBaseUrl)));
            StackRepoDetails repoDetails = new StackRepoDetails(stack, details.getRepo().getUtil());
            return new ImageStackDetails(details.getVersion(), repoDetails, details.getStackBuildNumber());
        }
        return null;
    }

    private List<List<String>> getPreWarmParcelsWithCustomBaseUrl(List<List<String>> preWarmParcels, String customBaseUrl) {
        return preWarmParcels.stream()
                .map(list -> list.stream()
                        .map(e -> changeBaseUrlToCustomUrl(e, customBaseUrl))
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
    }

    private List<String> getPreWarmCsdWithCustomBaseUrl(List<String> preWarmCsd, String customBaseUrl) {
        return preWarmCsd.stream().map(e -> changeBaseUrlToCustomUrl(e, customBaseUrl)).collect(Collectors.toList());
    }

    private String changeBaseUrlToCustomUrl(String value, String customBaseUrl) {
        if (!value.contains(INTERNAL_BASE_URL) || StringUtils.isEmpty(customBaseUrl)) {
            return value;
        }
        return customBaseUrl.endsWith("/") ?
                value.replace(INTERNAL_BASE_URL, customBaseUrl) :
                value.replace(INTERNAL_BASE_URL, customBaseUrl + "/");
    }
}
