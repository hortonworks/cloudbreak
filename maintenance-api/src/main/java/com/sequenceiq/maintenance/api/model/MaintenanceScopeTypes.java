package com.sequenceiq.maintenance.api.model;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

/**
 * Derives the {@link MaintenanceScopeType} of a maintenance task resource from its CRN.
 *
 * <p>Lives in the API module so the maintenance service and its clients resolve scope identically:
 * a task registered under one scope must be looked up under the same scope, otherwise a
 * resource-scoped schedule is silently missed.
 */
public final class MaintenanceScopeTypes {

    private MaintenanceScopeTypes() {
    }

    /**
     * @throws BadRequestException if {@code resourceCrn} is blank, not a CRN, or an unsupported resource type
     */
    public static MaintenanceScopeType fromResourceCrn(String resourceCrn) {
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

    private static MaintenanceScopeType scopeTypeForClusterService(Crn.Service service) {
        if (service == Crn.Service.DATAHUB) {
            return MaintenanceScopeType.DATAHUB;
        }
        throw new BadRequestException("Unsupported resourceCrn service for cluster resources: " + service);
    }
}
