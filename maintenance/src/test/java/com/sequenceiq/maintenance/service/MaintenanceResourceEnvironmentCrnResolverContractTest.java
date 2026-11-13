package com.sequenceiq.maintenance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXInternalV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaInternalV1Endpoint;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeTypes;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;

/**
 * Keeps {@link MaintenanceScopeTypes} (maintenance-api) and {@link MaintenanceResourceEnvironmentCrnResolver} aligned:
 * every datalake-scoped resource CRN shape accepted for dispatch must resolve environment inheritance without client-side
 * {@code ValidCrn(VM_DATALAKE)} rejection on {@link SdxEndpoint#getByCrn(String)}.
 */
@ExtendWith(MockitoExtension.class)
class MaintenanceResourceEnvironmentCrnResolverContractTest {

    private static final String ACCOUNT_ID = "acc-1";

    /**
     * Must stay in sync with {@code MaintenanceScopeTypesTest#SDX_CLUSTER_CRN}.
     */
    private static final String SDX_CLUSTER_CRN = "crn:cdp:datalake:us-west-1:" + ACCOUNT_ID + ":sdxcluster:sdx-1";

    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:" + ACCOUNT_ID + ":datalake:dl-1";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:" + ACCOUNT_ID + ":environment:env-1";

    @Mock
    private DistroXInternalV1Endpoint distroXInternalV1Endpoint;

    @Mock
    private SdxEndpoint sdxEndpoint;

    @Mock
    private FreeIpaInternalV1Endpoint freeIpaInternalV1Endpoint;

    @Test
    void scopeTypesAndResolverAgreeOnLegacySdxClusterCrn() {
        assertThat(MaintenanceScopeTypes.fromResourceCrn(SDX_CLUSTER_CRN)).isEqualTo(MaintenanceScopeType.DATALAKE);

        SdxClusterDetailResponse detail = new SdxClusterDetailResponse();
        detail.setEnvironmentCrn(ENV_CRN);
        when(sdxEndpoint.getDetailByCrn(SDX_CLUSTER_CRN, Set.of())).thenReturn(detail);

        MaintenanceResourceEnvironmentCrnResolver resolver = new MaintenanceResourceEnvironmentCrnResolver(
                distroXInternalV1Endpoint, sdxEndpoint, freeIpaInternalV1Endpoint);

        assertThat(resolver.resolveOptional(ACCOUNT_ID, SDX_CLUSTER_CRN, MaintenanceScopeType.DATALAKE)).contains(ENV_CRN);
        verify(sdxEndpoint, never()).getByCrn(SDX_CLUSTER_CRN);
    }

    @Test
    void scopeTypesAndResolverAgreeOnVmDatalakeCrn() {
        assertThat(MaintenanceScopeTypes.fromResourceCrn(DATALAKE_CRN)).isEqualTo(MaintenanceScopeType.DATALAKE);

        com.sequenceiq.sdx.api.model.SdxClusterResponse cluster = new com.sequenceiq.sdx.api.model.SdxClusterResponse();
        cluster.setEnvironmentCrn(ENV_CRN);
        when(sdxEndpoint.getByCrn(DATALAKE_CRN)).thenReturn(cluster);

        MaintenanceResourceEnvironmentCrnResolver resolver = new MaintenanceResourceEnvironmentCrnResolver(
                distroXInternalV1Endpoint, sdxEndpoint, freeIpaInternalV1Endpoint);

        assertThat(resolver.resolveOptional(ACCOUNT_ID, DATALAKE_CRN, MaintenanceScopeType.DATALAKE)).contains(ENV_CRN);
        verify(sdxEndpoint, never()).getDetailByCrn(DATALAKE_CRN, Set.of());
    }
}
