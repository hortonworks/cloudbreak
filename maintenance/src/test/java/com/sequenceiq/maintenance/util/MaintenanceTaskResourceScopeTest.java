package com.sequenceiq.maintenance.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.domain.MaintenanceTaskStatus;
import com.sequenceiq.maintenance.domain.MaintenanceWindowTask;
import com.sequenceiq.maintenance.service.model.MaintenanceWindowResourceIdentity;

class MaintenanceTaskResourceScopeTest {

    private static final String ACCOUNT_ID = "acc-1";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:" + ACCOUNT_ID + ":environment:env-1";

    private static final String DATAHUB_CRN = "crn:cdp:datahub:us-west-1:" + ACCOUNT_ID + ":cluster:dh-1";

    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:" + ACCOUNT_ID + ":datalake:dl-1";

    private static final String FREEIPA_CRN = "crn:cdp:freeipa:us-west-1:" + ACCOUNT_ID + ":freeipa:ipa-1";

    private MaintenanceTaskResourceScope underTest;

    @BeforeEach
    void setUp() {
        underTest = new MaintenanceTaskResourceScope();
    }

    @Test
    void scopeTypeFromDatahubClusterCrn() {
        assertThat(underTest.scopeTypeFromResourceCrn(DATAHUB_CRN))
                .isEqualTo(MaintenanceScopeType.DATAHUB);
    }

    @Test
    void scopeTypeFromDatalakeCrn() {
        assertThat(underTest.scopeTypeFromResourceCrn(DATALAKE_CRN))
                .isEqualTo(MaintenanceScopeType.DATALAKE);
    }

    @Test
    void scopeTypeFromFreeipaCrn() {
        assertThat(underTest.scopeTypeFromResourceCrn(FREEIPA_CRN))
                .isEqualTo(MaintenanceScopeType.FREEIPA);
    }

    @Test
    void validateTaskResourceCrnRejectsUnsupportedResourceType() {
        assertThatThrownBy(() -> underTest.validateTaskResourceCrn(ENV_CRN))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unsupported resourceCrn type");
    }

    @Test
    void scopeTypeRejectsUnsupportedResourceType() {
        assertThatThrownBy(() -> underTest.scopeTypeFromResourceCrn(ENV_CRN))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unsupported resourceCrn type");
    }

    @Test
    void resourceIdentityFromTaskFieldsUsesStoredEnvironmentCrnAndDerivedScopeType() {
        MaintenanceWindowTask task = new MaintenanceWindowTask();
        task.setAccountId(ACCOUNT_ID);
        task.setEnvironmentCrn(ENV_CRN);
        task.setResourceCrn(DATAHUB_CRN);
        task.setStatus(MaintenanceTaskStatus.ACTIVE);

        MaintenanceWindowResourceIdentity identity = new MaintenanceWindowResourceIdentity(
                task.getAccountId(),
                task.getEnvironmentCrn(),
                task.getResourceCrn(),
                underTest.scopeTypeFromResourceCrn(task.getResourceCrn()));

        assertThat(identity.accountId()).isEqualTo(ACCOUNT_ID);
        assertThat(identity.environmentCrn()).isEqualTo(ENV_CRN);
        assertThat(identity.resourceCrn()).isEqualTo(DATAHUB_CRN);
        assertThat(identity.resourceScopeType()).isEqualTo(MaintenanceScopeType.DATAHUB);
    }
}
