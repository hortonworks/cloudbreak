package com.sequenceiq.environment.environment.v1;

import jakarta.transaction.Transactional;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareParam;
import com.sequenceiq.cloudbreak.structuredevent.rest.annotation.AccountEntityType;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentDefaultComputeClusterEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.request.ExternalizedComputeCreateRequest;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.ExternalizedComputeClusterDto;
import com.sequenceiq.environment.environment.service.EnvironmentModificationService;
import com.sequenceiq.environment.environment.service.externalizedcompute.ExternalizedComputeFlowService;
import com.sequenceiq.environment.environment.v1.converter.EnvironmentApiConverter;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Controller
@Transactional(Transactional.TxType.NEVER)
@AccountEntityType(Environment.class)
public class EnvironmentDefaultComputeClusterController implements EnvironmentDefaultComputeClusterEndpoint {

    private final EnvironmentApiConverter environmentApiConverter;

    private final ExternalizedComputeFlowService externalizedComputeFlowService;

    private final EnvironmentModificationService environmentModificationService;

    public EnvironmentDefaultComputeClusterController(
            EnvironmentApiConverter environmentApiConverter,
            ExternalizedComputeFlowService externalizedComputeFlowService,
            EnvironmentModificationService environmentModificationService) {
        this.environmentApiConverter = environmentApiConverter;
        this.externalizedComputeFlowService = externalizedComputeFlowService;
        this.environmentModificationService = environmentModificationService;
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.EDIT_ENVIRONMENT)
    public FlowIdentifier createDefaultExternalizedComputeCluster(@ResourceCrn @TenantAwareParam String crn,
            ExternalizedComputeCreateRequest request, boolean force) {
        return triggerInitializeFlow(crn, request, force);
    }

    /**
     * It will be removed because I realized it is unnecessary and only complicates things. The create endpoint can be used instead.
     *
     * @deprecated use {@link #createDefaultExternalizedComputeCluster(String, ExternalizedComputeCreateRequest, boolean)} instead.
     */
    @Deprecated
    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.EDIT_ENVIRONMENT)
    public FlowIdentifier reinitializeDefaultExternalizedComputeCluster(@ResourceCrn @TenantAwareParam String crn,
            ExternalizedComputeCreateRequest request, boolean force) {
        return triggerInitializeFlow(crn, request, force);
    }

    private FlowIdentifier triggerInitializeFlow(String crn, ExternalizedComputeCreateRequest request, boolean force) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Environment environment = environmentModificationService.getEnvironment(accountId, NameOrCrn.ofCrn(crn));
        ExternalizedComputeClusterDto externalizedComputeClusterDto = environmentApiConverter.requestToExternalizedComputeClusterDto(request, accountId);
        return externalizedComputeFlowService.initializeDefaultExternalizedComputeCluster(environment, externalizedComputeClusterDto, force);
    }

}
