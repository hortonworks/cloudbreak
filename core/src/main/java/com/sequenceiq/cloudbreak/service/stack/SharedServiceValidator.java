package com.sequenceiq.cloudbreak.service.stack;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.ldap.LdapConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@Component
public class SharedServiceValidator {

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    @Qualifier("stackViewServiceDeprecated")
    private StackViewService stackViewService;

    @Inject
    private LdapConfigService ldapConfigService;

    public ValidationResult checkSharedServiceStackRequirements(StackV4Request request, Workspace workspace) {
        ValidationResultBuilder resultBuilder = ValidationResult.builder();
        if (request.getSharedService() != null) {
            Long workspaceId = workspace.getId();
            checkCloudPlatform(request, workspaceId, resultBuilder);
        }
        return resultBuilder.build();
    }

    private void checkCloudPlatform(StackV4Request request, Long workspaceId, ValidationResultBuilder resultBuilder) {
        Optional<StackView> datalakeStack = stackViewService.findByName(request.getSharedService().getDatalakeName(), workspaceId);
        if (datalakeStack.isEmpty()) {
            resultBuilder.error("Datalake stack with the requested name (in sharedService/sharedClusterName field) was not found.");
        } else {
            CloudPlatform requestedCloudPlatform = request.getCloudPlatform();
            String datalakeCloudPlatform = datalakeStack.get().cloudPlatform();
            if (!datalakeCloudPlatform.equals(requestedCloudPlatform.name())) {
                resultBuilder.error(String.format("Requested cloud platform [%s] does not match with the datalake"
                        + " cluser's cloud platform [%s].", requestedCloudPlatform, datalakeCloudPlatform));
            }
        }
    }
}
