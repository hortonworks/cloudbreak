package com.sequenceiq.distrox.v1.distrox.service.upgrade;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;

import java.util.List;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Account;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackImageChangeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.auth.CMLicenseParser;
import com.sequenceiq.cloudbreak.auth.JsonCMLicense;
import com.sequenceiq.cloudbreak.auth.PaywallAccessChecker;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.logger.MDCUtils;
import com.sequenceiq.cloudbreak.service.StackCommonService;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class DistroxUpgradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroxUpgradeService.class);

    @Value("${cb.paywall.url}")
    private String paywallUrl;

    @Inject
    private DistroxUpgradeAvailabilityService upgradeAvailabilityService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private GrpcUmsClient umsClient;

    @Inject
    private PaywallAccessChecker paywallAccessChecker;

    @Inject
    private DistroxUpgradeImageSelector imageSelector;

    @Inject
    private StackCommonService stackCommonService;

    @Inject
    private ReactorFlowManager reactorFlowManager;

    @Inject
    private StackService stackService;

    @Inject
    private CMLicenseParser cmLicenseParser;

    public UpgradeV4Response triggerUpgrade(NameOrCrn cluster, Long workspaceId, String userCrn, UpgradeV4Request request) {
        UpgradeV4Response upgradeV4Response = upgradeAvailabilityService.checkForUpgrade(cluster, workspaceId, request, userCrn);
        validateUpgradeCandidates(cluster, upgradeV4Response);
        verifyPaywallAccess(userCrn, request);
        return initUpgrade(request, upgradeV4Response.getUpgradeCandidates(), cluster, workspaceId);
    }

    private UpgradeV4Response initUpgrade(UpgradeV4Request request, List<ImageInfoV4Response> upgradeCandidates, NameOrCrn cluster, Long workspaceId) {
        ImageInfoV4Response image = imageSelector.determineImageId(request, upgradeCandidates);
        ImageChangeDto imageChangeDto = createImageChangeDto(cluster, workspaceId, image);
        Long stackId = stackService.getIdByNameOrCrnInWorkspace(cluster, workspaceId);
        FlowIdentifier flowIdentifier = reactorFlowManager.triggerDistroxUpgrade(stackId, imageChangeDto, request.getReplaceVms());
        return new UpgradeV4Response("Upgrade started with Image: " + image.getImageId(), flowIdentifier);
    }

    private ImageChangeDto createImageChangeDto(NameOrCrn cluster, Long workspaceId, ImageInfoV4Response image) {
        StackImageChangeV4Request stackImageChangeRequest = new StackImageChangeV4Request();
        stackImageChangeRequest.setImageId(image.getImageId());
        stackImageChangeRequest.setImageCatalogName(image.getImageCatalogName());
        return stackCommonService.createImageChangeDto(cluster, workspaceId, stackImageChangeRequest);
    }

    private void validateUpgradeCandidates(NameOrCrn cluster, UpgradeV4Response upgradeResponse) {
        if (StringUtils.isNotEmpty(upgradeResponse.getReason())) {
            throw new BadRequestException(String.format("The following error prevents the cluster upgrade process, please fix it and try again: %s",
                    upgradeResponse.getReason()));
        } else if (CollectionUtils.isEmpty(upgradeResponse.getUpgradeCandidates())) {
            throw new BadRequestException(String.format("There is no compatible image to upgrade for stack %s", cluster.getNameOrCrn()));
        }
    }

    private void verifyPaywallAccess(String userCrn, UpgradeV4Request upgradeRequest) {
        if (upgradeRequest != null && !Boolean.TRUE.equals(upgradeRequest.getLockComponents())) {
            if (!isInternalRepoAllowedForUpgrade(userCrn)) {
                verifyCMLicenseValidity(userCrn);
            } else {
                LOGGER.info("Internal repo is allowed for upgrade, skip CM license validation");
            }
        }
    }

    private boolean isInternalRepoAllowedForUpgrade(String userCrn) {
        String accountId = Crn.safeFromString(userCrn).getAccountId();
        return entitlementService.isInternalRepositoryForUpgradeAllowed(accountId);
    }

    private void verifyCMLicenseValidity(String userCrn) {
        LOGGER.info("Verify if the CM license is valid to authenticate to {}", paywallUrl);
        String accountId = Crn.safeFromString(userCrn).getAccountId();
        Account account = umsClient.getAccountDetails(INTERNAL_ACTOR_CRN, accountId, MDCUtils.getRequestId());
        JsonCMLicense license = cmLicenseParser.parseLicense(account.getClouderaManagerLicenseKey())
                .orElseThrow(() -> new BadRequestException("No valid CM license is present"));
        paywallAccessChecker.checkPaywallAccess(license, paywallUrl);
    }
}
