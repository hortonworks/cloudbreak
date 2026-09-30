package com.sequenceiq.maintenance.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;

class MaintenanceWindowScheduleScopeValidatorTest {

    private static final String ACCOUNT_ID = "acc-1";

    private static final String DATAHUB_CRN = "crn:cdp:datahub:us-west-1:acc-1:cluster:dh-1";

    private final MaintenanceWindowScheduleScopeValidator underTest = new MaintenanceWindowScheduleScopeValidator();

    @Test
    void acceptsResourceCrnInCallerAccount() {
        assertThatCode(() -> underTest.validate(MaintenanceScopeType.DATAHUB, DATAHUB_CRN, ACCOUNT_ID))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsTenantScopeWhenScopeIdIsAccountId() {
        assertThatCode(() -> underTest.validate(MaintenanceScopeType.TENANT, ACCOUNT_ID, ACCOUNT_ID))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsTenantScopeWithForeignAccountId() {
        assertThatThrownBy(() -> underTest.validate(MaintenanceScopeType.TENANT, "acc-2", ACCOUNT_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must equal accountId");
    }

    /**
     * Without this guard the value reaches the UMS client's CRN assertion, which throws IllegalArgumentException and —
     * with no mapper for it in this module — surfaces as a 500 rather than a 400.
     */
    @Test
    void rejectsNonCrnScopeIdForResourceScope() {
        assertThatThrownBy(() -> underTest.validate(MaintenanceScopeType.DATAHUB, "garbage", ACCOUNT_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must be a valid CRN");
    }

    @Test
    void rejectsBlankScopeId() {
        assertThatThrownBy(() -> underTest.validate(MaintenanceScopeType.DATAHUB, "   ", ACCOUNT_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    void rejectsCrnFromAnotherAccount() {
        assertThatThrownBy(() -> underTest.validate(
                MaintenanceScopeType.DATAHUB, "crn:cdp:datahub:us-west-1:acc-999:cluster:dh-1", ACCOUNT_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must belong to the same account");
    }

    @Test
    void rejectsNullScopeType() {
        assertThatThrownBy(() -> underTest.validate(null, DATAHUB_CRN, ACCOUNT_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("scopeType");
    }
}
