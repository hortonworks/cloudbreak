package com.sequenceiq.freeipa.api.v1.maintenance.model;

import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.validation.AccountIdMatchesResourceCrn;
import com.sequenceiq.cloudbreak.validation.AccountIdMatchesResourceCrnGroup;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.maintenance.api.execution.model.MaintenanceTaskDispatchRequestBase;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * FreeIPA maintenance window dispatch body. Wire format is shared with other submitters; see
 * {@link MaintenanceTaskDispatchRequestBase} and {@code MaintenanceTaskDispatchRequestContractTest} in freeipa.
 */
@Schema
@GroupSequence({MaintenanceTaskDispatchRequest.class, AccountIdMatchesResourceCrnGroup.class})
@AccountIdMatchesResourceCrn(groups = AccountIdMatchesResourceCrnGroup.class)
public class MaintenanceTaskDispatchRequest extends MaintenanceTaskDispatchRequestBase {

    @NotEmpty
    @ResourceCrn
    @ValidCrn(resource = CrnResourceDescriptor.FREEIPA)
    @JsonProperty("resource_crn")
    private String resourceCrn;

    @Override
    public String getResourceCrn() {
        return resourceCrn;
    }

    @Override
    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }
}
