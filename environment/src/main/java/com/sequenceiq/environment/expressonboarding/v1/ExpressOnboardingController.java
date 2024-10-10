package com.sequenceiq.environment.expressonboarding.v1;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.exception.UnauthorizedException;
import com.sequenceiq.environment.api.v1.expressonboarding.ExpressOnboardingEndpoint;
import com.sequenceiq.environment.api.v1.expressonboarding.model.response.DetailedExpressOnboardingRegionResponse;
import com.sequenceiq.environment.expressonboarding.controller.DeploymentInformationResponseConverter;
import com.sequenceiq.environment.expressonboarding.controller.ExpressOnboardingRegionsResponseConverter;
import com.sequenceiq.environment.expressonboarding.controller.TenantInformationResponseConverter;

@Controller
public class ExpressOnboardingController implements ExpressOnboardingEndpoint {

    @Inject
    private DeploymentInformationResponseConverter deploymentInformationResponseConverter;

    @Inject
    private TenantInformationResponseConverter tenantInformationResponseConverter;

    @Inject
    private ExpressOnboardingRegionsResponseConverter expressOnboardingRegionsResponseConverter;

    @Inject
    private EntitlementService entitlementService;

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public DetailedExpressOnboardingRegionResponse get() {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();

        if (entitlementService.isExpressOnboardingEnabled(accountId)) {
            DetailedExpressOnboardingRegionResponse detailedExpressOnboardingRegionResponse = new DetailedExpressOnboardingRegionResponse();

            detailedExpressOnboardingRegionResponse.setTenantInformation(tenantInformationResponseConverter.tenantInformationResponse());
            detailedExpressOnboardingRegionResponse.setDeploymentInformation(deploymentInformationResponseConverter.deploymentInformationResponse());
            detailedExpressOnboardingRegionResponse.setCloudProviders(expressOnboardingRegionsResponseConverter.expressOnboardingRegionsResponse());

            return detailedExpressOnboardingRegionResponse;
        } else {
            throw new UnauthorizedException("The 'CDP_EXPRESS_ONBOARDING' not granted to your tenant. Please contact the administrator.");
        }

    }
}
