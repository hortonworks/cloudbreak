package com.sequenceiq.distrox.v1.distrox.service.upgrade;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_2_12;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_2_7;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.common.model.UpgradeShowAvailableImages.LATEST_ONLY;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnParseException;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.RuntimeVersionService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.StackViewService;
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
    private ClusterService clusterService;

    @Inject
    private StackViewService stackViewService;

    @Inject
    private RuntimeVersionService runtimeVersionService;

    public boolean isRuntimeUpgradeEnabledByUserCrn(String userCrn) {
        return entitlementService.datahubRuntimeUpgradeEnabled(getAccountId(userCrn));
    }

    public boolean isRuntimeUpgradeEnabledByAccountId(String accountId) {
        return entitlementService.datahubRuntimeUpgradeEnabled(accountId);
    }

    public boolean isOsUpgradeEnabledByUserCrn(String userCrn) {
        return entitlementService.datahubOsUpgradeEnabled(getAccountId(userCrn));
    }

    public boolean isOsUpgradeEnabledByAccountId(String accountId) {
        return entitlementService.datahubOsUpgradeEnabled(accountId);
    }

    private String getAccountId(String userCrn) {
        try {
            return Crn.safeFromString(userCrn).getAccountId();
        } catch (NullPointerException | CrnParseException e) {
            LOGGER.warn("Can not parse CRN to find account ID: {}", userCrn, e);
            throw new BadRequestException("Can not parse CRN to find account ID: " + userCrn);
        }
    }

    public UpgradeV4Response checkForUpgrade(NameOrCrn nameOrCrn, Long workspaceId, UpgradeV4Request request, String userCrn) {
        Stack stack = stackService.getByNameOrCrnInWorkspace(nameOrCrn, workspaceId);
        String accountId = Crn.safeFromString(userCrn).getAccountId();
        UpgradeV4Response response = stackUpgradeOperations.checkForClusterUpgrade(accountId, stack, workspaceId, request);
        List<ImageInfoV4Response> filteredCandidates = filterCandidates(accountId, stack, request, response);
        List<ImageInfoV4Response> razValidatedCandidates = validateRangerRazCandidates(accountId, stack, filteredCandidates);
        response.setUpgradeCandidates(razValidatedCandidates);
        return response;
    }

    private List<ImageInfoV4Response> validateRangerRazCandidates(String accountId, Stack stack, List<ImageInfoV4Response> filteredCandidates) {
        if (isRuntimeUpgradeEnabledByAccountId(accountId)) {
            LOGGER.debug("Bypassing Data Hub upgrade Ranger RAZ constraint as CDP_RUNTIME_UPGRADE is enabled in [{}] account.", accountId);
            return filteredCandidates;
        }

        Stack datalakeStack = stackService.getByCrn(stack.getDatalakeCrn());
        Cluster datalakeCluster = clusterService.getCluster(datalakeStack);
        boolean rangerRazEnabled = datalakeCluster.isRangerRazEnabled();
        if (!rangerRazEnabled) {
            LOGGER.debug("Not a RAZ enabled cluster. Nothing to validate.");
            return filteredCandidates;
        }

        String runtimeVersion = runtimeVersionService.getRuntimeVersion(stack.getCluster().getId()).orElse("");
        boolean razRuntimeSupport = isVersionNewerOrEqualThanLimited(runtimeVersion, CLOUDERA_STACK_VERSION_7_2_12);
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
                datalakeCluster.getName(),
                runtimeVersion);
        LOGGER.debug(message);
        throw new BadRequestException(message);
    }

    private List<ImageInfoV4Response> filterCandidates(String accountId, Stack stack, UpgradeV4Request request, UpgradeV4Response upgradeV4Response) {
        filterOnlyPatchUpgradesIfRuntimeUpgradeDisabled(accountId, stack.getName(), upgradeV4Response);
        List<ImageInfoV4Response> filteredCandidates;
        String stackName = stack.getName();
        boolean differentDataHubAndDataLakeVersionAllowed = entitlementService.isDifferentDataHubAndDataLakeVersionAllowed(accountId);
        if (differentDataHubAndDataLakeVersionAllowed) {
            LOGGER.info("Different Data Hub version is enabled, not filtering based on Data Lake version, Data Hub: {}", stackName);
            filteredCandidates = upgradeV4Response.getUpgradeCandidates();
        } else {
            LOGGER.info("Filter Data Hub upgrade images based on the Data Lake version, Data Hub: {}", stackName);
            filteredCandidates = filterForDatalakeVersion(stack, upgradeV4Response);
        }
        if (CollectionUtils.isNotEmpty(filteredCandidates) && Objects.nonNull(request)) {
            if (LATEST_ONLY == request.getShowAvailableImages()) {
                filteredCandidates = filterForLatestImagePerRuntime(filteredCandidates);
            } else if (request.isDryRun()) {
                filteredCandidates = filterForLatestImage(filteredCandidates);
            }
        }
        return filteredCandidates;
    }

    private void filterOnlyPatchUpgradesIfRuntimeUpgradeDisabled(String accountId, String clusterName, UpgradeV4Response upgradeV4Response) {
        if (!isRuntimeUpgradeEnabledByAccountId(accountId)) {
            if (upgradeV4Response.getCurrent() != null) {
                List<ImageInfoV4Response> upgradeCandidates = upgradeV4Response.getUpgradeCandidates();
                String currentCdpVersion = upgradeV4Response.getCurrent().getComponentVersions().getCdp();
                LOGGER.debug("Only patch upgrade is possible on [{}] cluster for [{}] runtime as CDP_RUNTIME_UPGRADE is disabled in [{}] account.",
                        clusterName, currentCdpVersion, accountId);
                upgradeCandidates = upgradeCandidates.stream()
                        .filter(upgradeCandidate -> upgradeCandidate.getComponentVersions().getCdp().equals(currentCdpVersion))
                        .collect(Collectors.toList());
                upgradeV4Response.setUpgradeCandidates(upgradeCandidates);
                if (upgradeCandidates.isEmpty()) {
                    upgradeV4Response.appendReason(" No image is available for maintenance upgrade, CDP version: " + currentCdpVersion);
                }
                LOGGER.debug("Patch upgrade candidates for [{}] cluster: [{}]", clusterName, upgradeCandidates);
            } else {
                String message = String.format("No information about current image, cannot filter patch upgrade candidates based on it on [%s] cluster.",
                        clusterName);
                LOGGER.debug(message);
                upgradeV4Response.appendReason(message);
                upgradeV4Response.setUpgradeCandidates(List.of());
            }
        } else {
            LOGGER.debug("Data Hub Runtime upgrade is enabled, not filtering for patch upgrade.");
        }
    }

    private List<ImageInfoV4Response> filterForLatestImage(List<ImageInfoV4Response> candidates) {
        ImageInfoV4Response latestImage = candidates.stream().max(ImageInfoV4Response.creationBasedComparator()).orElseThrow();
        LOGGER.debug("Choosing latest image with id {} as dry-run is specified", latestImage.getImageId());
        return List.of(latestImage);
    }

    private List<ImageInfoV4Response> filterForLatestImagePerRuntime(List<ImageInfoV4Response> candidates) {
        Map<String, Optional<ImageInfoV4Response>> latestImageByRuntime = candidates.stream()
                .collect(Collectors.groupingBy(imageInfoV4Response -> imageInfoV4Response.getComponentVersions().getCdp(),
                        Collectors.maxBy(ImageInfoV4Response.creationBasedComparator())));
        List<ImageInfoV4Response> filteredCandidates = latestImageByRuntime.values()
                .stream()
                .flatMap(Optional::stream)
                .collect(Collectors.toList());
        LOGGER.debug("Filtering for latest image per runtimes {}", latestImageByRuntime.keySet());
        return filteredCandidates;
    }

    private List<ImageInfoV4Response> filterForDatalakeVersion(Stack stack, UpgradeV4Response upgradeV4Response) {
        List<ImageInfoV4Response> candidates = upgradeV4Response.getUpgradeCandidates();
        List<ImageInfoV4Response> result = candidates;
        if (CollectionUtils.isNotEmpty(candidates)) {
            Optional<StackView> datalakeViewOpt = stackViewService.findDatalakeViewByEnvironmentCrn(stack.getEnvironmentCrn());
            if (datalakeViewOpt.isPresent()) {
                Optional<String> datalakeVersionOpt = runtimeVersionService.getRuntimeVersion(datalakeViewOpt.get().getClusterView().getId());
                if (datalakeVersionOpt.isPresent()) {
                    String dlVersion = datalakeVersionOpt.get();
                    result = filterForDatalakeVersion(dlVersion, candidates);
                    if (result.isEmpty() && !candidates.isEmpty()) {
                        upgradeV4Response.setReason(String.format("Data Hub can only be upgraded to the same version as the Data Lake (%s)."
                                + " To upgrade your Data Hub, please upgrade your Data Lake first.", dlVersion));
                    }
                }
            }
        }
        return result;
    }

    private List<ImageInfoV4Response> filterForDatalakeVersion(String datalakeVersion, List<ImageInfoV4Response> candidates) {
        return candidates.stream().filter(imageInfo -> imageInfo.getComponentVersions().getCdp().equals(datalakeVersion)).collect(Collectors.toList());
    }

}
