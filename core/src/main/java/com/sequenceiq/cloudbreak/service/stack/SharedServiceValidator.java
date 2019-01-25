package com.sequenceiq.cloudbreak.service.stack;

import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.rds.RdsType;
import com.sequenceiq.cloudbreak.api.model.stack.StackRequest;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;

@Component
public class SharedServiceValidator {

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private StackViewService stackViewService;

    public ValidationResult checkSharedServiceStackRequirements(StackRequest request, Workspace workspace) {
        ValidationResultBuilder resultBuilder = ValidationResult.builder();
        if (request.getClusterToAttach() != null) {
            checkCloudPlatform(request, resultBuilder);
            checkSharedServiceRequirements(request, workspace, resultBuilder);
        }
        return resultBuilder.build();
    }

    private void checkCloudPlatform(StackRequest request, ValidationResultBuilder resultBuilder) {
        Optional<StackView> datalakeStack = stackViewService.findById(request.getClusterToAttach());
        if (!datalakeStack.isPresent()) {
            resultBuilder.error("Datalake stack with the requested ID (in clusterToAttach field) was not found.");
        } else {
            String requestedCloudPlatform = request.getCloudPlatform();
            String datalakeCloudPlatform = datalakeStack.get().cloudPlatform();
            if (!datalakeCloudPlatform.equals(requestedCloudPlatform)) {
                resultBuilder.error(String.format("Requested cloud platform [%s] does not match with the datalake"
                        + " cluser's cloud platform [%s].", requestedCloudPlatform, datalakeCloudPlatform));
            }
        }
    }

    private void checkSharedServiceRequirements(StackRequest request, Workspace workspace, ValidationResultBuilder resultBuilder) {
        if (!hasConfiguredLdap(request)) {
            resultBuilder.error("Shared service stack should have LDAP configured.");
        }
        if (!hasConfiguredRdsByType(request, workspace, RdsType.HIVE)) {
            resultBuilder.error("Shared service stack should have HIVE database configured.");

        }
        if (!hasConfiguredRdsByType(request, workspace, RdsType.RANGER)) {
            resultBuilder.error("Shared service stack should have RANGER database configured.");
        }
    }

    private boolean hasConfiguredLdap(StackRequest request) {
        return request.getClusterRequest().getLdapConfig() != null
                || StringUtils.isNotEmpty(request.getClusterRequest().getLdapConfigName())
                || request.getClusterRequest().getLdapConfigId() != null;
    }

    private boolean hasConfiguredRdsByType(StackRequest request, Workspace workspace, RdsType rdsType) {
        boolean hasConfiguredRds = false;
        if (!request.getClusterRequest().getRdsConfigJsons().isEmpty()) {
            hasConfiguredRds = request.getClusterRequest().getRdsConfigJsons().stream()
                    .anyMatch(rdsConfigRequest -> rdsType.name().equalsIgnoreCase(rdsConfigRequest.getType()));
        }
        if (!hasConfiguredRds && !request.getClusterRequest().getRdsConfigNames().isEmpty()) {
            for (String rds : request.getClusterRequest().getRdsConfigNames()) {
                if (rdsType.name().equalsIgnoreCase(rdsConfigService.getByNameForWorkspace(rds, workspace).getType())) {
                    hasConfiguredRds = true;
                    break;
                }
            }
        }
        if (!hasConfiguredRds && !request.getClusterRequest().getRdsConfigNames().isEmpty()) {
            for (Long rds : request.getClusterRequest().getRdsConfigIds()) {
                if (rdsType.name().equalsIgnoreCase(rdsConfigService.get(rds).getType())) {
                    hasConfiguredRds = true;
                    break;
                }
            }
        }
        return hasConfiguredRds;
    }

}
