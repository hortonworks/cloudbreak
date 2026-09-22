package com.sequenceiq.maintenance.util;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeTypes;
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
        return MaintenanceScopeTypes.fromResourceCrn(resourceCrn);
    }
}
