package com.sequenceiq.cloudbreak.service.image;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackDetails;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackRepoDetails;
import com.sequenceiq.cloudbreak.domain.CustomImage;
import com.sequenceiq.cloudbreak.domain.VmImage;

import org.apache.commons.lang3.StringUtils;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

@Component
public class CustomImageProvider {

    private static final String INTERNAL_BASE_URL = "https://archive.cloudera.com/p/";

    public StatedImage mergeSourceImageAndCustomImageProperties(
            StatedImage statedImage, CustomImage customImage, String imageCatalogUrl, String catalogName) {
        Image image = statedImage.getImage();
        Image result = new Image(image.getDate(),
                customImage.getCreated(),
                customImage.getDescription(),
                image.getOs(),
                customImage.getName(),
                image.getVersion(),
                getRepoWithCustomBaseUrl(image.getRepo(), customImage.getBaseParcelUrl()),
                getImageSetsByProvider(image.getImageSetsByProvider(), customImage.getVmImage()),
                getStackDetailsWithCustomBaseUrl(image.getStackDetails(), customImage.getBaseParcelUrl()),
                image.getOsType(),
                image.getPackageVersions(),
                getPreWarmParcelsWithCustomBaseUrl(image.getPreWarmParcels(), customImage.getBaseParcelUrl()),
                getPreWarmCsdWithCustomBaseUrl(image.getPreWarmCsd(), customImage.getBaseParcelUrl()),
                image.getCmBuildNumber(),
                image.isAdvertised(),
                customImage.getBaseParcelUrl(),
                customImage.getCustomizedImageId());
        return StatedImage.statedImage(result, imageCatalogUrl, catalogName);
    }

    private Map<String, Map<String, String>> getImageSetsByProvider(Map<String, Map<String, String>> imageSetsByProvider, Set<VmImage> vmImages) {
        Optional<String> provider = imageSetsByProvider.keySet().stream().findFirst();
        return provider.map(p -> Collections.singletonMap(p, vmImages.stream().collect(Collectors.toMap(VmImage::getRegion, VmImage::getImageReference))))
                .orElse(Collections.emptyMap());
    }

    private Map<String, String> getRepoWithCustomBaseUrl(Map<String, String> repo, String customBaseUrl) {
        return repo.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> changeBaseUrlToCustomUrl(e.getValue(), customBaseUrl)));
    }

    private StackDetails getStackDetailsWithCustomBaseUrl(StackDetails details, String customBaseUrl) {
        if (details != null) {
            Map<String, String> stack = details.getRepo().getStack().entrySet()
                    .stream().collect(toMap(Map.Entry::getKey, e -> changeBaseUrlToCustomUrl(e.getValue(), customBaseUrl)));
            StackRepoDetails repoDetails = new StackRepoDetails(stack, details.getRepo().getUtil());
            return new StackDetails(details.getVersion(), repoDetails, details.getStackBuildNumber());
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
