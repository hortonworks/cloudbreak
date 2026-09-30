package com.sequenceiq.maintenance.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;

/**
 * Validates schedule {@code scopeType}/{@code scopeId} before UMS authorization or persistence.
 * <p>
 * Without this, a non-CRN {@code scopeId} reaches {@code GrpcUmsClient.makeCheckRightCallAndHandleExceptions}, whose
 * CRN-format {@code checkArgument} throws {@link IllegalArgumentException}. The maintenance module registers no mapper
 * for that type, so {@code DefaultExceptionMapper} turns it into a 500 instead of a 400.
 * <p>
 * Mirrors {@code MaintenanceTaskResourceScope} / {@code MaintenanceWindowTaskValidator#validateCrnBelongsToAccount},
 * which already enforce both rules on the task side.
 */
@Component
public class MaintenanceWindowScheduleScopeValidator {

    /**
     * @throws BadRequestException if the scope is not usable: blank {@code scopeId}, a TENANT scope whose
     *         {@code scopeId} is not the caller's account, or a resource scope whose {@code scopeId} is not a CRN
     *         belonging to the caller's account.
     */
    public void validate(MaintenanceScopeType scopeType, String scopeId, String accountId) {
        if (scopeType == null) {
            throw new BadRequestException("scopeType must not be blank.");
        }
        if (StringUtils.isBlank(scopeId)) {
            throw new BadRequestException("scopeId must not be blank.");
        }
        if (scopeType == MaintenanceScopeType.TENANT) {
            if (!scopeId.equals(accountId)) {
                throw new BadRequestException("scopeId must equal accountId for TENANT scope.");
            }
            return;
        }
        if (!Crn.isCrn(scopeId)) {
            throw new BadRequestException(String.format("scopeId must be a valid CRN for %s scope.", scopeType));
        }
        String scopeAccountId = Crn.safeFromString(scopeId).getAccountId();
        if (!scopeAccountId.equals(accountId)) {
            throw new BadRequestException("scopeId must belong to the same account as the caller.");
        }
    }
}
