package com.sequenceiq.environment.environment.v1;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentInternalEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.PolicyValidationErrorResponses;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.v1.converter.EnvironmentResponseConverter;
import com.sequenceiq.environment.parameters.service.ParametersService;
import com.sequenceiq.notification.WebSocketNotificationController;
import com.sequenceiq.notification.domain.DistributionList;
import com.sequenceiq.notification.domain.NotificationGroupType;
import com.sequenceiq.notification.sender.DistributionListManagementService;

@Controller
public class EnvironmentInternalV1Controller extends WebSocketNotificationController implements EnvironmentInternalEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentInternalV1Controller.class);

    private final CredentialService credentialService;

    private final EnvironmentService environmentService;

    private final EnvironmentResponseConverter environmentResponseConverter;

    private final DistributionListManagementService distributionListManagementService;

    private final ParametersService parametersService;

    public EnvironmentInternalV1Controller(
            CredentialService credentialService,
            EnvironmentService environmentService,
            EnvironmentResponseConverter environmentResponseConverter,
            DistributionListManagementService distributionListManagementService,
            ParametersService parametersService) {
        this.credentialService = credentialService;
        this.environmentService = environmentService;
        this.environmentResponseConverter = environmentResponseConverter;
        this.distributionListManagementService = distributionListManagementService;
        this.parametersService = parametersService;
    }

    @Override
    @InternalOnly
    public PolicyValidationErrorResponses policyValidationByEnvironmentCrn(@ResourceCrn String crn, List<String> services) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return credentialService.validatePolicy(accountId, crn, services);
    }

    @Override
    @InternalOnly
    public SimpleEnvironmentResponse internalGetByCrn(@ResourceCrn String crn, boolean withNetwork) {
        EnvironmentDto environmentDto = environmentService.internalGetByCrn(crn);
        return environmentResponseConverter.dtoToSimpleResponse(environmentDto, withNetwork, false);
    }

    @Override
    @InternalOnly
    public void createOrUpdateDistributionListByEnvironmentCrn(@ResourceCrn String crn) {
        EnvironmentDto environmentDto = environmentService.internalGetByCrn(crn);
        Optional<DistributionList> distributionList = distributionListManagementService.createOrUpdateList(
                environmentDto.getResourceCrn(),
                environmentDto.getResourceName(),
                NotificationGroupType.ENVIRONMENT
        );
        distributionList.ifPresent(list ->
                parametersService.updateDistributionListDetails(environmentDto.getId(), list));
    }

}
