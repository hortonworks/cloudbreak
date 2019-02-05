package com.sequenceiq.cloudbreak.service.stack;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;

@Component
public class SharedServiceValidator {

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private StackViewService stackViewService;

    public ValidationResult checkSharedServiceStackRequirements(StackV4Request request, Workspace workspace) {
        ValidationResult.ValidationResultBuilder resultBuilder = ValidationResult.builder();
        if (request.getSharedService() != null) {
            checkCloudPlatform(request, workspace.getId(), resultBuilder);
            checkSharedServiceRequirements(request, workspace, resultBuilder);
        }
        return resultBuilder.build();
    }

    private void checkCloudPlatform(StackV4Request request, Long workspaceId, ValidationResult.ValidationResultBuilder resultBuilder) {
        StackView datalakeStack = stackViewService.findByName(request.getSharedService().getDatalakeName(), workspaceId);
        if (datalakeStack == null) {
            resultBuilder.error("Datalake stack with the requested name (in sharedService/sharedClusterName field) was not found.");
        } else {
            CloudPlatform requestedCloudPlatform = request.getCloudPlatform();
            String datalakeCloudPlatform = datalakeStack.cloudPlatform();
            if (!datalakeCloudPlatform.equals(requestedCloudPlatform.name())) {
                resultBuilder.error(String.format("Requested cloud platform [%s] does not match with the datalake"
                        + " cluser's cloud platform [%s].", requestedCloudPlatform, datalakeCloudPlatform));
            }
        }
    }

    private void checkSharedServiceRequirements(StackV4Request request, Workspace workspace, ValidationResult.ValidationResultBuilder resultBuilder) {
        if (!hasConfiguredLdap(request)) {
            resultBuilder.error("Shared service stack should have LDAP configured.");
        }
        if (!hasConfiguredRdsByType(request, workspace, DatabaseType.HIVE)) {
            resultBuilder.error("Shared service stack should have HIVE database configured.");

        }
        if (!hasConfiguredRdsByType(request, workspace, DatabaseType.RANGER)) {
            resultBuilder.error("Shared service stack should have RANGER database configured.");
        }
    }

    private boolean hasConfiguredLdap(StackV4Request request) {
        return StringUtils.isNotEmpty(request.getCluster().getLdapName());
    }

    private boolean hasConfiguredRdsByType(StackV4Request request, Workspace workspace, DatabaseType rdsType) {
        boolean hasConfiguredRds = false;
        if (!request.getCluster().getDatabases().isEmpty()) {
            for (String rds : request.getCluster().getDatabases()) {
                RDSConfig database = rdsConfigService.getByNameForWorkspace(rds, workspace);
                if (database != null && rdsType.name().equalsIgnoreCase(database.getType())) {
                    hasConfiguredRds = true;
                    break;
                }
            }
        }
        return hasConfiguredRds;
    }
}
