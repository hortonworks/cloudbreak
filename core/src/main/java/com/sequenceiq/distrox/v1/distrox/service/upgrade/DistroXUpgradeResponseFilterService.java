package com.sequenceiq.distrox.v1.distrox.service.upgrade;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageComponentVersions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.service.stack.RuntimeVersionService;
import com.sequenceiq.cloudbreak.service.stack.StackViewService;
import com.sequenceiq.cloudbreak.service.upgrade.ImageComponentVersionsComparator;

@Component
public class DistroXUpgradeResponseFilterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXUpgradeResponseFilterService.class);

    @Inject
    @Qualifier("stackViewServiceDeprecated")
    private StackViewService stackViewService;

    @Inject
    private RuntimeVersionService runtimeVersionService;

    @Inject
    private ImageComponentVersionsComparator imageComponentVersionsComparator;

    public List<ImageInfoV4Response> filterForLatestImagePerRuntimeAndOs(List<ImageInfoV4Response> candidates, ImageInfoV4Response currentImage) {
        LOGGER.debug("Selecting the latest images by runtime and OS from upgrade candidates {}", candidates);
        Map<String, Map<String, Optional<ImageInfoV4Response>>> imagesByRuntimeAndOS = getImagesByRuntimeAndOS(candidates);
        List<ImageInfoV4Response> filteredCandidates = extractImagesMap(imagesByRuntimeAndOS);
        String currentOs = currentImage.getComponentVersions().getOs();
        if (hasCandidateAvailableWithDifferentOs(currentOs, filteredCandidates)) {
            return filterRuntimeUpgradeCandidatesWithSameVersionAndDifferentOs(currentOs, currentImage.getComponentVersions(), filteredCandidates);
        } else {
            return filteredCandidates;
        }
    }

    private Map<String, Map<String, Optional<ImageInfoV4Response>>> getImagesByRuntimeAndOS(List<ImageInfoV4Response> candidates) {
        return candidates.stream()
                .collect(Collectors.groupingBy(imageInfoV4Response -> imageInfoV4Response.getComponentVersions().getCdp(),
                        Collectors.groupingBy(imageInfoV4Response -> imageInfoV4Response.getComponentVersions().getOs(),
                                Collectors.maxBy(Comparator.comparingLong(ImageInfoV4Response::getCreated)))));
    }

    private boolean hasCandidateAvailableWithDifferentOs(String currentOs, List<ImageInfoV4Response> filteredCandidates) {
        return filteredCandidates.stream().anyMatch(image -> !image.getComponentVersions().getOs().equals(currentOs));
    }

    private List<ImageInfoV4Response> extractImagesMap(Map<String, Map<String, Optional<ImageInfoV4Response>>> imagesByRuntimeAndOS) {
        return imagesByRuntimeAndOS.values().stream()
                .map(values -> values.values().stream()
                        .flatMap(Optional::stream)
                        .toList())
                .flatMap(List::stream)
                .toList();
    }

    private List<ImageInfoV4Response> filterRuntimeUpgradeCandidatesWithSameVersionAndDifferentOs(String currentOs,
            ImageComponentVersions currentComponentVersions, List<ImageInfoV4Response> candidates) {
        return candidates.stream().filter(candidateImage -> candidateImage.getComponentVersions().getOs().equals(currentOs) ||
                        imageComponentVersionsComparator.containsSamePackages(currentComponentVersions, candidateImage.getComponentVersions()) ||
                        noImagesAvailableWithTheSamePackages(candidates, candidateImage))
                .toList();
    }

    private boolean noImagesAvailableWithTheSamePackages(List<ImageInfoV4Response> candidates, ImageInfoV4Response candidateImage) {
        return candidates.stream().noneMatch(image -> !candidateImage.getComponentVersions().getOs().equals(image.getComponentVersions().getOs()) &&
                imageComponentVersionsComparator.containsSamePackages(candidateImage.getComponentVersions(), image.getComponentVersions()));
    }

    public List<ImageInfoV4Response> filterForDatalakeVersion(String environmentCrn, UpgradeV4Response upgradeV4Response) {
        List<ImageInfoV4Response> candidates = upgradeV4Response.getUpgradeCandidates();
        List<ImageInfoV4Response> result = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(candidates)) {
            Optional<StackView> datalakeViewOpt = stackViewService.findDatalakeViewByEnvironmentCrn(environmentCrn);
            if (datalakeViewOpt.isPresent()) {
                Optional<String> datalakeVersionOpt = runtimeVersionService.getRuntimeVersion(datalakeViewOpt.get().getClusterView().getId());
                datalakeVersionOpt.ifPresentOrElse(datalakeVersion ->
                        result.addAll(filterForDatalakeVersion(datalakeVersion, upgradeV4Response.getCurrent().getComponentVersions().getCdp(), candidates)),
                        () -> result.addAll(candidates));
            } else {
                LOGGER.debug("DataLake stack not found for environment {}", environmentCrn);
                return candidates;
            }
        }
        return result;
    }

    private List<ImageInfoV4Response> filterForDatalakeVersion(String datalakeVersion, String currentDataHubRuntimeVersion,
            List<ImageInfoV4Response> candidates) {
        return candidates.stream()
                .filter(imageInfo -> imageInfo.getComponentVersions().getCdp().equals(datalakeVersion)
                        || imageInfo.getComponentVersions().getCdp().equals(currentDataHubRuntimeVersion))
                .collect(Collectors.toList());
    }
}
