package com.sequenceiq.distrox.v1.distrox.service.upgrade;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_2_10;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_2_7;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.common.model.UpgradeShowAvailableImages.LATEST_ONLY;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageComponentVersions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.service.image.CurrentImageUsageCondition;
import com.sequenceiq.cloudbreak.service.stack.RuntimeVersionService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeAvailabilityService;
import com.sequenceiq.distrox.v1.distrox.StackUpgradeOperations;

@Service
public class DistroXUpgradeAvailabilityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXUpgradeAvailabilityService.class);

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private StackUpgradeOperations stackUpgradeOperations;

    @Inject
    private StackService stackService;

    @Inject
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    @Inject
    private CurrentImageUsageCondition currentImageUsageCondition;

    @Inject
    private DistroXUpgradeResponseFilterService distroXUpgradeResponseFilterService;

    @Inject
    private RuntimeVersionService runtimeVersionService;

    @Inject
    private ClusterUpgradeAvailabilityService clusterUpgradeAvailabilityService;

    public void validateUpgradeWithComponents(StackDtoDelegate stack, UpgradeV4Request request, String runtime, boolean replaceVms, String userCrn) {
        clusterUpgradeAvailabilityService.validateUpgradePreconditions(stack, replaceVms, request.getInternalUpgradeSettings());
        String currentRuntime = runtimeVersionService.getRuntimeVersion(stack.getCluster().getId()).orElse("");
        UpgradeV4Response response = new UpgradeV4Response(runtimeImage(currentRuntime), List.of(runtimeImage(runtime)), null);
        List<ImageInfoV4Response> candidates = filterCandidates(Crn.safeFromString(userCrn).getAccountId(), stack, request, response);
        if (validateRangerRazCandidates(stack, candidates).isEmpty()) {
            throw new BadRequestException(StringUtils.defaultIfBlank(response.getReason(), "The requested runtime is not allowed for this Data Hub."));
        }
    }

    private ImageInfoV4Response runtimeImage(String runtime) {
        ImageComponentVersions versions = new ImageComponentVersions();
        versions.setCdp(runtime);
        ImageInfoV4Response image = new ImageInfoV4Response();
        image.setComponentVersions(versions);
        return image;
    }

    public UpgradeV4Response checkForUpgrade(NameOrCrn nameOrCrn, Long workspaceId, UpgradeV4Request request, String userCrn) {
        Stack stack = stackService.getByNameOrCrnInWorkspace(nameOrCrn, workspaceId);
        String accountId = Crn.safeFromString(userCrn).getAccountId();
        UpgradeV4Response response = stackUpgradeOperations.checkForClusterUpgrade(accountId, stack, request);
        List<ImageInfoV4Response> filteredCandidates = filterCandidates(accountId, stack, request, response);
        filteredCandidates = addOsUpgradeOptionIfAvailable(stack, response, filteredCandidates);
        List<ImageInfoV4Response> razValidatedCandidates = validateRangerRazCandidates(stack, filteredCandidates);
        response.setUpgradeCandidates(razValidatedCandidates);
        return response;
    }

    private List<ImageInfoV4Response> addOsUpgradeOptionIfAvailable(Stack stack, UpgradeV4Response response, List<ImageInfoV4Response> filteredCandidates) {
        ImageInfoV4Response currentImage = response.getCurrent();
        boolean notContainsCurrentImage = filteredCandidates.stream().noneMatch(candidate -> candidate.getImageId().equals(currentImage.getImageId()));
        if (notContainsCurrentImage && !currentImageUsageCondition.isCurrentImageUsedOnInstances(stack.getId(), currentImage.getImageName(),
                currentImage.getImageId())) {
            LOGGER.debug("Adding the current image for image candidates to offer OS upgrade. Current image id: {}", currentImage.getImageId());
            filteredCandidates = new ArrayList<>(filteredCandidates);
            filteredCandidates.add(currentImage);
        }
        return filteredCandidates;
    }

    private List<ImageInfoV4Response> validateRangerRazCandidates(StackDtoDelegate stack, List<ImageInfoV4Response> filteredCandidates) {
        SdxBasicView sdxBasicView = platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(stack.getEnvironmentCrn()).orElseThrow();
        boolean rangerRazEnabled = sdxBasicView.razEnabled();
        if (!rangerRazEnabled) {
            LOGGER.debug("Not a RAZ enabled cluster. Nothing to validate.");
            return filteredCandidates;
        }

        String runtimeVersion = runtimeVersionService.getRuntimeVersion(stack.getCluster().getId()).orElse("");
        boolean razRuntimeSupport = isVersionNewerOrEqualThanLimited(runtimeVersion, CLOUDERA_STACK_VERSION_7_2_10);
        if (razRuntimeSupport) {
            LOGGER.debug("Runtime version [{}] is supported for RAZ cluster upgrades", runtimeVersion);
            return filteredCandidates;
        }

        boolean razPatchSupport = isVersionNewerOrEqualThanLimited(runtimeVersion, CLOUDERA_STACK_VERSION_7_2_7);
        if (razPatchSupport) {
            LOGGER.debug("DataHub patch upgrade is enabled for Ranger RAZ clusters with stack version 7.2.7 or newer.");
            return filteredCandidates.stream()
                    .filter(imgInfo -> runtimeVersion.equals(imgInfo.getComponentVersions().getCdp()))
                    .collect(Collectors.toList());
        }

        String message = String.format(
                "Data Hub Upgrade is not allowed as Ranger RAZ is enabled for [%s] cluster, because runtime version is [%s].",
                sdxBasicView.name(),
                runtimeVersion);
        LOGGER.debug(message);
        throw new BadRequestException(message);
    }

    private List<ImageInfoV4Response> filterCandidates(String accountId, StackDtoDelegate stack, UpgradeV4Request request, UpgradeV4Response upgradeV4Response) {
        List<ImageInfoV4Response> filteredCandidates;
        String stackName = stack.getName();
        boolean differentDataHubAndDataLakeVersionAllowed = entitlementService.isDifferentDataHubAndDataLakeVersionAllowed(accountId);
        if (differentDataHubAndDataLakeVersionAllowed) {
            LOGGER.info("Different Data Hub version is enabled, not filtering based on Data Lake version, Data Hub: {}", stackName);
            filteredCandidates = upgradeV4Response.getUpgradeCandidates();
        } else {
            LOGGER.info("Filter Data Hub upgrade images based on the Data Lake version, Data Hub: {}", stackName);
            filteredCandidates = distroXUpgradeResponseFilterService.filterForDatalakeVersion(stack.getEnvironmentCrn(), upgradeV4Response);
            if (filteredCandidates.isEmpty() && !upgradeV4Response.getUpgradeCandidates().isEmpty()) {
                upgradeV4Response.setReason("Data Hub can only be upgraded to the same version as the Data Lake."
                        + " To upgrade your Data Hub, please upgrade your Data Lake first.");
            }
        }
        if (CollectionUtils.isNotEmpty(filteredCandidates) && Objects.nonNull(request)) {
            if (LATEST_ONLY == request.getShowAvailableImages()) {
                filteredCandidates = distroXUpgradeResponseFilterService.filterForLatestImagePerRuntimeAndOs(filteredCandidates, upgradeV4Response.getCurrent());
            } else if (request.isDryRun()) {
                filteredCandidates = filterForLatestImage(filteredCandidates);
            }
        }
        return filteredCandidates;
    }

    private List<ImageInfoV4Response> filterForLatestImage(List<ImageInfoV4Response> candidates) {
        ImageInfoV4Response latestImage = candidates.stream().max(ImageInfoV4Response.defaultUpgradeCandidateComparator()).orElseThrow();
        LOGGER.debug("Choosing latest image with id {} as dry-run is specified", latestImage.getImageId());
        return List.of(latestImage);
    }

}
