package com.sequenceiq.distrox.v1.distrox.service.upgrade;

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
import com.sequenceiq.distrox.v1.distrox.StackOperations;

@Service
public class DistroXUpgradeAvailabilityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXUpgradeAvailabilityService.class);

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private StackOperations stackOperations;

    @Inject
    private StackService stackService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private StackViewService stackViewService;

    @Inject
    private RuntimeVersionService runtimeVersionService;

    public boolean isRuntimeUpgradeEnabledByUserCrn(String userCrn) {
        try {
            String accountId = Crn.safeFromString(userCrn).getAccountId();
            return entitlementService.datahubRuntimeUpgradeEnabled(accountId);
        } catch (NullPointerException | CrnParseException e) {
            LOGGER.warn("Can not parse CRN to find account ID: {}", userCrn, e);
            throw new BadRequestException("Can not parse CRN to find account ID: " + userCrn);
        }
    }

    public boolean isRuntimeUpgradeEnabledByAccountId(String accountId) {
        return entitlementService.datahubRuntimeUpgradeEnabled(accountId);
    }

    public UpgradeV4Response checkForUpgrade(NameOrCrn nameOrCrn, Long workspaceId, UpgradeV4Request request, String userCrn) {
        Stack stack = stackService.getByNameOrCrnInWorkspace(nameOrCrn, workspaceId);
        String accountId = Crn.safeFromString(userCrn).getAccountId();
        UpgradeV4Response response = stackOperations.checkForClusterUpgrade(accountId, stack, workspaceId, request);
        validateRangerRaz(accountId, stack);
        List<ImageInfoV4Response> filteredCandidates = filterCandidates(accountId, stack, request, response);
        response.setUpgradeCandidates(filteredCandidates);
        return response;
    }

    private void validateRangerRaz(String accountId, Stack stack) {
        if (!isRuntimeUpgradeEnabledByAccountId(accountId)) {
            Stack datalakeStack = stackService.getByCrn(stack.getDatalakeCrn());
            Cluster datalakeCluster = clusterService.getCluster(datalakeStack);
            boolean rangerRazEnabled = datalakeCluster.isRangerRazEnabled();
            String message = String.format(
                    "Data Hub Upgrade is %s as Ranger RAZ is %s for [%s] cluster.",
                    rangerRazEnabled ? "not allowed" : "allowed",
                    rangerRazEnabled ? "enabled" : "disabled",
                    datalakeCluster.getName());
            LOGGER.debug(message);
            if (rangerRazEnabled) {
                throw new BadRequestException(message);
            }
        } else {
            LOGGER.debug("Bypassing Data Hub upgrade Ranger RAZ constraint as CDP_RUNTIME_UPGRADE is enabled in [{}] account.", accountId);
        }
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
                    upgradeV4Response.setReason("No image is available for maintenance upgrade, CDP version: " + currentCdpVersion);
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
