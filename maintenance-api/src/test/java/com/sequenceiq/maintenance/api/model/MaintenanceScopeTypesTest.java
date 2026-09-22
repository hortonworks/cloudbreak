package com.sequenceiq.maintenance.api.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

class MaintenanceScopeTypesTest {

    private static final String ACCOUNT_ID = "acc-1";

    private static final String DATAHUB_CRN = "crn:cdp:datahub:us-west-1:" + ACCOUNT_ID + ":cluster:dh-1";

    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:" + ACCOUNT_ID + ":datalake:dl-1";

    private static final String SDX_CLUSTER_CRN = "crn:cdp:datalake:us-west-1:" + ACCOUNT_ID + ":sdxcluster:sdx-1";

    private static final String FREEIPA_CRN = "crn:cdp:freeipa:us-west-1:" + ACCOUNT_ID + ":freeipa:ipa-1";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:" + ACCOUNT_ID + ":environment:env-1";

    @Test
    void datahubClusterCrnMapsToDatahubScope() {
        assertThat(MaintenanceScopeTypes.fromResourceCrn(DATAHUB_CRN)).isEqualTo(MaintenanceScopeType.DATAHUB);
    }

    @Test
    void datalakeCrnMapsToDatalakeScope() {
        assertThat(MaintenanceScopeTypes.fromResourceCrn(DATALAKE_CRN)).isEqualTo(MaintenanceScopeType.DATALAKE);
    }

    @Test
    void sdxClusterCrnMapsToDatalakeScope() {
        assertThat(MaintenanceScopeTypes.fromResourceCrn(SDX_CLUSTER_CRN)).isEqualTo(MaintenanceScopeType.DATALAKE);
    }

    @Test
    void freeipaCrnMapsToFreeipaScope() {
        assertThat(MaintenanceScopeTypes.fromResourceCrn(FREEIPA_CRN)).isEqualTo(MaintenanceScopeType.FREEIPA);
    }

    @Test
    void environmentCrnIsNotADispatchableTaskScope() {
        assertThatThrownBy(() -> MaintenanceScopeTypes.fromResourceCrn(ENV_CRN))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unsupported resourceCrn type");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "not-a-crn", "crn:cdp:datahub"})
    void blankOrMalformedCrnIsRejected(String resourceCrn) {
        assertThatThrownBy(() -> MaintenanceScopeTypes.fromResourceCrn(resourceCrn))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must be a valid CRN");
    }
}
