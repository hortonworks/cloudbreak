package com.sequenceiq.cloudbreak.service.upgrade.sync.template;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@Service
public class ClusterManagerTemplateSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterManagerTemplateSyncService.class);

    @Inject
    private ClusterApiConnectors apiConnectors;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private StackDtoService stackService;

    public void sync(Long stackId) {
        Stack stack = stackService.getStackReferenceById(stackId);
        if (entitlementService.isCdpCBCmTemplateSyncEnabled(Crn.fromString(stack.getResourceCrn()).getAccountId())) {
            String currentDeploymentTemplate = getDeployment(stack);
            if (StringUtils.isNotBlank(currentDeploymentTemplate)) {
                LOGGER.info("Persisting template for stack {} with deployment {}", stack.getName(), currentDeploymentTemplate);
                clusterService.updateExtendedBlueprintText(stack.getCluster().getId(), currentDeploymentTemplate);
            } else {
                LOGGER.warn("No deployment template was queried for stack {}. Skipping persisting.", stack.getName());
            }
        } else {
            LOGGER.info("Skipping template persisting for stack {} as entitlement CDP_CB_CM_TEMPLATE_SYNC is not enabled", stack.getName());
        }
    }

    private String getDeployment(StackDtoDelegate stack) {
        try {
            LOGGER.info("Retrieving deployment template for stack {}", stack.getName());
            return apiConnectors.getConnector(stack)
                    .clusterStatusService()
                    .getDeployment(stack.getName());
        } catch (Exception e) {
            LOGGER.error("Failed to get deployment template for stack {}: {}", stack.getName(), e.getMessage(), e);
            return null;
        }
    }

}
