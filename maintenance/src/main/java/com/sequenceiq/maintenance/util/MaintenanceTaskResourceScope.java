package com.sequenceiq.maintenance.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.service.model.MaintenanceWindowResourceIdentity;

/**
 * Validates maintenance task resource CRNs and derives their {@link MaintenanceScopeType}
 * for use when building {@link MaintenanceWindowResourceIdentity} for schedule eligibility checks.
 */
@Component
public class MaintenanceTaskResourceScope {

    /**
     * @throws BadRequestException if {@code resourceCrn} is blank, not a CRN, or an unsupported resource type for tasks
     */
    public void validateTaskResourceCrn(String resourceCrn) {
        scopeTypeFromResourceCrn(resourceCrn);
    }

    /**
     * @throws BadRequestException if {@code resourceCrn} is blank, not a CRN, or an unsupported resource type
     */
    public MaintenanceScopeType scopeTypeFromResourceCrn(String resourceCrn) {
        if (StringUtils.isBlank(resourceCrn) || !Crn.isCrn(resourceCrn)) {
            throw new BadRequestException("resourceCrn must be a valid CRN.");
        }
        Crn crn = Crn.fromString(resourceCrn);
        return switch (crn.getResourceType()) {
            case CLUSTER -> scopeTypeForClusterService(crn.getService());
            case DATALAKE, SDX_CLUSTER -> MaintenanceScopeType.DATALAKE;
            case FREEIPA -> MaintenanceScopeType.FREEIPA;
            default -> throw new BadRequestException(
                    "Unsupported resourceCrn type for maintenance window tasks: " + crn.getResourceType());
        };
    }

    private MaintenanceScopeType scopeTypeForClusterService(Crn.Service service) {
        if (service == Crn.Service.DATAHUB) {
            return MaintenanceScopeType.DATAHUB;
        }
        throw new BadRequestException("Unsupported resourceCrn service for cluster resources: " + service);
    }
}
