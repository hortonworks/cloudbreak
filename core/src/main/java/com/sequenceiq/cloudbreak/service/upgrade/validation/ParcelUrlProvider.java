package com.sequenceiq.cloudbreak.service.upgrade.validation;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.service.parcel.ParcelService;

@Component
public class ParcelUrlProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParcelUrlProvider.class);

    @Inject
    private ParcelService parcelService;

    public Set<String> getRequiredParcelsFromImage(Image image, Stack stack) {
        LOGGER.debug("Retrieving parcel URLs from image {}", image.getUuid());
        String cdhParcelUrl = getCdhParcelUrl(image);
        return StackType.DATALAKE.equals(stack.getType()) ? Collections.singleton(cdhParcelUrl) : getAllParcels(cdhParcelUrl, image, stack);
    }

    private String getCdhParcelUrl(Image image) {
        Map<String, String> stack = image.getStackDetails().getRepo().getStack();
        String imageId = image.getUuid();
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

    private Set<String> getAllParcels(String cdhRepoUrl, Image image, Stack stack) {
        Set<String> requiredParcelNames = getRequiredParcelNames(stack, image);
        Set<String> parcelUrls = getPreWarmParcelUrls(image, requiredParcelNames);
        parcelUrls.addAll(getPreWarmCsdUrls(image, requiredParcelNames));
        parcelUrls.add(cdhRepoUrl);
        return parcelUrls;
    }

    private Set<String> getPreWarmParcelUrls(Image image, Set<String> requiredParcelNames) {
        return image.getPreWarmParcels()
                .stream()
                .filter(filterRequiredParcels(requiredParcelNames))
                .map(list -> removeUnnecessaryCharacters(getPreWarmParcelBaseUrl(list)).concat("/").concat(getPreWarmParcelName(list)))
                .collect(Collectors.toSet());
    }

    private Set<String> getPreWarmCsdUrls(Image image, Set<String> requiredParcelNames) {
        return image.getPreWarmCsd()
                .stream()
                .filter(csdUrl -> requiredParcelNames.stream().anyMatch(parcelName -> csdUrl.toLowerCase().contains(parcelName.toLowerCase())))
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

    private Predicate<List<String>> filterRequiredParcels(Set<String> requiredParcelNames) {
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
