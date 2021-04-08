package com.sequenceiq.cloudbreak.service.upgrade.validation;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.parcel.ParcelService;

@Component
class ParcelUrlProvider {

    @Inject
    private ParcelService parcelService;

    @Inject
    private ImageCatalogService imageCatalogService;

    Set<String> getRequiredParcelsFromImage(String imageCatalogUrl, String imageCatalogName, String imageId, Stack stack) {
        StatedImage targetImage = getTargetImage(imageCatalogUrl, imageCatalogName, imageId);
        String cdhParcelUrl = getCdhParcelUrl(targetImage);
        return StackType.DATALAKE.equals(stack.getType()) ? Collections.singleton(cdhParcelUrl) : getAllParcel(cdhParcelUrl, targetImage, stack);
    }

    private StatedImage getTargetImage(String imageCatalogUrl, String imageCatalogName, String imageId) {
        try {
            return imageCatalogService.getImage(imageCatalogUrl, imageCatalogName, imageId);
        } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException e) {
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    private String getCdhParcelUrl(StatedImage targetImage) {
        Map<String, String> stack = targetImage.getImage().getStackDetails().getRepo().getStack();
        String imageId = targetImage.getImage().getUuid();
        return getCdhBaseUrl(stack, imageId).concat("CDH-").concat(getRepoVersion(stack, imageId)).concat("-el7.parcel");
    }

    private String getCdhBaseUrl(Map<String, String> stack, String imageId) {
        return Optional.ofNullable(stack.get("redhat7"))
                .orElseThrow(() -> new CloudbreakServiceException(String.format("Stack base URL is not found on image: %s", imageId)));
    }

    private String getRepoVersion(Map<String, String> stack, String imageId) {
        return Optional.ofNullable(stack.get("repository-version"))
                .orElseThrow(() -> new CloudbreakServiceException(String.format("Stack repository version is not found on image: %s", imageId)));
    }

    private Set<String> getAllParcel(String cdhRepoUrl, StatedImage targetImage, Stack stack) {
        Set<String> preWarmParcelUrls = getPreWarmParcelUrls(targetImage, stack);
        preWarmParcelUrls.add(cdhRepoUrl);
        return preWarmParcelUrls;
    }

    private Set<String> getPreWarmParcelUrls(StatedImage targetImage, Stack stack) {
        Set<String> requiredParcelNames = getRequiredParcelNames(stack, targetImage.getImage());
        return targetImage.getImage()
                .getPreWarmParcels()
                .stream()
                .filter(filterRequiresParcels(requiredParcelNames))
                .map(list -> removeUnnecessaryCharacters(getPreWarmParcelBaseUrl(list)).concat("/").concat(getPreWarmParcelName(list)))
                .collect(Collectors.toSet());
    }

    private String getPreWarmParcelName(List<String> list) {
        return list.stream()
                .filter(parcelList -> !parcelList.startsWith("http"))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Parcel name not found in list: %s", list)));
    }

    private String getPreWarmParcelBaseUrl(List<String> list) {
        return list.stream()
                .filter(parcelList -> parcelList.startsWith("http"))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Parcel url not found in list: %s", list)));
    }

    private Predicate<List<String>> filterRequiresParcels(Set<String> requiredParcelNames) {
        return list -> list.stream().anyMatch(parcelName -> requiredParcelNames.stream().anyMatch(parcelName::startsWith));
    }

    private Set<String> getRequiredParcelNames(Stack stack, Image image) {
        return parcelService.getComponentsByImage(stack, image)
                .stream()
                .map(ClusterComponent::getName)
                .collect(Collectors.toSet());
    }

    private String removeUnnecessaryCharacters(String baseUrl) {
        while (baseUrl.endsWith(".") || baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }
}
